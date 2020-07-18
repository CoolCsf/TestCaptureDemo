package com.coolcsf.testcapturedemo

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class CameraHelper(private val activity: Activity, private val surfaceView: SurfaceView) :
    Camera.PreviewCallback {
    private var mCamera: Camera? = null
    private var mSurfaceHolder: SurfaceHolder = surfaceView.holder
    var mWidth = 640
    private var MAX_FPS = 1 //视频通话控制在15帧是足够的
    private var FRAME_PERIOD = 1000 / MAX_FPS // the frame period
    private var lastTime: Long = 0
    private var timeDiff: Long = 0
    private var framesSkipped = 0 // number of frames being skipped
    private var framesRecevied = 0 // number of frames being skipped
    private var framesSended = 0 // number of frames being skipped

    // 预设分辨率高
    var mHeight = 480
    private val queuedBuffers: MutableSet<ByteArray> = HashSet()
    var mCallBack: ((data: ByteArray?) -> Unit)? = null
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (!queuedBuffers.contains(data) || data == null) {
            return
        }
        camera?.addCallbackBuffer(data)
        timeDiff = System.currentTimeMillis() - lastTime
        framesRecevied++
        if (timeDiff < FRAME_PERIOD) {
            framesSkipped++
            mCallBack?.invoke(rotateYUVDegree270AndMirror(data, mWidth, mHeight))
            Log.d(
                "cameraHelper",
                "framesSkipped:$framesSkipped,framesRecevied:$framesRecevied, framesSended:$framesSended"
            )
            return
        }
        lastTime = System.currentTimeMillis();
        framesSended++;
    }

    private fun rotateYUVDegree270AndMirror(
        data: ByteArray,
        imageWidth: Int,
        imageHeight: Int
    ): ByteArray? {
        val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
        // Rotate and mirror the Y luma
        var i = 0
        var maxY = 0
        for (x in imageWidth - 1 downTo 0) {
            maxY = imageWidth * (imageHeight - 1) + x * 2
            for (y in 0 until imageHeight) {
                yuv[i] = data[maxY - (y * imageWidth + x)]
                i++
            }
        }
        // Rotate and mirror the U and V color components
        val uvSize = imageWidth * imageHeight
        i = uvSize
        var maxUV = 0
        var x = imageWidth - 1
        while (x > 0) {
            maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize
            for (y in 0 until imageHeight / 2) {
                yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)]
                i++
                yuv[i] = data[maxUV - (y * imageWidth + x)]
                i++
            }
            x -= 2
        }
        return yuv
    }

    fun creteCamera() {
        mCamera = openCamera()
        mCamera?.let {
            initParameters(it)
            createPool()
            it.setPreviewCallbackWithBuffer(this@CameraHelper)
            startPreView()
        } ?: Toast.makeText(activity, "无法打开相机", Toast.LENGTH_SHORT).show()
    }

    private fun initParameters(camera: Camera) {
        val param = camera.parameters
        val psz: Camera.Size = camera.Size(mWidth, mHeight)
        // 设置camera的采集视图size
        param.setPreviewSize(psz.width, psz.height)
        mWidth = psz.width
        mHeight = psz.height
        param.previewFormat = ImageFormat.NV21
        param.pictureFormat = ImageFormat.NV21
        camera.parameters = param
        val actParma = camera.parameters
        mWidth = actParma.previewSize.width
        mHeight = actParma.previewSize.height
    }

    fun releaseCamera() {
        if (mCamera != null) {
            mCamera?.stopPreview()
            mCamera?.setPreviewCallback(null)
            mCamera?.release()
            mCamera = null
        }
    }

    private fun startPreView() {
        mCamera?.let {
            it.setPreviewDisplay(mSurfaceHolder)
            setCameraDisplayOrientation()
            it.startPreview()
        }
    }

    // 为camera分配内存存放采集数据
    private fun createPool() {
        queuedBuffers.clear()
        val mFrameSize = mWidth * mHeight * 3 / 2
        for (i in 0 until 3) {
            val buffer = ByteBuffer.allocateDirect(mFrameSize)
            queuedBuffers.add(buffer.array())
            // 减少camera预览时的内存占用
            mCamera?.addCallbackBuffer(buffer.array())
        }
    }

    private fun setCameraDisplayOrientation() {
        var mDisplayOrientation = 0
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info)
        val rotation = activity.windowManager.defaultDisplay.rotation
        var screenDegree = 0
        when (rotation) {
            Surface.ROTATION_0 -> screenDegree = 0
            Surface.ROTATION_90 -> screenDegree = 90
            Surface.ROTATION_180 -> screenDegree = 180
            Surface.ROTATION_270 -> screenDegree = 270
        }
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + screenDegree) % 360
            mDisplayOrientation =
                (360 - mDisplayOrientation) % 360          // compensate the mirror
        }
        mCamera?.setDisplayOrientation(mDisplayOrientation)
    }

    private fun openCamera(cameraFacing: Int = Camera.CameraInfo.CAMERA_FACING_FRONT): Camera? {
        return supportCameraFacing(cameraFacing).takeIf { it }?.let {
            val camera = Camera.open(cameraFacing)
            camera
        }
    }

    fun setLostFrame(fps: Int) {
        MAX_FPS = fps
        FRAME_PERIOD = 1000 / MAX_FPS
    }

    private fun supportCameraFacing(facing: Int): Boolean {
        val info = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, info)
            if (info.facing == facing) return true
        }
        return false
    }
}

