package com.coolcsf.testcapturedemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zego.zegoavkit2.ZegoExternalVideoCapture
import com.zego.zegoavkit2.networktrace.*
import com.zego.zegoliveroom.ZegoLiveRoom
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback
import com.zego.zegoliveroom.callback.IZegoRoomCallback
import com.zego.zegoliveroom.constants.ZegoConstants
import com.zego.zegoliveroom.constants.ZegoVideoViewMode
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality
import com.zego.zegoliveroom.entity.ZegoStreamInfo
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private val liveroom by lazy {
        ZegoLiveRoom()
    }
    val appSign = byteArrayOf(
        0x1e.toByte(),
        0xc3.toByte(),
        0xf8.toByte(),
        0x5c.toByte(),
        0xb2.toByte(),
        0xf2.toByte(),
        0x13.toByte(),
        0x70.toByte(),
        0x26.toByte(),
        0x4e.toByte(),
        0xb3.toByte(),
        0x71.toByte(),
        0xc8.toByte(),
        0xc6.toByte(),
        0x5c.toByte(),
        0xa3.toByte(),
        0x7f.toByte(),
        0xa3.toByte(),
        0x3b.toByte(),
        0x9d.toByte(),
        0xef.toByte(),
        0xef.toByte(),
        0x2a.toByte(),
        0x85.toByte(),
        0xe0.toByte(),
        0xc8.toByte(),
        0x99.toByte(),
        0xae.toByte(),
        0x82.toByte(),
        0xc0.toByte(),
        0xf6.toByte(),
        0xf8.toByte()
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ZegoExternalVideoCapture.setVideoCaptureFactory(
                VideoCaptureFactory(this),
        ZegoConstants.PublishChannelIndex.MAIN
        )
        ZegoNetworktrace.getInstance().setNetworkTraceCallback(object : IZegoNetworkTraceCallback {
            override fun onNetworkTrace(
                p0: Long,
                p1: ZegoHttpTraceResult?,
                p2: ZegoTcpTraceResult?,
                p3: ZegoUdpTraceResult?,
                p4: ZegoTracerouteResult?
            ) {
                Log.d("Test","")
            }

        })
        val config = ZegoNetworkTraceConfig()
        config.needTraceroute = 0
        ZegoNetworktrace.getInstance().startNetworkTrace(config)
        ZegoLiveRoom.setConfig("prefer_play_ultra_source=1")
        btn_start_playing.setOnClickListener {
            liveroom.startPlayingStream("aa", tv_play)
            liveroom.setViewMode(ZegoVideoViewMode.ScaleAspectFill, "aa")
        }
        btn_start_capture.setOnClickListener {
            liveroom.setPreviewView(tv_bg)
            liveroom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill)
            liveroom.startPreview()
            liveroom.startPublishing("aa", "", ZegoConstants.PublishFlag.JoinPublish)
        }
        ZegoLiveRoom.setTestEnv(true)
        liveroom.initSDK(
            1739272706L, appSign
        ) {
            if (it == 0) {
                liveroom.setZegoLivePlayerCallback(object : IZegoLivePlayerCallback {
                    override fun onVideoSizeChangedTo(p0: String?, p1: Int, p2: Int) {

                    }

                    override fun onPlayStateUpdate(p0: Int, p1: String?) {
                        Log.d("aa", "")
                    }

                    override fun onInviteJoinLiveRequest(
                        p0: Int,
                        p1: String?,
                        p2: String?,
                        p3: String?
                    ) {
                    }

                    override fun onPlayQualityUpdate(p0: String?, p1: ZegoPlayStreamQuality?) {
                    }

                    override fun onRecvEndJoinLiveCommand(p0: String?, p1: String?, p2: String?) {
                    }

                })
                liveroom.setZegoLivePublisherCallback(object : IZegoLivePublisherCallback {
                    override fun onCaptureVideoFirstFrame() {
                    }

                    override fun onPublishQualityUpdate(
                        p0: String?,
                        p1: ZegoPublishStreamQuality?
                    ) {
                    }

                    override fun onJoinLiveRequest(p0: Int, p1: String?, p2: String?, p3: String?) {
                    }

                    override fun onCaptureVideoSizeChangedTo(p0: Int, p1: Int) {
                    }

                    override fun onCaptureAudioFirstFrame() {
                    }

                    override fun onPublishStateUpdate(
                        p0: Int,
                        p1: String?,
                        p2: HashMap<String, Any>?
                    ) {
                        Log.d("Test", "onPublishStateUpdate")
                    }

                })
                liveroom.setZegoRoomCallback(object : IZegoRoomCallback {
                    override fun onStreamExtraInfoUpdated(
                        p0: Array<out ZegoStreamInfo>?,
                        p1: String?
                    ) {

                    }

                    override fun onReconnect(p0: Int, p1: String?) {
                    }

                    override fun onKickOut(p0: Int, p1: String?, p2: String?) {
                    }

                    override fun onDisconnect(p0: Int, p1: String?) {
                    }

                    override fun onTempBroken(p0: Int, p1: String?) {
                    }

                    override fun onStreamUpdated(
                        p0: Int,
                        p1: Array<out ZegoStreamInfo>?,
                        p2: String?
                    ) {
                        Log.d("Test", "")
                    }

                    override fun onRecvCustomCommand(
                        p0: String?,
                        p1: String?,
                        p2: String?,
                        p3: String?
                    ) {
                    }

                })
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

    override fun onDestroy() {
        super.onDestroy()
        ZegoExternalVideoCapture.setVideoCaptureFactory(
            null,
            ZegoConstants.PublishChannelIndex.MAIN
        )
    }
}
