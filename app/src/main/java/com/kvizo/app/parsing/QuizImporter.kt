package com.kvizo.app.parsing

import com.kvizo.app.data.Question
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Detects and parses quiz files in .txt (Kvizo format), .json or .csv.
 *
 * JSON accepted shapes:
 *   { "title": "...", "questions": [ { "questionText": "...", "optionA": "...",
 *     "optionB": "...", "optionC": "...", "optionD": "...", "correctOption": "a" }, ... ] }
 * or a bare array of questions. Short aliases (q/a/b/c/d/ans/answer) also work.
 *
 * CSV format (header row required, RFC4180 quoting supported):
 *   question,option_a,option_b,option_c,option_d,answer
 *   "2+2?",4,5,3,6,a
 * The answer column may be a letter (a-d) or the exact option text.
 * option_c/option_d may be empty for True/False questions.
 */
object QuizImporter {

    fun detectAndParse(fileName: String?, content: String, quizId: String): List<Question> {
        val name = fileName?.lowercase() ?: ""
        return when {
            name.endsWith(".json") -> parseJson(content, quizId)
            name.endsWith(".csv") -> parseCsv(content, quizId)
            name.endsWith(".txt") || name.isEmpty() -> TxtParser.parse(content, quizId)
            else -> TxtParser.parse(content, quizId)
        }
    }

    fun parseJson(content: String, quizId: String): List<Question> {
        val trimmed = content.trim()
        val root = if (trimmed.startsWith("[")) JSONArray(trimmed) else {
            val obj = JSONObject(trimmed)
            obj.optJSONArray("questions") ?: throw ParseException("JSON: no 'questions' array found")
        }
        val out = mutableListOf<Question>()
        for (i in 0 until root.length()) {
            val j = root.getJSONObject(i)
            val qText = j.optString("questionText").ifBlank { j.optString("q") }
                .ifBlank { j.optString("question") }
            if (qText.isBlank()) throw ParseException("JSON question ${i + 1}: missing question text")
            val options = linkedMapOf<String, String>()
            fun opt(vararg keys: String): String = keys.firstOrNull { j.has(it) }?.let { j.optString(it) }?.trim() ?: ""
            listOf("a" to "optionA", "b" to "optionB", "c" to "optionC", "d" to "optionD").forEach { (letter, key) ->
                val v = opt(key, letter)
                if (v.isNotBlank()) options[letter] = v
            }
            j.optJSONObject("options")?.let { obj ->
                listOf("a", "b", "c", "d").forEach { letter ->
                    if (obj.has(letter)) {
                        val v = obj.optString(letter).trim()
                        if (v.isNotBlank()) options[letter] = v
                    }
                }
            }
            if (options.size != 2 && options.size != 4) {
                throw ParseException("JSON question ${i + 1}: need 2 (T/F) or 4 options, found ${options.size}")
            }
            val answerRaw = opt("correctOption", "ans", "answer").lowercase()
            val answer = when {
                answerRaw.length == 1 && answerRaw[0] in 'a'..'d' -> answerRaw
                options.entries.firstOrNull { it.value.equals(answerRaw, ignoreCase = true) }?.key != null ->
                    options.entries.first { it.value.equals(answerRaw, ignoreCase = true) }.key
                else -> throw ParseException("JSON question ${i + 1}: missing or invalid correct answer")
            }
            if (answer !in options) throw ParseException("JSON question ${i + 1}: answer '$answerRaw' not among options")
            out.add(
                Question(
                    id = UUID.randomUUID().toString(),
                    quizId = quizId,
                    questionText = qText.trim(),
                    optionA = options["a"] ?: "",
                    optionB = options["b"] ?: "",
                    optionC = options["c"] ?: "",
                    optionD = options["d"] ?: "",
                    correctOption = answer,
                    position = out.size,
                    isBookmarked = false
                )
            )
        }
        if (out.isEmpty()) throw ParseException("JSON: no valid questions found")
        return out
    }

