package com.gadware.android.cropimage

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.gadware.android.cropimage.databinding.BottomSheetCropOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CropOptionsBottomSheet : BottomSheetDialogFragment() {

  interface Listener {
    fun onOptionsApplySelected(options: CropImageOptions)
    fun onRotateImage(degrees: Int) {}
    fun onFlipImageHorizontally() {}
    fun onFlipImageVertically() {}
  }

  private var _binding: BottomSheetCropOptionsBinding? = null
  private val binding get() = _binding!!
  private lateinit var options: CropImageOptions

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    _binding = BottomSheetCropOptionsBinding.inflate(layoutInflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    options = arguments?.parcelable(OPTIONS_KEY) ?: CropImageOptions()
    // Enforce scaleType FIT_CENTER
    options = options.copy(scaleType = CropImageView.ScaleType.FIT_CENTER)
    updateOptions(options)

    bindingActions()
  }

  private fun updateOptions(options: CropImageOptions) {
    when (options.cropShape) {
      CropImageView.CropShape.RECTANGLE -> binding.cropShape.chipRectangle.isChecked = true
      CropImageView.CropShape.OVAL -> binding.cropShape.chipOval.isChecked = true
      else -> binding.cropShape.chipRectangle.isChecked = true
    }

    when (options.guidelines) {
      CropImageView.Guidelines.OFF -> binding.guidelines.chipOff.isChecked = true
      CropImageView.Guidelines.ON -> binding.guidelines.chipOn.isChecked = true
      CropImageView.Guidelines.ON_TOUCH -> binding.guidelines.chipOnTouch.isChecked = true
    }

    when (Pair(options.aspectRatioX, options.aspectRatioY).takeIf { options.fixAspectRatio }) {
      Pair(1, 1) -> binding.ratio.chipOneOne.isChecked = true
      Pair(4, 3) -> binding.ratio.chipFourThree.isChecked = true
      Pair(2, 1) -> binding.ratio.chipTwoOne.isChecked = true
      Pair(16, 9) -> binding.ratio.chipSixteenNine.isChecked = true
      else -> binding.ratio.chipFree.isChecked = true
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    listener?.onOptionsApplySelected(options)
    super.onDismiss(dialog)
  }

  private fun bindingActions() {
    binding.cropShape.chipRectangle.setOnClickListener {
      options = options.copy(cropShape = CropImageView.CropShape.RECTANGLE)
      listener?.onOptionsApplySelected(options)
    }

    binding.cropShape.chipOval.setOnClickListener {
      options = options.copy(cropShape = CropImageView.CropShape.OVAL)
      listener?.onOptionsApplySelected(options)
    }

    binding.guidelines.chipOff.setOnClickListener {
      options = options.copy(guidelines = CropImageView.Guidelines.OFF)
      listener?.onOptionsApplySelected(options)
    }

    binding.guidelines.chipOn.setOnClickListener {
      options = options.copy(guidelines = CropImageView.Guidelines.ON)
      listener?.onOptionsApplySelected(options)
    }

    binding.guidelines.chipOnTouch.setOnClickListener {
      options = options.copy(guidelines = CropImageView.Guidelines.ON_TOUCH)
      listener?.onOptionsApplySelected(options)
    }

    binding.ratio.chipFree.setOnClickListener {
      options = options.copy(fixAspectRatio = false, aspectRatioX = 1, aspectRatioY = 1)
      listener?.onOptionsApplySelected(options)
    }

    binding.ratio.chipOneOne.setOnClickListener {
      options = options.copy(fixAspectRatio = true, aspectRatioX = 1, aspectRatioY = 1)
      listener?.onOptionsApplySelected(options)
    }

    binding.ratio.chipTwoOne.setOnClickListener {
      options = options.copy(fixAspectRatio = true, aspectRatioX = 2, aspectRatioY = 1)
      listener?.onOptionsApplySelected(options)
    }

    binding.ratio.chipFourThree.setOnClickListener {
      options = options.copy(fixAspectRatio = true, aspectRatioX = 4, aspectRatioY = 3)
      listener?.onOptionsApplySelected(options)
    }

    binding.ratio.chipSixteenNine.setOnClickListener {
      options = options.copy(fixAspectRatio = true, aspectRatioX = 16, aspectRatioY = 9)
      listener?.onOptionsApplySelected(options)
    }

    binding.transform.chipRotateRight.setOnClickListener {
      listener?.onRotateImage(options.rotationDegrees)
    }

    binding.transform.chipRotateLeft.setOnClickListener {
      listener?.onRotateImage(-options.rotationDegrees)
    }

    binding.transform.chipFlipHorizontal.setOnClickListener {
      listener?.onFlipImageHorizontally()
    }

    binding.transform.chipFlipVertical.setOnClickListener {
      listener?.onFlipImageVertically()
    }
  }

  companion object {
    fun show(
      fragmentManager: FragmentManager,
      options: CropImageOptions?,
      listener: Listener,
    ) {
      Companion.listener = listener
      CropOptionsBottomSheet().apply {
        arguments = Bundle().apply { putParcelable(OPTIONS_KEY, options) }
        show(fragmentManager, "CropOptionsBottomSheet")
      }
    }

    private var listener: Listener? = null
    private const val OPTIONS_KEY = "OPTIONS_KEY"
  }
}
