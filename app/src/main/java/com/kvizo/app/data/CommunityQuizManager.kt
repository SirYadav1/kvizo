package com.kvizo.app.data

import android.content.Context
import com.kvizo.app.util.CommunityFetch
import com.kvizo.app.util.CommunityFetch.CacheProvider

class CommunityQuizManager(private val context: Context) {

    init { CacheProvider.appContext = context }

    fun fetchByCategory(category: String = "all"): List<CommunityQuiz> {
        return try {
            CommunityFetch.fetchQuizzesByCategory(category)
        } catch (e: Exception) {
            CommunityFetch.fetchLegacy()
        }
    }

    fun fetchQuizFull(quizId: String): CommunityQuiz? {
        return CommunityFetch.fetchQuizFull(quizId)
    }

    fun fetchAll(): List<CommunityQuiz> {
        return CommunityFetch.fetchLegacy()
    }

    fun importToDatabase(quizzes: List<CommunityQuiz>, repo: QuizRepository, profileId: Long): Int {
        var count = 0
        for (quiz in quizzes) {
            if (quiz.questions.isEmpty()) continue
            val quizId = java.util.UUID.randomUUID().toString()
            val q = Quiz(
                id = quizId,
                profileId = profileId,
                title = quiz.title,
                category = quiz.category,
                difficulty = quiz.difficulty,
                tags = "",
                timeLimitSeconds = null,
                status = "active",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                attemptsCount = 0,
                averageScore = 0.0,
                isRemote = true
            )
            repo.insertQuiz(q)
            val questions = quiz.questions.mapIndexed { i, cq ->
                Question(
                    id = java.util.UUID.randomUUID().toString(),
                    quizId = quizId,
                    questionText = cq.question,
                    optionA = cq.options.getOrElse(0) { "" },
                    optionB = cq.options.getOrElse(1) { "" },
                    optionC = cq.options.getOrElse(2) { "" },
                    optionD = cq.options.getOrElse(3) { "" },
                    correctOption = cq.options.getOrElse(cq.correctIndex) { "" },
                    position = i + 1,
                    isBookmarked = false
                )
            }
            repo.insertQuestions(questions)
            count++
        }
        return count
    }
}