    fun parseCsv(content: String, quizId: String): List<Question> {
        val rows = parseCsvRows(content)
        if (rows.isEmpty()) throw ParseException("CSV: file is empty")
        val header = rows.first().map { it.trim().lowercase() }
        val idx = mutableMapOf<String, Int>()
        val known = listOf("question", "q", "text", "question_text", "option_a", "optiona", "opt1", "a",
            "option_b", "optionb", "opt2", "b", "option_c", "optionc", "opt3", "c",
            "option_d", "optiond", "opt4", "d", "answer", "ans", "correct", "correct_option", "correctoption")
        header.forEachIndexed { i, h ->
            if (h in known && h !in idx) idx[h] = i
        }
        if (!idx.containsKey("question") && idx.containsKey("q")) idx["question"] = idx["q"]!!

        fun col(name: String): Int? = idx[name]
        val qIdx = col("question") ?: col("q") ?: col("text") ?: col("question_text")
        val aIdx = col("option_a") ?: col("optiona") ?: col("opt1") ?: col("a")
        val bIdx = col("option_b") ?: col("optionb") ?: col("opt2") ?: col("b")
        val cIdx = col("option_c") ?: col("optionc") ?: col("opt3") ?: col("c")
        val dIdx = col("option_d") ?: col("optiond") ?: col("opt4") ?: col("d")
        val ansIdx = col("answer") ?: col("ans") ?: col("correct") ?: col("correct_option") ?: col("correctoption")
        if (qIdx == null || aIdx == null || bIdx == null || ansIdx == null) {
            throw ParseException("CSV: header must include question, option_a, option_b and answer columns")
        }

        val out = mutableListOf<Question>()
        for ((rowNo, row) in rows.drop(1).withIndex()) {
            if (row.all { it.isBlank() }) continue
            fun at(i: Int): String = row.getOrNull(i)?.trim() ?: ""
            val qText = at(qIdx)
            if (qText.isBlank()) throw ParseException("CSV row ${rowNo + 2}: empty question")
            val options = linkedMapOf<String, String>()
            if (at(aIdx).isNotBlank()) options["a"] = at(aIdx)
            if (at(bIdx).isNotBlank()) options["b"] = at(bIdx)
            if (cIdx != null && at(cIdx).isNotBlank()) options["c"] = at(cIdx)
            if (dIdx != null && at(dIdx).isNotBlank()) options["d"] = at(dIdx)
            if (options.size != 2 && options.size != 4) {
                throw ParseException("CSV row ${rowNo + 2}: need 2 or 4 options, found ${options.size}")
            }
            val raw = at(ansIdx).lowercase()
            val answer = when {
                raw.length == 1 && raw[0] in 'a'..'d' -> raw
                options.entries.firstOrNull { it.value.equals(raw, ignoreCase = true) }?.key != null ->
                    options.entries.first { it.value.equals(raw, ignoreCase = true) }.key
                else -> throw ParseException("CSV row ${rowNo + 2}: invalid answer '$raw'")
            }
            if (answer !in options) throw ParseException("CSV row ${rowNo + 2}: answer not among options")
            out.add(
                Question(
                    id = UUID.randomUUID().toString(),
                    quizId = quizId,
                    questionText = qText,
                    optionA = options["a"] ?: "",
                    optionB = options["b"] ?: "",
                    optionC = options["c"] ?: "",
                    optionD = options["d"] ?: "",
                    correctOption = answer,
                    position = out.size,
                    isBookmarked = false
                )
            )
        }
        if (out.isEmpty()) throw ParseException("CSV: no valid questions found")
        return out
    }

    /** Minimal RFC4180-style CSV parser (quoted fields, escaped quotes, embedded newlines). */
    private fun parseCsvRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = content
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < s.length && s[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(field.toString().trim())
                    field.setLength(0)
                }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
                    row.add(field.toString().trim())
                    field.setLength(0)
                    if (row.isNotEmpty() && row.any { it.isNotBlank() }) rows.add(row.toList())
                    row.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString().trim())
            if (row.any { it.isNotBlank() }) rows.add(row.toList())
        }
        return rows
    }
}
