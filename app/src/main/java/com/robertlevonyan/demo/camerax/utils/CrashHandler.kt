/**
 * 功    能：
 * 类 列 表：CrashHandler
 * 作　　者：lzp
 * 创建日期：2016/7/11 0011  上午 9:46
 * 注　　意：
 * Copyright (c) ：2016 by .版权所有.<br></br>
 */
package com.robertlevonyan.demo.camerax.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.os.Process

import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * 功    能：
 * 作　　者：lzp <br></br>
 * 创建日期：2016/7/11 0011  上午 9:46 <br></br>
 * 注　　意： <br></br>
 */
class CrashHandler : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler // 系统默认的UncaughtException处理类
            : Thread.UncaughtExceptionHandler? = null
    private var mContext // 程序的Context对象
            : Context? = null
    private val info: MutableMap<String, String> =
        HashMap() // 用来存储设备信息和异常信息

    @SuppressLint("SimpleDateFormat")
    private val format =
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss") // 用于格式化日期,作为日志文件名的一部分

    /**
     * 初始化
     *
     * @param context
     */
    fun init(context: Context?) {
        mContext = context
        mDefaultHandler =
            Thread.getDefaultUncaughtExceptionHandler() // 获取系统默认的UncaughtException处理器
        Thread.setDefaultUncaughtExceptionHandler(this) // 设置该CrashHandler为程序的默认处理器
    }

    /**
     * 当UncaughtException发生时会转入该重写的方法来处理
     */
    override fun uncaughtException(
        thread: Thread,
        ex: Throwable
    ) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果自定义的没有处理则让系统默认的异常处理器来处理
            mDefaultHandler!!.uncaughtException(thread, ex)

        } else {
            val jsonObject = JSONObject()
            for ((key, value) in info) {
                try {
                    jsonObject.put(key, value)
                    jsonObject.put("Exception", ex.toString())
                    val error = getErrorInfo(ex).replace("\n".toRegex(), "<br>")
                    jsonObject.put("Error", error)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            try {
                Thread.sleep(3000) // 如果处理了，让程序继续运行3秒再退出，保证文件保存并上传到服务器
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // 退出程序
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex 异常信息
     * @return true 如果处理了该异常信息;否则返回false.
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false
        object : Thread() {
            override fun run() {
                Looper.prepare()
                Looper.loop()
            }
        }.start()
        // 收集设备参数信息
        collectDeviceInfo(mContext)
        // 保存日志文件
        saveCrashInfo2File(ex)

        return false
    }

    /**
     * 收集设备参数信息
     *
     * @param context
     */
    fun collectDeviceInfo(context: Context?) {
        try {
            val pm = context!!.packageManager // 获得包管理器
            val pi = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_ACTIVITIES
            ) // 得到该应用的信息，即主Activity
            if (pi != null) {
                val versionName =
                    if (pi.versionName == null) "null" else pi.versionName
                val versionCode = pi.versionCode.toString() + ""
                info["versionName"] = versionName
                info["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val fields =
            Build::class.java.declaredFields // 反射机制
        for (field in fields) {
            try {
                field.isAccessible = true
                info[field.name] = field[""].toString()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取错误的信息
     *
     * @param ex
     * @return
     */
    private fun getErrorInfo(ex: Throwable): String {
        val writer: Writer = StringWriter()
        val pw = PrintWriter(writer)
        ex.printStackTrace(pw)
        var cause = ex.cause
        // 循环着把所有的异常信息写入writer中
        while (cause != null) {
            cause.printStackTrace(pw)
            cause = cause.cause
        }
        pw.close() // 记得关闭
        return writer.toString()
    }

    private fun saveCrashInfo2File(ex: Throwable): String? {
        val sb = StringBuilder()
        for ((key, value) in info) {
            sb.append(key).append("=").append(value).append("\r\n")
        }
        val error = getErrorInfo(ex)
        sb.append(error)
        // 保存文件
        val time = format.format(Date())
        val fileName = "Error-$time.text"
        var fos: FileOutputStream? = null
        try {
            val filePath = SDCardUtils.getSDMouthPath("ErrorLog")
            fos = FileOutputStream(File(filePath, fileName))
            fos.write(sb.toString().toByteArray())
            return fileName
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "CrashHandler"
        private var INSTANCE: CrashHandler? = null

        /**
         * 获取CrashHandler实例 ,单例模式
         */
        val instance: CrashHandler?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = CrashHandler()
                }
                return INSTANCE
            }
    }
}