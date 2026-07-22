package com.roadtrippin.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roadtrippin.shared.data.AppScreen
import com.roadtrippin.shared.data.RoadtrippinStore
import com.roadtrippin.shared.cloud.CloudServices
import com.roadtrippin.shared.domain.Achievement
import com.roadtrippin.shared.domain.Country
import com.roadtrippin.shared.domain.JournalEntry
import com.roadtrippin.shared.domain.JournalEntryKind
import com.roadtrippin.shared.domain.JournalPhoto
import com.roadtrippin.shared.domain.Jurisdiction
import com.roadtrippin.shared.domain.JurisdictionCatalog
import com.roadtrippin.shared.domain.PlateSighting
import com.roadtrippin.shared.domain.Region
import com.roadtrippin.shared.domain.SyncState
import com.roadtrippin.shared.domain.TagQuantity
import com.roadtrippin.shared.domain.Trip
import com.roadtrippin.shared.platform.PlatformServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap
import roadtrippin.shared.generated.resources.Res
import kotlin.math.roundToInt

@Composable
fun SafetyScreen(onAccept: () -> Unit, padding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(padding).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("🛣️", fontSize = 58.sp)
                Text("Roadtrippin", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text(
                    "Made for passengers",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Never use Roadtrippin while driving. Hand the phone to a passenger and keep the driver focused on the road.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("I’m a passenger")
                }
            }
        }
    }
}

@Composable
fun HomeScreen(store: RoadtrippinStore, snackbar: SnackbarHostState, padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Roadtrippin", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("Spot plates. Save memories.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { store.screen = AppScreen.SETTINGS }) { Text("Settings") }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ready to roll?", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
                    Button(
                        onClick = {
                            scope.launch {
                                starting = true
                                store.startTrip(PlatformServices.currentLocation())
                                starting = false
                            }
                        },
                        enabled = !starting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        if (starting) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Start trippin")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trip history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${store.trips.size} trips", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (store.trips.isEmpty()) {
            item { EmptyCard("No trips yet", "Your completed and unfinished road trips will appear here.", "🧳") }
        } else {
            items(store.trips, key = { it.id }) { trip ->
                TripHistoryCard(
                    trip = trip,
                    onOpen = {
                        if (trip.isActive) store.openTrip(trip.id)
                        else store.reopenTrip(trip.id)
                    },
                )
            }
        }
        item {
            OutlinedButton(onClick = { store.screen = AppScreen.ACCOUNT }, modifier = Modifier.fillMaxWidth()) {
                Text("Cloud backup & account")
            }
        }
    }
}

