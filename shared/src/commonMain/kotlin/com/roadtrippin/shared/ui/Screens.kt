package com.roadtrippin.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.roadtrippin.shared.domain.Region
import com.roadtrippin.shared.domain.SyncState
import com.roadtrippin.shared.domain.TagQuantity
import com.roadtrippin.shared.domain.Trip
import com.roadtrippin.shared.platform.PlatformServices
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
                    Text(
                        "Start now—name the trip and add the details whenever you have time.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f),
                    )
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
                        else Text("Skip naming & start")
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
    val foreground = if (seen && jurisdiction.region in setOf(Region.PACIFIC_NORTHWEST, Region.NEW_ENGLAND, Region.MID_ATLANTIC)) Color.White else MaterialTheme.colorScheme.onSurface
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
                    onEdit = { editingEntryId = entry.id },
                    onDelete = { store.deleteJournalEntry(entry.id) },
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
                        snackbar.showSnackbar(if (kind == JournalEntryKind.STOP) "Stop logged" else "Journal note saved")
                    }
                }
                showEditor = false
                editingEntryId = null
            },
        )
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onEdit)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (entry.kind == JournalEntryKind.STOP) "📍" else "📝", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.title.ifBlank { if (entry.kind == JournalEntryKind.STOP) "Road stop" else "Road note" }, fontWeight = FontWeight.Bold)
                    Text(PlatformServices.formatDateTime(entry.occurredAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onEdit) { Text("Edit") }
            }
            if (entry.body.isNotBlank()) Text(entry.body)
            if (entry.location.placeName != null) Text("📌 ${entry.location.placeName}", color = MaterialTheme.colorScheme.primary)
            if (entry.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.take(3).forEach { AssistChip(onClick = {}, label = { Text("${it.tag} ×${it.quantity}") }) }
                }
            }
            if (entry.photos.isNotEmpty()) Text("${entry.photos.size} photo${if (entry.photos.size == 1) "" else "s"}")
            TextButton(onClick = onDelete) { Text("Delete entry", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun JournalEditorDialog(
    initialEntry: JournalEntry?,
    onDismiss: () -> Unit,
    onSave: (JournalEntryKind, String, String, List<TagQuantity>, Long, com.roadtrippin.shared.domain.LocationStamp, List<JournalPhoto>) -> Unit,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == JournalEntryKind.STOP) "Log a stop" else "Add a road note") },
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
}

