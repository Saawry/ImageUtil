# ImageCropper Android Module Usage Guide

This document outlines how to integrate and use the `ImageCropper` (`ImageUtil`) module in your Android project. This module provides a complete suite for:
1. **Zero-Boilerplate Image Cropping**:
   - **Basic Cropper (`CropImageActivity`)**: Ready-to-use activity with material toolbar, rotate, flip, and crop actions.
   - **Advance Cropper (`CustomActivity` / `SampleCustomActivity`)**: Extended custom activity with real-time crop metrics, dynamic rotation counter, and interactive bottom sheet (`SampleOptionsBottomSheet`).
   - **Automatic Oval / Circular Transparency**: Cropping in `CropShape.OVAL` automatically masks the output to a transparent oval/circular image (PNG).
2. **Automatic Internal Permission Handling**:
   - The cropper automatically handles camera and media/storage permissions internally across all Android versions (Android 14+, Android 13, and Android 6–12). No manual permission requests are required from the caller before launching.
3. **Universal Permission Helper**:
   - Standalone lifecycle-safe, OS-version-aware permission manager for any Android permissions in your app.
4. **Image Conversion & Compression**:
   - Fast utilities for `Bitmap`, `ByteArray`, `Uri` conversion, downsampling, and quality compression.

---

## 1. Module Integration

### 1.1. Add Dependency via JitPack (Recommended for Published Library)

1. In your **`settings.gradle.kts`**, add the JitPack repository:
   ```kotlin
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
           maven { url = uri("https://jitpack.io") }
       }
   }
   ```
2. In your **app module's `build.gradle.kts`**, add the dependency:
   ```kotlin
   dependencies {
       implementation("com.github.Saawry:ImageUtil:1.4.0")
   }
   ```

### 1.2. Or Add as Local Module Dependency

1. In your project's **`settings.gradle.kts`**, include the module:
   ```kotlin
   include(":ImageCropperModule")
   ```
2. In your **app module's `build.gradle.kts`**, add the project dependency:
   ```kotlin
   dependencies {
       implementation(project(":ImageCropperModule"))
   }
   ```

### 1.3. Manifest Configuration

Ensure your `AndroidManifest.xml` includes the necessary permissions and activity/provider declarations:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Camera feature & permissions -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- Storage permissions for Android 12 and below -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32"
        tools:ignore="ScopedStorage" />

    <!-- Media permissions for Android 13+ -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />

    <application ...>
        
        <!-- Basic Cropper Activity -->
        <activity
            android:name="com.gadware.android.cropimage.CropImageActivity"
            android:theme="@style/Theme.CropImage.Activity"
            android:exported="true" />

        <!-- Advance Cropper Activity -->
        <activity
            android:name="com.gadware.android.cropimage.SampleCustomActivity"
            android:theme="@style/Theme.CropImage.Activity"
            android:exported="true" />

        <!-- FileProvider for temporary camera/crop files -->
        <provider
            android:name="com.gadware.android.cropimage.CropFileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/cropper_library_file_paths"/>
        </provider>

    </application>
</manifest>
```

---

## 2. Launching the Image Cropper

The image cropper handles its own permission requests internally. You simply launch the intent using the standard Android `ActivityResultContracts.StartActivityForResult()` API.

### 2.1. Setting Up the Activity Result Launcher

```kotlin
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gadware.android.cropimage.CropImage
import com.gadware.android.cropimage.CropImageActivity
import com.gadware.android.cropimage.CropImageOptions
import com.gadware.android.cropimage.CustomActivity
import com.gadware.android.cropimage.SampleCustomActivity
import com.gadware.android.cropimage.parcelable

class MainActivity : AppCompatActivity() {

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val cropResult = result.data?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            cropResult?.let {
                // Access cropped image content URI (transparent if cropped in oval shape)
                val croppedUri: Uri? = it.uriContent
                binding.resultImage.setImageURI(croppedUri)
            }
        } else if (result.resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val cropResult = result.data?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            Toast.makeText(this, "Error: " + cropResult?.error?.message, Toast.LENGTH_SHORT).show()
        }
    }
