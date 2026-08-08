package com.quizforge.app.ui

import com.quizforge.app.BuildConfig

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import com.quizforge.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizforge.app.data.AppSettings
import com.quizforge.app.data.Attempt
import com.quizforge.app.data.AttemptResult
import com.quizforge.app.data.Badge
import com.quizforge.app.data.LeaderboardEntry
import com.quizforge.app.data.Profile
import com.quizforge.app.data.Quiz
import com.quizforge.app.data.Question
import com.quizforge.app.data.QuizRepository
import com.quizforge.app.data.RemoteApi
import com.quizforge.app.data.SettingsRepo
import com.quizforge.app.data.STATUS_DRAFT
import com.quizforge.app.data.STATUS_PUBLISHED
import com.quizforge.app.logic.XpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

/** Cached snapshot of everything the Home tab renders. */
class HomeData(
    val todayCount: Int,
    val totalAttempts: Int,
    val quizzes: List<Quiz>,
    val questionCounts: Map<String, Int>,
    val totalTime: Long,
    val streak: Int,
    val recentBadges: List<String>,
    val weeklyAccuracy: Float,
    val weakAreas: List<String>
)

/** Cached snapshot of everything the Profile tab renders. */
class ProfileData(
    val attempts: List<Attempt>,
    val quizzes: List<Quiz>,
    val totalTime: Long,
    val diffStats: Map<String, Pair<Int, Int>>,
    val badges: List<Badge>
)

/** Cached snapshot of everything the Stats tab renders. */
class StatsData(
    val attempts: List<Attempt>,
    val categories: Map<String, String>,
    val quizTitles: Map<String, String>
)

/** Cached snapshot of everything the Quizzes tab renders. */
class QuizListData(
    val quizzes: List<Quiz>,
    val questionCounts: Map<String, Int>
)

