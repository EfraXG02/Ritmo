package com.example.model

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class ArtistShuffleVibe(val label: String, val description: String) {
    ALL("Todas as Músicas", "Mix completo e aleatório de toda a discografia"),
    TOP_HITS("Maiores Sucessos", "As músicas mais tocadas e conhecidas"),
    DEEP_CUTS("Lado B & Raras", "Faixas menos conhecidas e tesouros escondidos"),
    ACOUSTIC("Acústico & Calmo", "Versões suaves, acústicas e relaxantes"),
    HIGH_ENERGY("Energético & Ao Vivo", "Faixas dançantes, com ritmo forte e ao vivo")
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val audioUrl: String = "",
    val durationMs: Long = 180000L,
    val genre: String = "Pop",
    val isFavorite: Boolean = false,
    val isLocal: Boolean = false,
    val lyricsSnippet: String = "",
    val releaseYear: String = "2024"
)

data class ArtistInfo(
    val name: String,
    val genre: String,
    val photoUrl: String,
    val trackCount: Int,
    val bio: String,
    val accentColorHex: Long = 0xFFA855F7
)

data class ArtistShuffleHistory(
    val artistName: String,
    val timestamp: Long,
    val songCount: Int,
    val vibe: ArtistShuffleVibe = ArtistShuffleVibe.ALL
)

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val speed: Float = 1.0f,
    val sleepTimerMinutesLeft: Int = 0,
    val currentArtistShuffleName: String? = null,
    val currentVibe: ArtistShuffleVibe = ArtistShuffleVibe.ALL,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val visualizerLevels: List<Float> = List(24) { 0.1f }
)
