package com.quizforge.app.data

/**
 * Trusted Ed25519 public keys for verifying community quiz signatures.
 * 
 * SECURITY: Private key is kept OFFLINE (never in repo).
 * Public keys are hardcoded here for verification.
 * 
 * Key Rotation:
 * - When rotating, add new key as CURRENT, move old to PREV
 * - PREV key stays trusted for 30 days (grace period)
 * - Remove PREV key after grace period
 */
object CommunityKeys {
    // Current key (Ed25519 public key, 32 bytes hex)
    const val CURRENT_KEY_ID = "kvizo-pub-2024-01"
    const val CURRENT_PUBLIC_KEY = "120f69eae51f05b9c73ef14a0848080d096904f0db9f8d8a3506e85b5a29aee1"

    // Previous key (grace period for rotation)
    const val PREV_KEY_ID = "kvizo-pub-2023-12"
    const val PREV_PUBLIC_KEY = "0000000000000000000000000000000000000000000000000000000000000000"

    // Trusted key list (for future rotation)
    val TRUSTED_KEYS = mapOf(
        CURRENT_KEY_ID to CURRENT_PUBLIC_KEY,
        PREV_KEY_ID to PREV_PUBLIC_KEY
    )

    // Community quiz URLs (GitHub Pages)
    const val COMMUNITY_JSON_URL = "https://kvizo.indevs.in/community.json"
    const val COMMUNITY_SIG_URL = "https://kvizo.indevs.in/community.sig"
    
    // Cache settings
    const val CACHE_FILE = "community_quiz_cache.json"
    const val CACHE_SIG_FILE = "community_quiz_cache.sig"
    const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
}
