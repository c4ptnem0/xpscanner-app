package com.example.xpscanner.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.xpscanner.MainActivity
import com.example.xpscanner.R
import com.example.xpscanner.databinding.ActivityAddItemBinding
import com.example.xpscanner.databinding.ActivityScanBinding
import com.google.zxing.BarcodeFormat
import me.dm7.barcodescanner.zxing.ZXingScannerView
import com.google.zxing.Result

// Class for BarcodeScanner
class ScanActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    var scannerView : ZXingScannerView? = null

    private lateinit var binding: ActivityScanBinding
    private lateinit var scanHintTextView: TextView

    companion object {
        const val RESULT = "SCAN_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val toolbar = findViewById<Toolbar>(com.example.xpscanner.R.id.toolbar)
        setSupportActionBar(toolbar)

        // set only the scanner view to portrait mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        scannerView = findViewById(R.id.scanner_view)
        scannerView?.setAutoFocus(true)
        scannerView?.setAspectTolerance(0.5f)
        scannerView?.setBorderColor(Color.parseColor("#00c8ac"))
        scannerView?.setLaserColor(Color.parseColor("#00c8ac"))
        scannerView?.setMaskColor(Color.argb(0, 0, 0, 0))
        scannerView?.setIsBorderCornerRounded(true)
        scannerView?.setBorderCornerRadius(10)
        scannerView?.setBorderLineLength(100)
        scannerView?.setBorderStrokeWidth(10)


        setPermission()
    }

    // Handle the scanned result
    override fun handleResult(p0: Result?) {
        val resultString = p0.toString()

        // Play beep sound after scanning successfully
        val mediaPlayer = MediaPlayer.create(this, R.raw.xp_beep_sound)
        mediaPlayer.start()

        // Intent to start the AddItemActivity and pass the scan result as an extra
        val intent = Intent(this, AddItemActivity::class.java).apply {
            putExtra(RESULT, resultString)
        }
        startActivity(intent)

        // Show a toast with the scanned result
        Toast.makeText(this, "Scanned Result: $resultString", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()

        scannerView?.setResultHandler(this)
        scannerView?.startCamera()

    }

    override fun onPause() {
        super.onPause()

        scannerView?.stopCamera()
    }

    // Check if camera permission is granted
    private fun setPermission() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    // Request for camera permission
    private fun makeRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
            100)
    }

    // Handle the result of the camera permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(applicationContext, "You need camera permission!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
        {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}