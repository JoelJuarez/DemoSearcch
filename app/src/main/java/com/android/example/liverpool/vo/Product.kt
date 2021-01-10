package com.android.example.liverpool.vo

import androidx.room.Entity
import com.google.gson.annotations.SerializedName


@Entity(primaryKeys = ["productID"])
data class Product (
        @SerializedName("productId")
        val productID: String,
        @SerializedName("skuRepositoryId")
        val skuRepositoryID: String,
        val productDisplayName: String,
        val productType: String,
        val productRatingCount: Int,
        val productAvgRating: Double,
        val listPrice: Double,
        val minimumListPrice: Double,
        val maximumListPrice: Double,
        val promoPrice: Double,
        val minimumPromoPrice: Double,
        val maximumPromoPrice: Double,
        val isHybrid: Boolean,
        val marketplaceSLMessage: String? = null,
        val marketplaceBTMessage: String? = null,
        val isMarketPlace: Boolean,
        val isImportationProduct: Boolean,
        val brand: String?,
        val seller: String?,
        val category: String?,
        val smImage: String?,
        val lgImage: String?,
        val xlImage: String?,
        val groupType: String?,
)
