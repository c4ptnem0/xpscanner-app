package com.example.xpscanner.ui.manage

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.example.xpscanner.Login
import com.example.xpscanner.R
import com.example.xpscanner.databinding.ActivityManageProductViewBinding
import com.example.xpscanner.ui.scan.AddItemActivity
import com.example.xpscanner.ui.scan.ScanActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ManageProductViewActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var binding: ActivityManageProductViewBinding
    private lateinit var deleteProductBtn: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var saveEditedProductBtn: TextView
    private lateinit var builder: AlertDialog.Builder
    private var imageUri: Uri? = null
    private lateinit var datePickerDialog: DatePickerDialog
    var databaseReference: DatabaseReference? = null

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

    companion object {
        const val REQUEST_IMAGE_PICKER = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductViewBinding.inflate(layoutInflater)
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
        saveEditedProductBtn = findViewById(R.id.saveEditedProductBtn)

        // load the product image using Picasso
        Picasso.get()
            .load(productImageInt)
            .transform(RoundedTransformation(16))
            .into(productImage)

        builder = AlertDialog.Builder(this)
        saveEditedProductBtn.setOnClickListener {
            // product information
            val productName = binding.productName.text.toString()
            val productCategory = binding.productCategory.text.toString()
            val productDescription = binding.productDescription.text.toString()
            val productExpiration = binding.productExpiration.text.toString()
            val uri = imageUri

            // checking if the EditText is not empty, else it will show error
            if (productName.isNotEmpty() && productCategory.isNotEmpty()
                && productDescription.isNotEmpty() && productExpiration.isNotEmpty()) {

                databaseReference = FirebaseDatabase.getInstance().getReference("Products").child(productIDInt!!)

                // Delete the previous image file if it exists and a new image is selected
                if (uri != null && productImageInt != null) {
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(productImageInt!!)
                    storageRef.delete()
                }

                if (uri == null) {
                    // Show the progress dialog
                    val progressDialog = showProgressDialog(this@ManageProductViewActivity)
                    // If the image is not updated, update only the product information
                    val updatesMap: MutableMap<String, Any> = mutableMapOf(
                        "productName" to productName,
                        "productCategory" to productCategory,
                        "productDescription" to productDescription,
                        "productExpiration" to productExpiration,
                    )
                    productImageInt?.let { previousImage ->
                        // add the previous image url to the updatesMap if it exists
                        updatesMap["productImage"] = previousImage
                    }

                    databaseReference?.updateChildren(updatesMap)?.addOnSuccessListener {
                        // Hide the progress dialog
                        progressDialog.dismiss()

                        showDialogBox()

                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
                    }?.addOnFailureListener {
                        // product update failed
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Create a storage reference with a unique name for the image
                    val storageRef = FirebaseStorage.getInstance().reference.child("productImages/${UUID.randomUUID()}.jpg")

                    // Upload the image to Firebase Storage if the image URI is not null
                    storageRef.putFile(uri).addOnSuccessListener { taskSnapshot ->

                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // update the product with the new image
                            val updatesMap = mapOf(
                                "productName" to productName,
                                "productCategory" to productCategory,
                                "productDescription" to productDescription,
                                "productExpiration" to productExpiration,
                                "productImage" to uri.toString()
                            )
                            databaseReference?.updateChildren(updatesMap)?.addOnSuccessListener {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
                            }?.addOnFailureListener {
                                // product update failed
                                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this@ManageProductViewActivity, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }


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
                    val progressDialog = showProgressDialog(this@ManageProductViewActivity)

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

    // function for showing dialog box
    fun showDialogBox() {
        builder.setTitle("Success!")
            .setMessage("Product Updated Successfully!")
            .setCancelable(true)
            .setPositiveButton("Close") { dialogInterface, it ->
                Handler().postDelayed({

                }, 3000) // delay for 1 second
            }
            .show()
    }

    fun showProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Loading")
        progressDialog.setMessage("Please wait while we update your product")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        return progressDialog
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
            Log.d("ProductViewActivity", "Back button pressed")
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}