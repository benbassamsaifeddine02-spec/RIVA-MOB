package com.musicplatform.app

import android.app.DownloadManager
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.MediaMetadata
import com.google.common.util.concurrent.ListenableFuture
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private data class Album(val id: String, val title: String, val artist: String, val year: String, val licence: String, val downloads: String) {
    val cover: String get() = "https://archive.org/services/img/" + id
}
private data class AudioFile(val name: String, val url: String, val size: Long)
private data class PlaylistTrack(val album: Album, val file: AudioFile)

class MainActivity : ComponentActivity() {
    private val importedTracksState = mutableStateOf<List<AudioFile>>(emptyList())
    private val integrationStatusState = mutableStateOf("")
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadImportedTracks()
        handleIncomingShare(intent)
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({ try { mediaController = controllerFuture?.get() } catch (_: Exception) { Toast.makeText(this, "Playback service unavailable", Toast.LENGTH_SHORT).show() } }, mainExecutor)
        setContent {
            val scope = rememberCoroutineScope()
            val importedTracks by importedTracksState
            val integrationStatus by integrationStatusState
            var query by remember { mutableStateOf("") }
            var albums by remember { mutableStateOf(emptyList<Album>()) }
            var selected by remember { mutableStateOf<Album?>(null) }
            var files by remember { mutableStateOf(emptyList<AudioFile>()) }
            var loading by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("Loading online music catalogue…") }
            var category by remember { mutableStateOf("Featured") }
            var searchMode by remember { mutableStateOf("Albums") }
            var moodTracks by remember { mutableStateOf(emptyList<PlaylistTrack>()) }
            var moodPlaylistTitle by remember { mutableStateOf("") }

            fun search(term: String, label: String, mode: String = searchMode) {
                category = label
                searchMode = mode
                selected = null
                moodTracks = emptyList()
                loading = true
                status = if (mode == "AI Mood") "Curating a mood playlist…" else "Searching the music catalogue…"
                scope.launch {
                    try {
                        if (mode == "AI Mood") {
                            val moodQuery = moodSearchTerms(term)
                            val found = withContext(Dispatchers.IO) { searchArchive(moodQuery, broadMatch = true) }
                            val curated = withContext(Dispatchers.IO) {
                                found.take(8).flatMap { album -> getAudioFiles(album.id).map { PlaylistTrack(album, it) } }.take(30)
                            }
                            albums = found
                            moodTracks = curated
                            moodPlaylistTitle = "WAVE MIX · " + (term.ifBlank { "Discover" }).take(48)
                            status = if (curated.isEmpty()) "No playable tracks found for this mood. Try another description." else curated.size.toString() + " tracks curated from openly licensed releases"
                        } else {
                            albums = withContext(Dispatchers.IO) { searchArchive(term) }
                            status = albums.size.toString() + " results · openly licensed catalogue"
                        }
                    } catch (e: Exception) {
                        status = "Network error: " + (e.message ?: "catalogue unavailable")
                    } finally { loading = false }
                }
            }

