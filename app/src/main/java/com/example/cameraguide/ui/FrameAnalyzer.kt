package com.example.cameraguide.ui;

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class FrameAnalyzer(private val outputFile: File, private val mycontext: Context) :
        ImageAnalysis.Analyzer {
        private fun toBitmap(image: Image): ByteArray {
                val planes = image.planes
                val yBuffer = planes[0].buffer
                val uBuffer = planes[1].buffer
                val vBuffer = planes[2].buffer
                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()
                val nv21 = ByteArray(ySize + uSize + vSize)


                yBuffer[nv21, 0, ySize]
                vBuffer[nv21, ySize, vSize]
                uBuffer[nv21, ySize + vSize, uSize]

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)


                val out = ByteArrayOutputStream()
                Log.d("errorx", "height is ${image.width} ${image.height}")



                yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)


                return out.toByteArray();
        }

        private fun allAre0or4():Boolean{

                for (states in videoStates){
                        if(states == 1 || states == 2 || states ==3){

                                return false

                        }
                }

                return true
        }
        private var yimages: Array<ArrayList<ByteArray>> = arrayOf(
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>(),
                ArrayList<ByteArray>()
        )
        private var videoStates: Array<Int> = arrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        private var z: Int = 0 //frame counter
        private val FrameRate = 30.0f
        private var currentarray: Int = 0

        public var is_recording = true

        val executor = Executors.newFixedThreadPool(10)

       fun  writer(){
            executor.execute {
                   Log.d("writerx","executor started")
                    while (yimages[0].size != (FrameRate.roundToInt()*10)){
                    Log.d("writerx","${yimages[0].size} ${FrameRate.roundToInt()*10}")

                    //  Thread.sleep(500)
                    }
                     Log.d("writerx","processing started")
                    while (!allAre0or4()){

                            for (i in 0..9){
//                                    Log.d("writerx","i is ${i} state is ${videoStates[i]}")
                                    if(videoStates[i] == 2){
                                           Log.d("writerm ${i}","start writing for ${i}")
                                            videoStates[i] = 3
                                            val myarray = i

                                            executor.execute{
                                                    val timex = System.currentTimeMillis()
                                                    val muxerConfig = MuxerConfig(
                                                            File(
                                                                    "/storage/emulated/0/Movies/",
                                                                    "test_${timex}.mp4"
                                                            ), 600, 480, "video/avc", 1, FrameRate, 1500000
                                                    )
                                                    val y = FrameBuilder(mycontext, muxerConfig, null)
                                                    y.start()
                                                    while (yimages[myarray].size != 0) {
                                                            val mybitmap = BitmapFactory.decodeByteArray(
                                                                    yimages[myarray][0],
                                                                    0,
                                                                    yimages[myarray][0].size
                                                            )
                                                            y.createFrame(mybitmap)
                                                            yimages[myarray].removeAt(0)
                                                    }



                                                    y.releaseVideoCodec()
                                                    y.releaseAudioExtractor()
                                                    y.releaseMuxer()
                                                    videoStates[myarray] = 4;
                                            }
                                    }

                            }
                    }
//                    Toast.makeText(mycontext,"execution complemented",Toast.LENGTH_SHORT).show()
Log.d("writer","all buffers are written to file")
            }
       }
        fun stop() {
                Log.d("muxerdebug", "stopping button clicked")
videoStates[currentarray] = 2;
//                if (videoStates[currentarray] == 1) {
//                        Log.d("muxerdebug", "stop saving started ${currentarray}")
//                        val timex = System.currentTimeMillis()
//                        val muxerConfig = MuxerConfig(
//                                File(
//                                        "/storage/emulated/0/Movies/",
//                                        "test_${timex}.mp4"
//                                ), 600, 480, "video/avc", 1, 20f, 1500000
//                        )
//                        val y = FrameBuilder(mycontext, muxerConfig, null)
//                        y.start()
//                        while (yimages[currentarray].size != 0) {
//                                val mybitmap = BitmapFactory.decodeByteArray(
//                                        yimages[currentarray][0],
//                                        0,
//                                        yimages[currentarray][0].size
//                                )
//                                y.createFrame(mybitmap)
//                                yimages[currentarray].removeAt(0)
//                        }
//
//
//
//                        y.releaseVideoCodec()
//                        y.releaseAudioExtractor()
//                        y.releaseMuxer()
//                        videoStates[currentarray] = 4
//                        Log.d("muxerdebug", "stop saving ended ${currentarray}")
//
//                } else {
//                        Log.d("muxerdebug", "last state is ${videoStates[currentarray]}  ${currentarray}")
//
//                }

//        while (true) {
//            var boolx = true
//            for (state in videoStates) {
//                if (state == 1 || state == 2 || state == 3) {
//                    Log.d("temptag", "state is ${state} ${currentarray}")
//                    boolx = false
//                }
//            }
//
//            if (boolx) break
//            else Thread.sleep(2000)
//        }
                Toast.makeText(mycontext, "recording stopped ${z}", Toast.LENGTH_SHORT).show()
        }

        override
        fun analyze(imageProxy: ImageProxy) {
                Log.d("videotag","${System.currentTimeMillis()}")
                val start_Time = System.currentTimeMillis();
                var End_Time: Long
                val image: Image? = imageProxy.image
                if (image != null) {
                        var x: ByteArray = toBitmap(image);
                        imageProxy.close()

                        yimages[currentarray].add(x);
                        z++;
                        if (z == 300) {
//                                Toast.makeText(mycontext,"file started",Toast.LENGTH_SHORT).show()
//                                Log.d("anatime", "muxer")
                                videoStates[currentarray] = 2;
//                                val priv = currentarray
//                                executor.execute(Runnable {
//
//
//                                        val time1 = System.currentTimeMillis()
//                                        val muxerConfig = MuxerConfig(
//                                                File("/storage/emulated/0/Movies/", "test_${time1}.mp4"),
//                                                600,
//                                                480,
//                                                "video/avc",
//                                                1,
//                                                20f,
//                                                1500000
//                                        )
//                                        Log.d("muxerdebug", "writing to file started test_${time1}.mp4 ${priv}")
//
//                                        val y = FrameBuilder(mycontext, muxerConfig, null)
//                                        y.start()
//                                        videoStates[priv] = 3;
//
//                                        while (yimages[priv].size != 0) {
//                                                val mybitmap = BitmapFactory.decodeByteArray(
//                                                        yimages[priv][0],
//                                                        0,
//                                                        yimages[priv][0].size
//                                                )
//                                                y.createFrame(mybitmap)
//                                                yimages[priv].removeAt(0)
//                                        }
//
//
//                                        y.releaseVideoCodec()
//                                        y.releaseAudioExtractor()
//                                        y.releaseMuxer()
//                                        videoStates[priv] = 4;
//
//
//                                        Log.d("muxerdebug", "writing to file ended test_${time1}.mp4 ${priv}")
//
//                                })

                                z = 0;

                                if (currentarray == 9) currentarray = 0
                                else currentarray++;
                                videoStates[currentarray] = 1
                                Log.d("maind", "currently entring data to ${currentarray}")
                        }
                }


                End_Time = System.currentTimeMillis()
                if ((End_Time - start_Time) >= 17) {
                        Log.d("errorlog", "fps is ${End_Time - start_Time}")
                }
                Log.d("videolog", "time is ${End_Time - start_Time} ${z} $currentarray}")
        }


}