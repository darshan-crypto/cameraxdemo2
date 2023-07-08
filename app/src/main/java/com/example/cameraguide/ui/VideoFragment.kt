package com.example.cameraguide.ui

import android.annotation.SuppressLint
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.media.*
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_BLOCK_PRODUCER
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cameraguide.databinding.FragmentVideoBinding
import com.example.cameraguide.viewmodels.SharedViewModel
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ResolutionFilterx : ResolutionFilter {
    override fun filter(
        supportedSizes: MutableList<Size>,
        rotationDegrees: Int
    ): MutableList<Size> {
        var avaliableresolution: MutableList<Size> = mutableListOf()
        for (size in supportedSizes) {
            Log.d("infox","avaliable resolution ${size.width} ${size.height}")
            if (size.width == 640 && size.height == 480)

                avaliableresolution.add(size)
        }


        Log.d("infox","returnong")
        return avaliableresolution;
    }

}

class VideoFragment : Fragment() {
    companion object {
        fun newInstance() = VideoFragment()
        private const val TAG = "CameraXApp"
        private lateinit var imageAnalyzer: ImageAnalysis;
        private lateinit var frameAnalyzer: FrameAnalyzer;

    }

    private var is_recording_on = false;
    private lateinit var _binding: FragmentVideoBinding
    private lateinit var sharedViewModel: SharedViewModel


    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            sharedViewModel = ViewModelProvider(it).get(SharedViewModel::class.java)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.isPermissionGranted.observe(viewLifecycleOwner) {
            if (it) startCamera()
        }
        _binding.videoCaptureButton.setOnClickListener {
            if (is_recording_on == false) {

                is_recording_on = true
                frameAnalyzer = FrameAnalyzer(File("j"), requireContext())

                imageAnalyzer.setAnalyzer(
                    ContextCompat.getMainExecutor(requireContext()),
                    frameAnalyzer
                )
                frameAnalyzer.writer()
                _binding.videoCaptureButton.apply {
                    text = "stop capture"
                }
            } else {
                _binding.videoCaptureButton.apply {
                    text = "stopping..."
                    isEnabled = false
                }
                _binding.videoCaptureButton.setText("stopping")

                is_recording_on = false
                frameAnalyzer.is_recording = false
                imageAnalyzer.clearAnalyzer()

                frameAnalyzer.stop()

                _binding.videoCaptureButton.apply {
                    text = "start capture"
                    isEnabled = true

                }
            }

        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            val cameraSelectorx = CameraSelector.Builder()

                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()



            val preview = Preview.Builder()
//                .setResolutionSelector(r)
                .build()
                .also {

                    it.setSurfaceProvider(_binding.videoPreviewView.surfaceProvider)
                }

            val qualitySelector = QualitySelector.from(Quality.HD)

            val recorder = Recorder.Builder()
                .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
                .build()

            val videocap =
                VideoCapture.Builder(recorder).setTargetFrameRate(Range<Int>(30, 30)).build()

            val builder = ImageAnalysis.Builder()

            val ext: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)

//            ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range<Int>(30, 30)

            )

            val r = ResolutionSelector.Builder().setResolutionFilter(ResolutionFilterx()).build()

            imageAnalyzer = builder
                .setBackpressureStrategy(STRATEGY_BLOCK_PRODUCER)
                .setResolutionSelector(r)
                .setImageQueueDepth(52)

                .build()


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelectorx,
                    preview,
                    imageAnalyzer,
                    videocap
                )


            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        frameAnalyzer.stop()
        cameraExecutor.shutdown()
    }
}
