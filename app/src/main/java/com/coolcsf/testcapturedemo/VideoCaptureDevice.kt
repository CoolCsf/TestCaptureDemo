package com.coolcsf.testcapturedemo

import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import android.view.View
import com.zego.zegoavkit2.ZegoVideoCaptureDevice
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class VideoCaptureDevice(val activity: MainActivity) : ZegoVideoCaptureDevice() {
    private var mAVCEncoder: AVCEncoder? = null
    private var mEncodedBuffer: ByteBuffer? = null
    private val TAG = "VideoCaptureDevice"
    private var mClient: Client? = null
    private val queuedBuffers: MutableSet<ByteArray> = HashSet()
    private var mCam: Camera? = null
    private var mCamInfo: CameraInfo? = null
    // 默认为后置摄像头
    private var mFront = 0
    // 预设分辨率宽
    private var mWidth = 640
    // 预设分辨率高
    private var mHeight = 480
    // 预设采集帧率
    private var mFrameRate = 15
    // 默认不旋转
    private var mRotation = 0

    override fun stopCapture(): Int {
        return 0
    }

    override fun setViewRotation(p0: Int): Int {
        return 0;
    }

    override fun setFrontCam(p0: Int): Int {
        return 0
    }

    override fun setResolution(p0: Int, p1: Int): Int {
        return 0
    }

    override fun setCaptureRotation(p0: Int): Int {
        return 0
    }

    override fun startPreview(): Int {
        return startCapture()
    }

    override fun startCapture(): Int {
        init()
        return 0
    }

    override fun supportBufferType(): Int {
        // 码流
        return PIXEL_BUFFER_TYPE_ENCODED_FRAME
    }

    private var surfaceView: SurfaceView? = null
    override fun setView(p0: View?): Int {
        surfaceView = p0 as SurfaceView?
        return 0
    }

    override fun allocateAndStart(p0: Client?) {
        mClient = p0
    }

    private fun init() {
        creteCamera()
    }

    private var cameraHelper: CameraHelper? = null
    private fun creteCamera() {
        surfaceView?.let { it ->
            cameraHelper = CameraHelper(activity, it).apply {
                this.mCallBack = {
                    viewData2Encode(it)
                }
            }.also { it.creteCamera() }
        }
    }

    private fun viewData2Encode(data: ByteArray?) {
        if (mAVCEncoder == null) { // 检测设备是否支持编码I420数据
            val isSupport = AVCEncoder.isSupportI420()
            if (isSupport) { // 创建编码器
                mAVCEncoder = AVCEncoder(mWidth, mHeight)
                // 为编码器分配内存
                mEncodedBuffer = ByteBuffer.allocateDirect(mWidth * mHeight * 3 / 2)
                // 启动编码器
                mAVCEncoder?.startEncoder()
            } else {
                Log.e("Zego", "This demo don't support color formats other than I420.")
            }
        }
//        mAVCEncoder?.init(cameraHelper!!.mWidth,cameraHelper!!.mHeight,15,4000000)
        if (mAVCEncoder != null) { // 编码器相关信息
            val config = VideoCodecConfig()
            // Android端的编码类型必须选用 ZegoVideoCodecTypeAVCANNEXB
            config.codec_type = ZegoVideoCodecType.ZegoVideoCodecTypeAVCANNEXB
            config.width = cameraHelper!!.mWidth
            config.height = cameraHelper!!.mHeight
            // 计算当前的纳秒时间
            var now: Long = 0
            now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                SystemClock.elapsedRealtimeNanos()
            } else {
                TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime())
            }
            // 将NV21格式的视频数据转为I420格式的
            val i420bytes =
                YV12toYUV420PackedSemiPlanar(
                    data!!,
                    cameraHelper!!.mWidth,
                    cameraHelper!!.mHeight
                )
            // 为编码器提供视频帧数据和时间戳
            mAVCEncoder?.inputFrameToEncoder(i420bytes, now)
            // 取编码后的视频数据，编码未完成时返回null
            val transferInfo = mAVCEncoder?.pollFrameFromEncoder()
            // 编码完成
            if (transferInfo != null) {
                if (mEncodedBuffer != null && transferInfo.inOutData.size > mEncodedBuffer?.capacity() ?: 0) {
                    mEncodedBuffer =
                        ByteBuffer.allocateDirect(transferInfo.inOutData.size)
                }
                mEncodedBuffer?.clear()
                // 将编码后的数据存入ByteBuffer中
                mEncodedBuffer?.put(transferInfo.inOutData, 0, transferInfo.inOutData.size)
                // 将编码后的视频数据传给ZEGO SDK，需要告知SDK当前传递帧是否为视频关键帧，以及当前视频帧的时间戳
                mClient!!.onEncodedFrameCaptured(
                    mEncodedBuffer,
                    transferInfo.inOutData.size,
                    config,
                    transferInfo.isKeyFrame,
                    transferInfo.timeStmp.toDouble()
                )
                // 打印第一次传递编码数据给SDK的时间
//                if (printCount == 0) {
//                    val date = Date(System.currentTimeMillis())
//                    Log.d(
//                        "Zego",
//                        "encode data transfer time: " + simpleDateFormat.format(date)
//                    )
//                    printCount++
//                }
            }
        }
    }

    //    // camera采集的是NV21格式的数据，编码器需要I420格式的数据，此处进行一个格式转换
