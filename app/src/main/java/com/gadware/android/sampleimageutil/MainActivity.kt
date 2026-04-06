package com.gadware.android.sampleimageutil

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gadware.android.cropimage.CropImage
import com.gadware.android.cropimage.CropImageActivity
import com.gadware.android.cropimage.CropImageOptions
import com.gadware.android.sampleimageutil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (cameraGranted && storageGranted) {
            // All permissions granted, proceed with your logic
            startImageCropper()
        } else {
            // Some or all permissions denied; show a message to the user
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val cropResult = result.data?.getParcelableExtra<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            cropResult?.let {
                // Handle the cropped image URI: it.uriContent
                binding.resultImage.setImageURI(it.uriContent)
            }
        } else if (result.resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val cropResult = result.data?.getParcelableExtra<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
            // Handle error: cropResult?.error
            Toast.makeText(this, "Error: "+cropResult?.error, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.button.setOnClickListener {
            askPermissions()
        }

    }
    private fun askPermissions() {
        requestMultiplePermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
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
            activityMenuIconColor = Color.WHITE,
            toolbarColor = Color.BLACK,
            toolbarTitleColor = Color.WHITE,
            toolbarBackButtonColor = Color.WHITE,

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