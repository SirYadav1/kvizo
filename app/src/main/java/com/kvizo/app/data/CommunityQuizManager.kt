package com.kvizo.app.data

import android.content.Context
import com.kvizo.app.util.CommunityFetch
import com.kvizo.app.util.CommunityFetch.CacheProvider
import java.util.UUID

/** What an import actually did, so the UI can report it without guessing. */
data class CommunityImportResult(val added: Int, val repaired: Int) {
    val total: Int get() = added + repaired
}

class CommunityQuizManager(private val context: Context) {

    init { CacheProvider.appContext = context }

    /** Verified community quizzes. Throws when nothing could be verified (see CommunityFetch). */
    fun fetchAll(): List<CommunityQuiz> = CommunityFetch.fetchAll()

    fun fetchByCategory(category: String = "all"): List<CommunityQuiz> = CommunityFetch.fetchByCategory(category)

    fun fetchQuizFull(quizId: String): CommunityQuiz? = CommunityFetch.fetchQuizFull(quizId)

    /**
     * Copies verified quizzes into the local database.
     *
     * Three things matter here:
     *  - quizzes the profile already has are matched on title, so a sync can never pile up duplicates;
     *  - rows are stored as [STATUS_PUBLISHED], the status the rest of the app actually reads
     *    (older builds wrote "active", which no screen recognises);
     *  - a quiz that an older build imported with the answer stored as text, or with no questions at
     *    all, is rebuilt in place. Every answer in such a quiz scored as wrong, and remote quizzes
     *    cannot be deleted from inside the app, so it would otherwise stay broken forever.
     */
    fun importToDatabase(
        quizzes: List<CommunityQuiz>,
        repo: QuizRepository,
        profileId: Long
    ): CommunityImportResult {
        val existingByTitle = repo.getQuizzes(profileId)
            .filter { it.isRemote }
            .associateBy { it.title.trim().lowercase() }

        var added = 0
        var repaired = 0

        for (quiz in quizzes) {
            val title = quiz.title.trim().lowercase()
            if (title.isEmpty()) continue

            val local = existingByTitle[title]
            if (local == null) {
                val quizId = UUID.randomUUID().toString()
                val rows = quiz.toLocalQuestions(quizId)
                if (rows.isEmpty()) continue // nothing playable — better no quiz than an empty one
                val now = System.currentTimeMillis()
                repo.insertQuiz(
                    Quiz(
                        id = quizId,
                        profileId = profileId,
                        title = quiz.title,
                        category = quiz.category,
                        difficulty = quiz.difficulty,
                        tags = "",
                        timeLimitSeconds = null,
                        status = STATUS_PUBLISHED,
                        createdAt = now,
                        updatedAt = now,
                        attemptsCount = 0,
                        averageScore = 0.0,
                        isRemote = true
                    )
                )
                repo.insertQuestions(rows)
                added++
            } else {
                val current = repo.getQuestions(local.id)
                val broken = current.isEmpty() || current.any { !isValidCorrectOption(it.correctOption) }
                if (!broken) continue
                val rows = quiz.toLocalQuestions(local.id)
                if (rows.isEmpty()) continue
                repo.replaceQuestions(local.id, rows)
                repaired++
            }
        }
        return CommunityImportResult(added, repaired)
    }
}
