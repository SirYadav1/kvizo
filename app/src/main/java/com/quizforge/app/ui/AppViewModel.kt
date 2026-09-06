package com.quizforge.app.ui

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizforge.app.R
import com.quizforge.app.data.AppSettings
import com.quizforge.app.data.AttemptResult
import com.quizforge.app.data.LeaderboardEntry
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
    private val communityQuizManager = com.quizforge.app.data.CommunityQuizManager(app)
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

    fun avatarCount(): Int = AVATAR_COUNT

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
        questions: List<Question>,
        description: String = ""
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
            averageScore = 0.0,
            description = description
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

    fun deleteQuiz(id: String): Boolean {
        val ok = repo.deleteQuiz(id)
        if (ok) refreshQuizzes()
        return ok
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
            averageScore = 0.0,
            description = shared.description
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
    private var soundPool: SoundPool? = null
    private var soundCorrect = 0
    private var soundWrong = 0
    private var soundWin = 0
    private var soundBell = 0
    private var soundsLoaded = false

    /** Load sounds from MP3 files */
    private fun loadSounds() {
        if (soundsLoaded) return
        try {
            val app = getApplication<android.app.Application>()
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            soundCorrect = soundPool!!.load(app, R.raw.sound_correct, 1)
            soundWrong = soundPool!!.load(app, R.raw.sound_wrong, 1)
            soundWin = soundPool!!.load(app, R.raw.sound_win, 1)
            soundBell = soundPool!!.load(app, R.raw.sound_bell, 1)
            soundsLoaded = true
        } catch (_: Exception) {}
    }

    /** Play correct/wrong sound */
    fun playSound(correct: Boolean) {
        if (!soundOn) return
        loadSounds()
        try {
            val soundId = if (correct) soundCorrect else soundWrong
            if (soundId != 0) soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {}
    }

        /** Play win sound */
    fun playSuccessJingle() {
        if (!soundOn) return
        loadSounds()
        try {
            if (soundWin != 0) soundPool?.play(soundWin, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {}
    }

    /** Play bell sound */
    fun playBell() {
        if (!soundOn) return
        loadSounds()
        try {
            if (soundBell != 0) soundPool?.play(soundBell, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {}
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

    fun avatarRes(id: Int): Int = AVATAR_RESOURCES[id % AVATAR_COUNT]

    // ---------- leaderboard ----------

    suspend fun leaderboardEntries(metric: String): List<LeaderboardEntry> {
        return localLeaderboard(metric)
    }

    private fun localLeaderboard(metric: String): List<LeaderboardEntry> {
        val selfId = profile?.id
        val raw = repo.getAllProfiles().map { p ->
            val attempts = repo.getAttempts(p.id)
            val answered = attempts.sumOf { it.totalQuestions }
            val correct = attempts.sumOf { it.correctAnswers }
            val acc = if (answered > 0) correct * 100 / answered else 0
            LeaderboardEntry(rank = 0, username = p.username, avatarId = p.avatarId,
                level = p.level, xp = p.xp, accuracy = acc, attempts = attempts.size, isSelf = p.id == selfId)
        }
        val sorted = when (metric) {
            "Accuracy" -> raw.sortedWith(compareByDescending<LeaderboardEntry> { it.accuracy }.thenByDescending { it.xp })
            else -> raw.sortedWith(compareByDescending<LeaderboardEntry> { it.xp }.thenByDescending { it.accuracy })
        }
        val ranked = sorted.mapIndexed { i, e -> e.copy(rank = i + 1) }
        val top = ranked.take(20)
        val self = ranked.firstOrNull { it.isSelf }
        return if (self != null && self.rank > 20) top + self else top
    }


    // ---------- community quizzes ----------

    var communityLoading by mutableStateOf(false)
        private set
    var communityError by mutableStateOf<String?>(null)
        private set
    var communityImportResult by mutableStateOf<String?>(null)
        private set

    suspend fun fetchCommunityQuizzes(): Result<List<com.quizforge.app.data.CommunityQuiz>> {
        communityLoading = true
        communityError = null
        // Try cached first for instant response
        val result = communityQuizManager.fetchAndImport()
        communityLoading = false
        result.onFailure { communityError = it.message }
        return result
    }

    fun importCommunityQuiz(quiz: com.quizforge.app.data.CommunityQuiz) {
        val pid = profile?.id ?: return
        viewModelScope.launch {
            val count = communityQuizManager.importToDatabase(listOf(quiz), repo, pid)
            communityImportResult = if (count > 0) "Imported: ${quiz.title}" else "Already imported"
        }
    }

    companion object {
        // Internet anime avatars (fetched from free APIs)
        val ANIME_AVATARS = listOf(
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Felix",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Luna",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Max",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Bella",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Charlie",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Daisy",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Eddie",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Fiona",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=George",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Hannah",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Ivan",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Julia",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Kevin",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Lily",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Mike",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Nina",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Oscar",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Penny",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Quinn",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Rachel",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Steve",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Tina",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Uma",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Victor",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Aria",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Bolt",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Cora",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Dax",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Echo",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Flint",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Gia",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Hiro",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Iris",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Jax",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Kira",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Leo",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Maya",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Neo",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Ori",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Piper",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Rio",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Sky",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Tara",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Uma",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Vera",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Wren",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Xio",
            "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=Zara"
        )

        // Memes/GIFs PFP options
        val MEME_AVATARS = listOf(
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Happy",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Cool",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Party",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Love",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Star",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Fire",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Rocket",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Crown",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Rainbow",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Moon",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Sun",
            "https://api.dicebear.com/7.x/fun-emoji/svg?seed=Lightning"
        )

        // All avatars combined (for backward compatibility)
        val AVATARS = ANIME_AVATARS + MEME_AVATARS
        val AVATAR_COUNT = AVATARS.size

        val AVATAR_RESOURCES = listOf(
            R.drawable.avatar_01, R.drawable.avatar_02, R.drawable.avatar_03,
            R.drawable.avatar_04, R.drawable.avatar_05, R.drawable.avatar_06,
            R.drawable.avatar_07, R.drawable.avatar_08, R.drawable.avatar_09,
            R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
            R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15,
            R.drawable.avatar_16, R.drawable.avatar_17, R.drawable.avatar_18,
            R.drawable.avatar_19, R.drawable.avatar_20, R.drawable.avatar_21,
            R.drawable.avatar_22, R.drawable.avatar_23, R.drawable.avatar_24
        )
    }

    fun avatarUrl(id: Int): String = ANIME_AVATARS[id % ANIME_AVATARS.size]


}