/** Runs a DB read off the main thread and returns its result. */
private suspend fun<T> ioLoad(block: () -> T): T = withContext(Dispatchers.IO) { block() }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = QuizRepository(app)
    private val settingsRepo = SettingsRepo(app)
    private val remoteApi = RemoteApi()

    val settings: Flow<AppSettings> = settingsRepo.settings

    var profile by mutableStateOf<Profile?>(null)
        private set

    // For dashboard recomposition we re-read these on each navigation refresh.
    var quizzes by mutableStateOf<List<Quiz>>(emptyList())
        private set

    /** Cached tab data — loaded off the main thread so tab switches stay instant. */
    var homeData by mutableStateOf<HomeData?>(null)
        private set
    var profileData by mutableStateOf<ProfileData?>(null)
        private set
    var statsData by mutableStateOf<StatsData?>(null)
        private set
    var quizListData by mutableStateOf<QuizListData?>(null)
        private set

    /** Flags so repeated calls don't spawn duplicate loads for the same profile. */
    private var loadedHomeFor = -1L
    private var loadedProfileFor = -1L
    private var loadedStatsFor = -1L
    private var loadedQuizzesFor = -1L

    /** In-app notification banner shown when new community quizzes are available. */
    var syncNotice by mutableStateOf<String?>(null)
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
        viewModelScope.launch { startSyncLoop() }
    }

    private suspend fun startSyncLoop() {
        val deviceId = settingsRepo.getDeviceId()
        var lastSync = 0L
        while (true) {
            remoteApi.sendHeartbeat(deviceId)
            val now = System.currentTimeMillis()
            if (now - lastSync > 5 * 60_000L) {
                syncRemoteQuizzesNow()
                lastSync = now
            }
            kotlinx.coroutines.delay(25_000)
        }
    }

    /** Fetch community quizzes from the server (used on app start / manual refresh). */
    fun syncRemoteQuizzes() {
        viewModelScope.launch { syncRemoteQuizzesNow() }
    }

    private suspend fun syncRemoteQuizzesNow() {
        val pid = profile?.id ?: return
        try {
            val remote = remoteApi.fetchQuizzes()
            if (remote.isEmpty()) return
            val added = repo.syncRemoteQuizzes(pid, remote)
            if (added.isNotEmpty()) {
                syncNotice = if (added.size == 1) "New community quiz added!"
                else "${added.size} new community quizzes added!"
            }
            refreshQuizzes()
            invalidateData()
        } catch (_: Exception) {
            // offline — keep existing data
        }
    }

    fun dismissSyncNotice() {
        syncNotice = null
    }

    fun refreshProfile() {
        profile = repo.getActiveProfile()
    }

    fun refreshQuizzes() {
        profile?.let { quizzes = repo.getQuizzes(it.id) }
    }

    // ---------- async tab data (off main thread) ----------

    private fun loadHome(pid: Long) = viewModelScope.launch {
        val data = ioLoad {
            val attempts = repo.getAttempts(pid)
            val q = (repo.getQuizzes(pid, STATUS_PUBLISHED) + repo.getQuizzes(pid, STATUS_DRAFT))
                .sortedByDescending { it.updatedAt }
            val daySet = attempts.map { XpEngine.dateStr(it.attemptedAt) }.toSet()
            val weekAgo = System.currentTimeMillis() - 7L * 86400000
            val weekAttempts = attempts.filter { it.attemptedAt >= weekAgo }
            HomeData(
                todayCount = repo.getAttemptCountToday(pid),
                totalAttempts = attempts.size,
                quizzes = q,
                questionCounts = q.associate { it.id to repo.getQuestions(it.id).size },
                totalTime = repo.totalTimeSpent(pid),
                streak = XpEngine.currentStreak(daySet),
                recentBadges = repo.getBadges(pid).takeLast(3).map { it.badgeName },
                weeklyAccuracy = if (weekAttempts.isEmpty()) 0f
                else weekAttempts.sumOf { it.correctAnswers }.toFloat() / weekAttempts.sumOf { it.totalQuestions }.coerceAtLeast(1),
                weakAreas = repo.categoryAccuracy(pid)
                    .filter { (c, p) -> p.second >= 3 && p.first * 100 / p.second < 60 }
                    .map { it.key }
                    .take(3)
            )
        }
        homeData = data
    }

    private fun loadProfile(pid: Long) = viewModelScope.launch {
        val data = ioLoad {
            ProfileData(
                attempts = repo.getAttempts(pid),
                quizzes = repo.getQuizzes(pid),
                totalTime = repo.totalTimeSpent(pid),
                diffStats = repo.difficultyStats(pid),
                badges = repo.getBadges(pid)
            )
        }
        profileData = data
    }

    private fun loadStats(pid: Long) = viewModelScope.launch {
        val data = ioLoad {
            StatsData(
                attempts = repo.getAttempts(pid),
                categories = repo.getQuizCategoriesById(),
                quizTitles = repo.getQuizTitlesById()
            )
        }
        statsData = data
    }

    private fun loadQuizList(pid: Long) = viewModelScope.launch {
        val data = ioLoad {
            val q = repo.getQuizzes(pid)
            QuizListData(
                quizzes = q,
                questionCounts = q.associate { it.id to repo.getQuestions(it.id).size }
            )
        }
        quizListData = data
    }

    /** Ensure Home cache is loaded (fast on repeat visits). */
    fun ensureHomeLoaded() {
        profile?.let { p ->
            if (homeData == null || loadedHomeFor != p.id) {
                loadedHomeFor = p.id
                loadHome(p.id)
            }
        }
    }

    /** Ensure Profile cache is loaded. */
    fun ensureProfileLoaded() {
        profile?.let { p ->
            if (profileData == null || loadedProfileFor != p.id) {
                loadedProfileFor = p.id
                loadProfile(p.id)
            }
        }
    }

    /** Ensure Stats cache is loaded. */
    fun ensureStatsLoaded() {
        profile?.let { p ->
            if (statsData == null || loadedStatsFor != p.id) {
                loadedStatsFor = p.id
                loadStats(p.id)
            }
        }
    }

    fun ensureQuizListLoaded() {
        profile?.let { p ->
            if (quizListData == null || loadedQuizzesFor != p.id) {
                loadedQuizzesFor = p.id
                loadQuizList(p.id)
            }
        }
    }

    /** Invalidate caches so the next tab visit reloads fresh data. */
    fun invalidateData() {
        homeData = null
        profileData = null
        statsData = null
        quizListData = null
    }

    // ---------- settings ----------

    fun setThemeMode(mode: String) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setSoundEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setSoundEnabled(v) }
    fun setHapticsEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setHapticsEnabled(v) }
    fun setAutoUpdateCheck(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoUpdateCheck(v) }
    fun setUpdateNotifications(v: Boolean) = viewModelScope.launch { settingsRepo.setUpdateNotifications(v) }

    // ---------- profile ----------

    fun createProfile(username: String, avatarId: Int, status: String, bio: String): Profile {
        val p = repo.createProfile(username, avatarId, status, bio)
        profile = p
        refreshQuizzes()
        invalidateData()
        return p
    }

    fun updateProfileStatus(bio: String, status: String) {
        profile?.let { p ->
            val np = p.copy(bio = bio, status = status)
            repo.updateProfile(np)
            profile = np
            invalidateData()
        }
    }

    fun renameProfile(name: String) {
        profile?.let { p ->
            val np = p.copy(username = name)
            repo.updateProfile(np)
            profile = np
            invalidateData()
        }
    }

    fun switchProfile(id: Long) {
        repo.switchProfile(id)
        refreshProfile()
        refreshQuizzes()
        invalidateData()
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
            invalidateData()
        }
    }

    fun avatarCount(): Int = AVATARS.size

    fun toggleBookmark(questionId: String) = repo.toggleBookmark(questionId)

    fun resetAll() {
        repo.resetAll()
        profile = null
        quizzes = emptyList()
        invalidateData()
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
        invalidateData()
        return quiz
    }

    fun updateQuiz(quiz: Quiz, questions: List<Question>) {
        repo.updateQuizMeta(quiz)
        repo.replaceQuestions(quiz.id, questions.mapIndexed { i, q ->
            q.copy(id = if (q.id.isEmpty()) UUID.randomUUID().toString() else q.id, quizId = quiz.id, position = i)
        })
        refreshQuizzes()
        invalidateData()
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
        invalidateData()
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
        invalidateData()
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
        invalidateData()
        return finalResult
    }

    // ---------- audio / haptics ----------

    private var soundOn = true
    private var hapticsOn = true
    private val appContext: Context = getApplication()
    private var soundPool: SoundPool? = null
    private var soundCorrectId = 0
    private var soundWrongId = 0
    private var soundWin = 0
    private var soundBell = 0

    private fun ensureSoundPool(): SoundPool? {
        soundPool?.let { return it }
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            soundCorrectId = sp.load(appContext, R.raw.sound_correct, 1)
            soundWrongId = sp.load(appContext, R.raw.sound_wrong, 1)
            soundWin = sp.load(appContext, R.raw.sound_win, 1)
            soundBell = sp.load(appContext, R.raw.sound_bell, 1)
            soundPool = sp
            sp
        } catch (_: Exception) {
            null
        }
    }

    /** Real sound effect for correct/wrong answers (crisp chime vs buzzer). */
    fun playSound(correct: Boolean) {
        if (!soundOn) return
        val sp = ensureSoundPool() ?: return
        try {
            sp.play(if (correct) soundCorrectId else soundWrongId, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {
        }
    }

    /** Upbeat victory fanfare used when a quiz is completed with a good score. */
    fun playSuccessJingle() {
        if (!soundOn) return
        val sp = ensureSoundPool() ?: return
        try {
            sp.play(soundWin, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {
        }
    }

    /** Gentle bell tone — e.g. UX accent / notification ding. */
    fun playBell() {
        if (!soundOn) return
        val sp = ensureSoundPool() ?: return
        try {
            sp.play(soundBell, 1f, 1f, 1, 0, 1f)
        } catch (_: Exception) {
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
        try { soundPool?.release() } catch (_: Exception) {
        }
        super.onCleared()
    }

    // ---------- misc ----------

    fun avatarEmoji(id: Int): String = AVATARS[id % AVATARS.size]

    // ---------- leaderboard ----------

    /**
     * Ranks device profiles by XP (or accuracy). When the online login system is
     * turned on (settingsRepo.setLeaderboardOnline(true)), this switches to the
     * global server leaderboard.
     */
    suspend fun leaderboardEntries(metric: String): List<LeaderboardEntry> {
        val online = settingsRepo.settings.first().leaderboardOnline
        if (!online) {
            return localLeaderboard(metric)
        }
        // TODO(login): fetch the global leaderboard from the server once the online
        // login system ships. Until then the screen shows an empty "coming soon" state.
        return emptyList()
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

    // ---------- updates ----------

    /** Checks GitHub for the newest release. Returns (tag, html url) or null on failure. */
    suspend fun latestRelease(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/SirYadav1/quizforge/releases/latest").openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "QuizForge")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(body)
            val tag = json.optString("tag_name", "").trim().removePrefix("v")
            val url = json.optString("html_url", "")
            if (tag.isEmpty()) null else tag to url
        } catch (_: Exception) {
            null
        }
    }

    /** Result of an update check. */
    data class UpdateInfo(val available: Boolean, val remoteVersion: String = "", val url: String = "")

    /** Compares GitHub's latest release with the installed version. */
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val release = latestRelease() ?: return@withContext UpdateInfo(false)
        val (tag, url) = release
        val current = BuildConfig.VERSION_NAME
        UpdateInfo(isNewerThan(current, tag), tag, url)
    }

    private fun isNewerThan(current: String, remote: String): Boolean {
        val a = current.split('.').map { it.toIntOrNull() ?: 0 }
        val b = remote.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (y != x) return y > x
        }
        return false
    }

    /** Posts a system notification that a new version is available on the releases page. */
    fun notifyUpdateAvailable() {
        try {
            val app = getApplication<Application>()
            val nm = app.getSystemService(android.app.NotificationManager::class.java)
            nm.createNotificationChannel(
                android.app.NotificationChannel("updates", "Update notifications", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            )
            val openReleases = android.app.PendingIntent.getActivity(
                app, 0,
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/SirYadav1/quizforge/releases")),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val notif = androidx.core.app.NotificationCompat.Builder(app, "updates")
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("QuizForge update available")
                .setContentText("A new version is out — tap to open the release page")
                .setContentIntent(openReleases)
                .setAutoCancel(true)
                .build()
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    app, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.NotificationManagerCompat.from(app).notify(42, notif)
            }
        } catch (_: Exception) {
        }
    }

    /** Silent launch-time check: if a newer GitHub version exists and notifications are on, notify. */
    fun checkForUpdatesAtLaunch() {
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            if (!s.autoUpdateCheck || !s.updateNotifications) return@launch
            val info = checkForUpdates()
            if (info.available) notifyUpdateAvailable()
        }
    }

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