@Composable
fun MapScreen(store: RoadtrippinStore, padding: PaddingValues) {
    val trip = store.activeTrip ?: return
    Column(Modifier.fillMaxSize().padding(padding)) {
        AppHeader(
            "Trip map",
            if (store.mapShowsJournal) "Clickable journal and stop pins" else "Seen states use their region color",
            onBack = { store.screen = AppScreen.DASHBOARD },
            modifier = Modifier.padding(20.dp),
        )
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeButton("Plate progress", !store.mapShowsJournal) { store.mapShowsJournal = false }
            ModeButton("Trip journal", store.mapShowsJournal) { store.mapShowsJournal = true }
        }
        Spacer(Modifier.height(14.dp))
        if (store.mapShowsJournal) JournalPinMap(trip, Modifier.fillMaxSize().padding(20.dp))
        else PlateTileMap(trip, Modifier.fillMaxSize().padding(horizontal = 12.dp))
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
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

@Composable
private fun PlateTileMap(trip: Trip, modifier: Modifier = Modifier) {
    val seen = trip.sightings.map { it.jurisdictionCode }.toSet()
    var vectorData by remember { mutableStateOf<StateVectorData?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching {
            vectorJson.decodeFromString<StateVectorData>(
                Res.readBytes("files/us_states_20m.json").decodeToString()
            )
        }.onSuccess { vectorData = it }.onFailure { loadFailed = true }
    }
    Column(modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("United States", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        when {
            vectorData != null -> StateVectorMap(
                data = vectorData!!,
                seen = seen,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.28f),
            )
            loadFailed -> EmptyCard("Map unavailable", "The bundled state vectors could not be loaded.", "🗺️")
            else -> Box(
                Modifier.fillMaxWidth().aspectRatio(1.28f),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        Text(
            "Offline U.S. Census 2025 cartographic boundaries • Alaska and Hawaii use insets • D.C. uses a callout",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        vectorData?.let { data ->
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    PlatformServices.shareMapImage(
                        title = "${trip.displayName} map",
                        svg = buildShareMapSvg(data, trip),
                        summary = "${trip.displayName}: ${seen.size} of 53 Roadtrippin plates spotted. Precise locations are not included.",
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            ) { Text("Share map image") }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountryBadge("British Columbia", "BC" in seen, "🍁")
            CountryBadge("Mexico", "MX" in seen, "🌵")
        }
        Spacer(Modifier.height(18.dp))
        Text("Region legend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Region.entries.filterNot { it == Region.NON_US }.forEach { region ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(region.colorHex)))
                Spacer(Modifier.width(8.dp))
                Text(region.displayName)
            }
        }
    }
}

@Composable
private fun StateVectorMap(
    data: StateVectorData,
    seen: Set<String>,
    modifier: Modifier = Modifier,
) {
    val unseenFill = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = .72f)
    val background = MaterialTheme.colorScheme.surface
    Canvas(
        modifier.background(background, RoundedCornerShape(18.dp))
            .semantics {
                contentDescription = "United States plate progress map, ${seen.count { it in JurisdictionCatalog.byCode && it !in setOf("BC", "MX") }} jurisdictions seen"
            }
            .padding(8.dp)
    ) {
        val borderWidth = (size.minDimension / 520f).coerceAtLeast(0.75f)
        data.states.forEach { state ->
            val jurisdiction = JurisdictionCatalog.byCode.getValue(state.code)
            val fill = if (state.code in seen) Color(jurisdiction.region.colorHex) else unseenFill
            state.polygons.forEach { polygon ->
                val path = Path().apply { fillType = PathFillType.EvenOdd }
                addStateRing(path, state.code, polygon.outer, size.width, size.height)
                polygon.holes.forEach { hole -> addStateRing(path, state.code, hole, size.width, size.height) }
                drawPath(path, fill)
                drawPath(path, outline, style = Stroke(borderWidth))
            }
        }

        val dc = projectStatePoint("DC", -77.0369, 38.9072, size.width, size.height)
        val callout = Offset(size.width * .93f, size.height * .21f)
        drawLine(outline, dc, callout, strokeWidth = borderWidth * 1.5f)
        val dcFill = if ("DC" in seen) Color(JurisdictionCatalog.byCode.getValue("DC").region.colorHex) else unseenFill
        drawCircle(dcFill, radius = size.minDimension * .018f, center = callout)
        drawCircle(outline, radius = size.minDimension * .018f, center = callout, style = Stroke(borderWidth))
    }
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

@Composable
private fun CountryBadge(label: String, seen: Boolean, icon: String) {
    Surface(
        color = if (seen) RoadGreen else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 23.sp)
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text(if (seen) "Seen ✓" else "Not seen", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun JournalPinMap(trip: Trip, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<JournalEntry?>(null) }
    var mapScale by remember { mutableStateOf(1f) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    val pinRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
    Column(modifier) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFDCEBF2))) {
            Canvas(
                Modifier.fillMaxSize()
                    .semantics { contentDescription = "Offline North America journal map. Pinch to zoom and drag to pan." }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (mapScale * zoom).coerceIn(1f, 5f)
                            val ratio = newScale / mapScale
                            mapOffset = centroid + (mapOffset - centroid) * ratio + pan
                            mapScale = newScale
                        }
                    }
                    .pointerInput(trip.journal, mapScale, mapOffset) {
                    detectTapGestures { tap ->
                        val mapTap = Offset(
                            (tap.x - mapOffset.x) / mapScale,
                            (tap.y - mapOffset.y) / mapScale,
                        )
                        selected = trip.journal.filter { it.location.hasCoordinates }.minByOrNull { entry ->
                            val point = projectNorthAmerica(entry.location.latitude!!, entry.location.longitude!!, size.width.toFloat(), size.height.toFloat())
                            (point - mapTap).getDistance()
                        }?.takeIf { entry ->
                            val point = projectNorthAmerica(entry.location.latitude!!, entry.location.longitude!!, size.width.toFloat(), size.height.toFloat())
                            (point - mapTap).getDistance() <= pinRadiusPx * 2 / mapScale
                        }
                    }
                }
            ) {
                val land = Path().apply {
                    moveTo(size.width * .12f, size.height * .18f)
                    lineTo(size.width * .38f, size.height * .06f)
                    lineTo(size.width * .72f, size.height * .15f)
                    lineTo(size.width * .88f, size.height * .32f)
                    lineTo(size.width * .78f, size.height * .52f)
                    lineTo(size.width * .67f, size.height * .57f)
                    lineTo(size.width * .61f, size.height * .80f)
                    lineTo(size.width * .47f, size.height * .94f)
                    lineTo(size.width * .36f, size.height * .75f)
                    lineTo(size.width * .20f, size.height * .60f)
                    close()
                }
                withTransform({
                    translate(mapOffset.x, mapOffset.y)
                    scale(mapScale, mapScale, Offset.Zero)
                }) {
                    drawPath(land, color = Color(0xFFE8DFBE))
                    drawPath(land, color = Color(0xFF7E8A70), style = Stroke(2.dp.toPx() / mapScale))
                    trip.journal.filter { it.location.hasCoordinates }.forEach { entry ->
                        val point = projectNorthAmerica(entry.location.latitude!!, entry.location.longitude!!, size.width, size.height)
                        drawCircle(if (entry.kind == JournalEntryKind.STOP) RoadOrange else Color(0xFFB07AA1), pinRadiusPx / mapScale, point)
                        drawCircle(Color.White, pinRadiusPx / mapScale, point, style = Stroke(2.dp.toPx() / mapScale))
                    }
                }
            }
            if (trip.journal.none { it.location.hasCoordinates }) {
                Text(
                    "Journal pins appear here when a location is available.",
                    Modifier.align(Alignment.Center).padding(28.dp),
                    color = Color(0xFF30434E),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("North America • offline", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            if (mapScale > 1f || mapOffset != Offset.Zero) {
                TextButton(onClick = { mapScale = 1f; mapOffset = Offset.Zero }) { Text("Reset map") }
            }
        }
        AnimatedVisibility(selected != null) {
            selected?.let { entry ->
                Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(entry.title.ifBlank { "Road note" }, fontWeight = FontWeight.Bold)
                        Text(entry.location.placeName ?: "Saved coordinates")
                        Text(entry.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
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
        SettingSwitch("Sound", "Play a celebration sound for a new plate", store.settings.soundEnabled) {
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
