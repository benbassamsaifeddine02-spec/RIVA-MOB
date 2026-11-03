package com.musicplatform.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri
)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) permissionGranted.value = true
        }

    private val permissionGranted = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        permissionGranted.value =
            ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED

        setContent {
            val scope = rememberCoroutineScope()
            var tracks by remember { mutableStateOf(emptyList<LocalTrack>()) }
            var status by remember { mutableStateOf("Scan your phone for music.") }
            var isScanning by remember { mutableStateOf(false) }

            LaunchedEffect(permissionGranted.value) {
                if (permissionGranted.value) {
                    isScanning = true
                    status = "Scanning local music…"
                    tracks = withContext(Dispatchers.IO) { scanLocalMusic() }
                    status = tracks.size.toString() + " tracks found on this device"
                    isScanning = false
                }
            }

            MusicPlatformTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Music Platform") },
                            actions = {
                                IconButton(
                                    onClick = {
                                        if (hasAudioPermission()) {
                                            scope.launch {
                                                isScanning = true
                                                tracks = withContext(Dispatchers.IO) { scanLocalMusic() }
                                                status = tracks.size.toString() + " tracks found on this device"
                                                isScanning = false
                                            }
                                        } else {
                                            permissionLauncher.launch(audioPermission)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Scan music")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "YOUR LIBRARY",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        if (isScanning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                        Spacer(Modifier.height(12.dp))

                        if (tracks.isEmpty()) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (hasAudioPermission()) {
                                        scope.launch {
                                            isScanning = true
                                            tracks = withContext(Dispatchers.IO) { scanLocalMusic() }
                                            status = tracks.size.toString() + " tracks found on this device"
                                            isScanning = false
                                        }
                                    } else {
                                        permissionLauncher.launch(audioPermission)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Find music on this phone")
                                        Text(
                                            "Tap to grant audio access and scan local files.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Icon(Icons.Default.Refresh, contentDescription = "Scan")
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(tracks, key = { it.id }) { track ->
                                    ListItem(
                                        headlineContent = { Text(track.title, maxLines = 1) },
                                        supportingContent = {
                                            Text(track.artist + " · " + track.album, maxLines = 1)
                                        },
                                        leadingContent = {
                                            Icon(Icons.Default.LibraryMusic, contentDescription = null)
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { play(track) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                            }
                                        },
                                        modifier = Modifier.clickable { play(track) }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED

    private fun scanLocalMusic(): List<LocalTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        val result = mutableListOf<LocalTrack>()
        contentResolver.query(
            collection,
            projection,
            MediaStore.Audio.Media.IS_MUSIC + " != 0",
            null,
            MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                result += LocalTrack(
                    id = id,
                    title = cursor.getString(titleColumn) ?: "Unknown title",
                    artist = cursor.getString(artistColumn)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                    album = cursor.getString(albumColumn)?.takeUnless { it == "<unknown>" } ?: "Unknown album",
                    uri = ContentUris.withAppendedId(collection, id)
                )
            }
        }
        return result
    }

    private fun play(track: LocalTrack) {
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun MusicPlatformTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Color(0xFF1ED760),
        background = Color(0xFF101010),
        surface = Color(0xFF181818)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
