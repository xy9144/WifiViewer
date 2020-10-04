package com.example.wsins.wifiviewer.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.wsins.wifiviewer.R
import com.example.wsins.wifiviewer.listener.OnNextListener
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

/**
 * Created by Sin on 2019/4/13
 */
object RootUtils {

    private const val binPath = "/system/bin/su"
    private const val xBinPath = "/system/xbin/su"
    private const val sBinPath = "/sbin/su"

    val isRoot: Boolean
        get() {
            var bool = false
            try {
                bool = (File(binPath).exists() || File(xBinPath).exists() || File(sBinPath).exists())
            } catch (e: Exception) {
            }
            return bool
        }

    val isGrant: Boolean
        get() {
            var bool = false
            try {
                bool = checkGrant()
            } catch (e: Exception) {
            }
            return bool
        }

    private fun checkGrant(): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        val rt: Int
        try {
            process = getSUProcess()
            os = DataOutputStream(process.outputStream)
            os.writeBytes("system/bin/mount -o rw,remount -t rootfs /data" + "\n")
            os.writeBytes("exit\n")
            os.flush()
            rt = process.waitFor()
        } catch (e: Exception) {
            return false
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            process?.destroy()
        }
        return rt == 0
    }

    fun getSUProcess(): Process = Runtime.getRuntime().exec("su")

    fun checkRootAccess(context: Context, listener: OnNextListener) {
        if (isRoot) {
            listener.onNext()
        } else {
            AlertDialog.Builder(context).run {
                setTitle(context.getString(R.string.root_privilege_check))
                setMessage(context.getString(R.string.this_equipment_is_not_authorized))
                setCancelable(false)
                setPositiveButton(context.getString(R.string.sign_out)) { _, _ ->
                    ActivityManager.exitApp(context)
                }
                show()
            }
        }
    }
}
