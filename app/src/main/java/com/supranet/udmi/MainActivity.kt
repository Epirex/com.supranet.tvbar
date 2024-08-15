package com.supranet.udmi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usbcameracommon.UVCCameraHandler
import com.serenegiant.widget.CameraViewInterface

class MainActivity : AppCompatActivity(), CameraDialog.CameraDialogParent {
    private lateinit var mUSBMonitor: USBMonitor
    private lateinit var mCameraHandler: UVCCameraHandler
    private lateinit var mUVCCameraView: CameraViewInterface

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

        val filter = IntentFilter(USB_PERMISSION)

        // Check SDK version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        }

        // Keep the screen always on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
        mUVCCameraView = findViewById(R.id.camera_view)
        mUVCCameraView.aspectRatio = (PREVIEW_WIDTH / PREVIEW_HEIGHT.toFloat()).toDouble()
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView, 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList

        // Check if device list is not empty
        if (deviceList.isNotEmpty()) {
            for (device in deviceList.values) {
                if (!usbManager.hasPermission(device)) {
                    val intent = PendingIntent.getBroadcast(this, 0, Intent(USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                    usbManager.requestPermission(device, intent)
                }
            }
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
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }

    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            mUSBMonitor.requestPermission(device)
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            try {
                mCameraHandler.open(ctrlBlock)
                startPreview()
            } catch (e: Exception) {
            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            mCameraHandler.close()
        }

        override fun onDettach(device: UsbDevice) {
        }

        override fun onCancel(device: UsbDevice) {}
    }

    private fun startPreview() {
        val st = mUVCCameraView.surfaceTexture
        mCameraHandler.startPreview(Surface(st))
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            val ctrlBlock = mUSBMonitor.openDevice(device)
                            ctrlBlock?.let {
                                mCameraHandler.open(it)
                                startPreview()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getUSBMonitor(): USBMonitor {
        return mUSBMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
    }

    companion object {
        private const val PREVIEW_WIDTH = 1280
        private const val PREVIEW_HEIGHT = 720
        private const val PREVIEW_MODE = 1
        private const val USB_PERMISSION = "com.supranet.udmi.USB_PERMISSION"
    }
}