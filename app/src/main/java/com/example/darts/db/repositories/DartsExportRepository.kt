package com.example.darts.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
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
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

class DartsExportRepository @Inject constructor(
    @ApplicationContext private val context: Context, // Injected application context to access storage streams
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
     * Builds a self-contained ecosystem package including physical media binaries.
     */
    suspend fun getExportPayloadForPlayer(playerId: Int): PlayerExportPayload {
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
            val battleGames = gameDao.getAllGamesByBattle(battleId).firstOrNull()?:emptyList()
            games.addAll(battleGames)
        }
        val distinctGames = games.distinctBy { it.idGame }
        val gameIds = distinctGames.map { it.idGame }

        val moments = mutableListOf<Moment>()
        for (gameId in gameIds) {
            val gameMoments = momentDao.getMomentsForGame(gameId).firstOrNull()
            if (gameMoments != null) moments.addAll(gameMoments)
        }

        // CRITICAL FIX: Convert local files to binary attachments
        val attachments = mutableListOf<MomentAttachment>()
        moments.forEach { moment ->
            if (moment.type == MomentType.PHOTO || moment.type == MomentType.AUDIO) {
                val base64String = encodeFileToBase64(moment.contentValue)
                if (base64String != null) {
                    val fallbackName = "${moment.type.name.lowercase()}_${moment.idMoment}" +
                            if (moment.type == MomentType.PHOTO) ".jpg" else ".mp3"

                    attachments.add(
                        MomentAttachment(
                            idMoment = moment.idMoment,
                            momentType = moment.type.name,
                            fileName = fallbackName,
                            base64Data = base64String
                        )
                    )
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
            attachments = attachments // Staged file system layers
        )
    }

    /**
     * Commits incoming data graph and builds local files out of incoming byte strings.
     */
    suspend fun importPayload(payload: PlayerExportPayload) {
        payload.players.forEach { playerDao.addPlayer(it) }
        payload.battles.forEach { battleDao.insertBattle(it) }
        payload.participations.forEach { participateDao.addParticipation(it) }
        payload.games.forEach { gameDao.addGame(it) }

        // Map containing key conversions if needed, but since we map directly:
        payload.moments.forEach { moment ->
            // Look up if this moment contains a packaged binary asset file
            val fileAsset = payload.attachments.find { it.idMoment == moment.idMoment }

            val updatedContentValue = if (fileAsset != null) {
                // Rebuild the physical file on the destination phone's internal memory
                val freshLocalFile = saveBase64ToFile(fileAsset.fileName, fileAsset.base64Data)
                freshLocalFile?.absolutePath ?: moment.contentValue
            } else {
                moment.contentValue
            }

            // Insert updated entity pointing to the newly generated file path safely
            momentDao.insertMoment(moment.copy(contentValue = updatedContentValue))
        }

        statDao.insertLegStats(payload.legStats)
        payload.careerStats.forEach { statDao.insertOrReplaceCareerStats(it) }
    }

    // -------------------------------------------------------
    // FILE ENCODING/DECODING HELPERS
    // -------------------------------------------------------

    private fun encodeFileToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null // Skips files that were deleted or inaccessible
        }
    }

    private fun saveBase64ToFile(fileName: String, base64Data: String): File? {
        return try {
            // Stores it safely in the app's isolated files directory: /data/user/0/com.example.darts/files
            val targetFile = File(context.filesDir, fileName)
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
    // TRANSIT ZIP WRAPPERS
    // -------------------------------------------------------
    suspend fun exportPlayerToJson(playerId: Int): String {
        return gson.toJson(getExportPayloadForPlayer(playerId))
    }

    suspend fun importFromJson(json: String) {
        val payload = gson.fromJson(json, PlayerExportPayload::class.java)
        importPayload(payload)
    }

    suspend fun exportPlayerToQrString(playerId: Int): String {
        return compressToBase64(exportPlayerToJson(playerId))
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