```

### 2.2. Launching Basic Cropper (`CropImageActivity`)

```kotlin
    fun startBasicCropper(sourceUri: Uri? = null) {
        val intent = Intent(this, CropImageActivity::class.java)
        val bundle = Bundle()

        // 1. (Optional) Provide existing image Uri to crop directly
        sourceUri?.let {
            bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE, it)
        }

        // 2. Configure options
        val options = CropImageOptions(
            // Rotation & Flipping
            allowRotation = true,
            allowFlipping = true,

            // Aspect Ratio
            aspectRatioX = 1,
            aspectRatioY = 1,
            fixAspectRatio = true,

            // UI Customization
            activityTitle = "Crop Image",
            cropMenuCropButtonTitle = "Done",
            activityMenuIconColor = Color.WHITE,
            toolbarColor = Color.BLACK,
            toolbarTitleColor = Color.WHITE,
            toolbarBackButtonColor = Color.WHITE,

            // Image Source (Camera / Gallery / Intent Chooser)
            showIntentChooser = true,
            imageSourceIncludeCamera = true,
            imageSourceIncludeGallery = true
        )

        bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS, options)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE, bundle)
        cropImageLauncher.launch(intent)
    }
```

### 2.3. Launching Advance Cropper (`CustomActivity` / `SampleCustomActivity`)

The **Advance Cropper** provides live aspect ratio chips, shape choices (rectangle, oval, vertical/horizontal only), guidelines, auto-zoom, multi-touch, center move, progress bar toggles, live crop dimension metrics, and automatic transparent oval outputs:

```kotlin
    fun startAdvanceCropper(sourceUri: Uri? = null) {
        val intent = Intent(this, CustomActivity::class.java) // or SampleCustomActivity::class.java
        val bundle = Bundle()

        sourceUri?.let {
            bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE, it)
        }

        val options = CropImageOptions(
            allowRotation = true,
            allowFlipping = true,
            showIntentChooser = true,
            imageSourceIncludeCamera = true,
            imageSourceIncludeGallery = true
        )

        bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS, options)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE, bundle)
        cropImageLauncher.launch(intent)
    }
```

### 2.4. Circular / Oval Cropping with Transparency

When `cropShape` is set to `CropShape.OVAL` (either via options or selected in the Advance bottom sheet), the output image is **automatically transparent** outside the oval/circle boundary and saved as a PNG.

```kotlin
val options = CropImageOptions(
    cropShape = CropImageView.CropShape.OVAL,
    fixAspectRatio = true,
    aspectRatioX = 1,
    aspectRatioY = 1,
)
```

You can also manually convert any Bitmap to a circular transparent Bitmap using:
```kotlin
val circularBitmap: Bitmap = CropImage.toOvalBitmap(bitmap)
```

---

## 3. Standalone Universal Permission Helper

The module includes a standalone, lifecycle-safe permission helper (`com.gadware.android.cropimage.permission.*`) that can be used for **any** runtime permissions in your application:

```kotlin
import com.gadware.android.cropimage.permission.PermissionGroup
import com.gadware.android.cropimage.permission.openAppSettings
import com.gadware.android.cropimage.permission.registerPermissionHelper

class MyActivity : AppCompatActivity() {

    // Register helper during Activity/Fragment creation (before STARTED state)
    private val permissionHelper = registerPermissionHelper()

