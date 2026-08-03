package com.quizforge.app.ui

import android.app.Application
import android.content.Context
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizforge.app.data.AppSettings
import com.quizforge.app.data.AttemptResult
import com.quizforge.app.data.Profile
import com.quizforge.app.data.Quiz
import com.quizforge.app.data.Question
import com.quizforge.app.data.QuizRepository
import com.quizforge.app.data.SettingsRepo
import com.quizforge.app.data.STATUS_PUBLISHED
import com.quizforge.app.logic.XpEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = QuizRepository(app)
    private val settingsRepo = SettingsRepo(app)

    val settings: Flow<AppSettings> = settingsRepo.settings

    var profile by mutableStateOf<Profile?>(null)
        private set

    // For dashboard recomposition we re-read these on each navigation refresh.
    var quizzes by mutableStateOf<List<Quiz>>(emptyList())
        private set

    init {
        refreshProfile()
        viewModelScope.launch {
            profile?.let { quizzes = repo.getQuizzes(it.id) }
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                soundOn = s.soundEnabled
                hapticsOn = s.hapticsEnabled
            }
        }
    }

    fun refreshProfile() {
        profile = repo.getActiveProfile()
    }

    fun refreshQuizzes() {
        profile?.let { quizzes = repo.getQuizzes(it.id) }
    }

    // ---------- settings ----------

    fun setThemeMode(mode: String) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setSoundEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setSoundEnabled(v) }
    fun setHapticsEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setHapticsEnabled(v) }
    fun setTimerOnBackground(v: String) = viewModelScope.launch { settingsRepo.setTimerOnBackground(v) }
    fun setTimerBehavior(v: String) = setTimerOnBackground(v)

    // ---------- profile ----------

    fun createProfile(username: String, avatarId: Int, status: String, bio: String): Profile {
        val p = repo.createProfile(username, avatarId, status, bio)
        profile = p
        refreshQuizzes()
        return p
    }

    fun updateProfileStatus(bio: String, status: String) {
        profile?.let { p ->
            val np = p.copy(bio = bio, status = status)
            repo.updateProfile(np)
            profile = np
        }
    }

    fun renameProfile(name: String) {
        profile?.let { p ->
            val np = p.copy(username = name)
            repo.updateProfile(np)
            profile = np
        }
    }

    fun switchProfile(id: Long) {
        repo.switchProfile(id)
        refreshProfile()
        refreshQuizzes()
    }

    fun deleteProfile(id: Long) {
        repo.deleteProfile(id)
        refreshProfile()
        refreshQuizzes()
    }

    fun getAllProfiles(): List<Profile> = repo.getAllProfiles()

    fun updateProfile(username: String, status: String, bio: String, avatarId: Int) {
        profile?.let { p ->
            val np = p.copy(username = username, status = status, bio = bio, avatarId = avatarId)
            repo.updateProfile(np)
            profile = np
        }
    }

    fun avatarCount(): Int = AVATARS.size

    fun toggleBookmark(questionId: String) = repo.toggleBookmark(questionId)

    fun resetAll() {
        repo.resetAll()
        profile = null
        quizzes = emptyList()
    }

    // ---------- quiz CRUD ----------

    fun createQuiz(
        title: String,
        category: String,
        difficulty: String,
        tags: String,
        timeLimit: Int?,
        status: String,
        questions: List<Question>
    ): Quiz {
        val pid = profile?.id ?: throw IllegalStateException("No active profile")
        val now = System.currentTimeMillis()
        val quiz = Quiz(
            id = UUID.randomUUID().toString(),
            profileId = pid,
            title = title,
            category = category,
            difficulty = difficulty,
            tags = tags,
            timeLimitSeconds = timeLimit,
            status = status,
            createdAt = now,
            updatedAt = now,
            attemptsCount = 0,
            averageScore = 0.0
        )
        repo.insertQuiz(quiz)
        repo.replaceQuestions(quiz.id, questions.mapIndexed { i, q ->
            q.copy(id = if (q.id.isEmpty()) UUID.randomUUID().toString() else q.id, quizId = quiz.id, position = i)
        })
        // First quiz bonus +30 XP
        repo.firstQuizBonus(pid)
        refreshProfile()
        refreshQuizzes()
        return quiz
    }

    fun updateQuiz(quiz: Quiz, questions: List<Question>) {
        repo.updateQuizMeta(quiz)
        repo.replaceQuestions(quiz.id, questions.mapIndexed { i, q ->
            q.copy(id = if (q.id.isEmpty()) UUID.randomUUID().toString() else q.id, quizId = quiz.id, position = i)
        })
        refreshQuizzes()
    }

    fun deleteQuiz(id: String) {
        repo.deleteQuiz(id)
        refreshQuizzes()
    }

    fun duplicateQuiz(quizId: String) {
        val src = repo.getQuizById(quizId) ?: return
        val now = System.currentTimeMillis()
        val copy = src.copy(
            id = UUID.randomUUID().toString(),
            title = src.title + " (Copy)",
            createdAt = now,
            updatedAt = now,
            attemptsCount = 0,
            averageScore = 0.0
        )
        repo.insertQuiz(copy)
        val qs = repo.getQuestions(quizId).map { q ->
            q.copy(id = UUID.randomUUID().toString(), quizId = copy.id)
        }
        repo.insertQuestions(qs)
        refreshQuizzes()
    }

    fun importSharedQuiz(shared: com.quizforge.app.util.ShareCodec.SharedQuiz): Quiz {
        val pid = profile?.id ?: throw IllegalStateException("No active profile")
        val now = System.currentTimeMillis()
        val quiz = Quiz(
            id = UUID.randomUUID().toString(),
            profileId = pid,
            title = shared.title,
            category = shared.category,
            difficulty = shared.difficulty,
            tags = shared.tags,
            timeLimitSeconds = shared.timeLimitSeconds,
            status = STATUS_PUBLISHED,
            createdAt = now,
            updatedAt = now,
            attemptsCount = 0,
            averageScore = 0.0
        )
        repo.insertQuiz(quiz)
        repo.replaceQuestions(quiz.id, shared.questions.mapIndexed { i, q ->
            q.copy(id = UUID.randomUUID().toString(), quizId = quiz.id, position = i)
        })
        repo.firstQuizBonus(pid)
        refreshProfile()
        refreshQuizzes()
        return quiz
    }

    fun getQuiz(id: String): Quiz? = repo.getQuizById(id)

    fun getQuestions(id: String): List<Question> = repo.getQuestions(id)

    // ---------- attempt ----------

    suspend fun recordAttempt(quiz: Quiz, questions: List<Question>, answers: Map<String, String>, timeTaken: Int): AttemptResult {
        val p = profile ?: throw IllegalStateException("No active profile")
        val result = repo.recordAttempt(p, quiz, questions, answers, timeTaken)
        val bonus = repo.dailyBonus(p.id)
        val finalResult = if (bonus > 0) {
            val rp = repo.getProfileById(p.id)!!
            result.copy(xpGained = result.xpGained + bonus)
        } else result
        profile = repo.getProfileById(p.id)
        refreshQuizzes()
        return finalResult
    }

    // ---------- audio / haptics ----------

    private var soundOn = true
    private var hapticsOn = true
    private var toneGen: ToneGenerator? = null

    /** Instant correct/wrong feedback tone. */
    fun playSound(correct: Boolean) {
        if (!soundOn) return
        try {
            val t = toneGen ?: ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 80).also { toneGen = it }
            if (correct) t.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            else t.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (_: Exception) {
        }
    }

    /** Ascending celebratory jingle for quiz completion. */
    fun playSuccessJingle() {
        if (!soundOn) return
        viewModelScope.launch {
            try {
                val t = toneGen ?: ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 85).also { toneGen = it }
                val notes = intArrayOf(
                    ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_3,
                    ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_7,
                    ToneGenerator.TONE_DTMF_9
                )
                for (n in notes) {
                    t.startTone(n, 140)
                    kotlinx.coroutines.delay(150)
                }
                t.startTone(ToneGenerator.TONE_PROP_ACK, 320)
            } catch (_: Exception) {
            }
        }
    }

    /** Small correct/wrong haptic feedback. */
    fun vibrate(correct: Boolean) {
        if (!hapticsOn) return
        val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (v == null || !v.hasVibrator()) return
        try {
            v.vibrate(
                if (correct) VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                else VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (_: Exception) {
        }
    }

    /** Epic multi-pulse celebration vibration. */
    fun epicVibrate() {
        if (!hapticsOn) return
        val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (v == null || !v.hasVibrator()) return
        try {
            v.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 90, 50, 90, 50, 140, 60, 180, 70, 280),
                    intArrayOf(0, 255, 0, 200, 0, 255, 0, 180, 0, 255),
                    -1
                )
            )
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        try { toneGen?.release() } catch (_: Exception) {
        }
        super.onCleared()
    }

    // ---------- misc ----------

    fun avatarEmoji(id: Int): String = AVATARS[id % AVATARS.size]

    companion object {
        val AVATARS = listOf(
            "🦊", "🐼", "🦁", "🐸", "🐙", "🦄", "🐯", "🐨",
            "🐧", "🦉", "🐺", "🐳", "🦋", "🐝", "🐢", "🦅",
            "🐰", "🦕", "🐬", "🦚", "🐲", "🦩", "🐹", "🦥",
            "🐆", "🦜", "🐋", "🦎", "🐿️", "🦔", "🐊", "🦍",
            "🐘", "🦛", "🐫", "🦓", "🦌", "🐃", "🐄", "🐖",
            "🐏", "🐑", "🐐", "🦙", "🦘", "🦡", "🐁", "🐀",
            "🦢", "🦤", "🦃", "🐓", "🦆", "🦇"
        )
    }
}