@Composable
private fun TripHistoryCard(trip: Trip, onOpen: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (trip.isActive) RoadGreen else RoadNavy),
                contentAlignment = Alignment.Center,
            ) { Text(if (trip.isActive) "▶" else "✓", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(trip.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(PlatformServices.formatDateTime(trip.startedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${trip.sightings.size}/53 plates • ${trip.journal.size} journal entries")
            }
            Text(if (trip.isActive) "Resume" else "View", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardScreen(store: RoadtrippinStore, snackbar: SnackbarHostState, padding: PaddingValues) {
    val trip = store.activeTrip ?: run {
        store.screen = AppScreen.HOME
        return
    }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppHeader(
            title = trip.displayName,
            subtitle = PlatformServices.formatDateTime(trip.startedAt),
            onBack = { store.screen = AppScreen.HOME },
            action = "Info",
            onAction = { store.screen = AppScreen.TRIP_INFO },
        )
        Surface(color = RoadRed.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
            Text("Passenger mode • Keep the driver focused", Modifier.padding(12.dp), color = RoadRed, fontWeight = FontWeight.Bold)
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Plate progress", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Text("${trip.sightings.size} / 53", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.displaySmall)
                LinearProgressIndicator(
                    progress = { trip.sightings.size / 53f },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .2f),
                )
            }
        }
        DashboardCard("🪪", "Plates", "${trip.sightings.size} spotted", RoadOrange) { store.screen = AppScreen.PLATES }
        DashboardCard("📖", "Journal", "${trip.journal.size} memories and stops", RoadGreen) { store.screen = AppScreen.JOURNAL }
        DashboardCard("🗺️", "Map", "Plate progress and journal pins", Color(0xFF4E79A7)) { store.screen = AppScreen.MAP }
        val earned = store.achievementsFor(trip).count { it.earned }
        DashboardCard("🏆", "Awards", "$earned badges earned", Color(0xFFB07AA1)) { store.screen = AppScreen.AWARDS }
        OutlinedButton(
            onClick = { PlatformServices.shareText(trip.displayName, store.shareSummary(trip)) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text("Share trip summary") }
        Button(
            onClick = {
                scope.launch {
                    store.endTrip(PlatformServices.currentLocation())
                    snackbar.showSnackbar("Trip saved to history")
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoadRed),
        ) { Text("End trip") }
    }
}

@Composable
private fun DashboardCard(icon: String, title: String, detail: String, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 27.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 30.sp, color = accent)
        }
    }
}

@Composable
fun PlatesScreen(store: RoadtrippinStore, snackbar: SnackbarHostState, padding: PaddingValues) {
    val trip = store.activeTrip ?: return
    val scope = rememberCoroutineScope()
    var editingCode by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            AppHeader(
                "License plates",
                "${trip.sightings.size} of 53 spotted",
                onBack = { store.screen = AppScreen.DASHBOARD },
                modifier = Modifier.padding(20.dp),
            )
        }
        JurisdictionCatalog.countrySections.forEach { (country, jurisdictions) ->
            item(key = country.name) {
                Text(
                    country.displayName,
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
            items(jurisdictions, key = { it.code }) { jurisdiction ->
                val sighting = trip.sightings.firstOrNull { it.jurisdictionCode == jurisdiction.code }
                JurisdictionRow(jurisdiction, sighting != null, sighting?.let { PlatformServices.formatDateTime(it.firstSeenAt) }) {
                    if (sighting != null) {
                        editingCode = jurisdiction.code
                        return@JurisdictionRow
                    }
                    scope.launch {
                        val location = PlatformServices.currentLocation()
                        val added = store.recordSighting(jurisdiction.code, location)
                        if (added) {
                            if (location?.latitude != null && location.longitude != null) {
                                scope.launch {
                                    PlatformServices.reverseGeocode(location)?.let { place ->
                                        store.updateSightingPlace(jurisdiction.code, place)
                                    }
                                }
                            }
                            PlatformServices.celebrate(store.settings.soundEnabled, store.settings.hapticsEnabled)
                            val result = snackbar.showSnackbar(
                                "${jurisdiction.name} spotted!",
                                actionLabel = "Undo",
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) store.removeSighting(jurisdiction.code)
                        }
                    }
                }
            }
        }
    }
    editingCode?.let { code ->
        val jurisdiction = JurisdictionCatalog.byCode.getValue(code)
        val sighting = store.activeTrip?.sightings?.firstOrNull { it.jurisdictionCode == code }
        if (sighting != null) {
            SightingEditorDialog(
                jurisdiction = jurisdiction,
                firstSeenAt = sighting.firstSeenAt,
                location = sighting.location,
                onDismiss = { editingCode = null },
                onSave = { time, location ->
                    store.updateSighting(code, time, location)
                    editingCode = null
                },
                onRemove = {
                    store.removeSighting(code)
                    editingCode = null
                },
            )
        } else editingCode = null
    }
}

@Composable
private fun SightingEditorDialog(
    jurisdiction: Jurisdiction,
    firstSeenAt: Long,
    location: com.roadtrippin.shared.domain.LocationStamp,
    onDismiss: () -> Unit,
    onSave: (Long, com.roadtrippin.shared.domain.LocationStamp) -> Unit,
    onRemove: () -> Unit,
) {
    var timeText by remember(firstSeenAt) { mutableStateOf(firstSeenAt.toString()) }
    var place by remember(location) { mutableStateOf(location.placeName.orEmpty()) }
    var latitude by remember(location) { mutableStateOf(location.latitude?.toString().orEmpty()) }
    var longitude by remember(location) { mutableStateOf(location.longitude?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${jurisdiction.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("First seen ${PlatformServices.formatDateTime(firstSeenAt)}")
                OutlinedTextField(timeText, { timeText = it.filter(Char::isDigit) }, label = { Text("Timestamp (milliseconds)") }, singleLine = true)
                OutlinedTextField(place, { place = it }, label = { Text("Place label") }, singleLine = true)
                OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, singleLine = true)
                OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, singleLine = true)
                TextButton(onClick = onRemove) { Text("Remove sighting", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    timeText.toLongOrNull() ?: firstSeenAt,
                    com.roadtrippin.shared.domain.LocationStamp(
                        latitude = latitude.toDoubleOrNull(),
                        longitude = longitude.toDoubleOrNull(),
                        placeName = place.trim().ifBlank { null },
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun JurisdictionRow(jurisdiction: Jurisdiction, seen: Boolean, timestamp: String?, onClick: () -> Unit) {
    val regionColor = Color(jurisdiction.region.colorHex)
    val background = if (seen) regionColor else MaterialTheme.colorScheme.surface
    val foreground = if (seen && jurisdiction.region in setOf(Region.WEST, Region.NORTHEAST, Region.DEEP_SOUTH)) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = "${jurisdiction.name}, ${jurisdiction.region.displayName}, ${if (seen) "seen" else "not seen"}" }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(regionColor).border(1.dp, foreground.copy(alpha = .35f), CircleShape))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(jurisdiction.name, color = foreground, fontWeight = FontWeight.Bold)
            Text(
                if (seen) listOfNotNull(jurisdiction.region.displayName, timestamp).joinToString(" • ") else jurisdiction.region.displayName,
                color = foreground.copy(alpha = .76f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(if (seen) "✓" else jurisdiction.code, color = foreground, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
}

@Composable
fun JournalScreen(store: RoadtrippinStore, snackbar: SnackbarHostState, padding: PaddingValues) {
    val trip = store.activeTrip ?: return
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var viewingEntryId by remember { mutableStateOf<String?>(null) }
    var editingEntryId by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppHeader(
                    "Road journal",
                    "${trip.journal.size} entries",
                    onBack = { store.screen = AppScreen.DASHBOARD },
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }
            if (trip.journal.isEmpty()) item { EmptyCard("No notes yet", "Save birds, broken-down trucks, landmarks, stops, and anything else worth remembering.", "📓") }
            items(trip.journal, key = { it.id }) { entry ->
                JournalEntryCard(
                    entry = entry,
                    onView = { viewingEntryId = entry.id },
                )
            }
        }
        FloatingActionButton(
            onClick = { showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp),
            containerColor = RoadOrange,
        ) { Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
    }
    if (showEditor || editingEntryId != null) {
        val editingEntry = editingEntryId?.let { id -> store.activeTrip?.journal?.firstOrNull { it.id == id } }
        JournalEditorDialog(
            initialEntry = editingEntry,
            onDismiss = {
                showEditor = false
                editingEntryId = null
            },
            onSave = { kind, title, body, tags, occurredAt, editedLocation, photos ->
                scope.launch {
                    if (editingEntry != null) {
                        store.updateJournalEntry(
                            entryId = editingEntry.id,
                            kind = kind,
                            title = title,
                            body = body,
                            occurredAt = occurredAt,
                            location = editedLocation,
                            tags = tags,
                            photos = photos,
                        )
                        viewingEntryId = editingEntry.id
                        snackbar.showSnackbar("Journal entry updated")
                    } else {
                        val location = editedLocation.takeIf {
                            it.hasCoordinates || !it.placeName.isNullOrBlank()
                        } ?: PlatformServices.currentLocation()
                        val entryId = store.addJournalEntry(kind, title, body, tags, location, photos)
                        if (entryId != null && location?.hasCoordinates == true && location.placeName == null) {
                            scope.launch {
                                PlatformServices.reverseGeocode(location)?.let { place ->
                                    store.updateJournalPlace(entryId, place)
                                }
                            }
                        }
                        viewingEntryId = entryId
                        snackbar.showSnackbar(if (kind == JournalEntryKind.STOP) "Stop logged" else "Journal note saved")
                    }
                }
                showEditor = false
                editingEntryId = null
            },
            onDelete = editingEntry?.let { entry ->
                {
                    store.deleteJournalEntry(entry.id)
                    showEditor = false
                    viewingEntryId = null
                    editingEntryId = null
                    scope.launch { snackbar.showSnackbar("Journal entry deleted") }
                }
            },
        )
    }
    val viewingEntry = viewingEntryId?.let { id ->
        store.activeTrip?.journal?.firstOrNull { it.id == id }
    }
    if (viewingEntry != null) {
        JournalEntryViewerDialog(
            entry = viewingEntry,
            onDismiss = { viewingEntryId = null },
            onEdit = {
                viewingEntryId = null
                editingEntryId = viewingEntry.id
            },
        )
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry, onView: () -> Unit) {
    OutlinedCard(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onView)
            .semantics { contentDescription = "View ${journalEntryTitle(entry)}" },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (entry.kind == JournalEntryKind.STOP) "📍" else "📝", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(journalEntryTitle(entry), fontWeight = FontWeight.Bold)
                    Text(PlatformServices.formatDateTime(entry.occurredAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("View ›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (entry.body.isNotBlank()) Text(entry.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.location.placeName != null) Text("📌 ${entry.location.placeName}", color = MaterialTheme.colorScheme.primary)
            if (entry.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.take(3).forEach { AssistChip(onClick = {}, label = { Text("${it.tag} ×${it.quantity}") }) }
                }
            }
            if (entry.photos.isNotEmpty()) Text("📷 ${entry.photos.size} photo${if (entry.photos.size == 1) "" else "s"}")
        }
    }
}

private fun journalEntryTitle(entry: JournalEntry): String =
    entry.title.ifBlank { if (entry.kind == JournalEntryKind.STOP) "Road stop" else "Road note" }

@Composable
private fun JournalEntryViewerDialog(
    entry: JournalEntry,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    var selectedPhoto by remember(entry.id) { mutableStateOf<JournalPhoto?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (entry.kind == JournalEntryKind.STOP) "📍" else "📝", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Text(journalEntryTitle(entry))
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    PlatformServices.formatDateTime(entry.occurredAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.location.placeName?.let { place ->
                    Text("📌 $place", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                if (entry.body.isNotBlank()) {
                    Text(entry.body, style = MaterialTheme.typography.bodyLarge)
                }
                if (entry.tags.isNotEmpty()) {
                    Text(
                        entry.tags.joinToString("  •  ") { "${it.tag} ×${it.quantity}" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry.photos.isNotEmpty()) {
                    Text(
                        "Photos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    entry.photos.chunked(2).forEachIndexed { rowIndex, rowPhotos ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPhotos.forEachIndexed { columnIndex, photo ->
                                val photoNumber = rowIndex * 2 + columnIndex + 1
                                JournalPhotoImage(
                                    photo = photo,
                                    contentDescription = "Photo $photoNumber of ${entry.photos.size}",
                                    contentScale = ContentScale.Crop,
                                    aspectRatio = 4f / 3f,
                                    onClick = { selectedPhoto = photo },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowPhotos.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text(
                        "Tap a photo to view it larger.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
        dismissButton = { OutlinedButton(onClick = onEdit) { Text("Edit") } },
    )
    selectedPhoto?.let { photo ->
        JournalPhotoViewerDialog(
            photo = photo,
            photoNumber = entry.photos.indexOfFirst { it.id == photo.id } + 1,
            photoCount = entry.photos.size,
            onDismiss = { selectedPhoto = null },
        )
    }
}

@Composable
private fun JournalPhotoImage(
    photo: JournalPhoto,
    contentDescription: String,
    contentScale: ContentScale,
    aspectRatio: Float?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(photo.id, photo.localPath) { mutableStateOf<ImageBitmap?>(null) }
    var loadFinished by remember(photo.id, photo.localPath) { mutableStateOf(false) }
    LaunchedEffect(photo.id, photo.localPath) {
        bitmap = runCatching {
            PlatformServices.readLocalFile(photo.localPath)?.let { bytes ->
                withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() }
            }
        }.getOrNull()
        loadFinished = true
    }
    val imageRatio = aspectRatio ?: bitmap?.let { image ->
        (image.width.toFloat() / image.height.coerceAtLeast(1)).coerceIn(.6f, 1.8f)
    } ?: 4f / 3f
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier.aspectRatio(imageRatio)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            !loadFinished -> CircularProgressIndicator(Modifier.size(28.dp))
            else -> Text(
                "Photo unavailable",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JournalPhotoViewerDialog(
    photo: JournalPhoto,
    photoNumber: Int,
    photoCount: Int,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Photo $photoNumber of $photoCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                JournalPhotoImage(
                    photo = photo,
                    contentDescription = "Photo $photoNumber of $photoCount, enlarged",
                    contentScale = ContentScale.Fit,
                    aspectRatio = null,
                    onClick = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun JournalEditorDialog(
    initialEntry: JournalEntry?,
    onDismiss: () -> Unit,
    onSave: (JournalEntryKind, String, String, List<TagQuantity>, Long, com.roadtrippin.shared.domain.LocationStamp, List<JournalPhoto>) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val initialLocation = initialEntry?.location ?: com.roadtrippin.shared.domain.LocationStamp()
    var kind by remember(initialEntry?.id) { mutableStateOf(initialEntry?.kind ?: JournalEntryKind.NOTE) }
    var title by remember(initialEntry?.id) { mutableStateOf(initialEntry?.title.orEmpty()) }
    var body by remember(initialEntry?.id) { mutableStateOf(initialEntry?.body.orEmpty()) }
    var tags by remember(initialEntry?.id) {
        mutableStateOf(initialEntry?.tags?.joinToString(", ") { "${it.tag}:${it.quantity}" }.orEmpty())
    }
    var occurredAt by remember(initialEntry?.id) {
        mutableStateOf((initialEntry?.occurredAt ?: PlatformServices.nowEpochMillis()).toString())
    }
    var place by remember(initialEntry?.id) { mutableStateOf(initialLocation.placeName.orEmpty()) }
    var latitude by remember(initialEntry?.id) { mutableStateOf(initialLocation.latitude?.toString().orEmpty()) }
    var longitude by remember(initialEntry?.id) { mutableStateOf(initialLocation.longitude?.toString().orEmpty()) }
    var photos by remember(initialEntry?.id) { mutableStateOf(initialEntry?.photos.orEmpty()) }
    var loadingPhoto by remember(initialEntry?.id) { mutableStateOf(false) }
    var confirmDelete by remember(initialEntry?.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                when {
                    initialEntry != null && kind == JournalEntryKind.STOP -> "Edit stop"
                    initialEntry != null -> "Edit road note"
                    kind == JournalEntryKind.STOP -> "Log a stop"
                    else -> "Add a road note"
                }
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(kind == JournalEntryKind.NOTE, onClick = { kind = JournalEntryKind.NOTE })
                    Text("Note")
                    Spacer(Modifier.width(12.dp))
                    RadioButton(kind == JournalEntryKind.STOP, onClick = { kind = JournalEntryKind.STOP })
                    Text("Stop")
                }
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it }, label = { Text("What did you see?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    tags,
                    { tags = it },
                    label = { Text("Tags and quantities") },
                    supportingText = { Text("Example: hawk:3, broken-down truck:1") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    occurredAt,
                    { occurredAt = it.filter(Char::isDigit) },
                    label = { Text("Timestamp (milliseconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(place, { place = it }, label = { Text("Place label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        latitude,
                        { latitude = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        longitude,
                        { longitude = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Photos: ${photos.size} of 5", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                loadingPhoto = true
                                val picked = PlatformServices.pickJournalPhotos(5 - photos.size)
                                photos = (photos + picked).distinctBy(JournalPhoto::id).take(5)
                                loadingPhoto = false
                            }
                        },
                        enabled = photos.size < 5 && !loadingPhoto,
                        modifier = Modifier.weight(1f),
                    ) { Text("Library") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                loadingPhoto = true
                                PlatformServices.takeJournalPhoto()?.let { photo -> photos = (photos + photo).take(5) }
                                loadingPhoto = false
                            }
                        },
                        enabled = photos.size < 5 && !loadingPhoto,
                        modifier = Modifier.weight(1f),
                    ) { Text("Camera") }
                }
                if (loadingPhoto) LinearProgressIndicator(Modifier.fillMaxWidth())
                photos.forEachIndexed { index, photo ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Photo ${index + 1}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { photos = photos.filterNot { it.id == photo.id } }) { Text("Remove") }
                    }
                }
                if (initialEntry != null && onDelete != null) {
                    HorizontalDivider(Modifier.padding(top = 4.dp))
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete entry", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = tags.split(',').mapNotNull { token ->
                        val pieces = token.trim().split(':', limit = 2)
                        pieces.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { tag ->
                            TagQuantity(tag, pieces.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                        }
                    }
                    onSave(
                        kind,
                        title,
                        body,
                        parsed,
                        occurredAt.toLongOrNull() ?: initialEntry?.occurredAt ?: PlatformServices.nowEpochMillis(),
                        com.roadtrippin.shared.domain.LocationStamp(
                            latitude = latitude.toDoubleOrNull(),
                            longitude = longitude.toDoubleOrNull(),
                            placeName = place.trim().ifBlank { null },
                        ),
                        photos,
                    )
                },
                enabled = title.isNotBlank() || body.isNotBlank(),
            ) { Text(if (initialEntry == null) "Save" else "Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (confirmDelete && initialEntry != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this journal entry?") },
            text = { Text("The note and its attached photos will be removed from this trip.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep entry") }
            },
        )
    }
}

@Composable
fun MapScreen(store: RoadtrippinStore, snackbar: SnackbarHostState, padding: PaddingValues) {
    val trip = store.activeTrip ?: return
    val scope = rememberCoroutineScope()
    val seen = trip.sightings.mapTo(mutableSetOf()) { it.jurisdictionCode }
    var overviewVectorData by remember { mutableStateOf<StateVectorData?>(null) }
    var vectorData by remember { mutableStateOf<StateVectorData?>(null) }
    var highwayData by remember { mutableStateOf<HighwayVectorData?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var highwayLoadFailed by remember { mutableStateOf(false) }
    var selectedStateCode by remember { mutableStateOf<String?>(null) }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }
    var viewingEntryId by remember { mutableStateOf<String?>(null) }
    var editingEntryId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.Default) {
                Pair(
                    vectorJson.decodeFromString<StateVectorData>(
                        Res.readBytes("files/us_states_20m.json").decodeToString()
                    ),
                    vectorJson.decodeFromString<StateVectorData>(
                        Res.readBytes("files/us_states_5m.json").decodeToString()
                    ),
                )
            }
        }.onSuccess {
            overviewVectorData = it.first
            vectorData = it.second
        }.onFailure { loadFailed = true }
    }
    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.Default) {
                decodeHighwayVectors(Res.readBytes("files/us_highways_2025.bin"))
            }
        }.onSuccess { highwayData = it }.onFailure { highwayLoadFailed = true }
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        AppHeader(
            "Trip map",
            "Seen plates and trip memories together",
            onBack = { store.screen = AppScreen.DASHBOARD },
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 10.dp),
        )
        when {
            vectorData != null && overviewVectorData != null -> {
                CombinedTripMap(
                    data = vectorData!!,
                    overviewData = overviewVectorData!!,
                    highways = highwayData,
                    highwaysUnavailable = highwayLoadFailed,
                    trip = trip,
                    selectedStateCode = selectedStateCode,
                    selectedEntryId = selectedEntryId,
                    onStateSelected = { code ->
                        selectedStateCode = code
                        selectedEntryId = null
                    },
                    onEntrySelected = { entryId ->
                        selectedEntryId = entryId
                        selectedStateCode = null
                    },
                    onEntryOpen = { viewingEntryId = it },
                    onShare = {
                        PlatformServices.shareMapImage(
                            title = "${trip.displayName} map",
                            svg = buildShareMapSvg(overviewVectorData!!, trip),
                            summary = "${trip.displayName}: ${seen.size} of 53 Roadtrippin plates spotted and ${trip.journal.size} journal entries. Precise locations are not included.",
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                )
            }
            loadFailed -> EmptyCard(
                "Map unavailable",
                "The bundled state vectors could not be loaded.",
                "🗺️",
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    val viewingEntry = viewingEntryId?.let { id ->
        store.activeTrip?.journal?.firstOrNull { it.id == id }
    }
    if (viewingEntry != null) {
        JournalEntryViewerDialog(
            entry = viewingEntry,
            onDismiss = { viewingEntryId = null },
            onEdit = {
                viewingEntryId = null
                editingEntryId = viewingEntry.id
            },
        )
    }

    val editingEntry = editingEntryId?.let { id ->
        store.activeTrip?.journal?.firstOrNull { it.id == id }
    }
    if (editingEntry != null) {
        JournalEditorDialog(
            initialEntry = editingEntry,
            onDismiss = { editingEntryId = null },
            onSave = { kind, title, body, tags, occurredAt, location, photos ->
                scope.launch {
                    store.updateJournalEntry(
                        entryId = editingEntry.id,
                        kind = kind,
                        title = title,
                        body = body,
                        occurredAt = occurredAt,
                        location = location,
                        tags = tags,
                        photos = photos,
                    )
                    viewingEntryId = editingEntry.id
                    snackbar.showSnackbar("Journal entry updated")
                }
                editingEntryId = null
            },
            onDelete = {
                store.deleteJournalEntry(editingEntry.id)
                selectedEntryId = null
                viewingEntryId = null
                editingEntryId = null
                scope.launch { snackbar.showSnackbar("Journal entry deleted") }
            },
        )
    }
}

@Serializable
private data class StateVectorData(
    val source: String,
    val states: List<StateVector>,
)

@Serializable
private data class StateVector(
    val code: String,
    val polygons: List<StatePolygon>,
)

@Serializable
private data class StatePolygon(
    val outer: List<List<Double>>,
    val holes: List<List<List<Double>>>,
)

private val vectorJson = Json { ignoreUnknownKeys = true }

private data class StateDrawPath(
    val code: String,
    val path: Path,
    val bounds: Rect,
)

private data class HighwayLabelCandidate(
    val text: String,
    val position: Offset,
)

private data class HighwayRenderCacheKey(
    val minimumScale: Float,
    val tileX: Int,
    val tileY: Int,
)

private data class HighwayMapLabel(
    val labelIndex: Int,
    val highwayClass: HighwayClass,
    val position: Offset,
)

private data class HighwayRenderTile(
    val paths: Array<Path>,
    val labels: List<HighwayMapLabel>,
)

private fun buildHighwayRenderTile(
    tile: HighwayTile,
    data: HighwayVectorData,
    width: Float,
    height: Float,
): HighwayRenderTile {
    val paths = Array(HighwayClass.entries.size) { Path() }
    val labels = mutableListOf<HighwayMapLabel>()
    val labelCells = mutableSetOf<String>()
    tile.roads.forEach { road ->
        val path = paths[road.highwayClass.ordinal]
        var pointIndex = 0
        while (pointIndex < road.coordinates.size) {
            val point = projectNorthAmerica(
                latitude = road.coordinates[pointIndex + 1].toDouble() / data.quantization,
                longitude = road.coordinates[pointIndex].toDouble() / data.quantization,
                width = width,
                height = height,
            )
            if (pointIndex == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            pointIndex += 2
        }
        if (road.labelIndex > 0) {
            val middle = ((road.coordinates.size / 2) / 2) * 2
            val longitude = road.coordinates[middle].toDouble() / data.quantization
            val latitude = road.coordinates[middle + 1].toDouble() / data.quantization
            val labelCell = "${road.labelIndex}:${(longitude * 2).roundToInt()}:${(latitude * 2).roundToInt()}"
            if (labelCells.add(labelCell)) {
                labels += HighwayMapLabel(
                    labelIndex = road.labelIndex,
                    highwayClass = road.highwayClass,
                    position = projectNorthAmerica(latitude, longitude, width, height),
                )
            }
        }
    }
    return HighwayRenderTile(paths, labels)
}

private fun buildStateDrawPaths(data: StateVectorData, width: Float, height: Float): List<StateDrawPath> {
    if (width <= 0f || height <= 0f) return emptyList()
    return data.states.map { state ->
        val path = Path().apply { fillType = PathFillType.EvenOdd }
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        state.polygons.forEach { polygon ->
            addNorthAmericaStateRing(path, state.code, polygon.outer, width, height)
            polygon.holes.forEach { hole ->
                addNorthAmericaStateRing(path, state.code, hole, width, height)
            }
            polygon.outer.forEach { point ->
                val projected = projectNorthAmericaStatePoint(state.code, point[0], point[1], width, height)
                left = minOf(left, projected.x)
                top = minOf(top, projected.y)
                right = maxOf(right, projected.x)
                bottom = maxOf(bottom, projected.y)
            }
        }
        StateDrawPath(state.code, path, Rect(left, top, right, bottom))
    }
}

private fun minimumHighwayLabelScale(highwayClass: HighwayClass): Float = when (highwayClass) {
    HighwayClass.INTERSTATE -> 4f
    HighwayClass.US_ROUTE -> 10f
    HighwayClass.STATE_ROUTE -> 24f
}

private fun visibleNorthAmericaBounds(
    offset: Offset,
    scale: Float,
    width: Float,
    height: Float,
): HighwayBounds {
    if (width <= 0f || height <= 0f) return HighwayBounds(-190.0, -50.0, 10.0, 75.0)
    val left = -offset.x / scale
    val right = (width - offset.x) / scale
    val top = -offset.y / scale
    val bottom = (height - offset.y) / scale
    val west = -170.0 + left / width * 120.0
    return HighwayBounds(
        west = if (left <= 0.01f) -190.0 else west,
        east = -170.0 + right / width * 120.0,
        south = 75.0 - bottom / height * 65.0,
        north = 75.0 - top / height * 65.0,
    )
}

private fun mapCenterLatitude(offset: Offset, scale: Float, height: Int): Double {
    if (height <= 0) return 39.5
    val centerMapY = (height / 2f - offset.y) / scale
    return (75.0 - centerMapY / height * 65.0).coerceIn(10.0, 75.0)
}

private fun visibleMapRect(offset: Offset, scale: Float, width: Float, height: Float): Rect = Rect(
    left = -offset.x / scale,
    top = -offset.y / scale,
    right = (width - offset.x) / scale,
    bottom = (height - offset.y) / scale,
)

private fun buildTripMapMarkerPlacements(
    entries: List<JournalEntry>,
    sightings: List<PlateSighting>,
    mapScale: Float,
    mapOffset: Offset,
    width: Float,
    height: Float,
    journalCollisionDiameterPx: Float,
    plateCollisionSize: Size,
    platePreferredOffsetPx: Float,
    markerGapPx: Float,
    markerRingStepPx: Float,
): List<MapMarkerPlacement> {
    if (width <= 0f || height <= 0f) return emptyList()
    fun screenAnchor(mapPoint: Offset): Offset = Offset(
        mapPoint.x * mapScale + mapOffset.x,
        mapPoint.y * mapScale + mapOffset.y,
    )

    val markers = buildList {
        entries.sortedWith(compareBy<JournalEntry> { it.occurredAt }.thenBy { it.id }).forEach { entry ->
            val anchor = screenAnchor(projectNorthAmericaLocation(entry, width, height))
            if (anchor.x in 0f..width && anchor.y in 0f..height) {
                add(
                    MapMarkerLayoutInput(
                        id = entry.id,
                        kind = MapMarkerKind.JOURNAL,
                        anchor = anchor,
                        preferredCenter = anchor,
                        collisionSize = Size(journalCollisionDiameterPx, journalCollisionDiameterPx),
                    )
                )
            }
        }
        sightings.sortedWith(compareBy<PlateSighting> { it.firstSeenAt }.thenBy { it.jurisdictionCode }).forEach { sighting ->
            val anchor = screenAnchor(projectNorthAmericaLocation(sighting, width, height))
            if (anchor.x in 0f..width && anchor.y in 0f..height) {
                add(
                    MapMarkerLayoutInput(
                        id = sighting.id,
                        kind = MapMarkerKind.PLATE,
                        anchor = anchor,
                        preferredCenter = anchor - Offset(0f, platePreferredOffsetPx),
                        collisionSize = plateCollisionSize,
                    )
                )
            }
        }
    }
    return placeMapMarkers(
        markers = markers,
        canvasSize = Size(width, height),
        gapPx = markerGapPx,
        ringStepPx = markerRingStepPx,
    )
}

@Composable
private fun CombinedTripMap(
    data: StateVectorData,
    overviewData: StateVectorData,
    highways: HighwayVectorData?,
    highwaysUnavailable: Boolean,
    trip: Trip,
    selectedStateCode: String?,
    selectedEntryId: String?,
    onStateSelected: (String) -> Unit,
    onEntrySelected: (String) -> Unit,
    onEntryOpen: (String) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seen = trip.sightings.map { it.jurisdictionCode }.toSet()
    val locatedEntries = trip.journal.filter { it.location.hasCoordinates }
    val locatedSightings = trip.sightings.filter { it.location.hasCoordinates }
    val selectedEntry = trip.journal.firstOrNull { it.id == selectedEntryId }
    val selectedJurisdiction = selectedStateCode?.let(JurisdictionCatalog.byCode::get)
    val selectedSighting = selectedStateCode?.let { code -> trip.sightings.firstOrNull { it.jurisdictionCode == code } }
    var mapScale by remember { mutableStateOf(1f) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var mapGestureActive by remember { mutableStateOf(false) }
    var mapCanvasSize by remember { mutableStateOf(IntSize.Zero) }
    val stateDrawPaths = remember(data, mapCanvasSize) {
        buildStateDrawPaths(data, mapCanvasSize.width.toFloat(), mapCanvasSize.height.toFloat())
    }
    val overviewStateDrawPaths = remember(overviewData, mapCanvasSize) {
        buildStateDrawPaths(overviewData, mapCanvasSize.width.toFloat(), mapCanvasSize.height.toFloat())
    }
    val highwayRenderCache = remember(highways, mapCanvasSize) {
        mutableMapOf<HighwayRenderCacheKey, HighwayRenderTile>()
    }
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)
    val pinRadiusPx = with(LocalDensity.current) { 8.dp.toPx() }
    val plateMarkerWidthPx = with(LocalDensity.current) { 24.dp.toPx() }
    val plateMarkerHeightPx = with(LocalDensity.current) { 16.dp.toPx() }
    val plateMarkerStemPx = with(LocalDensity.current) { 4.dp.toPx() }
    val plateSelectionPaddingPx = with(LocalDensity.current) { 3.dp.toPx() }
    val markerHitPaddingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val markerGapPx = with(LocalDensity.current) { 4.dp.toPx() }
    val markerRingStepPx = with(LocalDensity.current) { 38.dp.toPx() }
    val journalCollisionDiameterPx = pinRadiusPx * 3.1f
    val plateCollisionSize = Size(
        plateMarkerWidthPx + plateSelectionPaddingPx * 2f,
        plateMarkerHeightPx + plateSelectionPaddingPx * 2f,
    )
    val dcRadiusPx = with(LocalDensity.current) { 6.dp.toPx() }
    val stateBoundaryWidthPx = with(LocalDensity.current) { 1.5.dp.toPx() }
    val selectedStateBoundaryWidthPx = with(LocalDensity.current) { 3.dp.toPx() }
    val unseenFill = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = .72f)
    val selectionColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val ocean = if (isDark) Color(0xFF18303C) else Color(0xFFDCEBF2)
    val stateBoundary = if (isDark) Color(0xFF6F8792) else Color(0xFF43545C)
    val roadCasing = if (isDark) Color(0xFF172126) else Color(0xFFFFFCF4)
    val markerLeaderCasing = if (isDark) Color(0xFF101A1F) else Color(0xFFFFFCF4)
    val interstateRoad = if (isDark) Color(0xFF83B4FF) else Color(0xFF2F67B1)
    val usRouteRoad = if (isDark) Color(0xFFFFB36A) else Color(0xFFC65F18)
    val stateRouteRoad = if (isDark) Color(0xFFC4CDD2) else Color(0xFF59666D)
    val roadLabelBackground = if (isDark) Color(0xE6232E33) else Color(0xEEFFFDF7)
    val roadLabelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
    )
    val plateMarkerTextStyle = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 8.sp,
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Pinch to zoom • drag to pan • tap a state or pin",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier.fillMaxWidth()
                .aspectRatio(1.42f)
                .clip(RoundedCornerShape(18.dp))
                .background(ocean)
                .onSizeChanged { mapCanvasSize = it }
                .semantics {
                    contentDescription = "Combined offline trip map with ${seen.size} plates seen, ${locatedSightings.size} plate sighting marker${if (locatedSightings.size == 1) "" else "s"}, and ${locatedEntries.size} journal pin${if (locatedEntries.size == 1) "" else "s"}. Pinch to zoom, drag to pan, and tap a state or marker for details."
                }
                // Keep this detector alive for the entire touch sequence. Keying it to
                // mapScale/mapOffset restarts it after every movement and cancels the drag.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        mapGestureActive = true
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        mapGestureActive = false
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val previousScale = mapScale
                        val newScale = (previousScale * zoom).coerceIn(1f, MaxMapScale)
                        val ratio = newScale / previousScale
                        val candidate = centroid + (mapOffset - centroid) * ratio + pan
                        mapScale = newScale
                        mapOffset = clampMapOffset(candidate, newScale, size.width.toFloat(), size.height.toFloat())
                    }
                }
                .pointerInput(data, overviewData, locatedEntries, locatedSightings) {
                    detectTapGestures { tap ->
                        val markerPlacements = buildTripMapMarkerPlacements(
                            entries = locatedEntries,
                            sightings = locatedSightings,
                            mapScale = mapScale,
                            mapOffset = mapOffset,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            journalCollisionDiameterPx = journalCollisionDiameterPx,
                            plateCollisionSize = plateCollisionSize,
                            platePreferredOffsetPx = plateMarkerHeightPx / 2f + plateMarkerStemPx,
                            markerGapPx = markerGapPx,
                            markerRingStepPx = markerRingStepPx,
                        )
                        val markerHit = markerPlacements
                            .filter { placement ->
                                tap.x >= placement.bounds.left - markerHitPaddingPx &&
                                    tap.x <= placement.bounds.right + markerHitPaddingPx &&
                                    tap.y >= placement.bounds.top - markerHitPaddingPx &&
                                    tap.y <= placement.bounds.bottom + markerHitPaddingPx
                            }
                            .minByOrNull { (it.center - tap).getDistance() }
                        val mapTap = Offset(
                            (tap.x - mapOffset.x) / mapScale,
                            (tap.y - mapOffset.y) / mapScale,
                        )
                        when (markerHit?.marker?.kind) {
                            MapMarkerKind.PLATE -> locatedSightings
                                .firstOrNull { it.id == markerHit.marker.id }
                                ?.let { onStateSelected(it.jurisdictionCode) }
                            MapMarkerKind.JOURNAL -> onEntrySelected(markerHit.marker.id)
                            else -> {
                                findStateAtPoint(
                                    data = if (mapScale >= 20f) data else overviewData,
                                    point = mapTap,
                                    width = size.width.toFloat(),
                                    height = size.height.toFloat(),
                                    dcHitRadius = dcRadiusPx / mapScale,
                                )?.let(onStateSelected)
                            }
                        }
                    }
                }
        ) {
            val borderWidth = (size.minDimension / 540f).coerceAtLeast(.8f)
            val viewportRect = visibleMapRect(mapOffset, mapScale, size.width, size.height)
            val activeStatePaths = if (mapScale >= 20f) stateDrawPaths else overviewStateDrawPaths
            val visibleStatePaths = activeStatePaths.filter { it.bounds.overlaps(viewportRect) }
            val labelCandidates = Array(HighwayClass.entries.size) { mutableListOf<HighwayLabelCandidate>() }
            val labelCandidateKeys = mutableSetOf<String>()
            val highwayLabels = highways?.labels.orEmpty()
            val highwayLevel = highways?.levelForScale(mapScale)
            val visibleHighwayTiles = highways?.visibleTiles(
                mapScale,
                visibleNorthAmericaBounds(mapOffset, mapScale, size.width, size.height),
            ).orEmpty()
            val highwayRenderTiles = if (highways != null && highwayLevel != null) {
                visibleHighwayTiles.map { tile ->
                    val key = HighwayRenderCacheKey(highwayLevel.minimumScale, tile.x, tile.y)
                    highwayRenderCache.getOrPut(key) {
                        buildHighwayRenderTile(tile, highways, size.width, size.height)
                    }
                }
            } else {
                emptyList()
            }
            val markerPlacements = buildTripMapMarkerPlacements(
                entries = locatedEntries,
                sightings = locatedSightings,
                mapScale = mapScale,
                mapOffset = mapOffset,
                width = size.width,
                height = size.height,
                journalCollisionDiameterPx = journalCollisionDiameterPx,
                plateCollisionSize = plateCollisionSize,
                platePreferredOffsetPx = plateMarkerHeightPx / 2f + plateMarkerStemPx,
                markerGapPx = markerGapPx,
                markerRingStepPx = markerRingStepPx,
            )
            if (!mapGestureActive) highwayRenderTiles.forEach { renderTile ->
                renderTile.labels.forEach { roadLabel ->
                    val candidatesForClass = labelCandidates[roadLabel.highwayClass.ordinal]
                    if (
                        candidatesForClass.size < 160 &&
                        mapScale >= minimumHighwayLabelScale(roadLabel.highwayClass)
                    ) {
                        val label = highwayLabels[roadLabel.labelIndex]
                        if (label.isNotBlank()) {
                            val screenPoint = Offset(
                                roadLabel.position.x * mapScale + mapOffset.x,
                                roadLabel.position.y * mapScale + mapOffset.y,
                            )
                            if (
                                screenPoint.x in -40f..size.width + 40f &&
                                screenPoint.y in -30f..size.height + 30f
                            ) {
                                val cellX = (screenPoint.x / 110.dp.toPx()).roundToInt()
                                val cellY = (screenPoint.y / 54.dp.toPx()).roundToInt()
                                val key = "${roadLabel.highwayClass.ordinal}:$cellX:$cellY"
                                if (labelCandidateKeys.add(key)) {
                                    candidatesForClass += HighwayLabelCandidate(label, screenPoint)
                                }
                            }
                        }
                    }
                }
            }

            withTransform({
                translate(mapOffset.x, mapOffset.y)
                scale(mapScale, mapScale, Offset.Zero)
            }) {
                listOf(-150.0, -120.0, -90.0, -60.0).forEach { longitude ->
                    val top = projectNorthAmerica(75.0, longitude, size.width, size.height)
                    val bottom = projectNorthAmerica(10.0, longitude, size.width, size.height)
                    drawLine(outline.copy(alpha = .18f), top, bottom, borderWidth / mapScale)
                }
                listOf(20.0, 40.0, 60.0).forEach { latitude ->
                    val left = projectNorthAmerica(latitude, -170.0, size.width, size.height)
                    val right = projectNorthAmerica(latitude, -50.0, size.width, size.height)
                    drawLine(outline.copy(alpha = .18f), left, right, borderWidth / mapScale)
                }
                visibleStatePaths.forEach { statePath ->
                    val jurisdiction = JurisdictionCatalog.byCode.getValue(statePath.code)
                    val fill = if (statePath.code in seen) Color(jurisdiction.region.colorHex) else unseenFill
                    drawPath(statePath.path, fill)
                }
                visibleStatePaths.forEach { statePath ->
                    if (statePath.code != selectedStateCode) {
                        drawPath(
                            statePath.path,
                            stateBoundary,
                            style = Stroke(stateBoundaryWidthPx / mapScale),
                        )
                    }
                }

                val roadColors = arrayOf(interstateRoad, usRouteRoad, stateRouteRoad)
                val roadWidths = floatArrayOf(2.5f, 2.05f, 1.55f)
                val casingWidths = floatArrayOf(4.6f, 4f, 3.35f)
                listOf(HighwayClass.STATE_ROUTE, HighwayClass.US_ROUTE, HighwayClass.INTERSTATE).forEach { highwayClass ->
                    val index = highwayClass.ordinal
                    highwayRenderTiles.forEach { renderTile ->
                        drawPath(
                            renderTile.paths[index],
                            roadCasing,
                            style = Stroke(casingWidths[index].dp.toPx() / mapScale),
                        )
                    }
                    highwayRenderTiles.forEach { renderTile ->
                        drawPath(
                            renderTile.paths[index],
                            roadColors[index],
                            style = Stroke(roadWidths[index].dp.toPx() / mapScale),
                        )
                    }
                }

                visibleStatePaths.firstOrNull { it.code == selectedStateCode }?.let { statePath ->
                    drawPath(
                        statePath.path,
                        selectionColor,
                        style = Stroke(selectedStateBoundaryWidthPx / mapScale),
                    )
                }

                val dc = projectNorthAmerica(38.9072, -77.0369, size.width, size.height)
                val dcFill = if ("DC" in seen) Color(JurisdictionCatalog.byCode.getValue("DC").region.colorHex) else unseenFill
                drawCircle(dcFill, dcRadiusPx / mapScale, dc)
                drawCircle(
                    if (selectedStateCode == "DC") selectionColor else stateBoundary,
                    dcRadiusPx / mapScale,
                    dc,
                    style = Stroke(
                        (if (selectedStateCode == "DC") selectedStateBoundaryWidthPx else stateBoundaryWidthPx) / mapScale,
                    ),
                )

            }

            val occupiedLabelRects = mutableListOf<Rect>()
            var labelsDrawn = 0
            var labelsConsidered = 0
            labelCandidates.forEach { candidates ->
                candidates.forEach candidateLoop@{ candidate ->
                    if (labelsDrawn >= 50 || labelsConsidered >= 90) return@candidateLoop
                    labelsConsidered += 1
                    val layout = textMeasurer.measure(candidate.text, style = roadLabelStyle)
                    val horizontalPadding = 4.dp.toPx()
                    val verticalPadding = 2.dp.toPx()
                    val labelSize = Size(
                        layout.size.width + horizontalPadding * 2,
                        layout.size.height + verticalPadding * 2,
                    )
                    val topLeft = Offset(
                        candidate.position.x - labelSize.width / 2,
                        candidate.position.y - labelSize.height / 2,
                    )
                    val rect = Rect(topLeft, labelSize)
                    val fullyVisible = rect.left >= 2f && rect.top >= 2f &&
                        rect.right <= size.width - 2f && rect.bottom <= size.height - 2f
                    if (fullyVisible && occupiedLabelRects.none { it.overlaps(rect) }) {
                        drawRoundRect(
                            color = roadLabelBackground,
                            topLeft = topLeft,
                            size = labelSize,
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = topLeft + Offset(horizontalPadding, verticalPadding),
                        )
                        occupiedLabelRects += rect
                        labelsDrawn += 1
                    }
                }
            }

            markerPlacements.forEach { placement ->
                val markerColor = when (placement.marker.kind) {
                    MapMarkerKind.JOURNAL -> locatedEntries
                        .firstOrNull { it.id == placement.marker.id }
                        ?.let { if (it.kind == JournalEntryKind.STOP) RoadOrange else Color(0xFF6E56CF) }
                    MapMarkerKind.PLATE -> locatedSightings
                        .firstOrNull { it.id == placement.marker.id }
                        ?.let { Color(JurisdictionCatalog.byCode.getValue(it.jurisdictionCode).region.colorHex) }
                } ?: return@forEach
                if (placement.marker.kind == MapMarkerKind.PLATE || placement.isDisplaced) {
                    drawLine(
                        color = markerLeaderCasing,
                        start = placement.center,
                        end = placement.marker.anchor,
                        strokeWidth = 3.5.dp.toPx(),
                    )
                    drawLine(
                        color = markerColor,
                        start = placement.center,
                        end = placement.marker.anchor,
                        strokeWidth = 1.75.dp.toPx(),
                    )
                }
                if (placement.isDisplaced) {
                    drawCircle(markerLeaderCasing, 2.5.dp.toPx(), placement.marker.anchor)
                    drawCircle(markerColor, 1.25.dp.toPx(), placement.marker.anchor)
                }
            }

            markerPlacements.filter { it.marker.kind == MapMarkerKind.JOURNAL }.forEach { placement ->
                val entry = locatedEntries.firstOrNull { it.id == placement.marker.id } ?: return@forEach
                val fill = if (entry.kind == JournalEntryKind.STOP) RoadOrange else Color(0xFF6E56CF)
                if (entry.id == selectedEntryId) {
                    drawCircle(Color.White, pinRadiusPx * 1.55f, placement.center)
                    drawCircle(selectionColor, pinRadiusPx * 1.55f, placement.center, style = Stroke(2.dp.toPx()))
                }
                drawCircle(fill, pinRadiusPx, placement.center)
                drawCircle(Color.White, pinRadiusPx, placement.center, style = Stroke(2.dp.toPx()))
            }

            markerPlacements.filter { it.marker.kind == MapMarkerKind.PLATE }.forEach { placement ->
                val sighting = locatedSightings.firstOrNull { it.id == placement.marker.id } ?: return@forEach
                val markerCenter = placement.center
                val markerTopLeft = markerCenter - Offset(plateMarkerWidthPx / 2f, plateMarkerHeightPx / 2f)
                val markerSize = Size(plateMarkerWidthPx, plateMarkerHeightPx)
                val jurisdiction = JurisdictionCatalog.byCode.getValue(sighting.jurisdictionCode)
                val markerColor = Color(jurisdiction.region.colorHex)
                val selected = sighting.jurisdictionCode == selectedStateCode
                if (selected) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = markerTopLeft - Offset(plateSelectionPaddingPx, plateSelectionPaddingPx),
                        size = Size(
                            markerSize.width + plateSelectionPaddingPx * 2,
                            markerSize.height + plateSelectionPaddingPx * 2,
                        ),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                    )
                    drawRoundRect(
                        color = selectionColor,
                        topLeft = markerTopLeft - Offset(plateSelectionPaddingPx, plateSelectionPaddingPx),
                        size = Size(
                            markerSize.width + plateSelectionPaddingPx * 2,
                            markerSize.height + plateSelectionPaddingPx * 2,
                        ),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                        style = Stroke(2.dp.toPx()),
                    )
                }
                drawRoundRect(
                    color = markerColor,
                    topLeft = markerTopLeft,
                    size = markerSize,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = markerTopLeft,
                    size = markerSize,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(1.25.dp.toPx()),
                )
                val codeLayout = textMeasurer.measure(sighting.jurisdictionCode, style = plateMarkerTextStyle)
                drawText(
                    textLayoutResult = codeLayout,
                    topLeft = markerCenter - Offset(codeLayout.size.width / 2f, codeLayout.size.height / 2f),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${(mapScale * 100).roundToInt()}% • ≈${approximateMapMilesAcross(mapScale, mapCenterLatitude(mapOffset, mapScale, mapCanvasSize.height))} mi",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    val newScale = nextMapScale(mapScale, zoomIn = false)
                    val center = Offset(mapCanvasSize.width / 2f, mapCanvasSize.height / 2f)
                    val candidate = center + (mapOffset - center) * (newScale / mapScale)
                    mapScale = newScale
                    mapOffset = clampMapOffset(candidate, newScale, mapCanvasSize.width.toFloat(), mapCanvasSize.height.toFloat())
                },
                enabled = mapScale > 1f,
            ) { Text("−", fontSize = 22.sp) }
            TextButton(onClick = {
                val newScale = nextMapScale(mapScale, zoomIn = true)
                val center = Offset(mapCanvasSize.width / 2f, mapCanvasSize.height / 2f)
                val candidate = center + (mapOffset - center) * (newScale / mapScale)
                mapScale = newScale
                mapOffset = clampMapOffset(candidate, newScale, mapCanvasSize.width.toFloat(), mapCanvasSize.height.toFloat())
            }) {
                Text("+", fontSize = 22.sp)
            }
            TextButton(
                onClick = { mapScale = 1f; mapOffset = Offset.Zero },
                enabled = mapScale > 1f || mapOffset != Offset.Zero,
            ) { Text("Reset") }
        }
        Text(
            "State-code markers: ${locatedSightings.size} plate sighting${if (locatedSightings.size == 1) "" else "s"} • Round journal pins: ${locatedEntries.size}",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Crowded markers spread apart; leader lines point to their exact locations.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (highways == null) {
            Text(
                if (highwaysUnavailable) "Offline highway layer unavailable" else "Loading offline highways…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            selectedEntry != null -> JournalMapSelectionCard(selectedEntry) { onEntryOpen(selectedEntry.id) }
            selectedJurisdiction != null -> StateMapSelectionCard(selectedJurisdiction, selectedSighting)
            else -> Text(
                "Tap a state or state-code marker for plate details, or tap a round pin to open the journal note.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onShare, modifier = Modifier.weight(1f)) { Text("Share plate map") }
            Text("🍁 ${if ("BC" in seen) "✓" else "—"}", fontWeight = FontWeight.Bold)
            Text("🌵 ${if ("MX" in seen) "✓" else "—"}", fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Region.entries.filterNot { it == Region.NON_US }.forEach { region ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(region.colorHex)))
                    Text(region.displayName, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        val missingJournalLocations = trip.journal.size - locatedEntries.size
        val missingSightingLocations = trip.sightings.size - locatedSightings.size
        if (missingJournalLocations > 0) {
            Text(
                "$missingJournalLocations journal entr${if (missingJournalLocations == 1) "y has" else "ies have"} no map location.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        if (missingSightingLocations > 0) {
            Text(
                "$missingSightingLocations plate sighting${if (missingSightingLocations == 1) " has" else "s have"} no map location.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun StateMapSelectionCard(
    jurisdiction: Jurisdiction,
    sighting: PlateSighting?,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(jurisdiction.region.colorHex)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(jurisdiction.name, fontWeight = FontWeight.Bold)
                Text(jurisdiction.region.displayName, style = MaterialTheme.typography.bodySmall)
                if (sighting == null) {
                    Text("Plate not seen yet", style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(
                        "Seen ${PlatformServices.formatDateTime(sighting.firstSeenAt)}${sighting.location.placeName?.let { " • $it" } ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(if (sighting == null) jurisdiction.code else "✓", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun JournalMapSelectionCard(entry: JournalEntry, onOpen: () -> Unit) {
    OutlinedCard(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "Open ${entry.title.ifBlank { "journal entry" }}" },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (entry.kind == JournalEntryKind.STOP) "📍" else "📝", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title.ifBlank { if (entry.kind == JournalEntryKind.STOP) "Road stop" else "Road note" },
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    entry.location.placeName ?: PlatformServices.formatDateTime(entry.occurredAt),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (entry.body.isNotBlank()) Text(entry.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Open ›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

private fun clampMapOffset(offset: Offset, scale: Float, width: Float, height: Float): Offset {
    if (scale <= 1f) return Offset.Zero
    return Offset(
        offset.x.coerceIn(width * (1f - scale), 0f),
        offset.y.coerceIn(height * (1f - scale), 0f),
    )
}

private fun projectNorthAmericaLocation(entry: JournalEntry, width: Float, height: Float): Offset =
    projectNorthAmerica(entry.location.latitude!!, entry.location.longitude!!, width, height)

private fun projectNorthAmericaLocation(sighting: PlateSighting, width: Float, height: Float): Offset =
    projectNorthAmerica(sighting.location.latitude!!, sighting.location.longitude!!, width, height)

private fun findStateAtPoint(
    data: StateVectorData,
    point: Offset,
    width: Float,
    height: Float,
    dcHitRadius: Float,
): String? {
    val dc = projectNorthAmerica(38.9072, -77.0369, width, height)
    if ((dc - point).getDistance() <= dcHitRadius) return "DC"
    return data.states.firstOrNull { state ->
        state.code != "DC" && state.polygons.any { polygon ->
            pointInStateRing(point, state.code, polygon.outer, width, height) &&
                polygon.holes.none { pointInStateRing(point, state.code, it, width, height) }
        }
    }?.code
}

internal fun pointInStateRing(
    point: Offset,
    code: String,
    coordinates: List<List<Double>>,
    width: Float,
    height: Float,
): Boolean {
    if (coordinates.size < 3) return false
    var inside = false
    var previous = coordinates.lastIndex
    coordinates.indices.forEach { index ->
        val currentPoint = coordinates[index]
        val previousPoint = coordinates[previous]
        if (currentPoint.size >= 2 && previousPoint.size >= 2) {
            val current = projectNorthAmericaStatePoint(code, currentPoint[0], currentPoint[1], width, height)
            val prior = projectNorthAmericaStatePoint(code, previousPoint[0], previousPoint[1], width, height)
            val crosses = (current.y > point.y) != (prior.y > point.y)
            if (crosses) {
                val crossingX = (prior.x - current.x) * (point.y - current.y) / (prior.y - current.y) + current.x
                if (point.x < crossingX) inside = !inside
            }
        }
        previous = index
    }
    return inside
}

private fun addNorthAmericaStateRing(
    path: Path,
    code: String,
    coordinates: List<List<Double>>,
    width: Float,
    height: Float,
) {
    coordinates.forEachIndexed { index, point ->
        if (point.size < 2) return@forEachIndexed
        val projected = projectNorthAmericaStatePoint(code, point[0], point[1], width, height)
        if (index == 0) path.moveTo(projected.x, projected.y) else path.lineTo(projected.x, projected.y)
    }
    path.close()
}

private fun projectNorthAmericaStatePoint(
    code: String,
    longitude: Double,
    latitude: Double,
    width: Float,
    height: Float,
): Offset {
    val normalizedLongitude = if (code == "AK" && longitude > 0.0) longitude - 360.0 else longitude
    return projectNorthAmerica(latitude, normalizedLongitude, width, height)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.addStateRing(
    path: Path,
    code: String,
    coordinates: List<List<Double>>,
    width: Float,
    height: Float,
) {
    coordinates.forEachIndexed { index, point ->
        if (point.size < 2) return@forEachIndexed
        val projected = projectStatePoint(code, point[0], point[1], width, height)
        if (index == 0) path.moveTo(projected.x, projected.y) else path.lineTo(projected.x, projected.y)
    }
    path.close()
}

private fun projectStatePoint(
    code: String,
    longitude: Double,
    latitude: Double,
    width: Float,
    height: Float,
): Offset {
    val bounds = when (code) {
        "AK" -> doubleArrayOf(-188.0, -130.0, 51.0, 72.0, .02, .31, .69, .98)
        "HI" -> doubleArrayOf(-161.0, -154.0, 18.0, 23.0, .35, .53, .76, .97)
        else -> doubleArrayOf(-125.0, -66.0, 24.0, 50.0, .035, .98, .025, .76)
    }
    val normalizedLongitude = if (code == "AK" && longitude > 0.0) longitude - 360.0 else longitude
    val xFraction = bounds[4] + (normalizedLongitude - bounds[0]) / (bounds[1] - bounds[0]) * (bounds[5] - bounds[4])
    val yFraction = bounds[6] + (bounds[3] - latitude) / (bounds[3] - bounds[2]) * (bounds[7] - bounds[6])
    return Offset((xFraction * width).toFloat(), (yFraction * height).toFloat())
}

private fun buildShareMapSvg(data: StateVectorData, trip: Trip): String {
    val seen = trip.sightings.mapTo(mutableSetOf()) { it.jurisdictionCode }
    val usSeen = seen.count { it in JurisdictionCatalog.byCode && it !in setOf("BC", "MX") }
    fun escape(value: String) = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
    fun number(value: Float): String = ((value * 10f).roundToInt() / 10f).toString()
    fun ring(code: String, points: List<List<Double>>): String = points.mapIndexedNotNull { index, point ->
        if (point.size < 2) null else projectStatePoint(code, point[0], point[1], 1200f, 760f).let { projected ->
            "${if (index == 0) "M" else "L"}${number(projected.x)},${number(projected.y + 90f)}"
        }
    }.joinToString(" ") + " Z"
    return buildString {
        append("""<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="0 0 1200 900">""")
        append("""<rect width="1200" height="900" rx="36" fill="#FFF9ED"/>""")
        append("""<rect width="1200" height="90" rx="36" fill="#183B45"/><rect y="54" width="1200" height="36" fill="#183B45"/>""")
        append("""<text x="52" y="57" fill="#FFF9ED" font-family="sans-serif" font-size="34" font-weight="700">${escape(trip.displayName)} — Roadtrippin</text>""")
        append("""<defs><pattern id="unseen" width="12" height="12" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="12" height="12" fill="#E7E2D8"/><line x1="0" y1="0" x2="0" y2="12" stroke="#C8C2B8" stroke-width="3"/></pattern></defs>""")
        data.states.forEach { state ->
            val jurisdiction = JurisdictionCatalog.byCode.getValue(state.code)
            val fill = if (state.code in seen) "#${jurisdiction.region.colorHex.toString(16).takeLast(6).uppercase()}" else "url(#unseen)"
            state.polygons.forEach { polygon ->
                val path = buildString {
                    append(ring(state.code, polygon.outer))
                    polygon.holes.forEach { append(' ').append(ring(state.code, it)) }
                }
                append("""<path d="$path" fill="$fill" fill-rule="evenodd" stroke="#53656A" stroke-width="1.5"/>""")
            }
        }
        val dc = projectStatePoint("DC", -77.0369, 38.9072, 1200f, 760f)
        val dcFill = if ("DC" in seen) "#${JurisdictionCatalog.byCode.getValue("DC").region.colorHex.toString(16).takeLast(6).uppercase()}" else "url(#unseen)"
        append("""<line x1="${dc.x}" y1="${dc.y + 90}" x2="1115" y2="255" stroke="#53656A" stroke-width="2"/>""")
        append("""<circle cx="1115" cy="255" r="17" fill="$dcFill" stroke="#53656A" stroke-width="2"/><text x="1140" y="264" font-family="sans-serif" font-size="24" fill="#183B45">D.C.</text>""")
        append("""<rect x="42" y="810" width="1116" height="58" rx="18" fill="#183B45"/>""")
        append("""<text x="70" y="848" fill="#FFF9ED" font-family="sans-serif" font-size="25" font-weight="700">$usSeen / 51 U.S. jurisdictions</text>""")
        append("""<text x="625" y="848" fill="#FFF9ED" font-family="sans-serif" font-size="23">B.C. ${if ("BC" in seen) "✓" else "—"}   Mexico ${if ("MX" in seen) "✓" else "—"}</text>""")
        append("</svg>")
    }
}

private fun projectNorthAmerica(latitude: Double, longitude: Double, width: Float, height: Float): Offset {
    val x = ((longitude + 170.0) / 120.0).coerceIn(0.0, 1.0).toFloat() * width
    val y = ((75.0 - latitude) / 65.0).coerceIn(0.0, 1.0).toFloat() * height
    return Offset(x, y)
}

@Composable
fun AwardsScreen(store: RoadtrippinStore, padding: PaddingValues) {
    val trip = store.activeTrip ?: return
    val tripAwards = store.achievementsFor(trip)
    val lifetime = store.lifetimeAchievements()
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { AppHeader("Awards", "Badges follow your current trip data", onBack = { store.screen = AppScreen.DASHBOARD }) }
        item { AwardSectionTitle("This trip", tripAwards.count { it.earned }, tripAwards.size) }
        items(tripAwards, key = { it.id }) { AwardCard(it) }
        item { AwardSectionTitle("Lifetime", lifetime.count { it.earned }, lifetime.size) }
        items(lifetime, key = { it.id }) { AwardCard(it) }
    }
}

@Composable
private fun AwardSectionTitle(title: String, earned: Int, total: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("$earned / $total", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AwardCard(award: Achievement) {
    val alpha = if (award.earned) 1f else .55f
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (award.earned) award.icon else "🔒", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(award.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                Text(award.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                LinearProgressIndicator(
                    progress = { award.progress / award.target.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(5.dp).clip(CircleShape),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("${award.progress}/${award.target}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TripInfoScreen(store: RoadtrippinStore, padding: PaddingValues) {
    val trip = store.activeTrip ?: return
    var name by remember(trip.id) { mutableStateOf(trip.name) }
    var destination by remember(trip.id) { mutableStateOf(trip.destination) }
    var vehicle by remember(trip.id) { mutableStateOf(trip.vehicle) }
    var notes by remember(trip.id) { mutableStateOf(trip.notes) }
    var travelers by remember(trip.id) { mutableStateOf(trip.travelers.joinToString(", ") { it.name }) }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppHeader("Trip info", "Add details whenever you have time", onBack = { store.screen = AppScreen.DASHBOARD })
        ReadOnlyInfo("Started", PlatformServices.formatDateTime(trip.startedAt))
        ReadOnlyInfo("Starting place", trip.startLocation.placeName ?: if (trip.startLocation.hasCoordinates) "Saved coordinates" else "Location unavailable")
        OutlinedTextField(name, { name = it }, label = { Text("Trip name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(destination, { destination = it }, label = { Text("Destination") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(travelers, { travelers = it }, label = { Text("Travelers") }, supportingText = { Text("Separate names with commas") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(vehicle, { vehicle = it }, label = { Text("Vehicle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Trip notes") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                store.updateTripInfo(name, destination, vehicle, notes, travelers.split(','))
                store.screen = AppScreen.DASHBOARD
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Save trip info") }
    }
}

@Composable
private fun ReadOnlyInfo(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsScreen(store: RoadtrippinStore, padding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppHeader("Settings", "Roadtrippin preferences", onBack = { store.screen = if (store.activeTrip != null) AppScreen.DASHBOARD else AppScreen.HOME })
        SettingSwitch("Sound", "Say \u201cHazzah!\u201d for a new plate", store.settings.soundEnabled) {
            store.updateSettings { settings -> settings.copy(soundEnabled = it) }
        }
        SettingSwitch("Haptics", "Use a short vibration for a new plate", store.settings.hapticsEnabled) {
            store.updateSettings { settings -> settings.copy(hapticsEnabled = it) }
        }
        SettingSwitch("Animations", "Animate checks, celebrations, and progress", store.settings.animationsEnabled) {
            store.updateSettings { settings -> settings.copy(animationsEnabled = it) }
        }
        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "System", false to "Light", true to "Dark").forEach { (value, label) ->
                if (store.settings.forceDarkMode == value) Button(onClick = {}) { Text(label) }
                else OutlinedButton(onClick = { store.updateSettings { it.copy(forceDarkMode = value) } }) { Text(label) }
            }
        }
        OutlinedButton(onClick = { store.screen = AppScreen.ACCOUNT }, modifier = Modifier.fillMaxWidth()) { Text("Account & cloud backup") }
        Text("Roadtrippin 0.1.0 beta", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("Privacy: bentnail.studio/roadtrippin/privacy", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onChecked)
        }
    }
}

@Composable
fun AccountScreen(store: RoadtrippinStore, snackbar: SnackbarHostState, padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val accounts = CloudServices.accounts
    val session by accounts.observeSession().collectAsState(initial = null)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var showUploadConfirmation by remember { mutableStateOf(false) }
    val sessionId = session?.userId
    LaunchedEffect(sessionId) {
        val userId = sessionId ?: return@LaunchedEffect
        showUploadConfirmation = store.cloudUploadNeedsConfirmation(userId)
    }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppHeader("Cloud backup", "Optional Supabase account", onBack = { store.screen = if (store.activeTrip != null) AppScreen.DASHBOARD else AppScreen.HOME })
        Surface(color = RoadGreen.copy(alpha = .13f), shape = RoundedCornerShape(14.dp)) {
            Text("Roadtrippin always works offline. Sign in only if you want private automatic backup across devices.", Modifier.padding(14.dp))
        }
        if (!CloudServices.configured) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                Text("Cloud backup is disabled in this build. Add the Supabase URL and publishable key to ignored local configuration.", Modifier.padding(14.dp))
            }
        } else if (session != null) {
            Text("Signed in as ${session?.email ?: session?.userId}", fontWeight = FontWeight.Bold)
            Text("This device retains all trips after sign-out.", style = MaterialTheme.typography.bodySmall)
            val cloudStatus = when (store.syncState) {
                SyncState.LOCAL_ONLY -> "Waiting for account confirmation"
                SyncState.QUEUED -> "${store.pendingSyncCount} local change${if (store.pendingSyncCount == 1) "" else "s"} queued"
                SyncState.SYNCING -> "Syncing private trip data…"
                SyncState.SYNCED -> "Cloud backup is up to date"
                SyncState.ERROR -> "Sync paused after an error; local data is safe"
            }
            Surface(
                color = if (store.syncState == SyncState.ERROR) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (store.syncState == SyncState.SYNCING) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Column(Modifier.weight(1f)) {
                        Text(cloudStatus, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${store.trips.size} trips • ${store.localPhotoCount} photos",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val userId = sessionId ?: return@Button
                    if (store.cloudUploadNeedsConfirmation(userId)) {
                        showUploadConfirmation = true
                    } else {
                        scope.launch {
                            store.syncNow(userId).onFailure {
                                snackbar.showSnackbar(it.message ?: "Cloud sync failed")
                            }
                        }
                    }
                },
                enabled = store.syncState != SyncState.SYNCING,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (store.cloudUploadNeedsConfirmation(sessionId.orEmpty())) "Review backup" else "Sync now") }
            OutlinedButton(
                onClick = { scope.launch { runCatching { accounts.signOut() }.onFailure { snackbar.showSnackbar(it.message ?: "Sign-out failed") } } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
        } else {
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { scope.launch { runCatching { accounts.signIn(email, password) }.onFailure { snackbar.showSnackbar(it.message ?: "Sign-in failed") } } },
                enabled = email.isNotBlank() && password.length >= 8,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign in") }
            OutlinedButton(
                onClick = { scope.launch { runCatching { accounts.signUp(email, password) }.onFailure { snackbar.showSnackbar(it.message ?: "Sign-up failed") } } },
                enabled = email.isNotBlank() && password.length >= 8,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create account") }
            OutlinedButton(
                onClick = { scope.launch { runCatching { accounts.signInWithGoogle() }.onFailure { snackbar.showSnackbar(it.message ?: "Google sign-in failed") } } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue with Google") }
            if (PlatformServices.supportsNativeAppleSignIn) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching { accounts.signInWithApple() }
                                .onFailure { snackbar.showSnackbar(it.message ?: "Apple sign-in failed") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue with Apple") }
            }
        }
        Text("Cloud credentials are configured at build time. No service-role key is included in the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete shared account everywhere", color = MaterialTheme.colorScheme.error)
        }
    }
    if (showUploadConfirmation && session != null) {
        AlertDialog(
            onDismissRequest = { showUploadConfirmation = false },
            title = { Text("Back up this device?") },
            text = {
                Text(
                    "Upload all ${store.trips.size} retained trip${if (store.trips.size == 1) "" else "s"} and " +
                        "${store.localPhotoCount} photo${if (store.localPhotoCount == 1) "" else "s"} to " +
                        "${session?.email ?: "this account"}? Existing account data will be merged by edit time."
                )
            },
            confirmButton = {
                Button(onClick = {
                    val userId = sessionId ?: return@Button
                    showUploadConfirmation = false
                    scope.launch {
                        store.confirmCloudUploadAndSync(userId).onFailure {
                            snackbar.showSnackbar(it.message ?: "Cloud sync failed; local data is unchanged")
                        }
                    }
                }) { Text("Confirm and sync") }
            },
            dismissButton = {
                TextButton(onClick = { showUploadConfirmation = false }) { Text("Not now") }
            },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("This also deletes Muddlist") },
            text = { Text("This shared Supabase identity permanently deletes Roadtrippin trips, journal photos, local data, and all Muddlist tasks. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Disabled until isolated test passes") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AppHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("‹ Back") }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun EmptyCard(title: String, body: String, icon: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 42.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
