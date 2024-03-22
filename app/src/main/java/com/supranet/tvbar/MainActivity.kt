package com.supranet.tvbar

import android.content.ContentValues.TAG
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Toast
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
        setContentView(R.layout.activity_main)

        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
        mUVCCameraView = findViewById(R.id.camera_view)
        mUVCCameraView.aspectRatio = (PREVIEW_WIDTH / PREVIEW_HEIGHT.toFloat()).toDouble()
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView, 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE)
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
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480
        private const val PREVIEW_MODE = 1
    }

}