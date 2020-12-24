package com.robertlevonyan.demo.camerax

import android.app.Application
import android.content.Context

import com.robertlevonyan.demo.camerax.utils.CrashHandler


/**
 * @author: lico
 * @create：2020/5/21
 * @describe：
 */
class MyApplication : Application() {


    companion object {
        var context: Application? = null


        fun getApplication(): Context {
            return context!!
        }

    }

    init {
        context = this
    }

    override fun onCreate() {
        super.onCreate()

        CrashHandler.instance?.init(context)


    }


}