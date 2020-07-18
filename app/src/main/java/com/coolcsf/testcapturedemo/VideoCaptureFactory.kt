package com.coolcsf.testcapturedemo

import com.zego.zegoavkit2.ZegoVideoCaptureDevice
import com.zego.zegoavkit2.ZegoVideoCaptureFactory

class VideoCaptureFactory(val activity: MainActivity) : ZegoVideoCaptureFactory() {
    private var device: VideoCaptureDevice? = null

    init {
        device = VideoCaptureDevice(activity)
    }

    override fun destroy(p0: ZegoVideoCaptureDevice?) {
        device = null
    }

    fun throwI(isLow: Boolean) {
        device?.throwI = isLow
    }

    fun throwP(isLow: Boolean) {
        device?.throwP = isLow
    }

    fun setLostFrame(fps: Int) {
        device?.setLostFrame(fps)
    }

    fun setBitRate(bitRate: Int) {
        device?.setBitRate(bitRate)
    }

    override
    fun create(p0: String?): ZegoVideoCaptureDevice? {
        return device
    }
}