    private fun checkCustomPermissions() {
        permissionHelper.request(PermissionGroup.ImagePicker(includeCamera = true)) { result ->
            result
                .ifAllGranted { grantedPermissions ->
                    // All permissions granted
                }
                .ifPermanentlyDenied { permanentlyDenied, denied, granted ->
                    // User selected "Don't ask again" - redirect to settings
                    openAppSettings()
                }
                .ifDenied { denied, granted ->
                    // User denied permission
                }
        }
    }
}
```

### Available Permission Groups:
* `PermissionGroup.ImagePicker(includeCamera = true, includeVisualUserSelected = false)`: Resolves API 34+ / 33 / legacy storage and camera permissions.
* `PermissionGroup.Camera`: `Manifest.permission.CAMERA`.
* `PermissionGroup.Storage(writeAccess = false, includeImages = true, ...)`: Resolves media & storage access across all OS versions.
* `PermissionGroup.Notifications`: `POST_NOTIFICATIONS` on API 33+ (no-op on older APIs).
* `PermissionGroup.Contacts(writeAccess = false)`: `READ_CONTACTS` and `WRITE_CONTACTS`.
* `PermissionGroup.Location(fineLocation = true)`: Fine & Coarse location.
* `PermissionGroup.AudioRecord`: `RECORD_AUDIO`.
* `PermissionGroup.PhoneCall`: `CALL_PHONE`.
* `PermissionGroup.Custom("permission.A", "permission.B")`: Any custom manifest permissions.

---

## 4. Image Conversion & Compression Utilities

### 4.1. Universal Image to ByteArray & Format Conversions (`ImageHelper`)

`ImageHelper.toByteArray` is a universal conversion function supporting **`Bitmap`** or **`Uri`** inputs with optional parameters for **dimensions**, **format**, and **compression ratio**:

* **Dimensions (`width`, `height`)**:
  * If both are omitted: Defaults to **`200px * 200px`**.
  * If only one is provided (e.g., `width = 50`): The dimension defaults to a square **`50px * 50px`**.
  * If both are provided (e.g., `width = 300, height = 400`): Scaled to **`300px * 400px`**.
* **Format (`format`)**: Defaults to **`Bitmap.CompressFormat.PNG`** (also supports `JPEG`, `WEBP`, etc.).
* **Compression Ratio / Quality (`ratio`)**: Defaults to **`50`** (integer range `0..100`).

```kotlin
import android.graphics.Bitmap.CompressFormat
import com.gadware.android.cropimage.ImageHelper

// 1. Universal converter from Bitmap (defaults: 200x200, PNG, ratio 50)
val bytesDefault: ByteArray? = ImageHelper.toByteArray(bitmap)

// 2. Single dimension passed (e.g. 50 -> 50px * 50px, PNG, ratio 50)
val bytesSquare: ByteArray? = ImageHelper.toByteArray(
    bitmap = bitmap,
    width = 50 // or height = 50
)

// 3. Custom dimensions, JPEG format, and custom compression ratio
val bytesCustom: ByteArray? = ImageHelper.toByteArray(
    bitmap = bitmap,
    width = 300,
    height = 400,
    format = CompressFormat.JPEG,
    ratio = 80
)

// 4. Universal converter from Uri (memory-safe sampled decoding)
val bytesFromUri: ByteArray? = ImageHelper.toByteArray(
    context = context,
    uri = imageUri,
    width = 100, // becomes 100x100
    format = CompressFormat.PNG,
    ratio = 50
)

// 5. Universal entry point for either Bitmap or Uri
val bytesUniversal: ByteArray? = ImageHelper.toByteArrayUniversal(
    image = imageUri, // or bitmap
    context = context,
    width = 50
)

// 6. Convert ByteArray back to Bitmap
val bitmap: Bitmap = ImageHelper.toBitmap(bytesDefault!!)

// 7. Downscale Bitmap proportionally (max 300px bound maintaining aspect ratio)
val scaledBitmap: Bitmap = ImageHelper.getMaxSizeBitmap(bitmap)

// 8. Render any Android View to a Bitmap
val viewBitmap: Bitmap = ImageHelper.loadBitmapFromView(view, view.width, view.height)
```

### 4.2. Compression & Large Image Decoding (`ImageHelper`)

```kotlin
import android.graphics.Bitmap.CompressFormat
import com.gadware.android.cropimage.ImageHelper

// Compress and save Bitmap to file Uri with format and quality (0..100)
val savedUri: Uri = ImageHelper.writeBitmapToUri(
    context = context,
    bitmap = bitmap,
    destinationUri = destinationUri, // or null for auto-generated temp cache Uri
    format = CompressFormat.JPEG,
    quality = 85
)

// Efficiently decode large image from Uri using inSampleSize to prevent OOM
val sampledBitmap: Bitmap? = ImageHelper.decodeSampledBitmap(
    context = context,
    uri = imageUri,
    reqWidth = 1024,
    reqHeight = 1024
)
```
