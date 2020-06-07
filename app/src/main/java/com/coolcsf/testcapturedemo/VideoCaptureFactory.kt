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

    override fun create(p0: String?): ZegoVideoCaptureDevice? {
        return device
    }
}