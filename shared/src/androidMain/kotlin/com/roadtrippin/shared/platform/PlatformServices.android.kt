package com.roadtrippin.shared.platform

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.net.Uri
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.roadtrippin.shared.domain.JournalPhoto
import com.roadtrippin.shared.domain.LocationStamp
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

actual object PlatformServices {
    private var context: Context? = null
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var permissionResult: CompletableDeferred<Boolean>? = null
    private var libraryLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var photoResult: CompletableDeferred<List<JournalPhoto>>? = null
    private var cameraInput: File? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var pendingCheer = false

    fun initialize(activity: ComponentActivity) {
        val appContext = activity.applicationContext
        context = appContext
        if (speech == null) {
            speech = TextToSpeech(appContext) { status ->
                speechReady = status == TextToSpeech.SUCCESS
                if (speechReady) {
                    speech?.language = Locale.US
                    speech?.setSpeechRate(0.95f)
                    if (pendingCheer) {
                        pendingCheer = false
                        speakNewPlateCheer()
                    }
                } else {
                    pendingCheer = false
                }
            }
        }
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            permissionResult?.complete(result.values.any { it })
            permissionResult = null
        }
        libraryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK_COUNT),
        ) { uris ->
            val deferred = photoResult
            photoResult = null
            ioScope.launch {
                deferred?.complete(uris.mapNotNull(::processPhotoUri))
            }
        }
        cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val deferred = photoResult
            photoResult = null
            val source = cameraInput
            cameraInput = null
            ioScope.launch {
                val photo = if (saved && source != null) processPhotoFile(source) else null
                source?.delete()
                deferred?.complete(listOfNotNull(photo))
            }
        }
    }

    actual fun nowEpochMillis(): Long = System.currentTimeMillis()
    actual val supportsNativeAppleSignIn: Boolean = false
    actual fun randomId(): String = UUID.randomUUID().toString()
    actual fun formatDateTime(epochMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

    actual suspend fun currentLocation(): LocationStamp? {
        val appContext = context ?: return null
        if (!ensureLocationPermission(appContext)) return null
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = bestLastKnownLocation(manager) ?: withTimeoutOrNull(8_000) {
            currentLocation(manager, LocationManager.GPS_PROVIDER)
                ?: currentLocation(manager, LocationManager.NETWORK_PROVIDER)
        } ?: return null
        return LocationStamp(location.latitude, location.longitude)
    }

    actual suspend fun reverseGeocode(location: LocationStamp): String? {
        val appContext = context ?: return null
        val latitude = location.latitude ?: return null
        val longitude = location.longitude ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
            @Suppress("DEPRECATION")
            Geocoder(appContext, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.let { address -> listOfNotNull(address.locality, address.adminArea).joinToString(", ") }
            }.getOrNull()?.ifBlank { null }
        }
    }

    actual suspend fun pickJournalPhotos(limit: Int): List<JournalPhoto> {
        val launcher = libraryLauncher ?: return emptyList()
        val deferred = CompletableDeferred<List<JournalPhoto>>()
        photoResult?.cancel()
        photoResult = deferred
        withContext(Dispatchers.Main.immediate) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        return deferred.await().take(limit.coerceIn(1, MAX_PICK_COUNT))
    }

    actual suspend fun takeJournalPhoto(): JournalPhoto? {
        val appContext = context ?: return null
        val launcher = cameraLauncher ?: return null
        val input = withContext(Dispatchers.IO) {
            File.createTempFile("roadtrippin-camera-", ".jpg", appContext.cacheDir)
        }
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", input)
        val deferred = CompletableDeferred<List<JournalPhoto>>()
        photoResult?.cancel()
        photoResult = deferred
        cameraInput = input
        withContext(Dispatchers.Main.immediate) { launcher.launch(uri) }
        return deferred.await().firstOrNull()
    }

    actual suspend fun readLocalFile(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(path).takeIf(File::isFile)?.readBytes() }.getOrNull()
    }

    actual suspend fun saveJournalPhoto(id: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = checkNotNull(context)
            val directory = File(appContext.filesDir, JOURNAL_DIRECTORY).apply { mkdirs() }
            File(directory, "$id.jpg").apply { writeBytes(bytes) }.absolutePath
        }.getOrNull()
    }

    actual suspend fun requestAppleIdToken(): String? = null

    actual fun observeConnectivity(): Flow<Boolean> {
        val appContext = context ?: return flowOf(true)
        return callbackFlow {
            val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            fun connected(): Boolean = manager.activeNetwork
                ?.let(manager::getNetworkCapabilities)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            trySend(connected())
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { trySend(true) }
                override fun onLost(network: Network) { trySend(connected()) }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                }
            }
            manager.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                callback,
            )
            awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
        }
    }

    actual fun celebrate(sound: Boolean, haptics: Boolean) {
        val appContext = context ?: return
        if (haptics) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                    (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        if (sound) {
            runCatching(::speakNewPlateCheer)
        }
    }

    private fun speakNewPlateCheer() {
        val synthesizer = speech
        if (!speechReady || synthesizer == null) {
            pendingCheer = true
            return
        }
        synthesizer.speak(
            NEW_PLATE_CHEER,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "roadtrippin-new-plate",
        )
    }

    actual fun shareText(title: String, text: String) {
        val appContext = context ?: return
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            title,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    actual fun shareMapImage(title: String, svg: String, summary: String) {
        val appContext = context ?: return
        ioScope.launch {
            val directory = File(appContext.cacheDir, "shared_maps").apply { mkdirs() }
            val file = File(directory, "roadtrippin-map.svg").apply { writeText(svg) }
            val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", file)
            val intent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/svg+xml"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, summary)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri(title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                title,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
    }

    private suspend fun ensureLocationPermission(appContext: Context): Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (permissions.any { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }) {
            return true
        }
        val launcher = permissionLauncher ?: return false
        val deferred = CompletableDeferred<Boolean>()
        permissionResult?.cancel()
        permissionResult = deferred
        withContext(Dispatchers.Main.immediate) { launcher.launch(permissions) }
        return deferred.await()
    }

    private fun bestLastKnownLocation(manager: LocationManager): Location? = runCatching {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull(manager::getLastKnownLocation)
            .maxByOrNull(Location::getTime)
    }.getOrNull()

    private suspend fun currentLocation(manager: LocationManager, provider: String): Location? {
        if (!runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)) return null
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    manager.getCurrentLocation(provider, null, context?.mainExecutor ?: return@runCatching) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                }
                runCatching { manager.requestSingleUpdate(provider, listener, Looper.getMainLooper()) }
                    .onFailure { if (continuation.isActive) continuation.resume(null) }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        }
    }

    private fun processPhotoFile(file: File): JournalPhoto? = processPhotoSource(
        ImageDecoder.createSource(file),
    )

    private fun processPhotoUri(uri: Uri): JournalPhoto? {
        val appContext = context ?: return null
        return processPhotoSource(ImageDecoder.createSource(appContext.contentResolver, uri))
    }

    private fun processPhotoSource(source: ImageDecoder.Source): JournalPhoto? = runCatching {
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val longest = maxOf(width, height)
            if (longest > MAX_IMAGE_DIMENSION) {
                val scale = MAX_IMAGE_DIMENSION.toDouble() / longest.toDouble()
                decoder.setTargetSize((width * scale).toInt(), (height * scale).toInt())
            }
        }
        val id = randomId()
        val appContext = checkNotNull(context)
        val directory = File(appContext.filesDir, JOURNAL_DIRECTORY).apply { mkdirs() }
        val output = File(directory, "$id.jpg")
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        JournalPhoto(id = id, localPath = output.absolutePath)
    }.getOrNull()

    private const val MAX_PICK_COUNT = 5
    private const val MAX_IMAGE_DIMENSION = 2048
    private const val JOURNAL_DIRECTORY = "journal_photos"
}
