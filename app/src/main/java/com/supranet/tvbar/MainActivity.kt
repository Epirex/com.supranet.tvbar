package com.supranet.tvbar

import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usbcameracommon.UVCCameraHandler
import com.serenegiant.widget.CameraViewInterface

class MainActivity : AppCompatActivity(), CameraDialog.CameraDialogParent {
    private lateinit var mUSBMonitor: USBMonitor
    private lateinit var mCameraHandler: UVCCameraHandler
    private lateinit var mUVCCameraView: CameraViewInterface
    private val USB_PERMISSION = "com.supranet.tvbar.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        setContentView(R.layout.activity_main)

        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
        mUVCCameraView = findViewById(R.id.camera_view)
        mUVCCameraView.aspectRatio = (PREVIEW_WIDTH / PREVIEW_HEIGHT.toFloat()).toDouble()
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView, 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE)

        // Verificar y solicitar permiso
        if (ContextCompat.checkSelfPermission(this, USB_PERMISSION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(USB_PERMISSION), REQUEST_USB_PERMISSION)
        }
    }

    override fun onStart() {
        super.onStart()
        mUSBMonitor.register()
        mUVCCameraView.onResume()
    }

    override fun onStop() {
        mCameraHandler.close()
        mUVCCameraView.onPause()
        super.onStop()
    }

    override fun onDestroy() {
        mCameraHandler.release()
        mUSBMonitor.destroy()
        super.onDestroy()
    }

    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            Toast.makeText(this@MainActivity, "Dispositivo USB Conectado", Toast.LENGTH_SHORT).show()
            mUSBMonitor.requestPermission(device)
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            mCameraHandler.open(ctrlBlock)
            startPreview()
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            mCameraHandler.close()
        }

        override fun onDettach(device: UsbDevice) {
            Toast.makeText(this@MainActivity, "Dispositivo USB Desconectado", Toast.LENGTH_SHORT).show()
        }

        override fun onCancel(device: UsbDevice) {}
    }

    private fun startPreview() {
        val st = mUVCCameraView.surfaceTexture
        mCameraHandler.startPreview(Surface(st))
    }

    override fun getUSBMonitor(): USBMonitor {
        return mUSBMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
        Log.v(TAG, "onDialogResult:canceled=$canceled")
    }

    companion object {
        private const val PREVIEW_WIDTH = 1280
        private const val PREVIEW_HEIGHT = 720
        private const val PREVIEW_MODE = 1
        private const val TAG = "MainActivity"
        private const val REQUEST_USB_PERMISSION = 1
    }
}