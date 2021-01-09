package com.android.example.github.api


data class PlpNavigation (
        val ancester: List<Any?>,
        val current: List<PlpCurrent>,
        val childs: List<Any?>
)