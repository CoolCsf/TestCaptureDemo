package com.coolcsf.testcapturedemo

import android.content.pm.PackageManager
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.zego.zegoavkit2.ZegoExternalVideoCapture
import com.zego.zegoliveroom.ZegoLiveRoom
import com.zego.zegoliveroom.constants.ZegoConstants
import com.zego.zegoliveroom.constants.ZegoVideoViewMode
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    /**
     * so文件默认前缀带lib，在此引用时需要去掉"lib"和后缀".so"
     * */
    init {
        System.loadLibrary("yuv_utils")
        System.loadLibrary("yuv")
    }

    private val liveroom by lazy {
        ZegoLiveRoom()
    }
    private val appSign = byteArrayOf(
        0x28,
        0x89.toByte(),
        0x06,
        0xf5.toByte(),
        0x84.toByte(),
        0x14,
        0x6d,
        0x81.toByte(),
        0x4b,
        0xda.toByte(),
        0x5e,
        0x2c,
        0xd1.toByte(),
        0x71,
        0xc7.toByte(),
        0x8a.toByte(),
        0x63,
        0xc9.toByte(),
        0x6d,
        0x2a,
        0x45,
        0xb4.toByte(),
        0x0e,
        0x99.toByte(),
        0x11,
        0x56,
        0x50,
        0x32,
        0x87.toByte(),
        0x57,
        0x34,
        0x94.toByte()
    )
    private val permissionCode = 101
    // 需要申请 麦克风权限-读写sd卡权限-摄像头权限


    /**
     * 校验并请求权限
     */
    private fun checkOrRequestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.CAMERA"
                ) !== PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.RECORD_AUDIO"
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        "android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO"
                    ), permissionCode
                )
                return false
            }
        }
        return true
    }

    private lateinit var factory: VideoCaptureFactory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkOrRequestPermission()) {
            init()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionCode) {
            init()
        }
    }

    private fun init() {
        factory = VideoCaptureFactory(this)
        ZegoExternalVideoCapture.setVideoCaptureFactory(
            factory,
            ZegoConstants.PublishChannelIndex.MAIN
        )
        ZegoLiveRoom.setConfig("prefer_play_ultra_source=1")
        setCallBack()
        ZegoLiveRoom.setTestEnv(true)
        ZegoLiveRoom.enableCheckPoc(false)
        liveroom.initSDK(
            3991142514L, appSign
        ) {
            if (it == 0) {
                liveroom.loginRoom(
                    "aa",
                    ZegoConstants.RoomRole.Anchor
                ) { p0, p1 ->
                    if (p0 == 0) {
                        btn_start_capture.isEnabled = true
                    }
                }
            }
        }
    }

    private fun setCallBack() {
        btn_start_playing.setOnClickListener {
            liveroom.startPlayingStream("roomid001", tv_play)
            liveroom.setViewMode(ZegoVideoViewMode.ScaleAspectFill, "roomid001")
        }
        btn_start_capture.setOnClickListener {
            liveroom.setPreviewView(tv_bg)
            liveroom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill)
            liveroom.startPreview()
            liveroom.startPublishing("roomid001", "", ZegoConstants.PublishFlag.JoinPublish)
        }
        btn_start_throwI.setOnClickListener {
            factory.throwI(true)
            factory.throwP(false)
        }
        btn_start_throwP.setOnClickListener {
            factory.throwP(true)
            factory.throwI(false)
        }
        btn_start_no_throw.setOnClickListener {
            factory.throwP(false)
            factory.throwI(false)
        }
        btn_low_bit_rate.setOnClickListener {
            factory.setBitRate(30000)
        }
        btn_middle_bit_rate.setOnClickListener {
            factory.setBitRate(200000)
        }
        btn_high_bit_rate.setOnClickListener {
            factory.setBitRate(400000)
        }
        btn_low_frame_rate.setOnClickListener {
            factory.setLostFrame(26)
        }
        btn_high_frame_rate.setOnClickListener {
            factory.setLostFrame(1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoExternalVideoCapture.setVideoCaptureFactory(
            null,
            ZegoConstants.PublishChannelIndex.MAIN
        )
    }

}
