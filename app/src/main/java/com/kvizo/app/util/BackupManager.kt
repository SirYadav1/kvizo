package com.kvizo.app.util

import com.kvizo.app.data.Profile
import com.kvizo.app.data.Quiz
import com.kvizo.app.data.Question
import com.kvizo.app.data.Attempt
import com.kvizo.app.data.AttemptAnswer
import com.kvizo.app.data.Badge
import com.kvizo.app.data.DailyStat
import com.kvizo.app.data.QuizRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full data backup (all profiles) to a single JSON string and restore.
 * Plain text — users are warned to keep it secure.
 */
object BackupManager {

    fun export(repo: QuizRepository): String {
        val root = JSONObject()
        val profiles = JSONArray()
        for (p in repo.getAllProfiles()) {
            val pj = JSONObject().apply {
                put("id", p.id)
                put("username", p.username)
                put("avatar_id", p.avatarId)
                put("status", p.status)
                put("bio", p.bio)
                put("level", p.level)
                put("xp", p.xp)
                put("created_at", p.createdAt)
                put("is_active", p.isActive)
            }
            val quizzes = JSONArray()
            for (q in repo.getQuizzes(p.id)) {
                val qj = JSONObject().apply {
                    put("id", q.id)
                    put("title", q.title)
                    put("category", q.category)
                    put("difficulty", q.difficulty)
                    put("tags", q.tags)
                    if (q.timeLimitSeconds != null) put("time_limit", q.timeLimitSeconds)
                    put("status", q.status)
                    put("created_at", q.createdAt)
                    put("updated_at", q.updatedAt)
                    put("attempts_count", q.attemptsCount)
                    put("average_score", q.averageScore)
                    put("description", q.description)
                    val qs = JSONArray()
                    for (question in repo.getQuestions(q.id)) {
                        qs.put(JSONObject().apply {
                            put("id", question.id)
                            put("text", question.questionText)
                            put("a", question.optionA)
                            put("b", question.optionB)
                            put("c", question.optionC)
                            put("d", question.optionD)
                            put("ans", question.correctOption)
                            put("pos", question.position)
                            put("bm", question.isBookmarked)
                        })
                    }
                    put("questions", qs)
                }
                quizzes.put(qj)
            }
            pj.put("quizzes", quizzes)

            val attempts = JSONArray()
            for (a in repo.getAttempts(p.id)) {
                val aj = JSONObject().apply {
                    put("id", a.id)
                    put("quiz_id", a.quizId)
                    put("score", a.score)
                    put("total", a.totalQuestions)
                    put("correct", a.correctAnswers)
                    put("time", a.timeTakenSeconds)
                    put("xp", a.xpEarned)
                    put("at", a.attemptedAt)
                    val answers = JSONArray()
                    for (ans in repo.getAttemptAnswers(a.id)) {
                        answers.put(JSONObject().apply {
                            put("q", ans.questionId)
                            put("sel", ans.selectedOption)
                            put("ok", ans.isCorrect)
                        })
                    }
                    put("answers", answers)
                }
                attempts.put(aj)
            }
            pj.put("attempts", attempts)

            val badges = JSONArray()
            for (b in repo.getBadges(p.id)) {
                badges.put(JSONObject().apply {
                    put("code", b.badgeCode)
                    put("name", b.badgeName)
                    put("at", b.unlockedAt)
                })
            }
            pj.put("badges", badges)

            val stats = JSONArray()
            for (s in repo.getDailyStats(p.id)) {
                stats.put(JSONObject().apply {
                    put("date", s.date)
                    put("qa", s.quizzesAttempted)
                    put("answered", s.questionsAnswered)
                    put("correct", s.correctAnswers)
                    put("time", s.timeSpentSeconds)
                    put("xp", s.xpEarned)
                })
            }
            pj.put("daily", stats)
            profiles.put(pj)
        }
        root.put("app", "Kvizo")
        root.put("version", 1)
        root.put("exported_at", System.currentTimeMillis())
        root.put("profiles", profiles)
        return root.toString(2)
    }

