package com.android.example.github.api

import com.google.gson.annotations.SerializedName

data class PlpState (
        @SerializedName("categoryId")
        val categoryID: String,

        val currentSortOption: String,
        val currentFilters: String,
        val firstRecNum: Int,
        val lastRecNum: Int,
        val recsPerPage: Int,
        val totalNumRecs: Int,
        val originalSearchTerm: String
)