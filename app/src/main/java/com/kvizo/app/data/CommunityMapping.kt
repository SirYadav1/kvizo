package com.kvizo.app.data

import java.util.UUID

/**
 * Letters the app uses to identify options.
 *
 * `Question.options()` returns these letters, the quiz builder writes them, and scoring compares
 * the selected letter against `Question.correctOption`. Anything stored in `correctOption` that is
 * not one of these letters can never be scored as correct.
 */
const val OPTION_LETTERS = "abcd"

private const val OPTION_LETTERS_UPPER = "ABCD"

/** True when [value] is an option letter this app can score (upper case included). */
fun isValidCorrectOption(value: String): Boolean =
    value.length == 1 && (value[0] in OPTION_LETTERS || value[0] in OPTION_LETTERS_UPPER)

/**
 * Community JSON marks the correct answer with a 0-based index; the app stores a letter.
 *
 * Returns null when the index does not point at a real option — such a question is unanswerable,
 * so callers drop it instead of importing something that always scores as wrong.
 */
fun communityCorrectLetter(correctIndex: Int, optionCount: Int): String? {
    val limit = minOf(optionCount, OPTION_LETTERS.length)
    if (correctIndex < 0 || correctIndex >= limit) return null
    return OPTION_LETTERS[correctIndex].toString()
}

/**
 * Converts a verified community quiz into rows for the local database.
 *
 * The answer must become a letter here: the rest of the app compares the selected option letter
 * with `correctOption`, so storing the answer *text* (what older builds did) makes every single
 * answer in every community quiz score as wrong.
 */
fun CommunityQuiz.toLocalQuestions(quizId: String): List<Question> =
    questions.mapNotNull { cq ->
        val letter = communityCorrectLetter(cq.correctIndex, cq.options.size) ?: return@mapNotNull null
        Question(
            id = UUID.randomUUID().toString(),
            quizId = quizId,
            questionText = cq.question,
            optionA = cq.options.getOrElse(0) { "" },
            optionB = cq.options.getOrElse(1) { "" },
            optionC = cq.options.getOrElse(2) { "" },
            optionD = cq.options.getOrElse(3) { "" },
            correctOption = letter,
            position = 0,
            isBookmarked = false
        )
    }.mapIndexed { index, question -> question.copy(position = index + 1) }
