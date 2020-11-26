package com.allenlucas.lockscreen

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.io.DataOutputStream

/**
 * 一键锁屏
 * @link https://www.jianshu.com/p/ef1b05c7b1db 的kotlin实现
 */
class MainActivity : Activity() {

    private val mDevicePolicyManager by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val mComponentName by lazy { ComponentName(this, LockReceiver::class.java) }

    //休眠的命令
    private val closeScreen = "input keyevent 223"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreen()
    }

    override fun onStop() {
        super.onStop()
        android.os.Process.killProcess(android.os.Process.myPid())  //结束进程
    }

    /**
     * 锁屏
     * root权限的可以锁屏搭配指纹解锁
     * 无root权限的锁屏后无法使用指纹
     */
    private fun lockScreen() {
        if (rootCommand(closeScreen)) {
            return
        }
        if (!mDevicePolicyManager.isAdminActive(mComponentName)) {
            activeManager()
            return
        }
        mDevicePolicyManager.lockNow()
    }

    /**
     * app获取root权限
     */
    private fun rootCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream).apply {
                writeBytes("$command\n")
                writeBytes("exit\n")
                flush()
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.d("*** DEBUG ***", "Root REE${e.message}")
            return false
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (e: Exception) {
            }
        }
        Log.d("*** DEBUG ***", "Root SUC")
        return true
    }

    /**
     * 激活设备管理器获取权限
     */
    private fun activeManager() {
        startActivity(getAddDeviceIntent())
    }

    /**
     * 跳转设备管理器启用 Intent
     */
    private fun getAddDeviceIntent() = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName)
        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.app_name))
    }
}