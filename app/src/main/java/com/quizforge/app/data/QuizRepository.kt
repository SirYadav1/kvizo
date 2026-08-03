package com.quizforge.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.quizforge.app.logic.XpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class QuizRepository(context: Context) {

    private val db = DbHelper(context).writableDatabase

    // ---------------- PROFILES ----------------

    fun getActiveProfile(): Profile? {
        db.query("profiles", null, "is_active = 1", null, null, null, null, "1").use { c ->
            return if (c.moveToFirst()) profileFrom(c) else null
        }
    }

    fun getAllProfiles(): List<Profile> {
        val out = mutableListOf<Profile>()
        db.query("profiles", null, null, null, null, null, "created_at ASC").use { c ->
            while (c.moveToNext()) out.add(profileFrom(c))
        }
        return out
    }

    fun createProfile(username: String, avatarId: Int, status: String, bio: String): Profile {
        val values = ContentValues().apply {
            put("username", username)
            put("avatar_id", avatarId)
            put("status", status)
            put("bio", bio)
            put("level", 1)
            put("xp", 0)
            put("created_at", System.currentTimeMillis())
            put("is_active", 1)
        }
        val id = db.insert("profiles", null, values)
        return getProfileById(id)!!
    }

    fun getProfileById(id: Long): Profile? {
        db.query("profiles", null, "id = ?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) profileFrom(c) else null
        }
    }

    fun updateProfile(profile: Profile) {
        val values = ContentValues().apply {
            put("username", profile.username)
            put("avatar_id", profile.avatarId)
            put("status", profile.status)
            put("bio", profile.bio)
            put("level", profile.level)
            put("xp", profile.xp)
        }
        db.update("profiles", values, "id = ?", arrayOf(profile.id.toString()))
    }

    fun switchProfile(id: Long) {
        db.execSQL("UPDATE profiles SET is_active = 0")
        db.execSQL("UPDATE profiles SET is_active = 1 WHERE id = ?", arrayOf(id.toString()))
    }

    fun deleteProfile(id: Long) {
        db.execSQL("DELETE FROM daily_stats WHERE profile_id = ?", arrayOf(id.toString()))
        db.execSQL("DELETE FROM badges WHERE profile_id = ?", arrayOf(id.toString()))
        db.execSQL("DELETE FROM attempts WHERE profile_id = ?", arrayOf(id.toString()))
        db.execSQL("DELETE FROM quizzes WHERE profile_id = ?", arrayOf(id.toString()))
        db.execSQL("DELETE FROM profiles WHERE id = ?", arrayOf(id.toString()))
        // Make sure at least one profile stays active
        if (getActiveProfile() == null) {
            val any = getAllProfiles().firstOrNull()
            if (any != null) switchProfile(any.id)
        }
    }

    fun renameProfile(id: Long, username: String) {
        val values = ContentValues().apply { put("username", username) }
        db.update("profiles", values, "id = ?", arrayOf(id.toString()))
    }

    // ---------------- QUIZZES ----------------

    fun insertQuiz(quiz: Quiz): String {
        val values = ContentValues().apply {
            put("id", quiz.id)
            put("profile_id", quiz.profileId)
            put("title", quiz.title)
            put("category", quiz.category)
            put("difficulty", quiz.difficulty)
            put("tags", quiz.tags)
            if (quiz.timeLimitSeconds != null) put("time_limit_seconds", quiz.timeLimitSeconds) else putNull("time_limit_seconds")
            put("status", quiz.status)
            put("created_at", quiz.createdAt)
            put("updated_at", quiz.updatedAt)
            put("attempts_count", quiz.attemptsCount)
            put("average_score", quiz.averageScore)
        }
        db.insertWithOnConflict("quizzes", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return quiz.id
    }

    fun getQuizzes(profileId: Long, statusFilter: String? = null): List<Quiz> {
        val out = mutableListOf<Quiz>()
        val where = if (statusFilter != null) "profile_id = ? AND status = ?" else "profile_id = ?"
        val args = if (statusFilter != null) arrayOf(profileId.toString(), statusFilter) else arrayOf(profileId.toString())
        db.query("quizzes", null, where, args, null, null, "updated_at DESC").use { c ->
            while (c.moveToNext()) out.add(quizFrom(c))
        }
        return out
    }

    fun getQuizById(id: String): Quiz? {
        db.query("quizzes", null, "id = ?", arrayOf(id), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) quizFrom(c) else null
        }
    }

    fun getQuizByShareTitle(title: String): List<Quiz> {
        val out = mutableListOf<Quiz>()
        db.query("quizzes", null, "title = ?", arrayOf(title), null, null, "created_at DESC", "5").use { c ->
            while (c.moveToNext()) out.add(quizFrom(c))
        }
        return out
    }

    fun updateQuizMeta(quiz: Quiz) {
        val values = ContentValues().apply {
            put("title", quiz.title)
            put("category", quiz.category)
            put("difficulty", quiz.difficulty)
            put("tags", quiz.tags)
            if (quiz.timeLimitSeconds != null) put("time_limit_seconds", quiz.timeLimitSeconds) else putNull("time_limit_seconds")
            put("status", quiz.status)
            put("updated_at", quiz.updatedAt)
        }
        db.update("quizzes", values, "id = ?", arrayOf(quiz.id))
    }

    fun deleteQuiz(id: String) {
        db.delete("questions", "quiz_id = ?", arrayOf(id))
        db.delete("quizzes", "id = ?", arrayOf(id))
    }

    fun touchQuizStats(quizId: String, score: Int, total: Int) {
        val quiz = getQuizById(quizId) ?: return
        val count = quiz.attemptsCount + 1
        val avg = (quiz.averageScore * quiz.attemptsCount + (score * 100.0 / total)) / count
        val values = ContentValues().apply {
            put("attempts_count", count)
            put("average_score", avg)
            put("updated_at", System.currentTimeMillis())
        }
        db.update("quizzes", values, "id = ?", arrayOf(quizId))
    }

    // ---------------- QUESTIONS ----------------

    fun insertQuestions(questions: List<Question>) {
        db.beginTransaction()
        try {
            for (q in questions) {
                db.insertWithOnConflict("questions", null, questionValues(q), android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceQuestions(quizId: String, questions: List<Question>) {
        db.beginTransaction()
        try {
            db.delete("questions", "quiz_id = ?", arrayOf(quizId))
            for (q in questions) {
                db.insertWithOnConflict("questions", null, questionValues(q), android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getQuestions(quizId: String): List<Question> {
        val out = mutableListOf<Question>()
        db.query("questions", null, "quiz_id = ?", arrayOf(quizId), null, null, "position ASC").use { c ->
            while (c.moveToNext()) out.add(questionFrom(c))
        }
        return out
    }

    fun getBookmarkedQuestions(profileId: Long): List<Question> {
        val out = mutableListOf<Question>()
        val quizIds = getQuizzes(profileId).map { it.id }
        if (quizIds.isEmpty()) return out
        val placeholders = quizIds.joinToString(",") { "?" }
        db.query(
            "questions", null,
            "is_bookmarked = 1 AND quiz_id IN ($placeholders)",
            quizIds.toTypedArray(), null, null, "position ASC"
        ).use { c ->
            while (c.moveToNext()) out.add(questionFrom(c))
        }
        return out
    }

    fun toggleBookmark(questionId: String) {
        db.execSQL("UPDATE questions SET is_bookmarked = 1 - is_bookmarked WHERE id = ?", arrayOf(questionId))
    }

    // ---------------- ATTEMPTS ----------------

    /**
     * Records an attempt and all side effects: quiz stats, daily stats, XP, level,
     * badges. Runs in a transaction. Returns the full result.
     */
    suspend fun recordAttempt(
        profile: Profile,
        quiz: Quiz,
        questions: List<Question>,
        answers: Map<String, String>, // questionId -> selected option
        timeTakenSeconds: Int
    ): AttemptResult = withContext(Dispatchers.IO) {
        var correct = 0
        val sequence = mutableListOf<Boolean>()
        val answerRows = mutableListOf<AttemptAnswer>()
        for (q in questions) {
            val sel = answers[q.id]
            val ok = sel != null && sel == q.correctOption
            if (ok) correct++
            sequence.add(ok)
            answerRows.add(AttemptAnswer(UUID.randomUUID().toString(), "", q.id, sel ?: "", ok))
        }
        val total = questions.size
        val xp = XpEngine.xpBreakdown(correct, total, sequence)
        val now = System.currentTimeMillis()
        val attempt = Attempt(
            id = UUID.randomUUID().toString(),
            quizId = quiz.id,
            profileId = profile.id,
            score = if (total > 0) (correct * 100 / total) else 0,
            totalQuestions = total,
            correctAnswers = correct,
            timeTakenSeconds = timeTakenSeconds,
            xpEarned = xp,
            attemptedAt = now
        )
        val levelBefore = profile.level
        val newXp = profile.xp + xp
        val levelAfter = XpEngine.levelFromXp(newXp)

        db.beginTransaction()
        try {
            val av = ContentValues().apply {
                put("id", attempt.id)
                put("quiz_id", attempt.quizId)
                put("profile_id", attempt.profileId)
                put("score", attempt.score)
                put("total_questions", attempt.totalQuestions)
                put("correct_answers", attempt.correctAnswers)
                put("time_taken_seconds", attempt.timeTakenSeconds)
                put("xp_earned", attempt.xpEarned)
                put("attempted_at", attempt.attemptedAt)
            }
            db.insert("attempts", null, av)
            for (a in answerRows) {
                db.insert("attempt_answers", null, ContentValues().apply {
                    put("id", UUID.randomUUID().toString())
                    put("attempt_id", attempt.id)
                    put("question_id", a.questionId)
                    put("selected_option", a.selectedOption)
                    put("is_correct", if (a.isCorrect) 1 else 0)
                })
            }
            // quiz aggregate stats
            touchQuizStatsInTx(quiz.id, attempt.score, total)
            // profile xp + level
            db.execSQL("UPDATE profiles SET xp = ?, level = ? WHERE id = ?", arrayOf(newXp, levelAfter, profile.id.toString()))
            // daily stats (upsert)
            updateDailyStatsInTx(profile.id, 1, total, correct, timeTakenSeconds, xp)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val newBadges = evaluateBadges(
            profile.id,
            quiz.category,
            correct,
            total,
            timeTakenSeconds,
            newXp,
            now
        )
        AttemptResult(attempt, xp, levelBefore, levelAfter, newBadges, total > 0 && correct == total)
    }

    private fun touchQuizStatsInTx(quizId: String, score: Int, total: Int) {
        val quiz = getQuizById(quizId) ?: return
        val count = quiz.attemptsCount + 1
        val avg = (quiz.averageScore * quiz.attemptsCount + (score * 100.0 / total)) / count
        val values = ContentValues().apply {
            put("attempts_count", count)
            put("average_score", avg)
            put("updated_at", System.currentTimeMillis())
        }
        db.update("quizzes", values, "id = ?", arrayOf(quizId))
    }

    private fun updateDailyStatsInTx(profileId: Long, quizzes: Int, answered: Int, correct: Int, seconds: Int, xp: Int) {
        val today = XpEngine.todayStr()
        db.execSQL(
            """INSERT INTO daily_stats (profile_id, date, quizzes_attempted, questions_answered, correct_answers, time_spent_seconds, xp_earned)
               VALUES (?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT(profile_id, date) DO UPDATE SET
                 quizzes_attempted = quizzes_attempted + excluded.quizzes_attempted,
                 questions_answered = questions_answered + excluded.questions_answered,
                 correct_answers = correct_answers + excluded.correct_answers,
                 time_spent_seconds = time_spent_seconds + excluded.time_spent_seconds,
                 xp_earned = xp_earned + excluded.xp_earned""",
            arrayOf(profileId.toString(), today, quizzes.toString(), answered.toString(), correct.toString(), seconds.toString(), xp.toString())
        )
    }

    fun getAttempts(profileId: Long): List<Attempt> {
        val out = mutableListOf<Attempt>()
        db.query("attempts", null, "profile_id = ?", arrayOf(profileId.toString()), null, null, "attempted_at DESC").use { c ->
            while (c.moveToNext()) out.add(attemptFrom(c))
        }
        return out
    }

    fun getAttemptById(id: String): Attempt? {
        db.query("attempts", null, "id = ?", arrayOf(id), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) attemptFrom(c) else null
        }
    }

    fun getAttemptAnswers(attemptId: String): List<AttemptAnswer> {
        val out = mutableListOf<AttemptAnswer>()
        db.query("attempt_answers", null, "attempt_id = ?", arrayOf(attemptId), null, null, null).use { c ->
            while (c.moveToNext()) out.add(answerFrom(c))
        }
        return out
    }

    fun getAttemptCountToday(profileId: Long): Int {
        val today = XpEngine.todayStr()
        db.rawQuery(
            "SELECT quizzes_attempted FROM daily_stats WHERE profile_id = ? AND date = ?",
            arrayOf(profileId.toString(), today)
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    /** First attempt today? Used for the +25 daily-first-quiz bonus. */
    fun isFirstAttemptToday(profileId: Long): Boolean = getAttemptCountToday(profileId) == 0

    // ---------------- BADGES ----------------

    private fun evaluateBadges(
        profileId: Long,
        category: String,
        correct: Int,
        total: Int,
        timeTaken: Int,
        totalXp: Int,
        now: Long
    ): List<Badge> {
        val unlocked = mutableListOf<Badge>()
        fun grant(code: String, name: String) {
            if (hasBadge(profileId, code)) return
            val b = Badge(UUID.randomUUID().toString(), profileId, code, name, now)
            db.insertWithOnConflict("badges", null, ContentValues().apply {
                put("id", b.id)
                put("profile_id", b.profileId)
                put("badge_code", b.badgeCode)
                put("badge_name", b.badgeName)
                put("unlocked_at", b.unlockedAt)
            }, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
            if (hasBadge(profileId, code)) unlocked.add(b)
        }

        val attempts = getAttempts(profileId)
        val quizCount = getQuizzes(profileId).size
        val totalCorrect = attempts.sumOf { it.correctAnswers }
        val totalAnswered = attempts.sumOf { it.totalQuestions }
        val daySet = attempts.map { XpEngine.dateStr(it.attemptedAt) }.toSet()
        val streak = XpEngine.currentStreak(daySet)

        if (attempts.size == 1) grant("first_quiz", "First Quiz")
        if (total > 0 && correct == total) grant("perfect_score", "Perfect Score")
        if (streak >= 3) grant("streak_3", "3-Day Streak")
        if (streak >= 7) grant("streak_7", "7-Day Streak")
        if (streak >= 30) grant("streak_30", "30-Day Streak")
        if (timeTaken < 60 && total >= 1) grant("speed_demon", "Speed Demon")
        if (attempts.size >= 50) grant("bookworm", "Bookworm")
        if (totalCorrect >= 100) grant("century", "Century")
        if (attempts.size >= 10 && totalAnswered > 0 && totalCorrect * 100 / totalAnswered >= 90) {
            grant("sharpshooter", "Sharpshooter")
        }
        // Category King: 90%+ accuracy in a category with at least 3 attempts there.
        val catAttempts = attempts.filter { a ->
            getQuizById(a.quizId)?.category == category
        }
        if (catAttempts.size >= 3) {
            val catCorrect = catAttempts.sumOf { it.correctAnswers }
            val catTotal = catAttempts.sumOf { it.totalQuestions }
            if (catTotal > 0 && catCorrect * 100 / catTotal >= 90) grant("category_king", "Category King")
        }
        if (quizCount >= 5) grant("creator", "Creator")
        if (quizCount >= 20) grant("quiz_producer", "Quiz Producer")
        if (XpEngine.isNight(now)) grant("night_owl", "Night Owl")
        if (XpEngine.isEarlyMorning(now)) grant("early_bird", "Early Bird")
        if (totalXp >= 10000) grant("centurion", "Centurion")
        return unlocked
    }

    private fun hasBadge(profileId: Long, code: String): Boolean {
        db.query("badges", arrayOf("id"), "profile_id = ? AND badge_code = ?",
            arrayOf(profileId.toString(), code), null, null, null, "1").use { c ->
            return c.moveToFirst()
        }
    }

    fun getBadges(profileId: Long): List<Badge> {
        val out = mutableListOf<Badge>()
        db.query("badges", null, "profile_id = ?", arrayOf(profileId.toString()), null, null, "unlocked_at ASC").use { c ->
            while (c.moveToNext()) out.add(badgeFrom(c))
        }
        return out
    }

    /** XP bonus for creating the first quiz (+30). Returns bonus XP awarded. */
    fun firstQuizBonus(profileId: Long): Int {
        val count = getQuizzes(profileId).size
        if (count == 1) {
            val p = getProfileById(profileId) ?: return 0
            val newXp = p.xp + 30
            db.execSQL("UPDATE profiles SET xp = ?, level = ? WHERE id = ?",
                arrayOf(newXp, XpEngine.levelFromXp(newXp), profileId.toString()))
            return 30
        }
        return 0
    }

    /** +25 XP for daily first quiz + +50 for 5 quizzes in a day. Returns bonus XP. */
    fun dailyBonus(profileId: Long): Int {
        val count = getAttemptCountToday(profileId)
        var bonus = 0
        if (count == 1) bonus += 25
        if (count == 5) bonus += 50
        if (bonus > 0) {
            val p = getProfileById(profileId) ?: return 0
            val newXp = p.xp + bonus
            db.execSQL("UPDATE profiles SET xp = ?, level = ? WHERE id = ?",
                arrayOf(newXp, XpEngine.levelFromXp(newXp), profileId.toString()))
        }
        return bonus
    }

    // ---------------- STATS ----------------

    fun getDailyStats(profileId: Long): List<DailyStat> {
        val out = mutableListOf<DailyStat>()
        db.query("daily_stats", null, "profile_id = ?", arrayOf(profileId.toString()), null, null, "date ASC").use { c ->
            while (c.moveToNext()) out.add(dailyStatFrom(c))
        }
        return out
    }

    fun totalTimeSpent(profileId: Long): Long =
        getAttempts(profileId).sumOf { it.timeTakenSeconds.toLong() }

    fun categoryAccuracy(profileId: Long): Map<String, Pair<Int, Int>> {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        for (a in getAttempts(profileId)) {
            val quiz = getQuizById(a.quizId) ?: continue
            val cur = map[quiz.category] ?: (0 to 0)
            map[quiz.category] = (cur.first + a.correctAnswers) to (cur.second + a.totalQuestions)
        }
        return map
    }

    fun difficultyStats(profileId: Long): Map<String, Pair<Int, Int>> {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        for (a in getAttempts(profileId)) {
            val quiz = getQuizById(a.quizId) ?: continue
            val cur = map[quiz.difficulty] ?: (0 to 0)
            map[quiz.difficulty] = (cur.first + a.correctAnswers) to (cur.second + a.totalQuestions)
        }
        return map
    }

    fun questionSuccessRates(profileId: Long): Map<String, Pair<Int, Int>> {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        for (a in getAttempts(profileId)) {
            val answers = getAttemptAnswers(a.id)
            for (ans in answers) {
                val qid = ans.questionId
                val cur = map[qid] ?: (0 to 0)
                map[qid] = (cur.first + if (ans.isCorrect) 1 else 0) to (cur.second + 1)
            }
        }
        return map
    }

    fun storageInfo(): Triple<Int, Int, Int> {
        var quizzes = 0
        var questions = 0
        var attempts = 0
        db.rawQuery("SELECT COUNT(*) FROM quizzes", null).use { c -> if (c.moveToFirst()) quizzes = c.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM questions", null).use { c -> if (c.moveToFirst()) questions = c.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM attempts", null).use { c -> if (c.moveToFirst()) attempts = c.getInt(0) }
        return Triple(quizzes, questions, attempts)
    }

    fun dbSizeBytes(): Long {
        return db.path?.let { java.io.File(it).length() } ?: 0L
    }

    // ---------------- RAW OPS (backup restore) ----------------

    fun resetAll() {
        db.execSQL("DELETE FROM attempt_answers")
        db.execSQL("DELETE FROM attempts")
        db.execSQL("DELETE FROM badges")
        db.execSQL("DELETE FROM daily_stats")
        db.execSQL("DELETE FROM questions")
        db.execSQL("DELETE FROM quizzes")
        db.execSQL("DELETE FROM profiles")
    }

    fun storageBytes(): Long {
        try {
            db.rawQuery("SELECT SUM(pages * page_size) AS total FROM pragma_page_count(), pragma_page_size()", null).use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
        } catch (_: Exception) {
        }
        // fallback: sum of all rows
        var total = 0L
        listOf("profiles", "quizzes", "questions", "attempts", "attempt_answers", "badges", "daily_stats").forEach { t ->
            db.rawQuery("SELECT COUNT(*) FROM $t", null).use { c ->
                if (c.moveToFirst()) total += c.getLong(0) * 100
            }
        }
        return total
    }

    fun dbForRawOps() = db

    fun insertQuizRaw(db: android.database.sqlite.SQLiteDatabase, quiz: Quiz) {
        val values = ContentValues().apply {
            put("id", quiz.id)
            put("profile_id", quiz.profileId)
            put("title", quiz.title)
            put("category", quiz.category)
            put("difficulty", quiz.difficulty)
            put("tags", quiz.tags)
            if (quiz.timeLimitSeconds != null) put("time_limit_seconds", quiz.timeLimitSeconds) else putNull("time_limit_seconds")
            put("status", quiz.status)
            put("created_at", quiz.createdAt)
            put("updated_at", quiz.updatedAt)
            put("attempts_count", quiz.attemptsCount)
            put("average_score", quiz.averageScore)
        }
        db.insertWithOnConflict("quizzes", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertQuestionsRaw(db: android.database.sqlite.SQLiteDatabase, questions: List<Question>) {
        for (q in questions) {
            db.insertWithOnConflict("questions", null, questionValues(q), android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun insertAttemptRaw(db: android.database.sqlite.SQLiteDatabase, a: Attempt) {
        db.insertWithOnConflict("attempts", null, ContentValues().apply {
            put("id", a.id)
            put("quiz_id", a.quizId)
            put("profile_id", a.profileId)
            put("score", a.score)
            put("total_questions", a.totalQuestions)
            put("correct_answers", a.correctAnswers)
            put("time_taken_seconds", a.timeTakenSeconds)
            put("xp_earned", a.xpEarned)
            put("attempted_at", a.attemptedAt)
        }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertAttemptAnswerRaw(db: android.database.sqlite.SQLiteDatabase, a: AttemptAnswer) {
        db.insertWithOnConflict("attempt_answers", null, ContentValues().apply {
            put("id", a.id)
            put("attempt_id", a.attemptId)
            put("question_id", a.questionId)
            put("selected_option", a.selectedOption)
            put("is_correct", if (a.isCorrect) 1 else 0)
        }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertBadgeRaw(db: android.database.sqlite.SQLiteDatabase, b: Badge) {
        db.insertWithOnConflict("badges", null, ContentValues().apply {
            put("id", b.id)
            put("profile_id", b.profileId)
            put("badge_code", b.badgeCode)
            put("badge_name", b.badgeName)
            put("unlocked_at", b.unlockedAt)
        }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertDailyRaw(db: android.database.sqlite.SQLiteDatabase, s: DailyStat) {
        db.insertWithOnConflict("daily_stats", null, ContentValues().apply {
            put("profile_id", s.profileId)
            put("date", s.date)
            put("quizzes_attempted", s.quizzesAttempted)
            put("questions_answered", s.questionsAnswered)
            put("correct_answers", s.correctAnswers)
            put("time_spent_seconds", s.timeSpentSeconds)
            put("xp_earned", s.xpEarned)
        }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---------------- MAPPERS ----------------

    private fun profileFrom(c: Cursor) = Profile(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("username")),
        c.getInt(c.getColumnIndexOrThrow("avatar_id")),
        c.getString(c.getColumnIndexOrThrow("status")),
        c.getString(c.getColumnIndexOrThrow("bio")),
        c.getInt(c.getColumnIndexOrThrow("level")),
        c.getInt(c.getColumnIndexOrThrow("xp")),
        c.getLong(c.getColumnIndexOrThrow("created_at")),
        c.getInt(c.getColumnIndexOrThrow("is_active")) == 1
    )

    private fun quizFrom(c: Cursor): Quiz {
        val timeIdx = c.getColumnIndex("time_limit_seconds")
        val timeVal = if (timeIdx >= 0 && !c.isNull(timeIdx)) c.getInt(timeIdx) else null
        return Quiz(
            c.getString(c.getColumnIndexOrThrow("id")),
            c.getLong(c.getColumnIndexOrThrow("profile_id")),
            c.getString(c.getColumnIndexOrThrow("title")),
            c.getString(c.getColumnIndexOrThrow("category")),
            c.getString(c.getColumnIndexOrThrow("difficulty")),
            c.getString(c.getColumnIndexOrThrow("tags")),
            timeVal,
            c.getString(c.getColumnIndexOrThrow("status")),
            c.getLong(c.getColumnIndexOrThrow("created_at")),
            c.getLong(c.getColumnIndexOrThrow("updated_at")),
            c.getInt(c.getColumnIndexOrThrow("attempts_count")),
            c.getDouble(c.getColumnIndexOrThrow("average_score"))
        )
    }

    private fun questionFrom(c: Cursor) = Question(
        c.getString(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("quiz_id")),
        c.getString(c.getColumnIndexOrThrow("question_text")),
        c.getString(c.getColumnIndexOrThrow("option_a")),
        c.getString(c.getColumnIndexOrThrow("option_b")),
        c.getString(c.getColumnIndexOrThrow("option_c")),
        c.getString(c.getColumnIndexOrThrow("option_d")),
        c.getString(c.getColumnIndexOrThrow("correct_option")),
        c.getInt(c.getColumnIndexOrThrow("position")),
        c.getInt(c.getColumnIndexOrThrow("is_bookmarked")) == 1
    )

    private fun attemptFrom(c: Cursor) = Attempt(
        c.getString(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("quiz_id")),
        c.getLong(c.getColumnIndexOrThrow("profile_id")),
        c.getInt(c.getColumnIndexOrThrow("score")),
        c.getInt(c.getColumnIndexOrThrow("total_questions")),
        c.getInt(c.getColumnIndexOrThrow("correct_answers")),
        c.getInt(c.getColumnIndexOrThrow("time_taken_seconds")),
        c.getInt(c.getColumnIndexOrThrow("xp_earned")),
        c.getLong(c.getColumnIndexOrThrow("attempted_at"))
    )

    private fun answerFrom(c: Cursor) = AttemptAnswer(
        c.getString(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("attempt_id")),
        c.getString(c.getColumnIndexOrThrow("question_id")),
        c.getString(c.getColumnIndexOrThrow("selected_option")),
        c.getInt(c.getColumnIndexOrThrow("is_correct")) == 1
    )

    private fun badgeFrom(c: Cursor) = Badge(
        c.getString(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("profile_id")),
        c.getString(c.getColumnIndexOrThrow("badge_code")),
        c.getString(c.getColumnIndexOrThrow("badge_name")),
        c.getLong(c.getColumnIndexOrThrow("unlocked_at"))
    )

    private fun dailyStatFrom(c: Cursor) = DailyStat(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("profile_id")),
        c.getString(c.getColumnIndexOrThrow("date")),
        c.getInt(c.getColumnIndexOrThrow("quizzes_attempted")),
        c.getInt(c.getColumnIndexOrThrow("questions_answered")),
        c.getInt(c.getColumnIndexOrThrow("correct_answers")),
        c.getInt(c.getColumnIndexOrThrow("time_spent_seconds")),
        c.getInt(c.getColumnIndexOrThrow("xp_earned"))
    )

    private fun questionValues(q: Question) = ContentValues().apply {
        put("id", q.id)
        put("quiz_id", q.quizId)
        put("question_text", q.questionText)
        put("option_a", q.optionA)
        put("option_b", q.optionB)
        put("option_c", q.optionC)
        put("option_d", q.optionD)
        put("correct_option", q.correctOption)
        put("position", q.position)
        put("is_bookmarked", if (q.isBookmarked) 1 else 0)
    }
}
