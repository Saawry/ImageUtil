package com.gadware.android.sampleimageutil

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gadware.android.cropimage.CropImage
import com.gadware.android.cropimage.CropImageActivity
import com.gadware.android.cropimage.CropImageOptions
import com.gadware.android.cropimage.SampleCustomActivity
import com.gadware.android.cropimage.parcelable
import com.gadware.android.sampleimageutil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val cropResult = result.data?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            cropResult?.let {
                // Handle the cropped image URI: it.uriContent
                binding.resultImage.setImageURI(it.uriContent)
            }
        } else if (result.resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val cropResult = result.data?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            Toast.makeText(
                this,
                getString(R.string.error_prefix) + cropResult?.error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The image cropper internally handles all required permissions (Camera / Gallery / Media)
        binding.btnBasicCrop.setOnClickListener {
            startImageCropper(isAdvanced = false)
        }

        binding.btnAdvanceCrop.setOnClickListener {
            startImageCropper(isAdvanced = true)
        }
    }

    private fun startImageCropper(isAdvanced: Boolean) {
        val targetClass = if (isAdvanced) {
            SampleCustomActivity::class.java
        } else {
            CropImageActivity::class.java
        }

        val intent = Intent(this, targetClass)
        val bundle = Bundle()

        // Configure cropping options
        val options = CropImageOptions(
            // Allow rotation, flipping, etc.
            allowRotation = true,
            allowFlipping = true,

            // Set aspect ratio
            aspectRatioX = 1,
            aspectRatioY = 1,
            fixAspectRatio = true,

            // Customize UI elements
            activityTitle = if (isAdvanced) getString(R.string.advance_cropper) else getString(R.string.basic_cropper),
            cropMenuCropButtonTitle = getString(R.string.done),
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