            LaunchedEffect(Unit) { search("", "Featured", "Albums") }

            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF1ED760), background = Color(0xFF101218), surface = Color(0xFF191C24))) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Column { Text("WAVE MUSIC", fontWeight = FontWeight.Black); Text("ONLINE MUSIC DISCOVERY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } },
                            navigationIcon = { if (selected != null) IconButton(onClick = { selected = null; files = emptyList() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                            actions = { IconButton(enabled = !loading, onClick = { if (selected == null) search(query, category, searchMode) else { val album = selected!!; loading = true; status = "Refreshing audio files…"; scope.launch { try { files = withContext(Dispatchers.IO) { getAudioFiles(album.id) }; status = files.size.toString() + " MP3/OGG files available" } catch (e: Exception) { status = "Refresh failed: " + (e.message ?: "network error") } finally { loading = false } } } }) { Icon(Icons.Default.Refresh, "Refresh catalogue or album") } }
                        )
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        if (selected == null) {
                            Spacer(Modifier.height(12.dp))
                            Text("Discover independent music.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Real releases, artists and playlists from openly licensed sources.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E26))) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("PHONE INTEGRATIONS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            val text = query.ifBlank { "Search for music" }
                                            val link = "https://t.me/spotifybot?text=" + URLEncoder.encode(text, "UTF-8")
                                            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                                            catch (_: Exception) { integrationStatusState.value = "Telegram could not be opened." }
                                        }, modifier = Modifier.weight(1f)) { Text("Open Telegram") }
                                        OutlinedButton(onClick = { openMegaApp() }, modifier = Modifier.weight(1f)) { Text("Open MEGA") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(onClick = { integrationStatusState.value = "Use the MEGA action beside each imported track to upload it." }) { Text("MEGA upload help") }
                                        Text("${importedTracks.size} imported tracks", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    }
                                    if (integrationStatus.isNotBlank()) Text(integrationStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text("Telegram: share audio from its chat to WAVE MUSIC. MEGA: upload imported tracks through the installed MEGA app.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                }
                            }
                            if (importedTracks.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("TELEGRAM IMPORTS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                importedTracks.take(5).forEach { track ->
                                    ListItem(
                                        headlineContent = { Text(track.name, maxLines = 1) },
                                        supportingContent = { Text("Saved on this phone · ${track.size / 1000000} MB") },
                                        leadingContent = { Icon(Icons.Default.MusicNote, null) },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = {
                                                    val item = MediaItem.Builder().setUri(Uri.parse(track.url)).setMediaMetadata(MediaMetadata.Builder().setTitle(track.name).setArtist("Telegram import").build()).build()
                                                    mediaController?.setMediaItem(item); mediaController?.prepare(); mediaController?.play()
                                                }) { Icon(Icons.Default.PlayArrow, "Play imported track") }
                                                TextButton(onClick = { uploadWithMega(track) }) { Text("MEGA") }
                                            }
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(when (searchMode) { "Tracks" -> "Search song titles or artists…"; "AI Mood" -> "Describe a mood or activity…"; else -> "Search albums or artists…" }) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = { IconButton(enabled = !loading, onClick = { search(query, "Search", searchMode) }) { Icon(Icons.Default.Search, "Search") } }, singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Tracks", "Albums", "AI Mood").forEach { mode ->
                                    FilterChip(selected = searchMode == mode, onClick = { searchMode = mode }, label = { Text(mode) })
                                }
                            }
                            if (searchMode == "AI Mood") {
                                Text("Try: focus, night drive, gym, happy, chill, romantic, sad, study", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Featured", "Electronic", "Jazz", "Rock").forEach { label ->
                                    FilterChip(selected = category == label, onClick = {
                                        val term = if (label == "Featured") "" else label.lowercase()
                                        query = term
                                        search(term, label, searchMode)
                                    }, label = { Text(label) })
                                }
                            }
                            Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (searchMode == "AI Mood" && moodTracks.isNotEmpty()) {
                                Text(moodPlaylistTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val queue = moodTracks.map { track -> MediaItem.Builder().setUri(track.file.url).setMediaMetadata(MediaMetadata.Builder().setTitle(track.file.name.substringAfterLast('/')).setArtist(track.album.artist).build()).build() }
                                        mediaController?.setMediaItems(queue, 0, 0L); mediaController?.prepare(); mediaController?.play()
                                    }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play playlist") }
                                    OutlinedButton(onClick = { moodTracks = moodTracks.shuffled() }) { Text("Shuffle") }
                                }
                                LazyColumn(contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(moodTracks, key = { it.album.id + it.file.name }) { track ->
                                        ListItem(
                                            headlineContent = { Text(track.file.name.substringAfterLast('/'), maxLines = 2) },
                                            supportingContent = { Text(track.album.title + " · " + track.album.artist, maxLines = 1) },
                                            leadingContent = { Icon(Icons.Default.MusicNote, null) },
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    val queue = moodTracks.map { item -> MediaItem.Builder().setUri(item.file.url).setMediaMetadata(MediaMetadata.Builder().setTitle(item.file.name.substringAfterLast('/')).setArtist(item.album.artist).build()).build() }
                                                    val index = moodTracks.indexOf(track)
                                                    mediaController?.setMediaItems(queue, index, 0L); mediaController?.prepare(); mediaController?.play()
                                                }) { Icon(Icons.Default.PlayArrow, "Play track") }
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            } else if (searchMode == "AI Mood" && !loading) {
                                Text("Describe the sound or activity you want, then tap Search to build your mix.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                            if (searchMode != "AI Mood") LazyColumn(contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                                IconButton(onClick = { val items = files.map { audio -> MediaItem.Builder().setUri(audio.url).setMediaMetadata(MediaMetadata.Builder().setTitle(audio.name.substringAfterLast('/')).setArtist(selected?.artist ?: "WAVE MUSIC").build()).build() }; mediaController?.setMediaItems(items, files.indexOf(file), 0L); mediaController?.prepare(); mediaController?.play() }) { Icon(Icons.Default.PlayArrow, "Play queue") }
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

    private fun openMegaApp() {
        val launch = packageManager.getLaunchIntentForPackage("mega.privacy.android.app")
            ?: packageManager.getLaunchIntentForPackage("mega.privacy.android.app.lite")
        if (launch != null) startActivity(launch)
        else integrationStatusState.value = "Install the official MEGA app to upload imported audio."
    }

    private fun uploadWithMega(track: AudioFile) {
        try {
            val source = File(Uri.parse(track.url).path ?: throw IllegalStateException("Local audio path unavailable"))
            if (!source.exists()) throw IllegalStateException("Imported audio file not found")
            val uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", source)
            val mime = when (source.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "ogg", "oga" -> "audio/ogg"
                "m4a", "mp4" -> "audio/mp4"
                "wav" -> "audio/wav"
                "flac" -> "audio/flac"
                else -> "audio/*"
            }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Upload audio with MEGA"))
        } catch (e: Exception) {
            integrationStatusState.value = "Could not share with MEGA: " + (e.message ?: "unknown error")
        }
    }

    private fun loadImportedTracks() {
        val folder = File(filesDir, "imports")
        if (!folder.exists()) folder.mkdirs()
        importedTracksState.value = folder.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?.map { AudioFile(it.name, Uri.fromFile(it).toString(), it.length()) } ?: emptyList()
    }

    private fun handleIncomingShare(incoming: Intent?) {
        if (incoming == null || (incoming.action != Intent.ACTION_SEND && incoming.action != Intent.ACTION_SEND_MULTIPLE)) return
        val uris = mutableListOf<Uri>()
        if (incoming.action == Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            (incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uris.add(it) }
        } else {
            @Suppress("DEPRECATION")
            incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
        }
        incoming.clipData?.let { clip -> for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { uris.add(it) } }
        val folder = File(filesDir, "imports").apply { mkdirs() }
        var imported = 0
        uris.distinct().forEach { uri ->
            try {
                val displayName = queryDisplayName(uri) ?: "telegram-audio-${System.currentTimeMillis()}.audio"
                val safeName = displayName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_").take(120)
                var target = File(folder, safeName)
                if (target.exists()) target = File(folder, System.currentTimeMillis().toString() + "-" + safeName)
                contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                if (target.exists() && target.length() > 0L) imported++
            } catch (_: Exception) { }
        }
        loadImportedTracks()
        integrationStatusState.value = if (imported > 0) "$imported audio file(s) imported from Telegram." else "No audio file could be imported from the shared item."
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else null
        } catch (_: Exception) { null } finally { cursor?.close() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShare(intent)
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

    override fun onDestroy() { controllerFuture?.let { MediaController.releaseFuture(it) }; super.onDestroy() }
}

private fun getJson(url: String): JSONObject {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 15000
    c.readTimeout = 20000
    c.setRequestProperty("User-Agent", "WaveMusicAndroid/0.2")
    return try { JSONObject(c.inputStream.bufferedReader().use { it.readText() }) } finally { c.disconnect() }
}

private fun searchArchive(term: String, broadMatch: Boolean = false): List<Album> {
    val tokens = term.trim().split(Regex("\\s+")).map { it.replace(Regex("[^A-Za-z0-9_-]"), "") }.filter { it.isNotBlank() }
    val clauses = tokens.map { "(title:$it OR creator:$it OR subject:$it)" }
    val matchExpression = if (broadMatch) clauses.joinToString(" OR ") else clauses.joinToString(" AND ")
    val q = "mediatype:audio AND licenseurl:http*by*" + if (matchExpression.isBlank()) "" else " AND ($matchExpression)"
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

private fun moodSearchTerms(request: String): String {
    val text = request.lowercase()
    return when {
        listOf("focus", "study", "work", "concentrat").any { text.contains(it) } -> "ambient instrumental classical piano focus"
        listOf("night drive", "driving", "road", "car").any { text.contains(it) } -> "synthwave electronic jazz rock"
        listOf("gym", "workout", "training", "run").any { text.contains(it) } -> "energetic electronic rock funk"
        listOf("happy", "joy", "positive", "party", "dance").any { text.contains(it) } -> "funk soul disco dance pop"
        listOf("chill", "relax", "calm", "peace", "sleep").any { text.contains(it) } -> "ambient downtempo acoustic jazz"
        listOf("romantic", "love", "date").any { text.contains(it) } -> "soul jazz acoustic love songs"
        listOf("sad", "melancholy", "heartbreak").any { text.contains(it) } -> "blues soul acoustic melancholy"
        listOf("rock", "metal").any { text.contains(it) } -> "rock alternative metal"
        text.contains("jazz") -> "jazz bebop smooth jazz"
        listOf("electronic", "techno", "house").any { text.contains(it) } -> "electronic techno house"
        else -> request.trim().ifBlank { "independent music discovery" }
    }
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
