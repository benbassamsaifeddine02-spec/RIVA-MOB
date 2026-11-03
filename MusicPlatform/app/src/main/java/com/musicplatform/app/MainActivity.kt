package com.musicplatform.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private data class Album(val id: String, val title: String, val artist: String, val year: String, val licence: String, val downloads: String) {
    val cover: String get() = "https://archive.org/services/img/" + id
}
private data class AudioFile(val name: String, val url: String, val size: Long)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent {
            val scope = rememberCoroutineScope()
            var query by remember { mutableStateOf("") }
            var albums by remember { mutableStateOf(emptyList<Album>()) }
            var selected by remember { mutableStateOf<Album?>(null) }
            var files by remember { mutableStateOf(emptyList<AudioFile>()) }
            var loading by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("Loading online music catalogue…") }
            var category by remember { mutableStateOf("Featured") }

            fun search(term: String, label: String) {
                category = label
                selected = null
                loading = true
                status = "Searching Internet Archive…"
                scope.launch {
                    try {
                        albums = withContext(Dispatchers.IO) { searchArchive(term) }
                        status = albums.size.toString() + " results · Creative Commons catalogue"
                    } catch (e: Exception) {
                        status = "Network error: " + (e.message ?: "catalogue unavailable")
                    } finally { loading = false }
                }
            }

            LaunchedEffect(Unit) { search("", "Featured") }

            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF1ED760), background = Color(0xFF101218), surface = Color(0xFF191C24))) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Column { Text("WAVE MUSIC", fontWeight = FontWeight.Black); Text("ONLINE MUSIC DISCOVERY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } },
                            navigationIcon = { if (selected != null) IconButton(onClick = { selected = null; files = emptyList() }) { Icon(Icons.Default.ArrowBack, "Back") } }
                        )
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        if (selected == null) {
                            Spacer(Modifier.height(12.dp))
                            Text("Discover independent music.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Real releases, artists and playlists from openly licensed sources.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search albums, artists, genres…") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = { IconButton(onClick = { search(query, "Search") }) { Icon(Icons.Default.Search, "Search") } }, singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Featured", "Electronic", "Jazz", "Rock").forEach { label ->
                                    FilterChip(selected = category == label, onClick = {
                                        val term = if (label == "Featured") "" else label.lowercase()
                                        query = term
                                        search(term, label)
                                    }, label = { Text(label) })
                                }
                            }
                            Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            LazyColumn(contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(albums, key = { it.id }) { album ->
                                    Card(onClick = {
                                        selected = album
                                        loading = true
                                        scope.launch {
                                            try {
                                                files = withContext(Dispatchers.IO) { getAudioFiles(album.id) }
                                                status = files.size.toString() + " MP3/OGG files available"
                                            } catch (e: Exception) { status = "Could not load audio files." }
                                            loading = false
                                        }
                                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E26))) {
                                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = album.cover, contentDescription = "Cover", modifier = Modifier.size(88.dp), contentScale = ContentScale.Crop)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(album.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                Text(album.artist, color = Color.LightGray, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                                Text(album.year + " · CC licensed", color = Color(0xFF1ED760), style = MaterialTheme.typography.labelSmall)
                                                Text("Open album / playlist", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.height(10.dp))
                            AsyncImage(model = selected!!.cover, contentDescription = "Cover", modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
                            Spacer(Modifier.height(10.dp))
                            Text(selected!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(selected!!.artist, color = Color.LightGray)
                            Text(selected!!.year + " · " + selected!!.downloads + " archive downloads", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            Text("Licence: " + selected!!.licence, color = Color.LightGray, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Text(status, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            LazyColumn {
                                items(files, key = { it.name }) { file ->
                                    ListItem(
                                        headlineContent = { Text(file.name.substringAfterLast('/'), maxLines = 2) },
                                        supportingContent = { Text(if (file.size > 0) (file.size / 1000000).toString() + " MB" else "Audio file") },
                                        leadingContent = { Icon(Icons.Default.MusicNote, null) },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = { player.setMediaItem(MediaItem.fromUri(file.url)); player.prepare(); player.play() }) { Icon(Icons.Default.PlayArrow, "Play") }
                                                IconButton(onClick = { download(file) }) { Icon(Icons.Default.Download, "Download") }
                                            }
                                        }
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

    private fun download(file: AudioFile) {
        try {
            val request = DownloadManager.Request(Uri.parse(file.url))
                .setTitle(file.name.substringAfterLast('/'))
                .setDescription("Internet Archive audio download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, file.name.substringAfterLast('/'))
                .setAllowedOverMetered(true).setAllowedOverRoaming(false)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Download queued", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "Download failed to start", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() { player.release(); super.onDestroy() }
}

private fun getJson(url: String): JSONObject {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 15000
    c.readTimeout = 20000
    c.setRequestProperty("User-Agent", "WaveMusicAndroid/0.2")
    return try { JSONObject(c.inputStream.bufferedReader().use { it.readText() }) } finally { c.disconnect() }
}

private fun searchArchive(term: String): List<Album> {
    val q = "mediatype:audio AND licenseurl:http*by*" + if (term.isBlank()) "" else " AND (title:" + term + " OR creator:" + term + " OR subject:" + term + ")"
    val fields = "identifier,title,creator,year,licenseurl,downloads"
    val url = "https://archive.org/advancedsearch.php?q=" + URLEncoder.encode(q, "UTF-8") +
        "&fl[]=" + fields.replace(",", "&fl[]=") + "&rows=30&page=1&output=json&sort[]=downloads+desc"
    val docs = getJson(url).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
    val result = mutableListOf<Album>()
    for (i in 0 until docs.length()) {
        val d = docs.optJSONObject(i) ?: continue
        val id = d.optString("identifier")
        if (id.isNotBlank()) result.add(Album(id, d.optString("title", id), d.optString("creator", "Independent artist"), d.optString("year", "").take(4), d.optString("licenseurl", ""), d.optString("downloads", "—")))
    }
    return result
}

private fun getAudioFiles(id: String): List<AudioFile> {
    val json = getJson("https://archive.org/metadata/" + id)
    val arr = json.optJSONArray("files") ?: return emptyList()
    val base = "https://archive.org/download/" + id
    val result = mutableListOf<AudioFile>()
    for (i in 0 until arr.length()) {
        val f = arr.optJSONObject(i) ?: continue
        val name = f.optString("name")
        val lower = name.lowercase()
        if ((lower.endsWith(".mp3") || lower.endsWith(".ogg")) && !lower.contains("sample") && f.optString("source") != "metadata") {
            val encoded = name.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
            result.add(AudioFile(name, base + "/" + encoded, f.optString("size").toLongOrNull() ?: 0L))
        }
    }
    return result.take(80)
}
