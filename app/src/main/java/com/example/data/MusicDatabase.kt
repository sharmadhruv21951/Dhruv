package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String,       // CDN Image URL
    val audioUrl: String,       // Direct streaming MP3 URL
    val durationMs: Long,
    val durationString: String = "3:30",
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val genre: String = "Pop",
    val lyrics: String = ""      // Mock rolling lyrics lines split by "|"
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["songId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String
)

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<Song>
)

@Dao
interface MusicDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): Song?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("UPDATE songs SET isDownloaded = :isDownloaded WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: String, isFavorite: Boolean)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylistsDirect(): List<Playlist>

    @Query("SELECT * FROM playlist_song_cross_ref")
    suspend fun getAllPlaylistSongRefs(): List<PlaylistSongCrossRef>
}

@Database(
    entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "symphony_music_db"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInitialCatalog(): List<Song> {
            return listOf(
                Song(
                    id = "broke_night_owl",
                    title = "Night Owl",
                    artist = "Broke For Free",
                    album = "Directionless EP",
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    durationMs = 372000,
                    durationString = "6:12",
                    genre = "Synth Wave",
                    lyrics = "Welcome to the Night Owl session|[Instrumental Intro]|Chilled waves of ambient synthesis|Feel the sound flow inside your headspace|Floating in the cosmic black|Synthesizers dancing in the dark|[Beautiful Guitar Interlude]|Night turns into neon glow"
                ),
                Song(
                    id = "lakey_warm_nights",
                    title = "Warm Nights",
                    artist = "LAKEY INSPIRED",
                    album = "Chill Hop Vibes",
                    imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    durationMs = 423000,
                    durationString = "7:03",
                    genre = "Lo-Fi Beats",
                    lyrics = "[Instrumental Chill Hop]|Soft drum loops entering|A warm breeze of acoustic guitars|Perfect for late night studying|Let the melody drift your anxiety away|Steady bassline grounding your focus|Lo-Fi energy pure gold"
                ),
                Song(
                    id = "broke_summer",
                    title = "Summer Spliff",
                    artist = "Broke For Free",
                    album = "Slam EP",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    durationMs = 302000,
                    durationString = "5:02",
                    genre = "Electro Pop",
                    lyrics = "Sun rays on my face|Summer vibes are here to stay|Feeling the rhythm in our footsteps|No worries under the infinite sky|[Instrumental Drop]|Golden hours and summer splashes"
                ),
                Song(
                    id = "synth_neon_drive",
                    title = "Neon Drive",
                    artist = "Glow Runner",
                    album = "Outrun 1985",
                    imageUrl = "https://images.unsplash.com/photo-1515462277126-270d878326e5?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    durationMs = 302000,
                    durationString = "5:02",
                    genre = "Synth Wave",
                    lyrics = "[Retro Wave Intro]|Grid lines stretching to infinity|Unleash the horsepower of the synth|Driving under high-contrast purple skies|Accelerating without any speedometer limits|Analog filters opening wide|[Synthesizer Solo]"
                ),
                Song(
                    id = "bollywood_romance",
                    title = "Dil Se Tum Pukar",
                    artist = "Arijit & Shreya (Simulated)",
                    album = "Pyaar Ka Mausam",
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    durationMs = 362000,
                    durationString = "6:02",
                    genre = "Bollywood",
                    lyrics = "[Beautiful Sitar & Flute Intro]|Dil se tum pukar lo hume|Hum chale aayenge, saaya ban kar|Raahein milti hain khwaabo mein|Chahatein guzar rahi hain baharo mein|Mera dil tumhara hi geet sunaye|Dhadkanein badhi, raatein saji"
                ),
                Song(
                    id = "indie_spring",
                    title = "Springish",
                    artist = "Gillicuddy",
                    album = "Acoustic Seasons",
                    imageUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                    durationMs = 180000,
                    durationString = "3:00",
                    genre = "Indie Folk",
                    lyrics = "[Acoustic Guitar Picking]|Fresh green leaves on branches|Soft wind shaking the petals|A journey to simplicity|Taking slow deep breaths in nature|The morning sun warms the soil|Folk flutes dancing happily|[Mandolin Interlude]"
                ),
                Song(
                    id = "lofi_sunset",
                    title = "Sunset Coffee",
                    artist = "Cafe Beats",
                    album = "Lo-Fi Cafe Hour",
                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    durationMs = 328000,
                    durationString = "5:28",
                    genre = "Lo-Fi Beats",
                    lyrics = "[Coffee Grinder & Mug Sounds]|Cozy evening light|Warm coffee cup in my hands|Lo-Fi piano keys playing softly|Sipping away the tiring day|Raindrops touching the glass windowpane|Peaceful solitude"
                ),
                Song(
                    id = "global_dance",
                    title = "Ibiza Sunset Bass",
                    artist = "Club Frequency",
                    album = "Electronic Summer",
                    imageUrl = "https://images.unsplash.com/photo-1487058792275-0ad4aaf24ca7?w=300",
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    durationMs = 318000,
                    durationString = "5:18",
                    genre = "Electro Pop",
                    lyrics = "[Intro Electronic Drum Pad]|Put your hands in the atmosphere|Feel the bass rise inside your chest|One, two, three, drop!|Subwoofers shaking the sunset beach|Dancers in absolute synchronization|Pure club ecstasy"
                )
            )
        }
    }
}
