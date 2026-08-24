package com.gadware.android.cropimage

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable
import android.provider.MediaStore

internal object CropImageIntentChooser {
  const val GOOGLE_PHOTOS = "com.google.android.apps.photos"
  const val GOOGLE_PHOTOS_GO = "com.google.android.apps.photosgo"
  const val SAMSUNG_GALLERY = "com.sec.android.gallery3d"
  const val ONEPLUS_GALLERY = "com.oneplus.gallery"
  const val MIUI_GALLERY = "com.miui.gallery"

  private val DEFAULT_PRIORITY_LIST = listOf(
    GOOGLE_PHOTOS,
    GOOGLE_PHOTOS_GO,
    SAMSUNG_GALLERY,
    ONEPLUS_GALLERY,
    MIUI_GALLERY,
  )

  /**
   * Create a chooser intent to select the source to get image from.<br></br>
   * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br></br>
   * All possible sources are added to the intent chooser.
   *
   * [includeCamera] if to include camera intents
   * [includeGallery] if to include Gallery app intents
   * [cameraImgUri] required if includeCamera is set to true
   */
  fun createChooserIntent(
    context: Context,
    title: String = context.getString(R.string.pick_image_chooser_title),
    priorityIntentList: List<String> = DEFAULT_PRIORITY_LIST,
    includeCamera: Boolean,
    includeGallery: Boolean,
    cameraImgUri: Uri? = null,
  ): Intent {
    val allIntents: MutableList<Intent> = ArrayList()
    val packageManager = context.packageManager
    // collect all camera intents if Camera permission is available
    if (!isExplicitCameraPermissionRequired(context) && includeCamera) {
      allIntents.addAll(getCameraIntents(context, packageManager, cameraImgUri))
    }

    if (includeGallery) {
      var galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, priorityIntentList)
      if (galleryIntents.isEmpty()) {
        // if no intents found for get-content try to pick intent action (Huawei P9).
        galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_PICK, priorityIntentList)
      }
      allIntents.addAll(galleryIntents)
    }

    val target = if (allIntents.isEmpty()) {
      Intent()
    } else {
      Intent(Intent.ACTION_CHOOSER, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        if (includeGallery) {
          action = Intent.ACTION_PICK
          type = "image/*"
        }
      }
    }
    // Filter out duplicate intents by package name
    val uniqueIntents = allIntents.distinctBy { it.`package` ?: it.component?.packageName }

    // Create a chooser from the main intent
    val chooserIntent = Intent.createChooser(target, title)
    // Add all other intents
    chooserIntent.putExtra(
      Intent.EXTRA_INITIAL_INTENTS,
      uniqueIntents.toTypedArray<Parcelable>(),
    )
    return chooserIntent
  }

  /**
   * Get all Camera intents for capturing image using device camera apps.
   */
  private fun getCameraIntents(context: Context, packageManager: PackageManager, cameraImgUri: Uri?): List<Intent> {
    val allIntents: MutableList<Intent> = ArrayList()
    // Determine Uri of camera image to save.
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    val flags = 0
    val listCam = when {
      SDK_INT >= 33 -> packageManager.queryIntentActivities(captureIntent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
      else -> @Suppress("DEPRECATION") packageManager.queryIntentActivities(captureIntent, flags)
    }

    for (resolveInfo in listCam) {
      val intent = Intent(captureIntent)
      intent.component = ComponentName(
        resolveInfo.activityInfo.packageName,
        resolveInfo.activityInfo.name,
      )
      intent.setPackage(resolveInfo.activityInfo.packageName)
      if (cameraImgUri != null) {
        if (context is Activity) {
          context.grantUriPermission(
            resolveInfo.activityInfo.packageName,
            cameraImgUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImgUri)
      }
      allIntents.add(intent)
    }
    return allIntents
  }

  /**
   * Get all Gallery intents for getting image from one of the apps of the device that handle
   * images.
   */
  private fun getGalleryIntents(packageManager: PackageManager, action: String, priorityIntentList: List<String>): List<Intent> {
    val intents: MutableList<Intent> = ArrayList()
    val galleryIntent = if (action == Intent.ACTION_GET_CONTENT) {
      Intent(action)
    } else {
      Intent(action, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }
    galleryIntent.type = "image/*"

    val flags = 0
    val listGallery = when {
      SDK_INT >= 33 -> packageManager.queryIntentActivities(galleryIntent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
      else -> @Suppress("DEPRECATION") packageManager.queryIntentActivities(galleryIntent, flags)
    }
    for (res in listGallery) {
      val intent = Intent(galleryIntent)
      intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
      intent.setPackage(res.activityInfo.packageName)
      intents.add(intent)
    }
    // sort intents
    val priorityIntents = mutableListOf<Intent>()
    for (pkgName in priorityIntentList) {
      intents.firstOrNull { it.`package` == pkgName }?.let {
        intents.remove(it)
        priorityIntents.add(it)
      }
    }
    intents.addAll(0, priorityIntents)
    return intents
  }

  /**
   * Check if explicitly requesting camera permission is required.<br></br>
   * It is required in Android Marshmallow and above if "CAMERA" permission is requested in the
   * manifest.<br></br>
   * See [StackOverflow
   * question](http://stackoverflow.com/questions/32789027/android-m-camera-intent-permission-bug).
   */
  private fun isExplicitCameraPermissionRequired(context: Context): Boolean = SDK_INT >= Build.VERSION_CODES.M &&
          hasCameraPermissionInManifest(context) &&
          context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED

  /**
   * Check if the app requests a specific permission in the manifest.
   *
   * [context] the context of your activity to check for permissions
   * @return true - the permission in requested in manifest, false - not.
   */
  private fun hasCameraPermissionInManifest(context: Context): Boolean {
    val packageName = context.packageName
    try {
      val flags = PackageManager.GET_PERMISSIONS
      val packageInfo = when {
        SDK_INT >= 33 -> context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else -> @Suppress("DEPRECATION") context.packageManager.getPackageInfo(packageName, flags)
      }
      val declaredPermissions = packageInfo.requestedPermissions
      return declaredPermissions
        ?.any { it?.equals("android.permission.CAMERA", true) == true } == true
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
    }
    return false
  }
}
