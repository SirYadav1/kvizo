package com.quizforge.app.logic

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object XpEngine {

    /** Level titles per documentation. */
    fun levelTitle(level: Int): String = when {
        level >= 6 -> "Quiz Master"
        level == 5 -> "Expert"
        level == 4 -> "Advanced"
        level == 3 -> "Intermediate"
        level == 2 -> "Learner"
        else -> "Beginner"
    }

    /** XP thresholds per documentation. */
    fun xpForLevel(level: Int): Int = when (level) {
        1 -> 0
        2 -> 200
        3 -> 500
        4 -> 1000
        5 -> 2000
        else -> 5000
    }

    fun levelFromXp(xp: Int): Int {
        var level = 1
        while (level < 6 && xp >= xpForLevel(level + 1)) level++
        return level
    }

    /** Progress 0..1 toward next level. */
    fun levelProgress(xp: Int): Float {
        val lvl = levelFromXp(xp)
        if (lvl >= 6) return 1f
        val cur = xpForLevel(lvl)
        val next = xpForLevel(lvl + 1)
        return ((xp - cur).toFloat() / (next - cur).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * XP breakdown for a completed attempt.
     * completion +20, each correct +10, perfect +30, streak bonus +5 per streak of >=3
     * consecutive correct answers.
     */
    fun xpBreakdown(correct: Int, total: Int, answeredSequence: List<Boolean>): Int {
        var xp = 20
        xp += correct * 10
        if (total > 0 && correct == total) xp += 30
        var run = 0
        for (ok in answeredSequence) {
            if (ok) {
                run++
                if (run >= 3 && run % 3 == 0) xp += 5
            } else run = 0
        }
        return xp
    }

    fun todayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun dateStr(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))

    fun hourOfDay(ts: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    fun isNight(ts: Long): Boolean = hourOfDay(ts) >= 22

    fun isEarlyMorning(ts: Long): Boolean = hourOfDay(ts) < 6

    /** Consecutive-day streak ending today (or yesterday if no attempts today). */
    fun currentStreak(daysWithAttempts: Set<String>): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var streak = 0
        val cal = Calendar.getInstance()
        // If today has no activity yet, start counting from yesterday.
        if (fmt.format(cal.time) !in daysWithAttempts) cal.add(Calendar.DAY_OF_YEAR, -1)
        while (fmt.format(cal.time) in daysWithAttempts) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun longestStreak(daysWithAttempts: Set<String>): Int {
        if (daysWithAttempts.isEmpty()) return 0
        val days = daysWithAttempts.sorted()
        var best = 1
        var run = 1
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 1 until days.size) {
            val prev = fmt.parse(days[i - 1])!!
            val cur = fmt.parse(days[i])!!
            val diff = ((cur.time - prev.time) / 86_400_000L).toInt()
            run = if (diff == 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