//    fun NV21ToI420(data: ByteArray?, width: Int, height: Int): ByteArray? {
//        if (data == null) return null
//        val ret = ByteArray(width * height * 3 / 2)
//        val total = width * height
//        val bufferY = ByteBuffer.wrap(ret, 0, total)
//        val bufferV = ByteBuffer.wrap(ret, total, total / 4)
//        val bufferU =
//            ByteBuffer.wrap(ret, total + total / 4, total / 4)
//        bufferY.put(data, 7, total)
//        var i = total + 7
//        while (i < data.size) {
//            bufferV.put(data[i])
//            bufferU.put(data[i + 1])
//            i += 2
//        }
//        return ret
//    }
//    private fun NV21ToI420(data:    ByteArray?, width: Int, height: Int): ByteArray? {
//        if (data == null) return null
//        val ret = ByteArray(data.size)
//        val total = width * height
//        val bufferY = ByteBuffer.wrap(ret, 0, total)
//        val bufferU = ByteBuffer.wrap(ret, total, total / 4)
//        val bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4)
//        bufferY.put(data, 0, total)
//        var i = total
//        while (i < data.size) {
//            bufferV.put(data[i])
//            bufferU.put(data[i + 1])
//            i += 2
//        }
//        return ret
//    }
    private fun YV12toYUV420PackedSemiPlanar(
        input: ByteArray,
        width: Int,
        height: Int
    ): ByteArray? {
        val yStride = ceil((width / 16.0f).toDouble()).toInt() * 16
        val cStride = ceil((width / 32.0f).toDouble()).toInt() * 16
        val ySize = yStride * height
        val cSize = cStride * height / 2
        val output = ByteArray(width * height * 3 / 2)
        for (i in 0 until height) System.arraycopy(
            input,
            yStride * i,
            output,
            yStride * i,
            yStride
        ) // Y
        for (i in 0 until height / 2) {
            for (j in 0 until width / 2) {
                output[ySize + (i * width / 2 + j) * 2] =
                    input[ySize + cSize + i * cStride + j] // Cb (U)
                output[ySize + (i * height / 2 + j) * 2 + 1] =
                    input[ySize + i * cStride + j] // Cr (V)
            }
        }
        return output
    }

    override fun takeSnapshot(): Int {
        return 0
    }

    override fun setViewMode(p0: Int): Int {
        return 0
    }

    override fun setPowerlineFreq(p0: Int): Int {
        return 0
    }

    override fun stopAndDeAllocate() {
    }

    override fun setFrameRate(p0: Int): Int {
        return 0
    }

    override fun enableTorch(p0: Boolean): Int {
        return 0
    }

    override fun stopPreview(): Int {
        return 0
    }
}