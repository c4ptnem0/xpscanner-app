package com.example.xpscanner.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.xpscanner.R
import com.example.xpscanner.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.google.firebase.database.DataSnapshot
import java.text.SimpleDateFormat


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private lateinit var productCount: TextView
    private lateinit var expiredCount: TextView
    private lateinit var categoryCount: TextView
    private lateinit var scanCount: TextView

    private lateinit var pieChart: PieChart

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val view = binding.root

        productCount = view.findViewById(R.id.productCount)
        expiredCount = view.findViewById(R.id.expiredCount)
        categoryCount = view.findViewById(R.id.categoryCount)
        scanCount = view.findViewById(R.id.scanCount)

        pieChart = view.findViewById(R.id.pieChart)
        setupPieChart()
        loadPieChartData()
        countItems()

        return view
    }

    // function for setting up the pie chart
    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
        }
    }


    private fun loadPieChartData() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        // Retrieve the reference to the "Products" node in Firebase Realtime Database
        val productsRef = FirebaseDatabase.getInstance().getReference("Products")

        // Query the products for the current user by their user ID
        val userProductsQuery = productsRef.orderByChild("userID").equalTo(userID)

        userProductsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var expiredProductCount = 0
                var nonExpiredProductCount = 0

                // Iterate through the products
                for (productSnapshot in dataSnapshot.children) {
                    val productExpiration = productSnapshot.child("productExpiration").value as String
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy")
                    val expirationDate = dateFormat.parse(productExpiration)

                    // Check if the product has expired
                    val currentDate = System.currentTimeMillis()
                    if (expirationDate.time < currentDate) {
                        expiredProductCount++
                    } else {
                        nonExpiredProductCount++
                    }
                }

                val pieEntries = mutableListOf<PieEntry>()
                // Add expired products to the pie chart
                if (expiredProductCount > 0) {
                    pieEntries.add(PieEntry(expiredProductCount.toFloat(), "Expired ($expiredProductCount)"))
                }

                // Add non-expired products to the pie chart if there are any
                if (nonExpiredProductCount > 0) {
                    pieEntries.add(PieEntry(nonExpiredProductCount.toFloat(), "Non-Expired ($nonExpiredProductCount)"))
                }
                // Convert hexadecimal color strings to Color objects
                val redColor = Color.parseColor("#FF0000")
                val greenColor = Color.parseColor("#00C8AC")

                // Create the data set and data object for the pie chart
                val dataSet = PieDataSet(pieEntries, "Product Expiration")
                dataSet.colors = listOf(redColor, greenColor)

                val pieData = PieData(dataSet)
                pieData.setValueTextSize(12f)
                pieData.setValueTextColor(Color.WHITE)

                // Set the data for the pie chart
                pieChart.data = pieData
                pieChart.invalidate()

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun countItems()
    {
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        // product reference for checking products in the Products node with the current user's id
        val productsRef = FirebaseDatabase.getInstance().getReference("Products")
            .orderByChild("userID")
            .equalTo(userID)

        // category reference for checking categories in the Categories node with the current user's id
        val categoriesRef = FirebaseDatabase.getInstance().getReference("Products")
            .orderByChild("userID")
            .equalTo(userID)

        val categoryCountMap = mutableMapOf<String, Int>()


        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                productCount.text = dataSnapshot.childrenCount.toString()
                scanCount.text = dataSnapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // handle error
            }
        })

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var expiredProductCount = 0 // counter for expired products
                for (productSnapshot in snapshot.children)
                {
                    // get the expiration date of the product
                    val productExpiration = productSnapshot.child("productExpiration").value as String
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy")
                    val expirationDate = dateFormat.parse(productExpiration)

                    // check if the product has expired
                    val currentDate = System.currentTimeMillis()
                    if (expirationDate.time < currentDate)
                    {
                        expiredProductCount++
                    }
                }
                // set the text of expiredCount to the count of expired products
                expiredCount.text = expiredProductCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (productSnapshot in snapshot.children)
                {
                    val category = productSnapshot.child("productCategory").value as String
                    if (categoryCountMap.containsKey(category))
                    {
                        categoryCountMap[category] = categoryCountMap[category]!! + 1
                    } else
                    {
                        categoryCountMap[category] = 1
                    }
                }
                val categoryCountData = categoryCountMap.size.toString()
                categoryCount.text = categoryCountData
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }



}