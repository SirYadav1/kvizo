package com.kvizo.app.parsing

import com.kvizo.app.data.Question
import java.util.UUID

class ParseException(message: String) : Exception(message)

/**
 * Parses .txt quiz files per the documented format:
 *   - Questions start with a number followed by "." or ")"
 *   - Options are a) b) c) d)
 *   - Answer lines start with "Answer:" or "ANS:"
 *   - Empty lines ignored; "//" or "#" lines are comments
 *   - MCQ needs exactly 4 options, True/False needs exactly 2
 */
object TxtParser {

    fun parse(content: String, quizId: String): List<Question> {
        val lines = content.split("\n")
        val questions = mutableListOf<Question>()
        var current: MutableQuestion? = null
        var questionStartLine = 0

        for ((idx, raw) in lines.withIndex()) {
            val line = raw.trim()
            val lineNo = idx + 1
            if (line.isEmpty()) continue
            if (line.startsWith("//") || line.startsWith("#")) continue

            val qMatch = Regex("""^(\d+)[.)]\s*(.+)$""").find(line)
            if (qMatch != null) {
                current?.let { finalize(it, questions, questionStartLine) }
                current = MutableQuestion(quizId, qMatch.groupValues[2].trim())
                questionStartLine = lineNo
                continue
            }

            val ansMatch = Regex("""^(?:Answer|ANS)\s*[:=]\s*([a-dA-D])\.?\s*$""").find(line)
            if (ansMatch != null) {
                current?.answer = ansMatch.groupValues[1].lowercase()
                continue
            }

            val optMatch = Regex("""^([a-dA-D])[.)]\s*(.+)$""").find(line)
            if (optMatch != null) {
                val letter = optMatch.groupValues[1].lowercase()
                if (current == null) throw ParseException("Line $lineNo: option found before any question")
                current!!.options[letter] = optMatch.groupValues[2].trim()
                continue
            }

            // Unrecognized line: treat as continuation of the question text
            // (supports multi-line questions).
            if (current != null) {
                current!!.text = (current!!.text + " " + line).trim()
            }
        }
        current?.let { finalize(it, questions, questionStartLine) }
        return questions
    }

    private fun finalize(m: MutableQuestion, out: MutableList<Question>, lineNo: Int) {
        val opts = m.options
        if (opts.isEmpty()) throw ParseException("Line $lineNo: question has no options")
        val isTF = opts.size == 2
        val isMCQ = opts.size == 4
        if (!isTF && !isMCQ) {
            throw ParseException("Line $lineNo: question must have exactly 4 options (MCQ) or 2 (True/False), found ${opts.size}")
        }
        val answer = m.answer
        if (answer == null || answer !in opts) {
            throw ParseException("Line $lineNo: missing or invalid 'Answer:' line for question")
        }
        out.add(
            Question(
                id = UUID.randomUUID().toString(),
                quizId = m.quizId,
                questionText = m.text,
                optionA = opts["a"] ?: "",
                optionB = opts["b"] ?: "",
                optionC = opts["c"] ?: "",
                optionD = opts["d"] ?: "",
                correctOption = answer,
                position = out.size,
                isBookmarked = false
            )
        )
    }

    private class MutableQuestion(val quizId: String, var text: String) {
        val options = linkedMapOf<String, String>()
        var answer: String? = null
    }
}
