package com.android.example.github.api

import com.google.gson.annotations.SerializedName

data class PlpCurrent (
        val label: String,
        @SerializedName("categoryId")
        val categoryID: String
)