package com.kvizo.app.data

object CommunityKeys {
    const val CURRENT_KEY_ID = "kvizo-pub-2026-09"
    const val CURRENT_PUBLIC_KEY = "edca7daf5e8222d97918ed950441dcdbe295e5c41d23f9a5da665081323a20c5"

    const val PREV_KEY_ID = "kvizo-pub-2024-01"
    const val PREV_PUBLIC_KEY = "120f69eae51f05b9c73ef14a0848080d096904f0db9f8d8a3506e85b5a29aee1"

    val TRUSTED_KEYS = mapOf(
        CURRENT_KEY_ID to CURRENT_PUBLIC_KEY,
        PREV_KEY_ID to PREV_PUBLIC_KEY
    )

    private const val GITHUB_RAW = "https://raw.githubusercontent.com/SirYadav1/kvizo-community/master"
    const val COMMUNITY_FILE = "community.json"
    const val COMMUNITY_JSON_URL = "$GITHUB_RAW/community.json"
    const val COMMUNITY_SIG_URL = "$GITHUB_RAW/community.json.sig"
    const val NOTIFICATIONS_JSON_URL = "$GITHUB_RAW/notifications.json"
    const val NOTIFICATIONS_SIG_URL = "$GITHUB_RAW/notifications.json.sig"

    const val CACHE_FILE = "community_quiz_cache.json"
    const val CACHE_SIG_FILE = "community_quiz_cache.sig"
    const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L
}
