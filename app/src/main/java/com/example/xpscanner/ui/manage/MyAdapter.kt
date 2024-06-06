package com.example.xpscanner.ui.manage

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpscanner.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

class MyAdapter(private val context: Context, private val datalist:List<DataClass>): RecyclerView.Adapter<MyViewHolder>() {

    private var filteredData = datalist


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.product, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filteredData.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentItem = filteredData[position]

        holder.productID.text = filteredData[position].productID
        holder.productBarcode.text = filteredData[position].productBarcode
        holder.productName.text = filteredData[position].productName
        holder.productExpiration.text = filteredData[position].productExpiration
        holder.productCategory.text = filteredData[position].productCategory
        holder.productDescription.text = filteredData[position].productDescription

        // when a specific product is clicked in the RecyclerView,
        // it will pass product details and open the product view intent
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ManageProductViewActivity::class.java)
            intent.putExtra("productID", currentItem.productID)
            intent.putExtra("productBarcode", currentItem.productBarcode)
            intent.putExtra("productName", currentItem.productName)
            intent.putExtra("productExpiration", currentItem.productExpiration)
            intent.putExtra("productCategory", currentItem.productCategory)
            intent.putExtra("productDescription", currentItem.productDescription)
            intent.putExtra("productImage", currentItem.productImage)
            context.startActivity(intent)
        }

        // show product image
        Picasso.get()
            .load(currentItem.productImage)
            .transform(RoundedTransformation(16))
            .into(holder.productImage)


    }

    // function for searching a product
    fun filter(query: String) {
        filteredData = if (query.isEmpty()) {
            datalist
        } else {
            datalist.filter {
                it.productName?.toLowerCase()!!.contains(query.toLowerCase()) ||
                        it.productCategory?.toLowerCase()!!.contains(query.toLowerCase()) ||
                        it.productBarcode?.toLowerCase()!!.contains(query.toLowerCase()) ||
                        it.productExpiration?.toLowerCase()!!.contains(query.toLowerCase()) ||
                        it.productDescription?.toLowerCase()?.contains(query.toLowerCase()) == true
            }
        }
        notifyDataSetChanged()
    }
}

class RoundedTransformation(private val radius: Int) : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint()
        paint.isAntiAlias = true
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawRoundRect(
            RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
            radius.toFloat(),
            radius.toFloat(),
            paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        if (source != output) {
            source.recycle()
        }
        return output
    }

    override fun key(): String {
        return "rounded(radius=$radius)"
    }
}

class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var productID: TextView
    var productExpiration: TextView
    var productName: TextView
    var productCategory: TextView
    var productBarcode: TextView
    var productImage: ImageView
    var productDescription: TextView

    init {
        productID = itemView.findViewById(R.id.productIDTV)
        productExpiration = itemView.findViewById(R.id.productExpirationTV)
        productName = itemView.findViewById(R.id.productNameTV)
        productCategory = itemView.findViewById(R.id.productCategoryTV)
        productBarcode = itemView.findViewById(R.id.productBarcodeTV)
        productImage = itemView.findViewById(R.id.productImageIV)
        productDescription = itemView.findViewById(R.id.productDescriptionTV)
    }
}