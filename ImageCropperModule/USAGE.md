# ImageCropper Android Module Usage Guide

This document outlines how to integrate and use the `ImageCropper` module in your Android project. This module provides a complete suite for:
1. **Universal Permission Handling** (Lifecycle-safe, OS-version-aware permission manager for any Android permission).
2. **Image Picking & Cropping** (Rich, highly customizable image cropping view and ready-to-use activity).
3. **Image Conversion & Compression** (Fast utilities for Bitmap, ByteArray, Uri conversion, downsampling, and compression).

---

## 1. Module Integration

### 1.1. Add as a Module

1. Copy the `ImageCropperModule` directory into the root of your Android project.
2. In your project's **`settings.gradle.kts`** file (located in the root directory), include the module:
   ```kotlin
   include(":ImageCropperModule")
   ```
3. In your **app module's `build.gradle.kts`** file (usually in the `app/` directory), add the dependency:
   ```kotlin
   dependencies {
       implementation(project(":ImageCropperModule"))
   }
   ```

### 1.2. Permissions in Manifest

Ensure you have the necessary permissions declared in your app's `AndroidManifest.xml`:

```xml
<!-- Storage permissions for Android 12 and below -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />

<!-- Media permissions for Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- Optional visual media partial access for Android 14+ -->
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />

<!-- Camera permission -->
<uses-permission android:name="android.permission.CAMERA" />
```

### 1.3. FileProvider

The module uses a `FileProvider` for sharing temporary image URIs. Ensure your `AndroidManifest.xml` includes the provider declaration within the `<application>` tag:

```xml
<provider
    android:name="com.gadware.android.cropimage.CropFileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/cropper_library_file_paths"/>
</provider>
```

---

## 2. Universal Permission Request Helper

The module includes a lifecycle-safe, OS-version-aware permission helper (`com.gadware.android.cropimage.permission.*`) that can be used independently for **any** runtime permissions in any `ComponentActivity` or `Fragment`.

### 2.1. Basic Usage

```kotlin
import androidx.appcompat.app.AppCompatActivity
import com.gadware.android.cropimage.permission.PermissionGroup
import com.gadware.android.cropimage.permission.openAppSettings
import com.gadware.android.cropimage.permission.registerPermissionHelper

class YourActivity : AppCompatActivity() {

    // 1. Register helper during Activity/Fragment initialization
    private val permissionHelper = registerPermissionHelper()

    private fun requestPermissions() {
        // 2. Request any group (automatically resolves Android 13/14+ differences)
        permissionHelper.request(PermissionGroup.ImagePicker(includeCamera = true)) { result ->
            result
                .ifAllGranted { grantedList ->
                    // Proceed with camera / gallery / cropping action
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

### 2.2. Supported Permission Groups

* `PermissionGroup.ImagePicker(includeCamera: Boolean, includeVisualUserSelected: Boolean)`: Automatically requests `READ_MEDIA_IMAGES` on Android 13+, `READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+, or `READ_EXTERNAL_STORAGE` on Android 12 and below.
* `PermissionGroup.Camera`: `Manifest.permission.CAMERA`
* `PermissionGroup.Storage(writeAccess: Boolean, includeImages: Boolean, ...)`: Handles storage and media differences across API versions.
* `PermissionGroup.Notifications`: `POST_NOTIFICATIONS` on API 33+, automatically treated as granted on API < 33.
* `PermissionGroup.Contacts(writeAccess: Boolean)`: `READ_CONTACTS` and `WRITE_CONTACTS`.
* `PermissionGroup.PhoneCall`: `CALL_PHONE`.
* `PermissionGroup.Location(fineLocation: Boolean)`: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.
* `PermissionGroup.AudioRecord`: `RECORD_AUDIO`.
* `PermissionGroup.Custom(vararg permissions: String)`: Any custom array of permissions.

### 2.3. Requesting with Automatic Rationale / Settings Dialog

