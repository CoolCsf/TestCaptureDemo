package com.coolcsf.testcapturedemo

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AVCEncoder1(val mViewWidth: Int, val mViewHeight: Int) {

    /**
     * 视频数据信息结构体
     * 包含时间戳，视频数据，关键帧标记
     */
    internal class TransferInfo {
        var timeStmp: Long = 0
        var inOutData: ByteArray = ByteArray(0)
        var isKeyFrame = false
    }

    // 音视频编解码器组件
    private val mMediaCodec: MediaCodec by lazy {
        // 选用MIME类型为AVC、编码器来构造MediaCodec
        MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            this.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }
    // 媒体数据格式信息
    private val mMediaFormat: MediaFormat by lazy {
        // 设置MediaFormat，必须设置 KEY_COLOR_FORMAT，KEY_BIT_RATE，KEY_FRAME_RATE，KEY_I_FRAME_INTERVAL的值
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mViewWidth, mViewHeight)
            .apply {
                this.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                ) //COLOR_FormatYUV420PackedSemiPlanar
                this.setInteger(MediaFormat.KEY_BIT_RATE, 4000000)
                this.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
                this.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
    }
    private val configData = ByteBuffer.allocateDirect(1)

}