package com.gadware.android.cropimage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.gadware.android.cropimage.databinding.ExtendedActivityBinding

open class SampleCustomActivity : CropImageActivity(), SampleOptionsBottomSheet.Listener {

  companion object {
    fun start(activity: Activity) {
      activity.startActivity(Intent(activity, SampleCustomActivity::class.java))
    }
  }

  private lateinit var binding: ExtendedActivityBinding
  private var currentCropImageOptions: CropImageOptions = CropImageOptions()

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ExtendedActivityBinding.inflate(layoutInflater)

    val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
    currentCropImageOptions =
      bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS) ?: CropImageOptions()

    super.onCreate(savedInstanceState)

    setContentView(binding.root)
    setCropImageView(binding.cropImageView)

    binding.cropImageView.setImageCropOptions(currentCropImageOptions)

    if (savedInstanceState == null) {
      val sourceUri = bundle?.parcelable<Uri>(CropImage.CROP_IMAGE_EXTRA_SOURCE)
      if (sourceUri != null && sourceUri != Uri.EMPTY) {
        binding.cropImageView.setImageUriAsync(sourceUri)
      }
    }

    binding.saveBtn.setOnClickListener { cropImage() }
    binding.backBtn.setOnClickListener { setResultCancel() }
    binding.rotateText.setOnClickListener { onRotateClick() }

    binding.settings.setOnClickListener {
      SampleOptionsBottomSheet.show(
        supportFragmentManager,
        currentCropImageOptions,
        this,
      )
    }

    binding.cropImageView.setOnCropWindowChangedListener {
      updateExpectedImageSize()
    }

    updateRotationCounter()
    updateExpectedImageSize()
  }

  override fun onSetImageUriComplete(
    view: CropImageView,
    uri: Uri,
    error: Exception?,
  ) {
    super.onSetImageUriComplete(view, uri, error)
    updateRotationCounter()
    updateExpectedImageSize()
  }

  private fun updateExpectedImageSize() {
    try {
      binding.expectedImageSize.text = binding.cropImageView.expectedImageSize().toString()
    } catch (e: Exception) {
      Log.w("SampleCustomActivity", "Failed to update expected image size", e)
    }
  }

  private fun updateRotationCounter() {
    binding.rotateText.text = getString(
      R.string.rotation_value,
      binding.cropImageView.rotatedDegrees.toString(),
    )
  }

  override fun onPickImageResult(resultUri: Uri?) {
    super.onPickImageResult(resultUri)
    if (resultUri != null) {
      binding.cropImageView.setImageUriAsync(resultUri)
    }
  }

  private fun onRotateClick() {
    binding.cropImageView.rotateImage(90)
    updateRotationCounter()
  }

  override fun onOptionsApplySelected(options: CropImageOptions) {
    this.currentCropImageOptions = options
    binding.cropImageView.setImageCropOptions(options)
  }
}
