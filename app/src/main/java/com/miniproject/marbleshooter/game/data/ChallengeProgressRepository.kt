package com.miniproject.marbleshooter.game.data

import android.content.Context
import com.miniproject.marbleshooter.game.domain.ChallengeCatalog

/** Best local results for the offline challenge set. */
class ChallengeProgressRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val bestStarsByChallenge: Map<Int, Int>
        get() = ChallengeCatalog.all.associate { challenge ->
            challenge.id to readInt(starsKey(challenge.id)).coerceIn(0, 3)
        }

    fun bestStars(challengeId: Int): Int {
        if (ChallengeCatalog.find(challengeId) == null) return 0
        return readInt(starsKey(challengeId)).coerceIn(0, 3)
    }

    fun bestShots(challengeId: Int): Int? {
        if (ChallengeCatalog.find(challengeId) == null) return null
        return readInt(shotsKey(challengeId)).takeIf { it > 0 }
    }

    fun recordCompletion(challengeId: Int, stars: Int, shots: Int, remainingSeconds: Float): Boolean {
        if (ChallengeCatalog.find(challengeId) == null) return false
        if (stars !in 1..3 || shots <= 0 || !remainingSeconds.isFinite() || remainingSeconds < 0f) return false

        val previousStars = bestStars(challengeId)
        val previousShots = bestShots(challengeId)
        val improved = stars > previousStars || (stars == previousStars && (previousShots == null || shots < previousShots))
        val editor = preferences.edit()
            .putInt(starsKey(challengeId), maxOf(previousStars, stars))
        if (previousShots == null || shots < previousShots) {
            editor.putInt(shotsKey(challengeId), shots)
        }
        val previousRemaining = readInt(remainingKey(challengeId)).coerceAtLeast(0)
        val remainingMillis = (remainingSeconds * 1000f).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (remainingMillis > previousRemaining) editor.putInt(remainingKey(challengeId), remainingMillis)
        editor.apply()
        return improved
    }

    fun bestRemainingSeconds(challengeId: Int): Float? {
        if (ChallengeCatalog.find(challengeId) == null) return null
        val millis = readInt(remainingKey(challengeId))
        return millis.takeIf { it > 0 }?.div(1000f)
    }

    fun reset() = preferences.edit().clear().apply()

    private fun readInt(key: String): Int {
        if (!preferences.contains(key)) return 0
        return try {
            preferences.getInt(key, 0)
        } catch (_: ClassCastException) {
            0
        }
    }

    private fun starsKey(id: Int) = "challenge_${id}_stars"
    private fun shotsKey(id: Int) = "challenge_${id}_shots"
    private fun remainingKey(id: Int) = "challenge_${id}_remaining_ms"

    private companion object {
        const val PREFERENCES_NAME = "offline_challenge_progress"
    }
}
