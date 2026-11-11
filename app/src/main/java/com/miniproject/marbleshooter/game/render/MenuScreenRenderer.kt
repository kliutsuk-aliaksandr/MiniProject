package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import com.miniproject.marbleshooter.R
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.LevelCatalog
import com.miniproject.marbleshooter.game.domain.ChallengeCatalog
import com.miniproject.marbleshooter.game.domain.model.Vec2
import com.miniproject.marbleshooter.game.domain.model.OfflineAchievement

/** Renders the menu, campaign map, tutorial, settings and privacy screens. */
internal class MenuScreenRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawMenu(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        drawPanel(canvas, RectF(54f, 218f, 366f, 363f), 36f, Color.argb(115, 34, 54, 83))
        centeredText(canvas, tr(state, R.string.game_title), 41f, Color.WHITE, 210f, 145f)
        centeredText(
            canvas,
            tr(state, R.string.game_tagline),
            14f, Color.rgb(174, 193, 224), 210f, 180f
        )
        drawFrog(canvas, Vec2(210f, 305f), Vec2(210f, 155f), engine.ammoColor)
        drawPanel(canvas, RectF(96f, 351f, 324f, 396f), 17f, Color.argb(155, 12, 22, 42))
        centeredText(
            canvas,
            tr(state, R.string.best_score_menu, state.bestScore),
            15f, Color.rgb(209, 226, 247), 210f, 378f
        )

        if (state.savedGameAvailable) {
            button(canvas, 78f, 410f, 342f, 464f, tr(state, R.string.continue_game), GREEN)
            button(canvas, 78f, 474f, 342f, 528f, tr(state, R.string.new_game), BLUE)
            button(canvas, 78f, 538f, 342f, 592f, tr(state, R.string.level_map), BLUE)
            button(canvas, 78f, 602f, 342f, 656f, tr(state, R.string.tutorial), BLUE)
            button(canvas, 78f, 666f, 342f, 720f, tr(state, R.string.settings), BLUE)
            button(canvas, 78f, 730f, 342f, 784f, tr(state, R.string.achievements), PURPLE)
        } else {
            button(canvas, 78f, 438f, 342f, 496f, tr(state, R.string.play), GREEN)
            button(canvas, 78f, 506f, 342f, 564f, tr(state, R.string.level_map), BLUE)
            button(canvas, 78f, 574f, 342f, 632f, tr(state, R.string.tutorial), BLUE)
            button(canvas, 78f, 642f, 342f, 700f, tr(state, R.string.settings), BLUE)
            button(canvas, 78f, 710f, 342f, 768f, tr(state, R.string.achievements), PURPLE)
        }
        centeredText(canvas, tr(state, R.string.offline_version), 12f, Color.rgb(146, 165, 195), 210f, 835f)
    }

    internal fun drawLevelMap(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, R.string.level_map), 29f, Color.WHITE, 210f, 160f)
        centeredText(
            canvas,
            tr(state, R.string.levels_unlocked, state.unlockedLevel, LevelCatalog.MAX_LEVELS),
            14f, MUTED, 210f, 192f
        )

        val cellWidth = 68f
        val cellHeight = 58f
        val gapX = 8f
        val gapY = 8f
        val left = 22f
        val top = 220f

        for (level in 1..LevelCatalog.MAX_LEVELS) {
            val index = level - 1
            val column = index % 5
            val row = index / 5
            val x = left + column * (cellWidth + gapX)
            val y = top + row * (cellHeight + gapY)
            val unlocked = level <= state.unlockedLevel
            fillPaint.color = when {
                !unlocked -> Color.rgb(40, 49, 68)
                level == state.unlockedLevel -> Color.rgb(66, 194, 130)
                else -> Color.rgb(52, 81, 119)
            }
            canvas.drawRoundRect(RectF(x, y, x + cellWidth, y + cellHeight), 12f, 12f, fillPaint)
            centeredText(
                canvas,
                if (unlocked) level.toString() else "•",
                19f, if (unlocked) Color.WHITE else Color.rgb(123, 137, 158),
                x + cellWidth / 2f, y + 25f
            )
            val stars = state.starsByLevel[level] ?: 0
            centeredText(
                canvas,
                starsLine(stars),
                11f, Color.rgb(255, 209, 88),
                x + cellWidth / 2f, y + 46f
            )
        }

        button(canvas, 78f, 620f, 342f, 670f, tr(state, R.string.challenges), PURPLE)
        drawTrackLegend(canvas, state)
        button(canvas, 78f, 750f, 342f, 810f, tr(state, R.string.main_menu), GREEN)
    }

    internal fun drawChallenges(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, R.string.challenges_title), 27f, Color.WHITE, 210f, 130f)
        centeredText(canvas, tr(state, R.string.challenge_start_hint), 12f, MUTED, 210f, 162f)

        ChallengeCatalog.all.forEachIndexed { index, challenge ->
            val top = 205f + index * 145f
            val bottom = top + 130f
            val bestStars = state.challengeStarsById[challenge.id] ?: 0
            drawPanel(canvas, RectF(24f, top, 396f, bottom), 18f, Color.argb(180, 15, 26, 47))
            centeredText(
                canvas,
                tr(state, challengeTitleResource(challenge.id)),
                16f, Color.WHITE, 210f, top + 27f
            )
            drawParagraph(
                canvas,
                tr(state, challengeDescriptionResource(challenge.id)),
                11f, MUTED, RectF(42f, top + 39f, 378f, top + 77f)
            )
            val limit = if (challenge.timeLimitSeconds == null) {
                tr(state, R.string.challenge_shot_limit, challenge.maxShots)
            } else {
                tr(
                    state,
                    R.string.challenge_time_shot_limit,
                    challenge.timeLimitSeconds.toInt(),
                    challenge.maxShots
                )
            }
            centeredText(canvas, limit, 11f, Color.rgb(111, 228, 164), 210f, top + 94f)
            centeredText(
                canvas,
                if (bestStars == 0) tr(state, R.string.challenge_no_record)
                else tr(state, R.string.challenge_best_stars, starsLine(bestStars)),
                11f, Color.rgb(255, 209, 88), 210f, top + 116f
            )
        }
        button(canvas, 78f, 750f, 342f, 810f, tr(state, R.string.main_menu), GREEN)
    }

    private fun challengeTitleResource(id: Int): Int = when (id) {
        1 -> R.string.challenge_precision_title
        2 -> R.string.challenge_speed_title
        else -> R.string.challenge_expert_title
    }

    private fun challengeDescriptionResource(id: Int): Int = when (id) {
        1 -> R.string.challenge_precision_body
        2 -> R.string.challenge_speed_body
        else -> R.string.challenge_expert_body
    }

    private fun drawTrackLegend(canvas: Canvas, state: GameRenderState) {
        centeredText(
            canvas,
            tr(state, R.string.track_unlock_hint),
            11f, MUTED, 210f, 695f
        )
        textPaint.textSize = 11f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(178, 196, 221)
        canvas.drawText(
            tr(state, R.string.track_legend),
            210f, 720f, textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT
    }

    internal fun drawTutorial(canvas: Canvas, state: GameRenderState) {
        val page = state.tutorialPage.coerceIn(0, 4)
        val titleResources = listOf(R.string.tutorial_aim_title, R.string.tutorial_choose_title, R.string.tutorial_match_title, R.string.tutorial_boosters_title, R.string.tutorial_track_title)
        val bodyResources = listOf(R.string.tutorial_aim_body, R.string.tutorial_choose_body, R.string.tutorial_match_body, R.string.tutorial_boosters_body, R.string.tutorial_track_body)

        centeredText(canvas, tr(state, R.string.tutorial), 29f, Color.WHITE, 210f, 145f)
        centeredText(canvas, tr(state, R.string.tutorial_page, page + 1), 13f, MUTED, 210f, 177f)
        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(26f, 210f, 394f, 625f), 24f, 24f, fillPaint)
        drawTutorialIllustration(canvas, page, state)
        centeredText(
            canvas,
            tr(state, titleResources[page]),
            22f, Color.WHITE, 210f, 405f
        )
        drawParagraph(
            canvas,
            tr(state, bodyResources[page]),
            15f, Color.rgb(205, 218, 240), RectF(55f, 430f, 365f, 570f)
        )

        if (page == 4) {
            button(canvas, 60f, 640f, 360f, 695f, tr(state, R.string.tutorial_try), GREEN)
        } else {
            button(canvas, 80f, 640f, 340f, 695f, tr(state, R.string.next), GREEN)
        }
        button(canvas, 80f, 712f, 340f, 760f, tr(state, R.string.back), BLUE)
        button(canvas, 80f, 775f, 340f, 825f, tr(state, R.string.main_menu), BLUE)
    }

    private fun drawTutorialIllustration(canvas: Canvas, page: Int, state: GameRenderState) {
        when (page) {
            0 -> {
                drawFrog(canvas, Vec2(210f, 330f), Vec2(120f, 235f), 2)
                drawArrow(canvas, 240f, 255f, 320f, 220f, Color.rgb(111, 228, 164))
            }
            1 -> {
                drawMarble(canvas, 165f, 290f, 0, 19f)
                drawMarble(canvas, 255f, 290f, 3, 19f)
                centeredText(canvas, tr(state, R.string.next), 14f, MUTED, 210f, 345f)
            }
            2 -> {
                drawMarble(canvas, 145f, 285f, 1, 19f)
                drawMarble(canvas, 190f, 285f, 1, 19f)
                drawMarble(canvas, 235f, 285f, 1, 19f)
                drawArrow(canvas, 280f, 285f, 330f, 285f, Color.rgb(111, 228, 164))
            }
            3 -> {
                val colors = listOf(Color.rgb(84, 164, 255), Color.rgb(255, 91, 105),
                    Color.rgb(255, 194, 67), Color.rgb(184, 125, 255))
                colors.forEachIndexed { index, color ->
                    fillPaint.color = color
                    canvas.drawCircle(75f + index * 90f, 290f, 27f, fillPaint)
                }
                centeredText(canvas, "×", 24f, Color.WHITE, 75f, 298f)
                centeredText(canvas, "✦", 24f, Color.WHITE, 165f, 298f)
                centeredText(canvas, "↻", 24f, Color.WHITE, 255f, 298f)
                centeredText(canvas, "◀", 24f, Color.WHITE, 345f, 298f)
            }
            else -> {
                fillPaint.color = Color.rgb(73, 91, 121)
                canvas.drawRoundRect(RectF(75f, 250f, 345f, 335f), 28f, 28f, fillPaint)
                fillPaint.color = Color.rgb(140, 112, 75)
                canvas.drawCircle(135f, 292f, 20f, fillPaint)
                fillPaint.color = Color.rgb(113, 190, 232)
                canvas.drawCircle(210f, 292f, 20f, fillPaint)
                fillPaint.color = Color.rgb(118, 117, 125)
                canvas.drawCircle(285f, 292f, 20f, fillPaint)
            }
        }
    }


    internal fun drawAchievements(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, R.string.achievements_title), 29f, Color.WHITE, 210f, 105f)

        drawPanel(canvas, RectF(20f, 130f, 400f, 320f), 22f, Color.argb(170, 15, 26, 47))
        centeredText(canvas, tr(state, R.string.statistics_title), 16f, Color.rgb(111, 228, 164), 210f, 160f)
        centeredText(
            canvas,
            tr(state, R.string.achievements_unlocked_count, state.unlockedAchievements.size, OfflineAchievement.values().size),
            11f, MUTED, 210f, 181f
        )

        val stats = state.offlineStatistics
        val metrics = listOf(
            R.string.stat_shots to stats.totalShots,
            R.string.stat_matches to stats.matchesCleared,
            R.string.stat_marbles to stats.marblesCleared,
            R.string.stat_largest_combo to stats.largestMatch,
            R.string.stat_completed_levels to stats.levelsCompleted,
            R.string.stat_three_star_levels to stats.threeStarLevels,
            R.string.stat_highest_level to stats.highestLevel,
            R.string.stat_best_score to stats.bestScore
        )
        metrics.forEachIndexed { index, metric ->
            val column = index % 2
            val row = index / 2
            val centerX = if (column == 0) 112f else 308f
            val y = 218f + row * 25f
            centeredText(canvas, tr(state, metric.first), 10f, MUTED, centerX, y)
            centeredText(canvas, metric.second.toString(), 16f, Color.WHITE, centerX, y + 17f)
        }

        centeredText(canvas, tr(state, R.string.achievements_section), 17f, Color.WHITE, 210f, 357f)
        OfflineAchievement.values().forEachIndexed { index, achievement ->
            val top = 380f + index * 46f
            val unlocked = achievement in state.unlockedAchievements
            fillPaint.color = if (unlocked) Color.argb(155, 33, 73, 73) else Color.argb(125, 31, 41, 62)
            canvas.drawRoundRect(RectF(24f, top, 396f, top + 40f), 12f, 12f, fillPaint)
            strokePaint.color = if (unlocked) Color.argb(150, 111, 228, 164) else Color.argb(65, 174, 193, 224)
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(RectF(24f, top, 396f, top + 40f), 12f, 12f, strokePaint)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 12f
            textPaint.color = if (unlocked) Color.WHITE else MUTED
            canvas.drawText(tr(state, achievementTitle(achievement)), 40f, top + 25f, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = 10f
            textPaint.color = if (unlocked) Color.rgb(111, 228, 164) else Color.rgb(134, 147, 168)
            canvas.drawText(
                tr(state, if (unlocked) R.string.achievement_unlocked else R.string.achievement_locked),
                380f, top + 24f, textPaint
            )
            textPaint.textAlign = Paint.Align.LEFT
        }
        button(canvas, 78f, 785f, 342f, 845f, tr(state, R.string.main_menu), GREEN)
    }

    private fun achievementTitle(achievement: OfflineAchievement): Int = when (achievement) {
        OfflineAchievement.FIRST_SHOT -> R.string.achievement_first_shot
        OfflineAchievement.FIRST_MATCH -> R.string.achievement_first_match
        OfflineAchievement.BIG_COMBO -> R.string.achievement_big_combo
        OfflineAchievement.FIRST_LEVEL -> R.string.achievement_first_level
        OfflineAchievement.THREE_STARS -> R.string.achievement_three_stars
        OfflineAchievement.BOOSTER_COLLECTOR -> R.string.achievement_booster_collector
        OfflineAchievement.LEVEL_TEN -> R.string.achievement_level_ten
        OfflineAchievement.CAMPAIGN_COMPLETE -> R.string.achievement_campaign_complete
    }

    internal fun drawSettings(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, R.string.settings), 30f, Color.WHITE, 210f, 225f)
        centeredText(canvas, tr(state, R.string.language), 16f, MUTED, 210f, 320f)
        button(
            canvas, 35f, 350f, 200f, 410f, tr(state, R.string.language_russian),
            if (state.language == "ru") GREEN else BLUE
        )
        button(
            canvas, 220f, 350f, 385f, 410f, tr(state, R.string.language_english),
            if (state.language == "en") GREEN else BLUE
        )
        button(
            canvas, 60f, 465f, 360f, 525f,
            tr(state, R.string.sound_setting, tr(state, if (state.soundEnabled) R.string.enabled else R.string.disabled)),
            BLUE
        )
        button(
            canvas, 60f, 540f, 360f, 600f,
            tr(state, R.string.haptics_setting, tr(state, if (state.vibrationEnabled) R.string.enabled else R.string.disabled)),
            BLUE
        )
        button(canvas, 60f, 635f, 360f, 690f, tr(state, R.string.privacy_policy), BLUE)
        button(canvas, 80f, 735f, 340f, 795f, tr(state, R.string.back), GREEN)
    }

    internal fun drawPrivacy(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, R.string.privacy_title), 24f, Color.WHITE, 210f, 150f)
        val lines = listOf(
            R.string.privacy_line_1, R.string.privacy_line_2, R.string.privacy_line_3, R.string.privacy_line_4,
            R.string.privacy_line_5, R.string.privacy_line_6, R.string.privacy_line_7, R.string.privacy_line_8
        ).map { tr(state, it) }
        lines.forEachIndexed { index, line ->
            drawParagraph(
                canvas, line, 13f, Color.rgb(205, 218, 240),
                RectF(36f, 220f + index * 62f, 384f, 270f + index * 62f)
            )
        }
        button(canvas, 80f, 790f, 340f, 845f, tr(state, R.string.back), GREEN)
    }
}
