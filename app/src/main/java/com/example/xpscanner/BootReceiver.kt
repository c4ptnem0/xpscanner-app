package com.example.xpscanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the ProductExpirationService when the device boots up
            val serviceIntent = Intent(context, ProductExpirationService::class.java)
            context.startService(serviceIntent)
        }
    }
}
