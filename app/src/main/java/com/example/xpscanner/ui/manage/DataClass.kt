package com.example.xpscanner.ui.manage

class DataClass {
    var productID: String? = null
    var productExpiration: String? = null
    var productName: String? = null
    var productCategory: String? = null
    var productBarcode: String? = null
    var productImage: String? =  null
    var productDescription: String? = null

            constructor(productID: String?,
                        productExpiration: String?,
                        productName: String?, productCategory: String?,
                        productBarcode: String?,productImage: String?,productDescription: String?) {
                this.productID = productID
                this.productExpiration = productExpiration
                this.productName = productName
                this.productCategory = productCategory
                this.productBarcode = productBarcode
                this.productImage = productImage
                this.productDescription = productDescription
            }
    constructor(){

    }
}