```kotlin
permissionHelper.requestWithDialog(
    group = PermissionGroup.ImagePicker(),
    dialogConfig = PermissionDialogConfig.createSettingsConfig(
        context = this,
        title = "Storage & Camera Permission Required",
        message = "Please allow permissions in App Settings to pick and crop photos."
    )
) { result ->
    if (result.isAllGranted) {
        // Proceed with action
    }
}
```

---

## 3. Image Cropping Usage

### 3.1. Launching `CropImageActivity`

```kotlin
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gadware.android.cropimage.CropImage
import com.gadware.android.cropimage.CropImageActivity
import com.gadware.android.cropimage.CropImageOptions
import com.gadware.android.cropimage.parcelable

class YourActivity : AppCompatActivity() {

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val cropResult = result.data?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            cropResult?.let {
                // Access cropped image content URI: it.uriContent
                imageView.setImageURI(it.uriContent)
            }
        } else if (result.resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val cropResult = result.data?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            // Handle error: cropResult?.error
        }
    }

    private fun startImageCropper() {
        val intent = Intent(this, CropImageActivity::class.java)
        val bundle = Bundle()
        
        val options = CropImageOptions(
            // Enable/disable manipulation
            allowRotation = true,
            allowFlipping = true,
            
            // Set aspect ratio
            aspectRatioX = 1,
            aspectRatioY = 1,
            fixAspectRatio = true,
            
            // Custom UI styling
            activityTitle = "Crop Image",
            cropMenuCropButtonTitle = "Done",
            activityMenuIconColor = Color.WHITE,
            toolbarColor = Color.BLACK,
            toolbarTitleColor = Color.WHITE,
            toolbarBackButtonColor = Color.WHITE,
            
            // Image source options
            showIntentChooser = true,
            imageSourceIncludeCamera = true,
            imageSourceIncludeGallery = true
        )
        
        bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS, options)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE, bundle)
        cropImageLauncher.launch(intent)
    }
}
```

### 3.2. Handling Results

- **`RESULT_OK`**: Cropping was successful. The cropped image `Uri` is available in `cropResult.uriContent`.
- **`CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE`**: An error occurred. The `Exception` is available in `cropResult.error`.

---

## 4. Image Conversion & Compression Utilities

The module includes utilities in `ImageHelper` and `BitmapUtils` for converting formats and managing image data:

### 4.1. `Bitmap` & `ByteArray` Conversion (`ImageHelper`)

```kotlin
import com.gadware.android.cropimage.ImageHelper

// 1. Convert Bitmap to ByteArray
val byteArray: ByteArray? = ImageHelper.toByteArray(bitmap)

// 2. Convert ByteArray to Bitmap
val bitmap: Bitmap = ImageHelper.toBitmap(byteArray!!)

// 3. Read image Uri directly to ByteArray
val uriBytes: ByteArray = ImageHelper.convertImageUriToByteArray(context, imageUri)

// 4. Downscale Bitmap proportionally (max 300px bound maintaining aspect ratio)
val scaledBitmap: Bitmap = ImageHelper.getMaxSizeBitmap(bitmap)

// 5. Render any Android View to a Bitmap
val viewBitmap: Bitmap = ImageHelper.loadBitmapFromView(view, view.width, view.height)
```

### 4.2. Compression & Custom Saving (`BitmapUtils`)

```kotlin
import android.graphics.Bitmap.CompressFormat
import com.gadware.android.cropimage.BitmapUtils

// Compress and save Bitmap to file Uri with format and quality (0..100)
BitmapUtils.writeBitmapToUri(
    context = context,
    bitmap = bitmap,
    uri = destinationUri,
    compressFormat = CompressFormat.JPEG,
    compressQuality = 85
)

// Efficiently decode large image from Uri using inSampleSize to prevent OOM
val sampledBitmap = BitmapUtils.decodeSampledBitmap(
    context = context,
    uri = imageUri,
    reqWidth = 1024,
    reqHeight = 1024
)
```

---

## 5. Dependencies

The module relies on standard AndroidX and Material Design libraries:
- `androidx.exifinterface:exifinterface`
- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`
- `androidx.activity:activity`
- `androidx.constraintlayout:constraintlayout`
