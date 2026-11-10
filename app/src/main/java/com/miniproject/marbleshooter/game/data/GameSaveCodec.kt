package com.miniproject.marbleshooter.game.data

import com.miniproject.marbleshooter.game.domain.model.Ball

/** Pure codec for the serialized chain payload stored in SharedPreferences. */
internal object GameSaveCodec {
    const val CURRENT_SCHEMA_VERSION = 1
    const val LEGACY_SCHEMA_VERSION = 0

    fun encodeBalls(balls: List<Ball>): String =
        balls.joinToString(";") { "${it.colorIndex}:${it.progress}" }

    /**
     * Parses the complete chain or rejects the payload. Dropping one malformed token
     * would silently restore a different game state, so decoding is all-or-nothing.
     */
    fun decodeBalls(encoded: String): List<Ball>? {
        if (encoded.isBlank()) return null
        val tokens = encoded.split(';')
        if (tokens.isEmpty() || tokens.any(String::isBlank)) return null

        val balls = ArrayList<Ball>(tokens.size)
        for (token in tokens) {
            val parts = token.split(':')
            if (parts.size != 2) return null
            val color = parts[0].toIntOrNull() ?: return null
            val progress = parts[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return null
            balls += Ball(color, progress)
        }
        return balls
    }
}
