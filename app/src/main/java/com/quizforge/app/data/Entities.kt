package com.quizforge.app.data

data class Profile(
    val id: Long,
    val username: String,
    val avatarId: Int,
    val status: String,
    val bio: String,
    val level: Int,
    val xp: Int,
    val createdAt: Long,
    val isActive: Boolean
)

data class Quiz(
    val id: String,
    val profileId: Long,
    val title: String,
    val category: String,
    val difficulty: String,
    val tags: String,
    val timeLimitSeconds: Int?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val attemptsCount: Int,
    val averageScore: Double,
    val description: String = "",
    val isRemote: Boolean = false
)

data class Question(
    val id: String,
    val quizId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String,
    val position: Int,
    val isBookmarked: Boolean
) {
    fun options(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (optionA.isNotBlank()) list.add("a" to optionA)
        if (optionB.isNotBlank()) list.add("b" to optionB)
        if (optionC.isNotBlank()) list.add("c" to optionC)
        if (optionD.isNotBlank()) list.add("d" to optionD)
        return list
    }
}

data class Attempt(
    val id: String,
    val quizId: String,
    val profileId: Long,
    val score: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val timeTakenSeconds: Int,
    val xpEarned: Int,
    val attemptedAt: Long
)

data class AttemptAnswer(
    val id: String,
    val attemptId: String,
    val questionId: String,
    val selectedOption: String,
    val isCorrect: Boolean
)

data class Badge(
    val id: String,
    val profileId: Long,
    val badgeCode: String,
    val badgeName: String,
    val unlockedAt: Long
)

data class DailyStat(
    val id: Long,
    val profileId: Long,
    val date: String,
    val quizzesAttempted: Int,
    val questionsAnswered: Int,
    val correctAnswers: Int,
    val timeSpentSeconds: Int,
    val xpEarned: Int
)

/** Payload of a community quiz served by the QuizForge server. */
data class RemoteQuiz(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String,
    val tags: String,
    val timeLimitSeconds: Int?,
    val createdAt: Long,
    val description: String,
    val questions: List<RemoteQuestion>
)

data class RemoteQuestion(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String
)

/** Result of recording a quiz attempt: XP + newly unlocked badges. */
data class AttemptResult(
    val attempt: Attempt,
    val xpGained: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val newBadges: List<Badge>,
    val perfect: Boolean
)

const val STATUS_DRAFT = "draft"
const val STATUS_PUBLISHED = "published"
const val STATUS_ARCHIVED = "archived"

const val DIFF_EASY = "Easy"
const val DIFF_MEDIUM = "Medium"
const val DIFF_HARD = "Hard"

val PRESET_CATEGORIES = listOf(
    "General Knowledge", "Science", "Mathematics", "History", "Geography",
    "Computer Science", "Programming", "English Grammar", "Biology", "Chemistry",
    "Physics", "Sports", "Entertainment", "Current Affairs", "Literature",
    "Economics", "Psychology", "Philosophy", "Mythology", "Custom"
)

val BADGE_DEFS = listOf(
    "first_quiz" to "First Quiz",
    "perfect_score" to "Perfect Score",
    "streak_3" to "3-Day Streak",
    "streak_7" to "7-Day Streak",
    "streak_30" to "30-Day Streak",
    "speed_demon" to "Speed Demon",
    "bookworm" to "Bookworm",
    "century" to "Century",
    "sharpshooter" to "Sharpshooter",
    "category_king" to "Category King",
    "creator" to "Creator",
    "quiz_producer" to "Quiz Producer",
    "night_owl" to "Night Owl",
    "early_bird" to "Early Bird",
    "centurion" to "Centurion"
)
