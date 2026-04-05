# ImageCropper Android Module Usage Guide

This document outlines how to integrate and use the `ImageCropper` module in your Android project. This module provides image picking and cropping functionality based on the `CropImageActivity` from the original repository.

## 1. Module Structure

The `ImageCropperModule` contains the necessary source code and resources to provide image cropping capabilities. It has been refactored to be a standalone Android library module.

## 2. Integration

To integrate this module into your Android project, follow these steps:

### 2.1. Add as a Module

1.  Copy the `ImageCropperModule` directory into the root of your Android project.
2.  In your project's **`settings.gradle.kts`** file (located in the root directory), include the module:
    ```kotlin
    include(":ImageCropperModule")
    ```
3.  In your **app module's `build.gradle.kts`** file (usually in the `app/` directory), add the module as a dependency:
    ```kotlin
    dependencies {
        implementation(project(":ImageCropperModule"))
    }
    ```

**Note:** The module's `build.gradle.kts` has been simplified to use direct dependencies instead of a Version Catalog (`libs.versions.toml`). This ensures it works immediately when added to your project without needing to modify your root `settings.gradle.kts` for version catalogs.

### 2.2. Permissions

Ensure you have the necessary permissions declared in your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.CAMERA" />

<application
    ...
    android:requestLegacyExternalStorage="true">
    ...
</application>
```

### 2.3. FileProvider

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

And ensure the `cropper_library_file_paths.xml` file is present in your `res/xml` directory.

## 3. Usage

### 3.1. Launching `CropImageActivity`

You can launch `CropImageActivity` using `ActivityResultContracts.StartActivityForResult` and pass `CropImageOptions` to customize the cropping behavior.

```kotlin
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gadware.android.cropimage.CropImage
import com.gadware.android.cropimage.CropImageActivity
import com.gadware.android.cropimage.CropImageOptions

class YourActivity : AppCompatActivity() {

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val cropResult = result.data?.getParcelableExtra<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            cropResult?.let {
                // Handle the cropped image URI: it.uriContent
                // Example: imageView.setImageURI(it.uriContent)
            }
        } else if (result.resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val cropResult = result.data?.getParcelableExtra<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            // Handle error: cropResult?.error
        }
    }

    private fun startImageCropper() {
        val intent = Intent(this, CropImageActivity::class.java)
        val bundle = Bundle()
        
        // Configure cropping options
        val options = CropImageOptions(
            // Set initial image URI if you have one, otherwise it will prompt for selection
            // imageUri = Uri.parse("file:///path/to/your/image.jpg"), 
            
            // Allow rotation, flipping, etc.
            allowRotation = true,
            allowFlipping = true,
            
            // Set aspect ratio
            aspectRatioX = 1,
            aspectRatioY = 1,
            fixAspectRatio = true,
            
            // Customize UI elements
            activityTitle = "Crop Image",
            cropMenuCropButtonTitle = "Done",
            activityMenuIconColor = android.graphics.Color.WHITE,
            toolbarColor = android.graphics.Color.BLACK,
            toolbarTitleColor = android.graphics.Color.WHITE,
            toolbarBackButtonColor = android.graphics.Color.WHITE,
            
            // Image source options
            showIntentChooser = true, // Show a chooser dialog for camera/gallery
            imageSourceIncludeCamera = true,
            imageSourceIncludeGallery = true
        )
        
        bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS, options)
        // If you want to pass an initial image URI directly without showing the chooser:
        // bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE, yourImageUri)
        
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE, bundle)
        cropImageLauncher.launch(intent)
    }
}
```

### 3.2. Handling Results

The result from `CropImageActivity` is returned via `ActivityResultContracts.StartActivityForResult`. The cropped image URI (or error) can be retrieved from the `Intent` data using `CropImage.CROP_IMAGE_EXTRA_RESULT`.

-   **`RESULT_OK`**: The cropping was successful. The cropped image `Uri` is available in `cropResult.uriContent`.
-   **`CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE`**: An error occurred during cropping. The `Exception` is available in `cropResult.error`.

## 4. Customization

The `CropImageOptions` class provides extensive customization options for the cropping activity, including:

-   **`imageUri`**: The initial image URI to load. If not provided, the activity will prompt the user to select an image.
-   **`allowRotation`, `allowFlipping`**: Enable/disable rotation and flipping controls.
-   **`aspectRatioX`, `aspectRatioY`, `fixAspectRatio`**: Define the aspect ratio of the crop window.
-   **UI Customization**: `activityTitle`, `cropMenuCropButtonTitle`, `activityMenuIconColor`, `toolbarColor`, `toolbarTitleColor`, `toolbarBackButtonColor` to match your app's theme.
-   **Image Source**: `showIntentChooser`, `imageSourceIncludeCamera`, `imageSourceIncludeGallery` to control how the user selects an image.

Refer to the `CropImageOptions.kt` file for a complete list of available options and their descriptions.

## 5. Dependencies

The module relies on the following AndroidX and Material Design libraries:

-   `androidx.exifinterface:exifinterface`
-   `androidx.core:core-ktx`
-   `androidx.appcompat:appcompat`
-   `com.google.android.material:material`
-   `androidx.activity:activity`
-   `androidx.constraintlayout:constraintlayout`

These dependencies are declared in the module's `build.gradle.kts` and `libs.versions.toml` files. Ensure your project's `build.gradle.kts` (top-level) and `settings.gradle.kts` are configured to resolve these dependencies correctly.
