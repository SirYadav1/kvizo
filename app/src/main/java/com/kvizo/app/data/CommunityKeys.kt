package com.kvizo.app.data

/**
 * Trust anchors for community content.
 *
 * The private half of [CURRENT_PUBLIC_KEY] exists only as the `ED25519_PRIVATE_KEY` secret inside
 * the `SirYadav1/kvizo-community` GitHub repository, where the signing Action uses it. It is never
 * in the app and never in the repository, so neither a leaked repo nor a hostile CDN can make the
 * app show a quiz that was not published by the owner.
 *
 * Rotating this key means shipping a new app build, so it is deliberately a single, explicit
 * constant rather than a collection of keys nobody remembers the provenance of.
 */
object CommunityKeys {

    const val CURRENT_KEY_ID = "kvizo-pub-2026-09"

    /** Raw 32-byte Ed25519 public key as hex — same value as `public_key.hex` in the content repo. */
    const val CURRENT_PUBLIC_KEY = "bbbd809c2cf94f734f50868f5d6fd447d13526373539f21778ec145f970ef9c0"

    /** Key id -> raw public key. The manifest's `key_id` selects which one must have signed it. */
    val TRUSTED_KEYS: Map<String, String> = mapOf(CURRENT_KEY_ID to CURRENT_PUBLIC_KEY)

    /**
     * Free, static mirrors of the same signed bytes ($0, no server, nothing to deploy).
     * The first one that verifies wins; the mirror exists so one CDN being down or blocked
     * does not take community quizzes with it.
     */
    val SOURCES: List<String> = listOf(
        "https://raw.githubusercontent.com/SirYadav1/kvizo-community/master",
        "https://cdn.jsdelivr.net/gh/SirYadav1/kvizo-community@master",
    )

    const val MANIFEST_FILE = "manifest.json"
    const val COMMUNITY_FILE = "community.json"

    const val CACHE_DIR = "community"
    const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L
}
