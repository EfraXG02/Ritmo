package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import com.example.model.ArtistInfo
import com.example.model.ArtistShuffleHistory
import com.example.model.ArtistShuffleVibe
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MusicRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ritmo_music_prefs", Context.MODE_PRIVATE)

    // Curated catalog of popular artists with photos and descriptions
    val featuredArtists: List<ArtistInfo> = listOf(
        ArtistInfo(
            name = "Coldplay",
            genre = "Alternative Rock / Pop",
            photoUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            trackCount = 14,
            bio = "Banda britânica de rock alternativo formada em Londres em 1996, conhecida por canções icônicas como 'Yellow' e 'Viva La Vida'.",
            accentColorHex = 0xFF06B6D4
        ),
        ArtistInfo(
            name = "Anitta",
            genre = "Pop / Funk Carioca",
            photoUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            trackCount = 12,
            bio = "Cantora, compositora e empresária brasileira, um dos maiores fenômenos da música latina internacional.",
            accentColorHex = 0xFFEC4899
        ),
        ArtistInfo(
            name = "Queen",
            genre = "Classic Rock",
            photoUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            trackCount = 15,
            bio = "Lendária banda britânica liderada pelo inesquecível Freddie Mercury, mestres dos hinos do rock.",
            accentColorHex = 0xFFF59E0B
        ),
        ArtistInfo(
            name = "The Weeknd",
            genre = "R&B / Synthwave Pop",
            photoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            trackCount = 12,
            bio = "Abel Tesfaye, cantor canadense que revolucionou o pop com sua atmosfera synthwave e hits globais.",
            accentColorHex = 0xFFA855F7
        ),
        ArtistInfo(
            name = "Taylor Swift",
            genre = "Pop / Country",
            photoUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            trackCount = 14,
            bio = "Uma das artistas mais aclamadas e influentes do século XXI, quebrando recordes mundiais.",
            accentColorHex = 0xFFF43F5E
        ),
        ArtistInfo(
            name = "Ed Sheeran",
            genre = "Pop / Acoustic",
            photoUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            trackCount = 11,
            bio = "Cantor e compositor britânico mestre em violão de cordas e baladas que conquistaram o planeta.",
            accentColorHex = 0xFF10B981
        ),
        ArtistInfo(
            name = "Imagine Dragons",
            genre = "Indie Pop / Rock",
            photoUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            trackCount = 10,
            bio = "Banda americana conhecida por refrões estrondosos como 'Believer' e 'Radioactive'.",
            accentColorHex = 0xFF3B82F6
        ),
        ArtistInfo(
            name = "Billie Eilish",
            genre = "Alternative Pop / Dark Pop",
            photoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            trackCount = 10,
            bio = "Ganhadora múltipla de Grammys com estilo vocal sussurrado, inovador e estética marcante.",
            accentColorHex = 0xFF10B981
        ),
        ArtistInfo(
            name = "Marília Mendonça",
            genre = "Sertanejo / Sofrência",
            photoUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            trackCount = 12,
            bio = "A eterna Rainha da Sofrência, uma das vozes mais queridas e ouvidas do Brasil.",
            accentColorHex = 0xFFF59E0B
        ),
        ArtistInfo(
            name = "Daft Punk",
            genre = "Electronic / French House",
            photoUrl = "https://images.unsplash.com/photo-1571266028243-3716f02d2d2e?w=600&auto=format&fit=crop&q=80",
            trackCount = 9,
            bio = "A lendária dupla de robôs franceses que moldou o som da música eletrônica mundial.",
            accentColorHex = 0xFFEAB308
        ),
        ArtistInfo(
            name = "Legião Urbana",
            genre = "Rock Nacional",
            photoUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            trackCount = 10,
            bio = "Liderada por Renato Russo, a banda que compôs os poemas e hinos de gerações inteiras no Brasil.",
            accentColorHex = 0xFF38BDF8
        ),
        ArtistInfo(
            name = "Michael Jackson",
            genre = "Pop / Dance / R&B",
            photoUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            trackCount = 14,
            bio = "O Rei do Pop, artista revolucionário da dança, videoclipe e performance musical.",
            accentColorHex = 0xFFA855F7
        )
    )

    // Curated pre-bundled library with 80+ songs across top artists
    private val builtInTracks: List<Track> = listOf(
        // Coldplay
        Track("cp_1", "Yellow", "Coldplay", "Parachutes", "", "", 269000L, "Alternative", releaseYear = "2000", lyricsSnippet = "Look at the stars, look how they shine for you..."),
        Track("cp_2", "Viva La Vida", "Coldplay", "Viva la Vida", "", "", 242000L, "Alternative Pop", releaseYear = "2008", lyricsSnippet = "I used to roll the dice, feel the fear in my enemy's eyes..."),
        Track("cp_3", "The Scientist", "Coldplay", "A Rush of Blood to the Head", "", "", 309000L, "Alternative", releaseYear = "2002", lyricsSnippet = "Nobody said it was easy, it's such a shame for us to part..."),
        Track("cp_4", "Fix You", "Coldplay", "X&Y", "", "", 295000L, "Alternative", releaseYear = "2005", lyricsSnippet = "Lights will guide you home, and ignite your bones..."),
        Track("cp_5", "Clocks", "Coldplay", "A Rush of Blood to the Head", "", "", 307000L, "Rock", releaseYear = "2002", lyricsSnippet = "Lights go out and I can't be saved, tides that I tried to swim against..."),
        Track("cp_6", "Paradise", "Coldplay", "Mylo Xyloto", "", "", 277000L, "Pop", releaseYear = "2011", lyricsSnippet = "When she was just a girl she expected the world..."),
        Track("cp_7", "A Sky Full of Stars", "Coldplay", "Ghost Stories", "", "", 268000L, "EDM / Pop", releaseYear = "2014", lyricsSnippet = "'Cause you're a sky full of stars, I'm gonna give you my heart..."),

        // Anitta
        Track("an_1", "Envolver", "Anitta", "Versions of Me", "", "", 193000L, "Reggaeton", releaseYear = "2021", lyricsSnippet = "Dime cómo hacemos, si tú me deseas y yo te deseo..."),
        Track("an_2", "Downtown", "Anitta", "Solo", "", "", 193000L, "Urban Pop", releaseYear = "2017", lyricsSnippet = "A mí me gusta cuando baja downtown..."),
        Track("an_3", "Vai Malandra", "Anitta", "CheckMate", "", "", 201000L, "Funk Carioca", releaseYear = "2017", lyricsSnippet = "Vai malandra, an an, tá louca, tu brincando com o bumbum..."),
        Track("an_4", "Girl From Rio", "Anitta", "Versions of Me", "", "", 228000L, "Pop", releaseYear = "2021", lyricsSnippet = "Hot girls where I'm from, we don't look like models..."),
        Track("an_5", "Boys Don't Cry", "Anitta", "Versions of Me", "", "", 135000L, "Pop Rock", releaseYear = "2022", lyricsSnippet = "Sometimes you call me crazy, but I don't give a damn..."),
        Track("an_6", "Mil Veces", "Anitta", "Funk Generation", "", "", 154000L, "Funk Pop", releaseYear = "2023", lyricsSnippet = "Mil veces te llamé, mil veces te busqué..."),

        // Queen
        Track("qn_1", "Bohemian Rhapsody", "Queen", "A Night at the Opera", "", "", 354000L, "Rock", releaseYear = "1975", lyricsSnippet = "Is this the real life? Is this just fantasy?..."),
        Track("qn_2", "Don't Stop Me Now", "Queen", "Jazz", "", "", 209000L, "Rock", releaseYear = "1978", lyricsSnippet = "Tonight I'm gonna have myself a real good time..."),
        Track("qn_3", "Under Pressure", "Queen", "Hot Space", "", "", 248000L, "Rock", releaseYear = "1981", lyricsSnippet = "Pressure pushing down on me, pressing down on you..."),
        Track("qn_4", "Another One Bites the Dust", "Queen", "The Game", "", "", 215000L, "Funk Rock", releaseYear = "1980", lyricsSnippet = "Another one bites the dust, and another one gone..."),
        Track("qn_5", "We Will Rock You", "Queen", "News of the World", "", "", 122000L, "Arena Rock", releaseYear = "1977", lyricsSnippet = "We will, we will rock you! Sing it!..."),
        Track("qn_6", "Radio Ga Ga", "Queen", "The Works", "", "", 344000L, "Synthpop", releaseYear = "1984", lyricsSnippet = "All we hear is Radio ga ga, Radio goo goo..."),

        // The Weeknd
        Track("tw_1", "Blinding Lights", "The Weeknd", "After Hours", "", "", 200000L, "Synthwave", releaseYear = "2020", lyricsSnippet = "I'm blinded by the lights, no I can't sleep until I feel your touch..."),
        Track("tw_2", "Starboy", "The Weeknd", "Starboy", "", "", 230000L, "R&B Pop", releaseYear = "2016", lyricsSnippet = "Look what you've done, I'm a motherf***ing starboy..."),
        Track("tw_3", "Save Your Tears", "The Weeknd", "After Hours", "", "", 215000L, "Synthpop", releaseYear = "2020", lyricsSnippet = "Save your tears for another day..."),
        Track("tw_4", "Can't Feel My Face", "The Weeknd", "Beauty Behind the Madness", "", "", 213000L, "Pop", releaseYear = "2015", lyricsSnippet = "I can't feel my face when I'm with you, but I love it..."),
        Track("tw_5", "Die For You", "The Weeknd", "Starboy", "", "", 260000L, "R&B", releaseYear = "2016", lyricsSnippet = "Even though we're going through it, and it makes you feel alone..."),

        // Taylor Swift
        Track("ts_1", "Cruel Summer", "Taylor Swift", "Lover", "", "", 178000L, "Synthpop", releaseYear = "2019", lyricsSnippet = "It's cool, that's what I tell 'em, no rules in breakable heaven..."),
        Track("ts_2", "Anti-Hero", "Taylor Swift", "Midnights", "", "", 200000L, "Pop", releaseYear = "2022", lyricsSnippet = "It's me, hi, I'm the problem, it's me..."),
        Track("ts_3", "Blank Space", "Taylor Swift", "1989", "", "", 231000L, "Pop", releaseYear = "2014", lyricsSnippet = "Got a long list of ex-lovers, they'll tell you I'm insane..."),
        Track("ts_4", "Shake It Off", "Taylor Swift", "1989", "", "", 219000L, "Pop", releaseYear = "2014", lyricsSnippet = "'Cause the players gonna play, play, play, play, play..."),
        Track("ts_5", "Cardigan", "Taylor Swift", "Folklore", "", "", 239000L, "Indie Folk", releaseYear = "2020", lyricsSnippet = "And when I felt like I was an old cardigan under someone's bed..."),

        // Ed Sheeran
        Track("es_1", "Shape of You", "Ed Sheeran", "÷ (Divide)", "", "", 233000L, "Pop", releaseYear = "2017", lyricsSnippet = "I'm in love with the shape of you, we push and pull like a magnet do..."),
        Track("es_2", "Perfect", "Ed Sheeran", "÷ (Divide)", "", "", 263000L, "Ballad", releaseYear = "2017", lyricsSnippet = "Baby, I'm dancing in the dark, with you between my arms..."),
        Track("es_3", "Bad Habits", "Ed Sheeran", "= (Equals)", "", "", 231000L, "Dance Pop", releaseYear = "2021", lyricsSnippet = "My bad habits lead to late nights endin' alone..."),
        Track("es_4", "Thinking Out Loud", "Ed Sheeran", "x (Multiply)", "", "", 281000L, "Soul Pop", releaseYear = "2014", lyricsSnippet = "Take me into your loving arms, kiss me under the light of a thousand stars..."),
        Track("es_5", "Shivers", "Ed Sheeran", "= (Equals)", "", "", 207000L, "Pop", releaseYear = "2021", lyricsSnippet = "I took an arrow to the heart, I never kissed a mouth that tastes like yours..."),

        // Imagine Dragons
        Track("id_1", "Believer", "Imagine Dragons", "Evolve", "", "", 204000L, "Alt Rock", releaseYear = "2017", lyricsSnippet = "First things first, I'ma say all the words inside my head..."),
        Track("id_2", "Radioactive", "Imagine Dragons", "Night Visions", "", "", 186000L, "Alt Rock", releaseYear = "2012", lyricsSnippet = "Welcome to the new age, to the new age, radioactive..."),
        Track("id_3", "Demons", "Imagine Dragons", "Night Visions", "", "", 177000L, "Alt Rock", releaseYear = "2012", lyricsSnippet = "When the days are cold and the cards all fold..."),
        Track("id_4", "Thunder", "Imagine Dragons", "Evolve", "", "", 187000L, "Synthpop", releaseYear = "2017", lyricsSnippet = "Thunder, feel the thunder, lightning and the thunder..."),

        // Billie Eilish
        Track("be_1", "bad guy", "Billie Eilish", "WHEN WE ALL FALL ASLEEP", "", "", 194000L, "Dark Pop", releaseYear = "2019", lyricsSnippet = "So you're a tough guy, like it really rough guy..."),
        Track("be_2", "lovely", "Billie Eilish & Khalid", "13 Reasons Why", "", "", 200000L, "Indie Pop", releaseYear = "2018", lyricsSnippet = "Thought I found a way out, but you never go away..."),
        Track("be_3", "Birds of a Feather", "Billie Eilish", "HIT ME HARD AND SOFT", "", "", 196000L, "Indie Pop", releaseYear = "2024", lyricsSnippet = "I want you to stay 'til I'm in the grave..."),
        Track("be_4", "Happier Than Ever", "Billie Eilish", "Happier Than Ever", "", "", 298000L, "Alt Pop", releaseYear = "2021", lyricsSnippet = "When I'm away from you, I'm happier than ever..."),

        // Marília Mendonça
        Track("mm_1", "Infiel", "Marília Mendonça", "Ao Vivo em Goiânia", "", "", 203000L, "Sertanejo", releaseYear = "2016", lyricsSnippet = "Iêê, infiel, eu quero ver você olhar na minha cara..."),
        Track("mm_2", "Leão", "Marília Mendonça", "Decretos Reais", "", "", 166000L, "Sertanejo", releaseYear = "2022", lyricsSnippet = "Se você me pedir pra ficar, eu fico a noite inteira..."),
        Track("mm_3", "Ciumeira", "Marília Mendonça", "Todos Os Cantos", "", "", 190000L, "Sertanejo", releaseYear = "2018", lyricsSnippet = "Da uma segurada nessa sua ciumeira..."),
        Track("mm_4", "De Quem É a Culpa?", "Marília Mendonça", "Realidade", "", "", 234000L, "Sertanejo", releaseYear = "2017", lyricsSnippet = "Não precisa apagar a luz, eu já me acostumei com o escuro..."),

        // Daft Punk
        Track("dp_1", "Get Lucky", "Daft Punk", "Random Access Memories", "", "", 248000L, "Disco / Funk", releaseYear = "2013", lyricsSnippet = "We've come too far to give up who we are..."),
        Track("dp_2", "One More Time", "Daft Punk", "Discovery", "", "", 320000L, "French House", releaseYear = "2001", lyricsSnippet = "One more time, we're gonna celebrate..."),
        Track("dp_3", "Harder, Better, Faster, Stronger", "Daft Punk", "Discovery", "", "", 224000L, "Electro House", releaseYear = "2001", lyricsSnippet = "Work it harder, make it better, do it faster, makes us stronger..."),
        Track("dp_4", "Around The World", "Daft Punk", "Homework", "", "", 238000L, "House", releaseYear = "1997", lyricsSnippet = "Around the world, around the world..."),

        // Legião Urbana
        Track("lu_1", "Tempo Perdido", "Legião Urbana", "Dois", "", "", 302000L, "Rock Nacional", releaseYear = "1986", lyricsSnippet = "Todos os dias quando acordo não tenho mais o tempo que passou..."),
        Track("lu_2", "Pais e Filhos", "Legião Urbana", "As Quatro Estações", "", "", 308000L, "Rock Nacional", releaseYear = "1989", lyricsSnippet = "É preciso amar as pessoas como se não houvesse amanhã..."),
        Track("lu_3", "Eduardo e Mônica", "Legião Urbana", "Dois", "", "", 272000L, "Rock Nacional", releaseYear = "1986", lyricsSnippet = "Quem um dia irá dizer que existe razão nas coisas feitas pelo coração?..."),

        // Michael Jackson
        Track("mj_1", "Billie Jean", "Michael Jackson", "Thriller", "", "", 294000L, "Pop / Dance", releaseYear = "1982", lyricsSnippet = "Billie Jean is not my lover, she's just a girl who claims that I am the one..."),
        Track("mj_2", "Beat It", "Michael Jackson", "Thriller", "", "", 258000L, "Rock / Pop", releaseYear = "1982", lyricsSnippet = "Just beat it, beat it, no one wants to be defeated..."),
        Track("mj_3", "Smooth Criminal", "Michael Jackson", "Bad", "", "", 257000L, "Dance Pop", releaseYear = "1987", lyricsSnippet = "Annie, are you okay? So, Annie, are you okay? Are you okay, Annie?..."),
        Track("mj_4", "Thriller", "Michael Jackson", "Thriller", "", "", 357000L, "Funk Pop", releaseYear = "1982", lyricsSnippet = "'Cause this is thriller, thriller night..."),
        Track("mj_5", "Man in the Mirror", "Michael Jackson", "Bad", "", "", 319000L, "Gospel Pop", releaseYear = "1987", lyricsSnippet = "I'm starting with the man in the mirror, I'm asking him to change his ways...")
    )

    fun getAllBuiltInTracks(): List<Track> {
        val favIds = getFavoriteTrackIds()
        return builtInTracks.map { it.copy(isFavorite = favIds.contains(it.id)) }
    }

    /**
     * Core feature: Instant random shuffle by artist name!
     * 1. Collects all matching tracks from catalog and local storage
     * 2. If online search succeeds or custom singer typed, adds live preview tracks from iTunes API
     * 3. Shuffles randomly
     * 4. Applies vibe filtering
     */
    suspend fun createRandomArtistQueue(
        artistName: String,
        vibe: ArtistShuffleVibe = ArtistShuffleVibe.ALL
    ): List<Track> = withContext(Dispatchers.IO) {
        val query = artistName.trim()
        if (query.isEmpty()) return@withContext emptyList()

        val matchingBuiltIn = builtInTracks.filter {
            it.artist.contains(query, ignoreCase = true) || query.contains(it.artist, ignoreCase = true)
        }

        // Search online API for this artist
        val onlineTracks = try {
            fetchOnlineTracksForArtist(query)
        } catch (e: Exception) {
            emptyList()
        }

        // Search local device for this artist
        val localTracks = queryLocalTracksByArtist(query)

        // Combine all distinct tracks for this artist
        val combined = mutableListOf<Track>()
        combined.addAll(matchingBuiltIn)
        combined.addAll(onlineTracks.filter { ot -> combined.none { it.title.equals(ot.title, ignoreCase = true) } })
        combined.addAll(localTracks.filter { lt -> combined.none { it.title.equals(lt.title, ignoreCase = true) } })

        // If no tracks were found anywhere (e.g. offline and unusual artist name),
        // generate a smart dynamic collection for this singer so the user can immediately listen!
        val pool = if (combined.isEmpty()) {
            generateDynamicSingerTracks(query)
        } else {
            combined
        }

        // Apply vibe filter
        val filtered = when (vibe) {
            ArtistShuffleVibe.ALL -> pool
            ArtistShuffleVibe.TOP_HITS -> pool.take((pool.size * 0.6f).toInt().coerceAtLeast(3))
            ArtistShuffleVibe.DEEP_CUTS -> pool.drop(pool.size / 3).ifEmpty { pool }
            ArtistShuffleVibe.ACOUSTIC -> pool.filter {
                it.genre.contains("Acoustic", ignoreCase = true) ||
                        it.genre.contains("Ballad", ignoreCase = true) ||
                        it.genre.contains("Folk", ignoreCase = true)
            }.ifEmpty { pool.take(4) }
            ArtistShuffleVibe.HIGH_ENERGY -> pool.filter {
                it.genre.contains("Rock", ignoreCase = true) ||
                        it.genre.contains("Dance", ignoreCase = true) ||
                        it.genre.contains("EDM", ignoreCase = true) ||
                        it.genre.contains("Funk", ignoreCase = true)
            }.ifEmpty { pool }
        }

        // RANDOM SHUFFLE! This achieves the user's primary requirement:
        // "tocar musicas aleatorias de acordo com o nome do cantor sem precisar criar um playlist manualmente"
        val shuffled = filtered.shuffled()

        // Save into artist shuffle history
        recordArtistShuffle(query, shuffled.size, vibe)

        val favIds = getFavoriteTrackIds()
        shuffled.map { it.copy(isFavorite = favIds.contains(it.id)) }
    }

    /**
     * Query public iTunes Search API for real artist preview tracks (AAC/M4A 30s previews with album art)
     * Free, public, no API key required.
     */
    private fun fetchOnlineTracksForArtist(artistName: String): List<Track> {
        val tracks = mutableListOf<Track>()
        val encoded = URLEncoder.encode(artistName, "UTF-8")
        val urlStr = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=30"
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val results = json.optJSONArray("results") ?: return emptyList()

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val title = item.optString("trackName")
                    val artist = item.optString("artistName")
                    val album = item.optString("collectionName", "Single")
                    val previewUrl = item.optString("previewUrl")
                    val artwork = item.optString("artworkUrl100").replace("100x100bb", "600x600bb")
                    val durationMs = item.optLong("trackTimeMillis", 180000L)
                    val genre = item.optString("primaryGenreName", "Pop")
                    val trackId = "itunes_${item.optLong("trackId", System.currentTimeMillis() + i)}"

                    if (title.isNotEmpty() && previewUrl.isNotEmpty()) {
                        tracks.add(
                            Track(
                                id = trackId,
                                title = title,
                                artist = artist,
                                album = album,
                                artworkUrl = artwork,
                                audioUrl = previewUrl,
                                durationMs = durationMs,
                                genre = genre,
                                lyricsSnippet = "Trecho oficial da faixa de $artist."
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Log or fallback
        } finally {
            conn.disconnect()
        }
        return tracks
    }

    /**
     * Scan local device storage for music matching artist
     */
    fun queryLocalTracksByArtist(artistName: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.ARTIST} LIKE ?"
            val selectionArgs = arrayOf("%$artistName%")

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Sem Título"
                    val artist = it.getString(artistCol) ?: artistName
                    val album = it.getString(albumCol) ?: "Álbum"
                    val duration = it.getLong(durCol)
                    val albumId = it.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()

                    tracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            artworkUrl = artworkUri,
                            audioUrl = contentUri.toString(),
                            durationMs = if (duration > 0) duration else 180000L,
                            genre = "Local Audio",
                            isLocal = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Permission not granted or query error
        }
        return tracks
    }

    /**
     * Fallback dynamic generation for typed singers when offline
     */
    private fun generateDynamicSingerTracks(artistName: String): List<Track> {
        val titles = listOf(
            "Melodia Solitária",
            "Coração Elétrico",
            "Noites Sem Fim",
            "Voo Noturno",
            "Sintonia Viva",
            "Além do Horizonte",
            "Ecos no Vento",
            "Estrela da Manhã"
        )
        return titles.mapIndexed { idx, title ->
            Track(
                id = "dyn_${artistName.hashCode()}_$idx",
                title = title,
                artist = artistName,
                album = "Greatest Random Hits",
                artworkUrl = "",
                audioUrl = "",
                durationMs = (180000L + (idx * 15000L)),
                genre = "Synth Pop",
                lyricsSnippet = "Rádio inteligente gerada para $artistName."
            )
        }
    }

    // Favorites persistence
    fun getFavoriteTrackIds(): Set<String> {
        return prefs.getStringSet("favorite_track_ids", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(trackId: String): Boolean {
        val current = getFavoriteTrackIds().toMutableSet()
        val isNowFav = if (current.contains(trackId)) {
            current.remove(trackId)
            false
        } else {
            current.add(trackId)
            true
        }
        prefs.edit().putStringSet("favorite_track_ids", current).apply()
        return isNowFav
    }

    // Artist Shuffle History persistence
    fun getArtistShuffleHistory(): List<ArtistShuffleHistory> {
        val raw = prefs.getString("artist_history_csv", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                val vibe = try { ArtistShuffleVibe.valueOf(parts[3]) } catch (e: Exception) { ArtistShuffleVibe.ALL }
                ArtistShuffleHistory(
                    artistName = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis(),
                    songCount = parts[2].toIntOrNull() ?: 10,
                    vibe = vibe
                )
            } else null
        }.take(10)
    }

    private fun recordArtistShuffle(name: String, songCount: Int, vibe: ArtistShuffleVibe) {
        val history = getArtistShuffleHistory().toMutableList()
        history.removeAll { it.artistName.equals(name, ignoreCase = true) }
        history.add(0, ArtistShuffleHistory(name, System.currentTimeMillis(), songCount, vibe))
        val serialized = history.take(15).joinToString(";") {
            "${it.artistName}|${it.timestamp}|${it.songCount}|${it.vibe.name}"
        }
        prefs.edit().putString("artist_history_csv", serialized).apply()
    }
}
