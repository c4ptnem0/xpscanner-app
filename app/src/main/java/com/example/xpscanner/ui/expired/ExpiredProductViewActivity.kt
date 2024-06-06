package com.example.xpscanner.ui.expired

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.xpscanner.R
import com.example.xpscanner.databinding.ActivityExpiredProductViewBinding
import com.example.xpscanner.ui.scan.AddItemActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ExpiredProductViewActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    var databaseReference: DatabaseReference? = null
    private lateinit var binding: ActivityExpiredProductViewBinding
    private lateinit var deleteProductBtn: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var builder: AlertDialog.Builder
    private lateinit var datePickerDialog: DatePickerDialog
    private var imageUri: Uri? = null

    private lateinit var productID: TextView
    private lateinit var productBarcode: TextView
    private lateinit var productName: TextView
    private lateinit var productExpiration: TextView
    private lateinit var productCategory: TextView
    private lateinit var productDescription: TextView
    private lateinit var productImage: ImageView

    // Declare productImageInt and productIDInt as instance variables
    private var productImageInt: String? = null
    private var productIDInt: String? = null

    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationID = 101

    companion object {
        const val REQUEST_IMAGE_PICKER = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpiredProductViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val toolbar = findViewById<Toolbar>(com.example.xpscanner.R.id.toolbar)
        setSupportActionBar(toolbar)

        productImage = binding.productImage
        productExpiration = binding.productExpiration
        fab = binding.fab

        // Set up the date picker
        val calendar = Calendar.getInstance()
        datePickerDialog = DatePickerDialog(
            this, this, calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )


        // Show the date picker when the user clicks the expiration date field
        binding.productExpiration.setOnClickListener {
            datePickerDialog.show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Product Expired"
            val descriptionText = "Notify when a product is expired"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // image picker
        fab.setOnClickListener {
            ImagePicker.with(this)
                .crop(1f, 1f)                            //Crop image into 1:1 ratio
                .compress(1024)                        // image size will be less than 1 MB
                .maxResultSize(1080, 1080)        // image resolution will be less than 1080 x 1080
                .start(REQUEST_IMAGE_PICKER)
        }

        // get the passed extra values from the previous activity
        productIDInt = intent.getStringExtra("productID")
        productImageInt = intent.getStringExtra("productImage")
        val productBarcodeInt = intent.getStringExtra("productBarcode")
        val productNameInt = intent.getStringExtra("productName")
        val productExpirationInt = intent.getStringExtra("productExpiration")
        val productCategoryInt = intent.getStringExtra("productCategory")
        val productDescriptionInt = intent.getStringExtra("productDescription")


        // get references from the activity_product_view Textviews
        productID = findViewById(R.id.productID)
        productBarcode = findViewById(R.id.productBarcode)
        productName = findViewById(R.id.productName)
        productExpiration = findViewById(R.id.productExpiration)
        productCategory = findViewById(R.id.productCategory)
        productDescription = findViewById(R.id.productDescription)
        productImage = findViewById(R.id.productImage)

        deleteProductBtn = findViewById(R.id.deleteProductBtn)

        // load the product image using Picasso
        Picasso.get()
            .load(productImageInt)
            .transform(RoundedTransformation(16))
            .into(productImage)


        // set the text of the Textviews using the retrieved values
        productID.text = productIDInt
        productBarcode.text = productBarcodeInt
        productName.text = productNameInt
        productExpiration.text = productExpirationInt
        productCategory.text = productCategoryInt
        productDescription.text = productDescriptionInt

        // delete button is clicked, delete the product
        deleteProductBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Product Delete")
                .setMessage("Are you sure you want to delete it?")
                .setPositiveButton("Yes") { _, _ ->
                    // Show the progress dialog
                    val progressDialog = showProgressDialog(this@ExpiredProductViewActivity)

                    // Delete the product with a seconds delay to display progress dialog
                    Handler(Looper.getMainLooper()).postDelayed({
                        deleteProduct()
                        // Hide the progress dialog
                        progressDialog.dismiss()
                    }, 1000)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

    }

    fun showProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Loading")
        progressDialog.setMessage("Please wait while we delete your product")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        return progressDialog
    }

    fun isExpired(expirationDate: String): Boolean {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val currentDate = Calendar.getInstance().time
        val expiryDate = dateFormat.parse(expirationDate)
        return expiryDate.before(currentDate)
    }

    // function for deleting the product
    private fun deleteProduct() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Products")
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.getReferenceFromUrl(productImageInt!!)
        storageReference.delete().addOnSuccessListener {
            databaseReference?.child(productIDInt!!)?.removeValue()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK)
        {
            when(requestCode) {
                AddItemActivity.REQUEST_IMAGE_PICKER -> {
                    imageUri = data?.data
                    productImage.setImageURI(imageUri)
                }
            }
        }
        productImage.setImageURI(data?.data)
    }

    // Update the expiration date text when the user sets a new date
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = dateFormat.format(calendar.time)
        productExpiration.setText(date)

        Toast.makeText(this, "Selected date: $date", Toast.LENGTH_SHORT).show()
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