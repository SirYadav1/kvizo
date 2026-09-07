package com.kvizo.app.util

import android.util.Base64
import com.kvizo.app.data.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * Share codes: base64-encoded JSON payload containing the full quiz
 * (title, category, difficulty, tags, time limit, questions).
 */
object ShareCodec {

    data class SharedQuiz(
        val title: String,
        val category: String,
        val difficulty: String,
        val tags: String,
        val timeLimitSeconds: Int?,
        val questions: List<Question>,
        val description: String = ""
    )

    fun encode(quiz: SharedQuiz): String {
        val arr = JSONArray()
        for (q in quiz.questions) {
            arr.put(
                JSONObject().apply {
                    put("q", q.questionText)
                    put("a", q.optionA)
                    put("b", q.optionB)
                    put("c", q.optionC)
                    put("d", q.optionD)
                    put("ans", q.correctOption)
                }
            )
        }
        val obj = JSONObject().apply {
            put("title", quiz.title)
            put("category", quiz.category)
            put("difficulty", quiz.difficulty)
            put("tags", quiz.tags)
            if (quiz.timeLimitSeconds != null) put("time", quiz.timeLimitSeconds)
            if (quiz.description.isNotBlank()) put("desc", quiz.description)
            put("questions", arr)
        }
        return Base64.encodeToString(obj.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun decode(code: String): SharedQuiz {
        val json = String(Base64.decode(code.trim(), Base64.NO_WRAP), Charsets.UTF_8)
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("questions")
        val questions = mutableListOf<Question>()
        for (i in 0 until arr.length()) {
            val q = arr.getJSONObject(i)
            questions.add(
                Question(
                    id = java.util.UUID.randomUUID().toString(),
                    quizId = "", // assigned by caller
                    questionText = q.getString("q"),
                    optionA = q.optString("a"),
                    optionB = q.optString("b"),
                    optionC = q.optString("c"),
                    optionD = q.optString("d"),
                    correctOption = q.getString("ans"),
                    position = i,
                    isBookmarked = false
                )
            )
        }
        return SharedQuiz(
            title = obj.getString("title"),
            category = obj.optString("category", "General Knowledge"),
            difficulty = obj.optString("difficulty", "Easy"),
            tags = obj.optString("tags", ""),
            timeLimitSeconds = if (obj.has("time")) obj.getInt("time") else null,
            questions = questions,
            description = obj.optString("desc", "")
        )
    }
}
