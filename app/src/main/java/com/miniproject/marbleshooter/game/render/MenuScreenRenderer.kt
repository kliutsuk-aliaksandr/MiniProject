package com.miniproject.marbleshooter.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import com.miniproject.marbleshooter.game.domain.GameEngine
import com.miniproject.marbleshooter.game.domain.LevelCatalog
import com.miniproject.marbleshooter.game.domain.model.Vec2

/** Renders the menu, campaign map, tutorial, settings and privacy screens. */
internal class MenuScreenRenderer(resources: GameRenderResources) : GameCanvasRendererBase(resources) {
    internal fun drawMenu(canvas: Canvas, engine: GameEngine, state: GameRenderState) {
        centeredText(canvas, "MARBLE FROG", 39f, Color.WHITE, 210f, 145f)
        centeredText(
            canvas,
            tr(state, "ЦВЕТНАЯ ГОЛОВОЛОМКА С ЛЯГУШКОЙ", "A COLORFUL FROG MARBLE PUZZLE"),
            14f, Color.rgb(174, 193, 224), 210f, 180f
        )
        drawFrog(canvas, Vec2(210f, 305f), Vec2(210f, 155f), engine.ammoColor)
        centeredText(
            canvas,
            tr(state, "РЕКОРД  ", "BEST  ") + state.bestScore,
            15f, Color.rgb(194, 212, 240), 210f, 378f
        )

        if (state.savedGameAvailable) {
            button(canvas, 78f, 410f, 342f, 464f, tr(state, "ПРОДОЛЖИТЬ", "CONTINUE"), GREEN)
            button(canvas, 78f, 474f, 342f, 528f, tr(state, "НОВАЯ ИГРА", "NEW GAME"), BLUE)
            button(canvas, 78f, 538f, 342f, 592f, tr(state, "КАРТА УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 78f, 602f, 342f, 656f, tr(state, "ОБУЧЕНИЕ", "TUTORIAL"), BLUE)
            button(canvas, 78f, 666f, 342f, 720f, tr(state, "НАСТРОЙКИ", "SETTINGS"), BLUE)
        } else {
            button(canvas, 78f, 438f, 342f, 496f, tr(state, "ИГРАТЬ", "PLAY"), GREEN)
            button(canvas, 78f, 506f, 342f, 564f, tr(state, "КАРТА УРОВНЕЙ", "LEVEL MAP"), BLUE)
            button(canvas, 78f, 574f, 342f, 632f, tr(state, "ОБУЧЕНИЕ", "TUTORIAL"), BLUE)
            button(canvas, 78f, 642f, 342f, 700f, tr(state, "НАСТРОЙКИ", "SETTINGS"), BLUE)
        }
        centeredText(canvas, "OFFLINE  •  0.5.0", 12f, Color.rgb(122, 143, 177), 210f, 835f)
    }

    internal fun drawLevelMap(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, "КАРТА УРОВНЕЙ", "LEVEL MAP"), 29f, Color.WHITE, 210f, 160f)
        centeredText(
            canvas,
            tr(state, "Открыто уровней: ", "Levels unlocked: ") + state.unlockedLevel +
                " / " + LevelCatalog.MAX_LEVELS,
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

        drawTrackLegend(canvas, state)
        button(canvas, 78f, 750f, 342f, 810f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), GREEN)
    }

