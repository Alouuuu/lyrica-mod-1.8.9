package com.tx24.spicyplayer.lyrics.spicy.models

/**
 * Represents a single word or syllable group in the lyrics.
 */
data class Word(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    /** Internal flag indicating if this is part of a larger word (UI specific). */
    val isPartOfWord: Boolean = false,
    /** If true, the word contains individual [Letter] timings for syllabic highlighting. */
    val isLetterGroup: Boolean = false,
    val letters: List<Letter> = emptyList(),
    /**
     * Romanized/transliterated form of [text], if available. Preferred from TTML/API
     * metadata; otherwise filled by on-device romanization. When romanization is
     * displayed this replaces [text] for layout and highlighting.
     */
    val romanizedText: String? = null,
    /** Reserved for a future translation feature; not yet displayed. */
    val translatedText: String? = null,
) {
    /** The duration of the word in milliseconds, guaranteed to be at least 1ms. */
    val duration: Long
        get() = (endMs - startMs).coerceAtLeast(1)
}
