package com.example.xpscanner.ui.scan

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.xpscanner.databinding.ActivityAddItemBinding
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*


class AddItemActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var binding:ActivityAddItemBinding
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var productExpiration: EditText
    private lateinit var productBarcode: EditText
    private lateinit var productImage: ImageView
    private lateinit var generateBarcode: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var builder: AlertDialog.Builder
    private var imageUri: Uri? = null

    companion object {
        const val REQUEST_IMAGE_PICKER = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // product information to be used by other functions below
        productBarcode = binding.productBarcode
        productExpiration = binding.productExpiration
        productImage = binding.productImage
        generateBarcode = binding.generateBarcode
        fab = binding.fab

        val result = intent.getStringExtra(ScanActivity.RESULT)
        val toolbar = findViewById<Toolbar>(com.example.xpscanner.R.id.toolbar)
        setSupportActionBar(toolbar)

        if (result != null) {
            // if result is from scanner, set result and hide generate barcode button
            productBarcode.setText(result)
            generateBarcode.visibility = View.INVISIBLE
        } else {
            // else, set result to empty "" and show generate barcode button
            productBarcode.setText("")
            generateBarcode.visibility = View.VISIBLE
        }

        // Set up the date picker
        val calendar = Calendar.getInstance()
        datePickerDialog = DatePickerDialog(
            this, this, calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Generate 13 digits random number for barcode
        binding.generateBarcode.setOnClickListener {
            val randomBarcode = (1000000000000..9999999999999).random().toString()
            productBarcode.setText(randomBarcode)
        }

        // Show the date picker when the user clicks the expiration date field
        binding.productExpiration.setOnClickListener {
            datePickerDialog.show()
        }

        fab.setOnClickListener{
            ImagePicker.with(this)
                .crop(1f, 1f)	    			        //Crop image into 1:1 ratio
                .compress(1024)			            // image size will be less than 1 MB
                .maxResultSize(1080, 1080)	    // image resolution will be less than 1080 x 1080
                .start(REQUEST_IMAGE_PICKER)
        }

        builder = AlertDialog.Builder(this)

        binding.saveProductBtn.setOnClickListener {
            val database = FirebaseDatabase.getInstance()
            // get the current user's ID
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            // get a reference to the "Products" node
            val productRef = database.getReference("Products")

            // product informations
            val productBarcode = "XPS-" + binding.productBarcode.text.toString()
            val productName = binding.productName.text.toString()
            val productCategory = binding.productCategory.text.toString()
            val productDescription = binding.productDescription.text.toString()
            val expirationDate = binding.productExpiration.text.toString()
            val uri = imageUri

            // checking if the EditText is not empty, else it will show error
            if(productBarcode.isNotEmpty() && productName.isNotEmpty() && productCategory.isNotEmpty()
                && productDescription.isNotEmpty() && expirationDate.isNotEmpty())
            {
                if(imageUri == null)
                {
                    // The image URI is null, show a toast message
                    Toast.makeText(this@AddItemActivity, "Please capture or select an image!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener // exit the function
                }

                // check if the product already exists in the database
                productRef.orderByChild("productBarcode").equalTo(productBarcode).addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SuspiciousIndentation")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var productExists = false
                        // Loop through the results to check if any match the current user's ID
                        for (productSnapshot in snapshot.children) {
                            val userID = productSnapshot.child("userID").value as String
                            if (userID == uid)
                            {
                                // Product with the same barcode and user ID exists, show an error message
                                Toast.makeText(this@AddItemActivity, "Product already added!", Toast.LENGTH_SHORT).show()
                                productExists = true
                                break
                            }
                        }
                        if(!productExists)
                        {
                            // Show the progress dialog
                            val progressDialog = showProgressDialog(this@AddItemActivity)

                            // Create a storage reference with a unique name for the image
                            val storageRef = FirebaseStorage.getInstance().reference.child("productImages/${UUID.randomUUID()}.jpg")

                            // Upload the image to Firebase Storage if the image URI is not null
                            storageRef.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
                                // Get the download URL of the image
                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                    // generate a new unique key id for the product
                                    val newProductRef = productRef.push()

                                    // create a new hashMap with the product information
                                    val productValues = hashMapOf(
                                        "userID" to uid,
                                        "productBarcode" to productBarcode,
                                        "productName" to productName,
                                        "productCategory" to productCategory,
                                        "productDescription" to productDescription,
                                        "productExpiration" to expirationDate,
                                        "productImage" to uri.toString() // Store the download URL of the image
                                    )

                                        // Save the product information to the database
                                        newProductRef.setValue(productValues).addOnCompleteListener { saveProduct ->
                                            if (saveProduct.isSuccessful)
                                            {
                                                // Hide the progress dialog
                                                progressDialog.dismiss()

                                                showDialogBox()

                                                Toast.makeText(this@AddItemActivity, "Product added!", Toast.LENGTH_SHORT).show()
                                            }
                                        else
                                        {
                                            // Hide the progress dialog
                                            progressDialog.dismiss()

                                            Toast.makeText(this@AddItemActivity, "Product failed to add!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }.addOnFailureListener { exception ->
                                Toast.makeText(this@AddItemActivity, "Failed to upload image!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@AddItemActivity, "Error: Something went wrong!", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            else
            {
                Toast.makeText(this@AddItemActivity, "Complete all the fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // function for showing dialog box
    fun showDialogBox() {
        builder.setTitle("Success!")
            .setMessage("Product Added Successfully!")
            .setCancelable(true)
            .setPositiveButton("Close") { dialogInterface, it ->
                finish()
            }
            .show()
    }

    fun showProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Adding Product")
        progressDialog.setMessage("Please wait while we add your product")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        return progressDialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK)
        {
            when(requestCode) {
                REQUEST_IMAGE_PICKER -> {
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
