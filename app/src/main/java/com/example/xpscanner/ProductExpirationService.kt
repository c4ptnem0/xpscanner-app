package com.example.xpscanner

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.xpscanner.ui.manage.DataClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class ProductExpirationService : Service() {
    private val INTERVAL: Long = 8 * 60 * 60 * 1000 // 8 hours in milliseconds
    private val CHANNEL_ID = "expired_product_channel"
    private val NOTIFICATION_ID = 1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startTask()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Expired Product Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // create notiication
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent: PendingIntent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Product Expiration Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.mipmap.xpscanner_icon)
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startTask() {
        Thread {
            while (true) {
                checkForExpiredProducts { expiredProducts ->
                    if (expiredProducts.isNotEmpty()) {
                        showExpiredProductNotification(expiredProducts)
                    }
                }
                Thread.sleep(INTERVAL)
            }
        }.start()
    }

    // Checks the product expiration and compare currents date if it is expired or not
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkForExpiredProducts(callback: (List<DataClass>) -> Unit) {
        val expiredProducts = mutableListOf<DataClass>()
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val productsRef = FirebaseDatabase.getInstance().getReference("Products")
            .orderByChild("userID").equalTo(userID)
        val currentDate = LocalDate.now()

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expiredProducts.clear() // Clear the list before adding new expired products
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(DataClass::class.java)
                    if (dataClass != null) {
                        val expirationDate =
                            LocalDate.parse(
                                dataClass.productExpiration,
                                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            )
                        if (expirationDate.isEqual(currentDate) || expirationDate.isBefore(
                                currentDate.plusDays(
                                    4
                                )
                            )
                        ) {
                            dataClass.productID = itemSnapshot.key
                            expiredProducts.add(dataClass)
                        }
                    }
                }

                // Check for newly added products with today's expiration date
                for (newItemSnapshot in snapshot.children) {
                    if (!expiredProducts.any { it.productID == newItemSnapshot.key }) {
                        val dataClass = newItemSnapshot.getValue(DataClass::class.java)
                        if (dataClass != null) {
                            val expirationDate = LocalDate.parse(
                                dataClass.productExpiration,
                                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            )
                            if (expirationDate.isEqual(currentDate)) {
                                dataClass.productID = newItemSnapshot.key
                                expiredProducts.add(dataClass)
                            }
                        }
                    }
                }

                callback(expiredProducts)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showExpiredProductNotification(expiredProducts: List<DataClass>) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existingNotifications = notificationManager.activeNotifications

        for (product in expiredProducts) {
            val productId = product.productID
            val notificationId = productId.hashCode() // Generate a unique notification ID based on the product ID

            // Check if the notification for this product already exists
            val existingNotification = existingNotifications.find { it.id == notificationId }
            if (existingNotification != null) {
                continue // Skip this product, as the notification already exists
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
            )
            val productName = product.productName
            val expirationDate = LocalDate.parse(
                product.productExpiration,
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
            )
            val daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate)

            val notificationText = when (daysUntilExpiration) {
                3L -> "$productName will expire in 3 days! Please use it to avoid wasting product."
                2L -> "$productName will expire in 2 days! Please use it to avoid wasting product."
                1L -> "$productName will expire tomorrow! Please use it to avoid wasting product."
                0L -> "$productName has expired today! Please dispose it properly."
                else -> {
                    if (daysUntilExpiration < 0) {
                        val expirationDateString =
                            expirationDate.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))
                        "$productName has expired on $expirationDateString! Please dispose it properly."
                    } else {
                        "" // Don't show a notification for other cases
                    }
                }
            }

            if (notificationText.isNotEmpty()) {
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Product Expiration Reminder")
                    .setContentText(notificationText)
                    .setSmallIcon(R.mipmap.xpscanner_icon)
                    .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(notificationId, notification)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}
