package com.example.darts.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.darts.db.DartsDatabase
import com.example.darts.db.dao.StatDao
import com.example.darts.db.daos.BattleDAO
import com.example.darts.db.daos.GameDAO
import com.example.darts.db.daos.MomentDAO
import com.example.darts.db.daos.ParticipateDAO
import com.example.darts.db.daos.PlayerDAO
import com.example.darts.db.entities.*
import com.example.darts.dto.PlayerExportPayload
import com.example.darts.dto.MomentAttachment
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

class DartsExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DartsDatabase,
    private val playerDao: PlayerDAO,
    private val battleDao: BattleDAO,
    private val gameDao: GameDAO,
    private val momentDao: MomentDAO,
    private val participateDao: ParticipateDAO,
    private val statDao: StatDao
) {

    private val gson = Gson()

    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers()

    /**
     * Builds a self-contained ecosystem package.
     * Added [excludeAttachments] to keep data small enough for QR codes when needed.
     */
    suspend fun getExportPayloadForPlayer(
        playerId: Int,
        excludeAttachments: Boolean = false // Added flag here
    ): PlayerExportPayload {
        val primaryPlayer = playerDao.getPlayerById(playerId)
            ?: throw IllegalArgumentException("Player with ID $playerId not found.")

        val primaryParticipations = participateDao.getParticipationsByPlayerIdDirect(playerId)
        val battleIds = primaryParticipations.map { it.idBattle }.distinct()

        val battles = if (battleIds.isNotEmpty()) battleDao.getBattlesByIds(battleIds) else emptyList()

        val allParticipations = mutableListOf<Participate>()
        for (battleId in battleIds) {
            val battleParts = participateDao.getParticipationsByBattleIdDirect(battleId)
            allParticipations.addAll(battleParts)
        }
        val distinctParticipations = allParticipations.distinctBy { Pair(it.idPlayer, it.idBattle) }

        val involvedPlayerIds = distinctParticipations.map { it.idPlayer }.distinct()
        val players = involvedPlayerIds.mapNotNull { playerDao.getPlayerById(it) }

        val games = mutableListOf<Game>()
        for (battleId in battleIds) {
            val battleGames = gameDao.getAllGamesByBattle(battleId).firstOrNull() ?: emptyList()
            games.addAll(battleGames)
        }
        val distinctGames = games.distinctBy { it.idGame }
        val gameIds = distinctGames.map { it.idGame }

        val moments = mutableListOf<Moment>()
        for (gameId in gameIds) {
            val gameMoments = momentDao.getMomentsForGame(gameId).firstOrNull()
            if (gameMoments != null) moments.addAll(gameMoments)
        }

        val attachments = mutableListOf<MomentAttachment>()

        // Only compile binary payloads if we aren't prepping a tight QR stream
        if (!excludeAttachments) {
            // A) Pack Player Avatars safely
            players.forEach { player ->
                if (!player.avatar.isNullOrBlank()) {
                    val base64Avatar = encodeFileToBase64(player.avatar)
                    if (base64Avatar != null) {
                        attachments.add(
                            MomentAttachment(
                                idMoment = player.idPlayer,
                                momentType = "AVATAR",
                                fileName = "avatar_player_${player.idPlayer}.jpg",
                                base64Data = base64Avatar
                            )
                        )
                    }
                }
            }

            // B) Pack Moment Photos and Audio Clips
            moments.forEach { moment ->
                if (moment.type == MomentType.PHOTO || moment.type == MomentType.AUDIO) {
                    val base64String = encodeFileToBase64(moment.contentValue)
                    if (base64String != null) {
                        val extension = if (moment.type == MomentType.PHOTO) ".jpg" else ".mp3"
                        attachments.add(
                            MomentAttachment(
                                idMoment = moment.idMoment,
                                momentType = moment.type.name,
                                fileName = "${moment.type.name.lowercase()}_moment_${moment.idMoment}$extension",
                                base64Data = base64String
                            )
                        )
                    }
                }
            }
        }

        val legStats = if (gameIds.isNotEmpty()) statDao.getLegStatsForGames(gameIds) else emptyList()
        val careerStats = involvedPlayerIds.mapNotNull { statDao.getCareerStats(it) }

        return PlayerExportPayload(
            players = players,
            careerStats = careerStats,
            battles = battles,
            participations = distinctParticipations,
            games = distinctGames,
            moments = moments,
            legStats = legStats,
            attachments = attachments
        )
    }

    /**
     * Commits the incoming data graph.
     *
     * The payload carries the *sending* device's primary keys, which almost always
     * collide with local ones (both devices number from 1). Reusing them made
     * `insertBattle` fail with a UNIQUE constraint violation, and would have bound
     * imported games to unrelated local battles had it not. So every row is inserted
     * with id = 0 to get a fresh local id, and child references are translated through
     * old → new maps built as we go.
     *
     * The whole graph goes in one transaction: a failure half-way used to leave
     * orphaned players behind.
     */
    suspend fun importPayload(payload: PlayerExportPayload) {
        // Decode media to disk *before* opening the transaction — base64 decoding of
        // photos would otherwise hold the write lock for its whole duration.
        val avatarPaths = mutableMapOf<Int, String>()   // original playerId -> local file
        val momentPaths = mutableMapOf<Int, String>()   // original momentId -> local file

        payload.attachments.forEach { asset ->
            val file = saveBase64ToFile(asset.fileName, asset.base64Data) ?: return@forEach
            if (asset.momentType == "AVATAR") {
                avatarPaths[asset.idMoment] = file.absolutePath
            } else {
                momentPaths[asset.idMoment] = file.absolutePath
            }
        }

        database.withTransaction {
            val playerIdMap = mutableMapOf<Int, Int>()
            val battleIdMap = mutableMapOf<Int, Int>()
            val gameIdMap = mutableMapOf<Int, Int>()

            // Original ids of players this import actually created, as opposed to
            // matched against someone already on this device.
            val createdPlayers = mutableSetOf<Int>()

            // LAYER 1: Players — matched by username so importing the same friend
            // twice does not create a second profile.
            payload.players.forEach { player ->
                val existing = playerDao.getPlayerByUsername(player.username)
                if (existing != null) {
                    playerIdMap[player.idPlayer] = existing.idPlayer
                } else {
                    val avatar = avatarPaths[player.idPlayer] ?: player.avatar
                    playerIdMap[player.idPlayer] =
                        playerDao.addPlayer(player.copy(idPlayer = 0, avatar = avatar)).toInt()
                    createdPlayers.add(player.idPlayer)
                }
            }

            // LAYER 2: Battles
            payload.battles.forEach { battle ->
                battleIdMap[battle.idBattle] =
                    battleDao.insertBattle(battle.copy(idBattle = 0)).toInt()
            }

            // LAYER 3: Junction rows
            payload.participations.forEach { participation ->
                val playerId = playerIdMap[participation.idPlayer] ?: return@forEach
                val battleId = battleIdMap[participation.idBattle] ?: return@forEach
                participateDao.addParticipation(
                    participation.copy(idPlayer = playerId, idBattle = battleId)
                )
            }

            // LAYER 4: Games. `history` holds turn summaries keyed by player *name*,
            // so it needs no remapping.
            payload.games.forEach { game ->
                val battleId = battleIdMap[game.idBattle] ?: return@forEach
                gameIdMap[game.idGame] =
                    gameDao.addGame(game.copy(idGame = 0, idBattle = battleId)).toInt()
            }

            // LAYER 5: Moments, repointed at the freshly written local media files
            payload.moments.forEach { moment ->
                val gameId = gameIdMap[moment.idGame] ?: return@forEach
                momentDao.insertMoment(
                    moment.copy(
                        idMoment = 0,
                        idGame = gameId,
                        contentValue = momentPaths[moment.idMoment] ?: moment.contentValue
                    )
                )
            }

            // LAYER 6: Statistics
            val legStats = payload.legStats.mapNotNull { stat ->
                val gameId = gameIdMap[stat.gameId] ?: return@mapNotNull null
                val playerId = playerIdMap[stat.playerId] ?: return@mapNotNull null
                stat.copy(id = 0, gameId = gameId, playerId = playerId)
            }
            statDao.insertLegStats(legStats)

            // Career totals are taken only for players this import created. For a
            // player who already existed here, local totals win: both devices recorded
            // the same matches, so overwriting or adding would corrupt the numbers.
            payload.careerStats.forEach { careerStat ->
                if (careerStat.playerId !in createdPlayers) return@forEach
                val playerId = playerIdMap[careerStat.playerId] ?: return@forEach
                statDao.insertOrReplaceCareerStats(careerStat.copy(playerId = playerId))
            }
        }
    }

    // -------------------------------------------------------
    // FILE ENCODING/DECODING HELPERS
    // -------------------------------------------------------
    private fun encodeFileToBase64(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://") && !uriString.contains("/")) {
            return null
        }
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } else {
                val cleanPath = if (uriString.startsWith("file://")) uri.path else uriString
                val file = File(cleanPath ?: uriString)
                if (file.exists()) {
                    val bytes = file.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            println("DEBUG_MEDIA_ERROR: Failed to encode media path: $uriString. Error: ${e.localizedMessage}")
            null
        }
    }

    private fun saveBase64ToFile(fileName: String, base64Data: String): File? {
        return try {
            // Attachment names are derived from the sending device's row ids
            // ("avatar_player_3.jpg"), so importing two different people's data would
            // have them overwrite each other's media. Namespace every imported file.
            val unique = UUID.randomUUID().toString().take(8)
            val targetFile = File(context.filesDir, "${unique}_$fileName")
            val fileBytes = Base64.decode(base64Data, Base64.NO_WRAP)
            FileOutputStream(targetFile).use { fos ->
                fos.write(fileBytes)
                fos.flush()
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // -------------------------------------------------------
    // TRANSIT DUAL-CHANNEL ROUTING
    // -------------------------------------------------------

    // Default flag is false: File sharing remains 100% complete with full media assets
    suspend fun exportPlayerToJson(playerId: Int, excludeAttachments: Boolean = false): String {
        return gson.toJson(getExportPayloadForPlayer(playerId, excludeAttachments))
    }
    suspend fun exportPlayerToCompressedFileString(playerId: Int): String {
        val json = exportPlayerToJson(playerId, excludeAttachments = false)
        return compressToBase64(json)
    }

    suspend fun importFromJson(json: String) {
        val payload = gson.fromJson(json, PlayerExportPayload::class.java)
        importPayload(payload)
    }

    // CRITICAL QR FIX: Forces excludeAttachments to true to drop high-density media blobs
    suspend fun exportPlayerToQrString(playerId: Int): String {
        val json = exportPlayerToJson(playerId, excludeAttachments = true)
        return compressToBase64(json)
    }

    suspend fun importFromQrString(encodedPayload: String) {
        importFromJson(decompressFromBase64(encodedPayload))
    }

    private fun compressToBase64(text: String): String {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun decompressFromBase64(encoded: String): String {
        val sanitizedEncoded = encoded.trim().replace("\\s".toRegex(), "")
        val compressedBytes = Base64.decode(sanitizedEncoded, Base64.NO_WRAP)
        return GZIPInputStream(compressedBytes.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}