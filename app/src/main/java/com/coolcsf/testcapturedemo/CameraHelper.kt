package com.coolcsf.testcapturedemo

import android.app.Activity
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.abs

class CameraHelper(private val activity: Activity, private val surfaceView: SurfaceView) :
    Camera.PreviewCallback {
    private var mCamera: Camera? = null
    private var mParameters: Camera.Parameters? = null
    private var mSurfaceHolder: SurfaceHolder = surfaceView.holder
    var mWidth = 640
    // 预设分辨率高
    var mHeight = 480
    private val queuedBuffers: MutableSet<ByteArray> = HashSet()
    var mCallBack: ((data: ByteArray?) -> Unit)? = null
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
//        if (!queuedBuffers.contains(data)) {
//            return
//        }
        mCallBack?.invoke(data)
        camera?.addCallbackBuffer(data)
    }

    //    init {
//        mSurfaceHolder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceChanged(
//                holder: SurfaceHolder?,
//                format: Int,
//                width: Int,
//                height: Int
//            ) {
//
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder?) {
//                releaseCamera()
//            }
//
//            override fun surfaceCreated(holder: SurfaceHolder?) {
//                mCamera = openCamera()
//                mCamera?.let {
//                    initParameters(it)
//                    it.setPreviewCallbackWithBuffer(this@CameraHelper)
//                    startPreView()
//                } ?: Toast.makeText(activity, "无法打开相机", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
    fun creteCamera() {
        mCamera = openCamera()
        mCamera?.let {
            initParameters(it)
            it.setPreviewCallback(this@CameraHelper)
            startPreView()
        } ?: Toast.makeText(activity, "无法打开相机", Toast.LENGTH_SHORT).show()
    }

    private fun initParameters(camera: Camera) {
//        mParameters = camera.parameters?.apply {
//            this.previewFormat = ImageFormat.YV12
//        }?.let { parameter ->
//            getBestSize(
//                mWidth,
//                mHeight,
//                parameter.supportedPreviewSizes
//            )?.let {
//                parameter.setPreviewSize(mWidth, mHeight)
//            }
//            parameter
//        }
        val param = camera.parameters
        // hardcode
        val psz: Camera.Size = camera.Size(640, 480)
        // 设置camera的采集视图size
        param.setPreviewSize(psz.width, psz.height)
        mWidth = psz.width
        mHeight = psz.height
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

    fun startPreView() {
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
        val mCamInfo = Camera.CameraInfo()
        var result: Int
        if (mCamInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = mCamInfo.orientation % 360
            result = (360 - result) % 360 // compensate the mirror
        } else { // back-facing
            result = (mCamInfo.orientation + 360) % 360
        }
        // 设置预览图像的转方向
        mCamera?.setDisplayOrientation(result)
//        val info = Camera.CameraInfo()
//        Camera.getCameraInfo(mCameraFacing, info)
//        val rotation = MApplication.instance.windowManager.defaultDisplay.rotation
//        var screenDegree = 0
//        when (rotation) {
//            Surface.ROTATION_0 -> screenDegree = 0
//            Surface.ROTATION_90 -> screenDegree = 90
//            Surface.ROTATION_180 -> screenDegree = 180
//            Surface.ROTATION_270 -> screenDegree = 270
//        }
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            mDisplayOrientation = (info.orientation + screenDegree) % 360
//            mDisplayOrientation =
//                (360 - mDisplayOrientation) % 360          // compensate the mirror
//        } else {
//            mDisplayOrientation = (info.orientation - screenDegree + 360) % 360
//        }
//        mCamera?.setDisplayOrientation(mDisplayOrientation)
    }

    //获取与指定宽高相等或最接近的尺寸
    private fun getBestSize(
        targetWidth: Int,
        targetHeight: Int,
        sizeList: List<Camera.Size>
    ): Camera.Size? {
        var bestSize: Camera.Size? = null
        val targetRatio = (targetHeight.toDouble() / targetWidth)  //目标大小的宽高比
        var minDiff = targetRatio

        for (size in sizeList) {
            val supportedRatio = (size.width.toDouble() / size.height)
            Log.d("test", "系统支持的尺寸 : ${size.width} * ${size.height} ,    比例$supportedRatio")
        }

        for (size in sizeList) {
            if (size.width == targetHeight && size.height == targetWidth) {
                bestSize = size
                break
            }
            val supportedRatio = (size.width.toDouble() / size.height)
            if (abs(supportedRatio - targetRatio) < minDiff) {
                minDiff = abs(supportedRatio - targetRatio)
                bestSize = size
            }
        }
        Log.d("test", "目标尺寸 ：$targetWidth * $targetHeight ，   比例  $targetRatio")
        Log.d("test", "最优尺寸 ：${bestSize?.height} * ${bestSize?.width}")
        return bestSize
    }

    private fun openCamera(cameraFacing: Int = Camera.CameraInfo.CAMERA_FACING_FRONT): Camera? {
        return supportCameraFacing(cameraFacing).takeIf { it }?.let {
            val camera = Camera.open(cameraFacing)
            camera
        }
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

