package com.kvizo.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.kvizo.app.data.Quiz
import com.kvizo.app.data.Question
import com.kvizo.app.data.QuizRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV / PDF export. Files are written to filesDir/exports and shared via FileProvider.
 */
object Exporter {

    private fun exportsDir(context: Context): File =
        File(context.filesDir, "exports").apply { mkdirs() }

    fun share(context: Context, file: File, mime: String): Uri? {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = android.content.Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share export"))
        return uri
    }

    // ---------- CSV ----------

    fun exportAttemptsCsv(context: Context, repo: QuizRepository, profileId: Long): File {
        val file = File(exportsDir(context), "kvizo_attempts_${stamp()}.csv")
        val sb = StringBuilder()
        sb.append("Date,Quiz,Category,Difficulty,Score,Correct,Total,Time (s),XP\n")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (a in repo.getAttempts(profileId)) {
            val quiz = repo.getQuizById(a.quizId)
            val qTitle = quiz?.title ?: "?"
            val qCat = quiz?.category ?: "?"
            val qDiff = quiz?.difficulty ?: "?"
            sb.append("${fmt.format(Date(a.attemptedAt))},${csv(qTitle)},${csv(qCat)},$qDiff,${a.score},${a.correctAnswers},${a.totalQuestions},${a.timeTakenSeconds},${a.xpEarned}\n")
        }
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        return file
    }

    fun exportQuestionsCsv(context: Context, quiz: Quiz, questions: List<Question>): File {
        val file = File(exportsDir(context), "kvizo_${safeName(quiz.title)}_${stamp()}.csv")
        val sb = StringBuilder()
        sb.append("#,Question,Option A,Option B,Option C,Option D,Correct Answer\n")
        for ((i, q) in questions.withIndex()) {
            sb.append("${i + 1},${csv(q.questionText)},${csv(q.optionA)},${csv(q.optionB)},${csv(q.optionC)},${csv(q.optionD)},${q.correctOption}\n")
        }
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        return file
    }

    // ---------- PDF ----------

    fun exportStatsPdf(context: Context, repo: QuizRepository, profileId: Long): File {
        val file = File(exportsDir(context), "kvizo_stats_${stamp()}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        var currentPage = doc.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2D, 0x25, 0x4E)
            textSize = 20f
            isFakeBoldText = true
        }
        val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x44, 0x44, 0x44)
            textSize = 11f
            isFakeBoldText = true
        }
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x66, 0x66, 0x66)
            textSize = 11f
        }

        var y = 60f
        val margin = 45f
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val attempts = repo.getAttempts(profileId)

        canvas.drawText("Kvizo — Statistics Report", margin, y, titlePaint); y += 22f
        canvas.drawText("Generated ${fmt.format(Date())}  •  ${attempts.size} attempts", margin, y, subPaint); y += 26f

        canvas.drawText("Date", margin, y, hdrPaint)
        canvas.drawText("Quiz", margin + 130f, y, hdrPaint)
        canvas.drawText("Score", margin + 300f, y, hdrPaint)
        canvas.drawText("Correct", margin + 360f, y, hdrPaint)
        canvas.drawText("Time (s)", margin + 440f, y, hdrPaint)
        canvas.drawText("XP", margin + 520f, y, hdrPaint)
        y += 14f

        var totalCorrect = 0
        var totalQuestions = 0
        for (a in attempts) {
            if (y > 800f) {
                doc.finishPage(currentPage)
                currentPage = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageInfo.pageNumber + 2).create())
                canvas = currentPage.canvas
                y = 60f
            }
            val qTitle = (repo.getQuizById(a.quizId)?.title ?: "?").take(28)
            canvas.drawText(fmt.format(Date(a.attemptedAt)), margin, y, rowPaint)
            canvas.drawText(qTitle, margin + 130f, y, rowPaint)
            canvas.drawText("${a.score}%", margin + 300f, y, rowPaint)
            canvas.drawText("${a.correctAnswers}/${a.totalQuestions}", margin + 360f, y, rowPaint)
            canvas.drawText("${a.timeTakenSeconds}", margin + 440f, y, rowPaint)
            canvas.drawText("${a.xpEarned}", margin + 520f, y, rowPaint)
            y += 14f
            totalCorrect += a.correctAnswers
            totalQuestions += a.totalQuestions
        }
        y += 10f
        val overall = if (totalQuestions > 0) totalCorrect * 100 / totalQuestions else 0
        canvas.drawText("Overall accuracy: $overall%", margin, y, hdrPaint)
        y += 18f
        canvas.drawText("Total time spent: ${attempts.sumOf { it.timeTakenSeconds }}s  •  Total XP: ${attempts.sumOf { it.xpEarned }}", margin, y, subPaint)

        doc.finishPage(currentPage)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun exportQuizPdf(context: Context, quiz: Quiz, questions: List<Question>, withAnswerKey: Boolean): File {
        val file = File(exportsDir(context), "kvizo_${safeName(quiz.title)}_${stamp()}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        var currentPage = doc.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2D, 0x25, 0x4E)
            textSize = 20f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x66, 0x66, 0x66)
            textSize = 11f
        }
        val qPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
        }
        val optPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x33, 0x33, 0x33)
            textSize = 11f
        }

        var y = 60f
        val margin = 45f
        val maxWidth = 505f

        canvas.drawText(quiz.title, margin, y, titlePaint); y += 22f
        canvas.drawText("Category: ${quiz.category}  |  Difficulty: ${quiz.difficulty}  |  Questions: ${questions.size}", margin, y, subPaint); y += 28f

        for ((i, q) in questions.withIndex()) {
            if (y > 800f) {
                doc.finishPage(currentPage)
                currentPage = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageInfo.pageNumber + 2).create())
                canvas = currentPage.canvas
                y = 60f
            }
            val wrappedQ = wrapText(q.questionText, qPaint, maxWidth)
            for (line in wrappedQ) {
                canvas.drawText(line, margin, y, qPaint); y += 16f
            }
            val opts = q.options()
            for ((letter, text) in opts) {
                val wrapped = wrapText("$letter) $text", optPaint, maxWidth - 15f)
                for ((li, line) in wrapped.withIndex()) {
                    canvas.drawText(if (li == 0) "$letter) $text" else line, margin + 15f, y, optPaint)
                    y += 14f
                }
            }
            if (withAnswerKey) {
                canvas.drawText("Answer: ${q.correctOption}", margin + 15f, y, optPaint.apply { color = Color.rgb(0x1B, 0x5E, 0x20) })
            }
            y += 10f
        }
        doc.finishPage(currentPage)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in words) {
            val test = if (cur.isEmpty()) w else "$cur $w"
            if (paint.measureText(test) <= maxWidth) {
                if (cur.isNotEmpty()) cur.append(' ')
                cur.append(w)
            } else {
                if (cur.isNotEmpty()) lines.add(cur.toString())
                cur = StringBuilder(w)
            }
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines
    }

    private fun csv(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun safeName(s: String): String = s.replace(Regex("[^A-Za-z0-9_-]"), "_").take(24)

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
