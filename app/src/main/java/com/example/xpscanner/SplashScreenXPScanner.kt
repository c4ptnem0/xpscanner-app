package com.example.xpscanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

class SplashScreenXPScanner : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen_xpscanner)
        supportActionBar?.hide()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            // Get the Firebase Authentication instance
            val auth = FirebaseAuth.getInstance()

            // Check if a user is currently logged in
            if (auth.currentUser != null) {
                // User is logged in, redirect to dashboard activity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // User is not logged in, redirect to login activity
                startActivity(Intent(this, Login::class.java))
                finish()
            }
            finish()
        },3000)
    }
}