package com.example.xpscanner.ui.manage
import android.app.Activity.RESULT_OK
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.xpscanner.databinding.FragmentManageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.appcompat.widget.SearchView
import com.example.labadabango.ProductSearchActivity
import java.text.SimpleDateFormat


class ManageFragment : Fragment() {

    private var _binding: FragmentManageBinding? = null
    private lateinit var dataList: ArrayList<DataClass>
    private lateinit var adapter: MyAdapter
    var databaseReference: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null

    private lateinit var noResultsTextView: TextView
    private lateinit var scanBtn: ImageView
    private lateinit var searchSV: SearchView

    private val CHANNEL_ID = "channel_id_example_01"
    val notificationID = 1

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            val result = data?.getStringExtra(ProductSearchActivity.RESULT)
            searchSV.setQuery(result, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Remove the value event listener
        eventListener?.let {
            val productsRef = FirebaseDatabase.getInstance().getReference("Products")
            productsRef.removeEventListener(it)
        }

        // Clear the notification
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationID)

        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBinding.inflate(inflater, container, false)
        val view = binding.root

        val gridLayoutManager = GridLayoutManager(requireContext(), 1)
        binding.recyclerView.layoutManager = gridLayoutManager

        dataList = ArrayList()
        adapter = MyAdapter(requireContext(), dataList)
        binding.recyclerView.adapter = adapter

        scanBtn = binding.scanBtn
        searchSV = binding.searchSV
        noResultsTextView = binding.noResultsTextView

        binding.scanBtn.setOnClickListener {
            val intent = Intent(activity, ProductSearchActivity::class.java)
            startActivityForResult(intent, 1)
        }

        // get the current user's id
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        // product reference for checking products in the Products node with the current user's id
        val productsRef = FirebaseDatabase.getInstance().getReference("Products").orderByChild("userID").equalTo(userID)

        // loop and show the datas
        eventListener = productsRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
                for(itemSnapshot in snapshot.children)
                {
                    val dataClass = itemSnapshot.getValue(DataClass::class.java)

                    if(dataClass != null)
                    {
                        // add the product's unique id to a product Text View
                        dataClass.productID = itemSnapshot.key
                        // check if the product has expired
                        val dateFormat = SimpleDateFormat("MM/dd/yyyy")
                        val expirationDate = dateFormat.parse(dataClass.productExpiration).time
                        val currentDate = System.currentTimeMillis()

                        if (expirationDate > currentDate)
                        {
                            dataList.add(dataClass)
                        }
                    }
                }
                adapter.filter("")
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        binding.searchSV.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                checkForSearchResults(newText ?: "")
                return true
            }
        })


        return view
    }

    private fun checkForSearchResults(searchQuery: String) {
        if (searchQuery.isEmpty())
        {
            noResultsTextView?.visibility = View.GONE
        }
        else if (adapter.itemCount == 0)
        {
            noResultsTextView?.visibility = View.VISIBLE
        } else
        {
            noResultsTextView?.visibility = View.GONE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val searchQuery = binding.searchSV.query.toString()
        checkForSearchResults(searchQuery)
    }
}