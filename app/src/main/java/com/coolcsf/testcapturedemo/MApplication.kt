package com.coolcsf.testcapturedemo

import android.app.Application
import com.zego.zegoliveroom.ZegoLiveRoom

class MApplication : Application() {
    companion object{
        lateinit var instance: MApplication
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        ZegoLiveRoom.setSDKContext(object : ZegoLiveRoom.SDKContext {
            override fun getAppContext(): Application {
                return instance
            }

            override fun getSoFullPath(): String? {
                return null;
            }

            override fun getLogPath(): String? {
                return null;
            }

        })
        ZegoLiveRoom.setUser("aa", "aa")
    }
}