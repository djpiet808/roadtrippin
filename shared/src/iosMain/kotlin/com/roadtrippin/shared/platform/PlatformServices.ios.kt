package com.roadtrippin.shared.platform

import com.roadtrippin.shared.domain.LocationStamp
import com.roadtrippin.shared.domain.JournalPhoto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreGraphics.CGRectMake
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSError
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIWindow
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.posix.memcpy
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object PlatformServices {
    private const val EPOCH_TO_REFERENCE_SECONDS = 978_307_200.0
    private var activeLocationRequest: IosLocationRequest? = null
    private var activePhotoRequest: IosPhotoRequest? = null
    private var activeAppleRequest: IosAppleSignInRequest? = null
    private val speechSynthesizer = AVSpeechSynthesizer()

    actual fun nowEpochMillis(): Long =
        ((NSDate().timeIntervalSinceReferenceDate + EPOCH_TO_REFERENCE_SECONDS) * 1_000.0).toLong()
    actual val supportsNativeAppleSignIn: Boolean = true
    actual fun randomId(): String = NSUUID().UUIDString()
    actual fun formatDateTime(epochMillis: Long): String {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = NSDateFormatterShortStyle
        }
        return formatter.stringFromDate(
            NSDate(timeIntervalSinceReferenceDate = epochMillis / 1_000.0 - EPOCH_TO_REFERENCE_SECONDS)
        )
    }

    actual suspend fun currentLocation(): LocationStamp? = suspendCancellableCoroutine { continuation ->
        dispatch_async(dispatch_get_main_queue()) {
            val request = IosLocationRequest { location ->
                activeLocationRequest = null
                if (continuation.isActive) continuation.resume(location?.toLocationStamp())
            }
            activeLocationRequest?.finish(null)
            activeLocationRequest = request
            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) {
                    if (activeLocationRequest === request) activeLocationRequest = null
                    request.finish(null)
                }
            }
            request.start()
        }
    }

    actual suspend fun reverseGeocode(location: LocationStamp): String? {
        val latitude = location.latitude ?: return null
        val longitude = location.longitude ?: return null
        return suspendCancellableCoroutine { continuation ->
            val geocoder = CLGeocoder()
            geocoder.reverseGeocodeLocation(CLLocation(latitude, longitude)) { placemarks, _ ->
                val placemark = placemarks?.firstOrNull() as? CLPlacemark
                val place = listOfNotNull(placemark?.locality, placemark?.administrativeArea)
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { null }
                if (continuation.isActive) continuation.resume(place)
            }
            continuation.invokeOnCancellation { geocoder.cancelGeocode() }
        }
    }

    actual suspend fun pickJournalPhotos(limit: Int): List<JournalPhoto> =
        requestPhoto(useCamera = false)?.let(::listOf).orEmpty().take(limit.coerceAtLeast(1))

    actual suspend fun takeJournalPhoto(): JournalPhoto? = requestPhoto(useCamera = true)

    private suspend fun requestPhoto(useCamera: Boolean): JournalPhoto? =
        suspendCancellableCoroutine { continuation ->
            dispatch_async(dispatch_get_main_queue()) {
                val request = IosPhotoRequest(useCamera) { photo ->
                    activePhotoRequest = null
                    if (continuation.isActive) continuation.resume(photo)
                }
                activePhotoRequest?.finish(null)
                activePhotoRequest = request
                continuation.invokeOnCancellation {
                    dispatch_async(dispatch_get_main_queue()) {
                        if (activePhotoRequest === request) activePhotoRequest = null
                        request.finish(null)
                    }
                }
                request.start()
            }
        }

    actual suspend fun readLocalFile(path: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    actual suspend fun saveJournalPhoto(id: String, bytes: ByteArray): String? = runCatching {
        val path = journalPhotoPath(id)
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        if (data.writeToFile(path, atomically = true)) path else null
    }.getOrNull()

    actual fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_queue_create("com.roadtrippin.connectivity", null)
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }

    actual suspend fun requestAppleIdToken(): String? = suspendCancellableCoroutine { continuation ->
        dispatch_async(dispatch_get_main_queue()) {
            val request = IosAppleSignInRequest { token ->
                activeAppleRequest = null
                if (continuation.isActive) continuation.resume(token)
            }
            activeAppleRequest?.finish(null)
            activeAppleRequest = request
            continuation.invokeOnCancellation {
                if (activeAppleRequest === request) activeAppleRequest = null
            }
            request.start()
        }
    }

    actual fun celebrate(sound: Boolean, haptics: Boolean) {
        dispatch_async(dispatch_get_main_queue()) {
            if (haptics) {
                UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium).impactOccurred()
            }
            if (sound) {
                speechSynthesizer.speakUtterance(AVSpeechUtterance(string = NEW_PLATE_CHEER))
            }
        }
    }

    actual fun shareText(title: String, text: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val presenter = topViewController() ?: return@dispatch_async
            val controller = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            presenter.presentViewController(controller, animated = true, completion = null)
        }
    }

    actual fun shareMapImage(title: String, svg: String, summary: String) {
        val documents = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return
        val directory = "$documents/shared_maps"
        NSFileManager.defaultManager.createDirectoryAtPath(
            directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val path = "$directory/roadtrippin-map.svg"
        val bytes = svg.encodeToByteArray()
        val saved = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(path, atomically = true)
        }
        if (!saved) return
        dispatch_async(dispatch_get_main_queue()) {
            val presenter = topViewController() ?: return@dispatch_async
            val controller = UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(path), summary),
                applicationActivities = null,
            )
            presenter.presentViewController(controller, animated = true, completion = null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosAppleSignInRequest(
    private val onComplete: (String?) -> Unit,
) : NSObject(), ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {
    private var completed = false
    private var controller: ASAuthorizationController? = null

    fun start() {
        val request = ASAuthorizationAppleIDProvider().createRequest().apply {
            requestedScopes = listOfNotNull(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
        }
        controller = ASAuthorizationController(listOf(request)).also {
            it.delegate = this
            it.presentationContextProvider = this
            it.performRequests()
        }
    }

    fun finish(token: String?) {
        if (completed) return
        completed = true
        controller?.delegate = null
        controller?.presentationContextProvider = null
        controller = null
        onComplete(token)
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        finish(credential?.identityToken?.toByteArray()?.decodeToString())
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        finish(null)
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController,
    ): UIWindow? = UIApplication.sharedApplication.keyWindow
}

@OptIn(ExperimentalForeignApi::class)
private class IosPhotoRequest(
    private val useCamera: Boolean,
    private val onComplete: (JournalPhoto?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    private val picker = UIImagePickerController()
    private var completed = false

    fun start() {
        val source = if (useCamera) {
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        } else {
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        }
        if (!UIImagePickerController.isSourceTypeAvailable(source)) {
            finish(null)
            return
        }
        val presenter = topViewController() ?: run { finish(null); return }
        picker.delegate = this
        picker.sourceType = source
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    fun finish(photo: JournalPhoto?) {
        if (completed) return
        completed = true
        picker.delegate = null
        if (picker.presentingViewController != null) {
            picker.dismissViewControllerAnimated(true, completion = { onComplete(photo) })
        } else {
            onComplete(photo)
        }
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        finish(image?.let(::processIosImage))
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        finish(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun processIosImage(source: UIImage): JournalPhoto? = runCatching {
    val sourceSize = source.size
    val (sourceWidth, sourceHeight) = sourceSize.useContents { width to height }
    val longest = maxOf(sourceWidth, sourceHeight)
    val scale = if (longest > 2048.0) 2048.0 / longest else 1.0
    val width = sourceWidth * scale
    val height = sourceHeight * scale
    UIGraphicsBeginImageContextWithOptions(platform.CoreGraphics.CGSizeMake(width, height), false, 1.0)
    source.drawInRect(CGRectMake(0.0, 0.0, width, height))
    val normalized = UIGraphicsGetImageFromCurrentImageContext() ?: source
    UIGraphicsEndImageContext()
    val data = UIImageJPEGRepresentation(normalized, 0.88) ?: return null
    val id = NSUUID().UUIDString()
    val path = journalPhotoPath(id)
    check(data.writeToFile(path, atomically = true))
    JournalPhoto(id = id, localPath = path)
}.getOrNull()

@OptIn(ExperimentalForeignApi::class)
private fun journalPhotoPath(id: String): String {
    val documents = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: error("Documents directory is unavailable")
    val directory = "$documents/journal_photos"
    NSFileManager.defaultManager.createDirectoryAtPath(
        directory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return "$directory/$id.jpg"
}

@OptIn(ExperimentalForeignApi::class)
private class IosLocationRequest(
    private val onComplete: (CLLocation?) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var completed = false

    fun start() {
        manager.delegate = this
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> manager.requestLocation()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> finish(null)
            else -> finish(null)
        }
    }

    fun finish(location: CLLocation?) {
        if (completed) return
        completed = true
        manager.stopUpdatingLocation()
        manager.delegate = null
        onComplete(location)
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        finish(didUpdateLocations.lastOrNull() as? CLLocation)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        finish(null)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> manager.requestLocation()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> finish(null)
            else -> Unit
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toLocationStamp(): LocationStamp = coordinate.useContents {
    LocationStamp(latitude = latitude, longitude = longitude)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    if (length == 0uL) return byteArrayOf()
    return ByteArray(length.toInt()).also { destination ->
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