    /**
     * Restores a backup, replacing all current data.
     * Returns the list of restored profile usernames, or throws on invalid JSON.
     */
    fun restore(json: String, repo: QuizRepository): List<String> {
        val root = JSONObject(json)
        if (root.optString("app") != "Kvizo") throw IllegalArgumentException("Not a Kvizo backup file")
        val profilesArr = root.getJSONArray("profiles")
        val restoredNames = mutableListOf<String>()

        val db = repo.dbForRawOps()
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM attempt_answers")
            db.execSQL("DELETE FROM attempts")
            db.execSQL("DELETE FROM badges")
            db.execSQL("DELETE FROM daily_stats")
            db.execSQL("DELETE FROM questions")
            db.execSQL("DELETE FROM quizzes")
            db.execSQL("DELETE FROM profiles")
            for (i in 0 until profilesArr.length()) {
                val pj = profilesArr.getJSONObject(i)
                val p = Profile(
                    id = pj.getLong("id"),
                    username = pj.getString("username"),
                    avatarId = pj.getInt("avatar_id"),
                    status = pj.optString("status"),
                    bio = pj.optString("bio"),
                    level = pj.getInt("level"),
                    xp = pj.getInt("xp"),
                    createdAt = pj.getLong("created_at"),
                    isActive = pj.getBoolean("is_active")
                )
                db.execSQL(
                    "INSERT INTO profiles (id, username, avatar_id, status, bio, level, xp, created_at, is_active) VALUES (?,?,?,?,?,?,?,?,?)",
                    arrayOf(p.id, p.username, p.avatarId, p.status, p.bio, p.level, p.xp, p.createdAt, if (p.isActive) 1 else 0)
                )
                val quizzesArr = pj.optJSONArray("quizzes") ?: JSONArray()
                for (j in 0 until quizzesArr.length()) {
                    val qj = quizzesArr.getJSONObject(j)
                    val quiz = Quiz(
                        id = qj.getString("id"),
                        profileId = p.id,
                        title = qj.getString("title"),
                        category = qj.optString("category", "General Knowledge"),
                        difficulty = qj.optString("difficulty", "Easy"),
                        tags = qj.optString("tags"),
                        timeLimitSeconds = if (qj.has("time_limit") && !qj.isNull("time_limit")) qj.getInt("time_limit") else null,
                        status = qj.optString("status", "published"),
                        createdAt = qj.getLong("created_at"),
                        updatedAt = qj.getLong("updated_at"),
                        attemptsCount = qj.getInt("attempts_count"),
                        averageScore = qj.getDouble("average_score"),
                        description = qj.optString("description", "")
                    )
                    repo.insertQuizRaw(db, quiz)
                    val qsArr = qj.optJSONArray("questions") ?: JSONArray()
                    val questions = mutableListOf<Question>()
                    for (k in 0 until qsArr.length()) {
                        val q = qsArr.getJSONObject(k)
                        questions.add(
                            Question(
                                id = q.getString("id"),
                                quizId = quiz.id,
                                questionText = q.getString("text"),
                                optionA = q.optString("a"),
                                optionB = q.optString("b"),
                                optionC = q.optString("c"),
                                optionD = q.optString("d"),
                                correctOption = q.getString("ans"),
                                position = q.getInt("pos"),
                                isBookmarked = q.optBoolean("bm")
                            )
                        )
                    }
                    repo.insertQuestionsRaw(db, questions)
                }
                val attemptsArr = pj.optJSONArray("attempts") ?: JSONArray()
                for (j in 0 until attemptsArr.length()) {
                    val aj = attemptsArr.getJSONObject(j)
                    val a = Attempt(
                        id = aj.getString("id"),
                        quizId = aj.getString("quiz_id"),
                        profileId = p.id,
                        score = aj.getInt("score"),
                        totalQuestions = aj.getInt("total"),
                        correctAnswers = aj.getInt("correct"),
                        timeTakenSeconds = aj.getInt("time"),
                        xpEarned = aj.getInt("xp"),
                        attemptedAt = aj.getLong("at")
                    )
                    repo.insertAttemptRaw(db, a)
                    val ansArr = aj.optJSONArray("answers") ?: JSONArray()
                    for (k in 0 until ansArr.length()) {
                        val ans = ansArr.getJSONObject(k)
                        repo.insertAttemptAnswerRaw(
                            db, AttemptAnswer(
                                id = java.util.UUID.randomUUID().toString(),
                                attemptId = a.id,
                                questionId = ans.getString("q"),
                                selectedOption = ans.optString("sel"),
                                isCorrect = ans.getBoolean("ok")
                            )
                        )
                    }
                }
                val badgesArr = pj.optJSONArray("badges") ?: JSONArray()
                for (j in 0 until badgesArr.length()) {
                    val bj = badgesArr.getJSONObject(j)
                    val b = Badge(
                        id = java.util.UUID.randomUUID().toString(),
                        profileId = p.id,
                        badgeCode = bj.getString("code"),
                        badgeName = bj.getString("name"),
                        unlockedAt = bj.getLong("at")
                    )
                    repo.insertBadgeRaw(db, b)
                }
                val dailyArr = pj.optJSONArray("daily") ?: JSONArray()
                for (j in 0 until dailyArr.length()) {
                    val dj = dailyArr.getJSONObject(j)
                    val s = DailyStat(
                        id = 0,
                        profileId = p.id,
                        date = dj.getString("date"),
                        quizzesAttempted = dj.getInt("qa"),
                        questionsAnswered = dj.getInt("answered"),
                        correctAnswers = dj.getInt("correct"),
                        timeSpentSeconds = dj.getInt("time"),
                        xpEarned = dj.getInt("xp")
                    )
                    repo.insertDailyRaw(db, s)
                }
                restoredNames.add(p.username)
            }
            // Re-sync AUTOINCREMENT counters so the next auto-generated id never
            // collides with restored ids.
            syncSequence(db, "profiles")
            syncSequence(db, "daily_stats")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return restoredNames
    }

    private fun syncSequence(db: android.database.sqlite.SQLiteDatabase, table: String) {
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table))
        db.execSQL(
            "INSERT INTO sqlite_sequence(name, seq) SELECT ?, COALESCE(MAX(id), 0) FROM $table",
            arrayOf(table)
        )
    }
}
