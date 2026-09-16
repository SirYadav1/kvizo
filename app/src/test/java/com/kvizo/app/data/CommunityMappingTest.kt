package com.kvizo.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Community quizzes arrive with the answer as an index, but the whole app scores against an option
 * letter. Storing the answer text instead marked every answer wrong, so this mapping is pinned down
 * by tests.
 */
class CommunityMappingTest {

    @Test
    fun `index becomes the matching option letter`() {
        assertEquals("a", communityCorrectLetter(0, 4))
        assertEquals("b", communityCorrectLetter(1, 4))
        assertEquals("c", communityCorrectLetter(2, 4))
        assertEquals("d", communityCorrectLetter(3, 4))
    }

    @Test
    fun `two option questions only use a and b`() {
        assertEquals("a", communityCorrectLetter(0, 2))
        assertEquals("b", communityCorrectLetter(1, 2))
        assertNull(communityCorrectLetter(2, 2))
    }

    @Test
    fun `out of range index is rejected instead of guessed`() {
        assertNull(communityCorrectLetter(-1, 4))
        assertNull(communityCorrectLetter(4, 4))
        assertNull(communityCorrectLetter(9, 4))
        assertNull(communityCorrectLetter(0, 0))
    }

    @Test
    fun `only option letters count as a valid correct option`() {
        assertTrue(isValidCorrectOption("a"))
        assertTrue(isValidCorrectOption("d"))
        assertTrue(isValidCorrectOption("B"))
        assertFalse(isValidCorrectOption("Bjarne Stroustrup"))
        assertFalse(isValidCorrectOption(""))
        assertFalse(isValidCorrectOption("e"))
    }

    @Test
    fun `community quiz is stored with letter answers and running positions`() {
        val quiz = CommunityQuiz(
            id = "cpp-001",
            title = "C++ Basics",
            category = "Programming",
            difficulty = "Medium",
            author = "tester",
            questions = listOf(
                CommunityQuestion("Creator of C++?", listOf("Ritchie", "Stroustrup", "Gosling", "Rossum"), 1, ""),
                CommunityQuestion("Which operator dereferences?", listOf("*", "&"), 0, "")
            )
        )

        val rows = quiz.toLocalQuestions("quiz-1")

        assertEquals(2, rows.size)
        assertEquals("b", rows[0].correctOption)
        assertEquals("Stroustrup", rows[0].optionB)
        assertEquals(1, rows[0].position)
        assertEquals("a", rows[1].correctOption)
        assertEquals(2, rows[1].position)
        assertTrue(rows.all { it.quizId == "quiz-1" })
    }

    @Test
    fun `tapping the right option is scored correct and a wrong one is not`() {
        val quiz = CommunityQuiz(
            id = "geo-001",
            title = "Geography",
            category = "Geography",
            difficulty = "Easy",
            author = "tester",
            questions = listOf(
                CommunityQuestion("Capital of Japan?", listOf("Osaka", "Tokyo", "Kyoto", "Nagoya"), 1, ""),
                CommunityQuestion("Largest desert?", listOf("Sahara", "Gobi", "Antarctic", "Kalahari"), 2, ""),
                CommunityQuestion("Longest river?", listOf("Nile", "Amazon", "Yangtze", "Mississippi"), 0, "")
            )
        )

        val rows = quiz.toLocalQuestions("quiz-3")
        assertEquals(3, rows.size)

        quiz.questions.forEachIndexed { index, source ->
            val question = rows[index]
            // Scoring compares the tapped letter (from Question.options()) with correctOption.
            val rightTap = question.options()[source.correctIndex].first
            assertEquals(rightTap, question.correctOption)

            val wrongTap = question.options().first { it.first != question.correctOption }.first
            assertFalse(wrongTap == question.correctOption)
        }
    }

    @Test
    fun `question with an unusable answer index is dropped`() {
        val quiz = CommunityQuiz(
            id = "broken",
            title = "Broken",
            category = "Misc",
            difficulty = "Easy",
            author = "tester",
            questions = listOf(
                CommunityQuestion("Good question?", listOf("a", "b", "c", "d"), 3, ""),
                CommunityQuestion("No valid answer?", listOf("a", "b", "c", "d"), 7, "")
            )
        )

        val rows = quiz.toLocalQuestions("quiz-2")

        assertEquals(1, rows.size)
        assertEquals("d", rows[0].correctOption)
    }
}
