package com.example.darts.repository

import android.util.Base64
import com.example.darts.db.dao.StatDao
import com.example.darts.db.daos.BattleDAO
import com.example.darts.db.daos.GameDAO
import com.example.darts.db.daos.MomentDAO
import com.example.darts.db.daos.ParticipateDAO
import com.example.darts.db.daos.PlayerDAO
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.Player
import com.example.darts.db.entities.Game
import com.example.darts.db.entities.Participate
import com.example.darts.dto.PlayerExportPayload
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import kotlin.collections.emptyList

class DartsExportRepository @Inject constructor(
    private val playerDao: PlayerDAO,
    private val battleDao: BattleDAO,
    private val gameDao: GameDAO,
    private val momentDao: MomentDAO,
    private val participateDao: ParticipateDAO,
    private val statDao: StatDao
) {

    private val gson = Gson()

    fun getAllPlayers(): Flow<List<Player>> =
        playerDao.getAllPlayers()

    /**
     * Builds a self-contained relational ecosystem package around a single player's historical battles.
     */
    suspend fun getExportPayloadForPlayer(playerId: Int): PlayerExportPayload {
        // 1. Validate exporting player profile
        val primaryPlayer = playerDao.getPlayerById(playerId)
            ?: throw IllegalArgumentException("Player with ID $playerId not found.")

        // 2. Fetch all raw participations for this specific target player
        val primaryParticipations = participateDao.getParticipationsByPlayerIdDirect(playerId)

        // 3. Isolate the unique Battle keys this user was involved in
        val battleIds = primaryParticipations.map { it.idBattle }.distinct()

        val battles = if (battleIds.isNotEmpty()) {
            battleDao.getBattlesByIds(battleIds)
        } else {
            emptyList()
        }

        // 4. CRITICAL FIX: Find ALL participations for these battles (grabs opponent links too)
        val allParticipations = mutableListOf<Participate>()
        for (battleId in battleIds) {
            val battleParts = participateDao.getParticipationsByBattleIdDirect(battleId)
            allParticipations.addAll(battleParts)
        }
        val distinctParticipations = allParticipations.distinctBy { Pair(it.idPlayer, it.idBattle) }

        // 5. CRITICAL FIX: Gather every distinct Player profile participating in these matches
        val involvedPlayerIds = distinctParticipations.map { it.idPlayer }.distinct()
        val players = involvedPlayerIds.mapNotNull { id ->
            playerDao.getPlayerById(id)
        }

        // 6. Gather all games configured under these specific structural battles
        val games = mutableListOf<Game>()
        for (battleId in battleIds) {
            val battleGames = gameDao.getAllGamesByBattle(battleId).firstOrNull() ?: emptyList()// Ensure this exists in GameDAO
            games.addAll(battleGames)
        }
        val distinctGames = games.distinctBy { it.idGame }
        val gameIds = distinctGames.map { it.idGame }

        // 7. Extract structural Child leaves (Moments and Leg Stats) tied to found matches
        val moments = mutableListOf<Moment>()
        for (gameId in gameIds) {
            val gameMoments = momentDao.getMomentsForGame(gameId).firstOrNull()
            if (gameMoments != null) {
                moments.addAll(gameMoments)
            }
        }

        // Fetch leg metrics for EVERY involved player in those games (Self + Opponents)
        val legStats = if (gameIds.isNotEmpty()) {
            statDao.getLegStatsForGames(gameIds)
        } else {
            emptyList()
        }

        // 8. Capture overall lifetime profiles for all involved accounts
        val careerStats = involvedPlayerIds.mapNotNull { id ->
            statDao.getCareerStats(id)
        }

        return PlayerExportPayload(
            players = players,                 // Relational requirement: Parent 1
            careerStats = careerStats,
            battles = battles,                 // Relational requirement: Parent 2
            participations = distinctParticipations, // Link table row mapping
            games = distinctGames,             // Relational requirement: Child of Battle, Parent of Stats/Moments
            moments = moments,                 // Leaf
            legStats = legStats                // Leaf
        )
    }

    /**
     * Commits the incoming historical data graph using explicit structural layering order.
     */
    suspend fun importPayload(payload: PlayerExportPayload) {
        // LAYER 1: Core Independent Roots (Must exist first)
        payload.players.forEach { player ->
            playerDao.addPlayer(player)
        }

        payload.battles.forEach { battle ->
            battleDao.insertBattle(battle)
        }

        // LAYER 2: Junction dependencies (Safe because Parents exist)
        payload.participations.forEach { participation ->
            participateDao.addParticipation(participation)
        }

        // LAYER 3: Core Match Records (Safe because Battle parent exists)
        payload.games.forEach { game ->
            gameDao.addGame(game)
        }

        // LAYER 4: Final metric data leaves (Safe because Game and Player parents exist)
        payload.moments.forEach { moment ->
            momentDao.insertMoment(moment)
        }

        statDao.insertLegStats(payload.legStats)

        payload.careerStats.forEach { careerStat ->
            statDao.insertOrReplaceCareerStats(careerStat)
        }
    }

    // --- (Keep GZIP/Base64 handling tools exactly as optimized before) ---

    suspend fun exportPlayerToJson(playerId: Int): String {
        val payload = getExportPayloadForPlayer(playerId)
        return gson.toJson(payload)
    }

    suspend fun importFromJson(json: String) {
        val payload = gson.fromJson(json, PlayerExportPayload::class.java)
        importPayload(payload)
    }

    suspend fun exportPlayerToQrString(playerId: Int): String {
        val json = exportPlayerToJson(playerId)
        return compressToBase64(json)
    }

    suspend fun importFromQrString(encodedPayload: String) {
        val json = decompressFromBase64(encodedPayload)
        importFromJson(json)
    }

    private fun compressToBase64(text: String): String {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use {
            it.write(text.toByteArray(Charsets.UTF_8))
        }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun decompressFromBase64(encoded: String): String {
        val sanitizedEncoded = encoded.trim().replace("\\s".toRegex(), "")
        val compressedBytes = Base64.decode(sanitizedEncoded, Base64.NO_WRAP)
        return GZIPInputStream(compressedBytes.inputStream())
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }
}