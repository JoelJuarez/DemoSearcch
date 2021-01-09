package com.android.example.github.api

data class PlpResults (
        val label: String,
        val plpState: PlpState,
        val sortOptions: List<Any?>,
        val refinementGroups: List<Any?>,
        val records: List<PlpRecord>,
        val navigation: PlpNavigation
)