    private fun drawTrackLegend(canvas: Canvas, state: GameRenderState) {
        centeredText(
            canvas,
            tr(state, "Новые типы дорожек открываются по мере прохождения",
                "New track styles unlock as you progress"),
            11f, MUTED, 210f, 695f
        )
        textPaint.textSize = 11f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.rgb(178, 196, 221)
        canvas.drawText(
            tr(state, "1–8: Классика   9–15: Петли   16–22: Зигзаг   23–30: Спираль",
                "1–8: Classic   9–15: Loops   16–22: Switchback   23–30: Spiral"),
            210f, 720f, textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT
    }

    internal fun drawTutorial(canvas: Canvas, state: GameRenderState) {
        val page = state.tutorialPage.coerceIn(0, 4)
        val titlesRu = listOf(
            "Прицелься пальцем",
            "Выбери нужный шар",
            "Собирай комбинации",
            "Используй бустеры",
            "Следи за дорожкой"
        )
        val titlesEn = listOf(
            "Aim with your finger",
            "Choose the right marble",
            "Make matches",
            "Use boosters",
            "Watch the track"
        )
        val bodiesRu = listOf(
            "Коснись поля и веди пальцем — лягушка повернётся за ним. Отпусти палец, чтобы выстрелить.",
            "В пасти лягушки показан текущий шар. Кнопка NEXT меняет его со следующим.",
            "Попади в группу из трёх или более шаров одного цвета. Передняя часть цепочки откатится назад, дальняя останется на месте.",
            "Замедление даёт время на прицел. Бомба убирает участок цепочки. Смена цвета выбирает наиболее частый цвет. Реверс временно двигает цепочку назад.",
            "Грязь замедляет цепочку, лёд ускоряет её, камни создают опасные участки. Каждая группа уровней меняет форму дорожки."
        )
        val bodiesEn = listOf(
            "Touch the field and drag. The frog follows your finger. Release to shoot.",
            "The marble in the frog's mouth is the loaded shot. NEXT swaps it with the upcoming marble.",
            "Hit a group of three or more marbles of the same color. The front segment rolls backward while the far side stays still.",
            "Slow Time gives you room to aim. Bomb clears a chain segment. Color Change selects the most common chain color. Reverse moves the chain backward briefly.",
            "Mud slows the chain, ice speeds it up, and rocks mark hazardous sections. Track layouts change between level groups."
        )

        centeredText(canvas, tr(state, "ОБУЧЕНИЕ", "TUTORIAL"), 29f, Color.WHITE, 210f, 145f)
        centeredText(canvas, (page + 1).toString() + " / 5", 13f, MUTED, 210f, 177f)
        fillPaint.color = Color.rgb(30, 43, 71)
        canvas.drawRoundRect(RectF(26f, 210f, 394f, 625f), 24f, 24f, fillPaint)
        drawTutorialIllustration(canvas, page)
        centeredText(
            canvas,
            if (state.language == "en") titlesEn[page] else titlesRu[page],
            22f, Color.WHITE, 210f, 405f
        )
        drawParagraph(
            canvas,
            if (state.language == "en") bodiesEn[page] else bodiesRu[page],
            15f, Color.rgb(205, 218, 240), RectF(55f, 430f, 365f, 570f)
        )

        if (page == 4) {
            button(canvas, 60f, 640f, 360f, 695f, tr(state, "ПОПРОБОВАТЬ В ИГРЕ", "TRY IT IN GAME"), GREEN)
        } else {
            button(canvas, 80f, 640f, 340f, 695f, tr(state, "ДАЛЕЕ", "NEXT"), GREEN)
        }
        button(canvas, 80f, 712f, 340f, 760f, tr(state, "НАЗАД", "BACK"), BLUE)
        button(canvas, 80f, 775f, 340f, 825f, tr(state, "В ГЛАВНОЕ МЕНЮ", "MAIN MENU"), BLUE)
    }

    private fun drawTutorialIllustration(canvas: Canvas, page: Int) {
        when (page) {
            0 -> {
                drawFrog(canvas, Vec2(210f, 330f), Vec2(120f, 235f), 2)
                drawArrow(canvas, 240f, 255f, 320f, 220f, Color.rgb(111, 228, 164))
            }
            1 -> {
                drawMarble(canvas, 165f, 290f, 0, 19f)
                drawMarble(canvas, 255f, 290f, 3, 19f)
                centeredText(canvas, "NEXT", 14f, MUTED, 210f, 345f)
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

    internal fun drawSettings(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, "НАСТРОЙКИ", "SETTINGS"), 30f, Color.WHITE, 210f, 225f)
        centeredText(canvas, tr(state, "ЯЗЫК", "LANGUAGE"), 16f, MUTED, 210f, 320f)
        button(
            canvas, 35f, 350f, 200f, 410f, "РУССКИЙ",
            if (state.language == "ru") GREEN else BLUE
        )
        button(
            canvas, 220f, 350f, 385f, 410f, "ENGLISH",
            if (state.language == "en") GREEN else BLUE
        )
        button(
            canvas, 60f, 465f, 360f, 525f,
            tr(state, "ЗВУК: ", "SOUND: ") + tr(state, "ВКЛ", "ON").takeIf { state.soundEnabled }
                .orEmpty().ifBlank { tr(state, "ВЫКЛ", "OFF") },
            BLUE
        )
        button(
            canvas, 60f, 540f, 360f, 600f,
            tr(state, "ВИБРАЦИЯ: ", "HAPTICS: ") + tr(state, "ВКЛ", "ON").takeIf { state.vibrationEnabled }
                .orEmpty().ifBlank { tr(state, "ВЫКЛ", "OFF") },
            BLUE
        )
        button(canvas, 60f, 635f, 360f, 690f, tr(state, "ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ", "PRIVACY POLICY"), BLUE)
        button(canvas, 80f, 735f, 340f, 795f, tr(state, "НАЗАД", "BACK"), GREEN)
    }

    internal fun drawPrivacy(canvas: Canvas, state: GameRenderState) {
        centeredText(canvas, tr(state, "КОНФИДЕНЦИАЛЬНОСТЬ", "PRIVACY"), 24f, Color.WHITE, 210f, 150f)
        val lines = if (state.language == "en") {
            listOf(
                "Marble Frog works offline. No account or network access is needed.",
                "The game does not collect, transmit or share personal data.",
                "No ads, analytics SDKs or server sync are included.",
                "Best score, saved game, level stars, language and settings",
                "are stored locally on this device with Android preferences.",
                "Cloud backup and device transfer of these preferences are disabled.",
                "Local data can be removed in Android app settings or by uninstalling.",
                "Support: alex.kliutsuk@gmail.com"
            )
        } else {
            listOf(
                "Marble Frog работает без интернета. Аккаунт не требуется.",
                "Игра не собирает, не передаёт и не раскрывает персональные данные.",
                "Рекламы, аналитических SDK и синхронизации с сервером нет.",
                "Рекорд, сохранённая игра, звёзды, язык и настройки",
                "хранятся локально на этом устройстве в Android preferences.",
                "Облачное резервирование и перенос этих данных отключены.",
                "Данные можно удалить в настройках Android или удалив игру.",
                "Поддержка: alex.kliutsuk@gmail.com"
            )
        }
        lines.forEachIndexed { index, line ->
            drawParagraph(
                canvas, line, 13f, Color.rgb(205, 218, 240),
                RectF(36f, 220f + index * 62f, 384f, 270f + index * 62f)
            )
        }
        button(canvas, 80f, 790f, 340f, 845f, tr(state, "НАЗАД", "BACK"), GREEN)
    }
}
