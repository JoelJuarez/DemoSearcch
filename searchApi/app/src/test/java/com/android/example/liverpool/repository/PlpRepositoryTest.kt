/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.liverpool.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.example.liverpool.api.*
import com.android.example.liverpool.db.LiverpoolDb
import com.android.example.liverpool.db.ProductDao
import com.android.example.liverpool.util.AbsentLiveData
import com.android.example.liverpool.util.InstantAppExecutors
import com.android.example.liverpool.util.TestUtil
import com.android.example.liverpool.util.mock
import com.android.example.liverpool.vo.PlpSearchResult
import com.android.example.liverpool.vo.Product
import com.android.example.liverpool.vo.Resource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import retrofit2.Response

@RunWith(JUnit4::class)
class PlpRepositoryTest {
    private lateinit var repository: PlpRepository
    private val dao = mock(ProductDao::class.java)
    private val service = mock(LiverpoolService::class.java)
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val db = mock(LiverpoolDb::class.java)
        `when`(db.productDao()).thenReturn(dao)
        `when`(db.runInTransaction(ArgumentMatchers.any())).thenCallRealMethod()
        repository = PlpRepository(InstantAppExecutors(), db, dao, service)
    }



    @Test
    fun searchNextPage_null() {
        `when`(dao.findSearchResult("foo")).thenReturn(null)
        val observer = mock<Observer<Resource<Boolean>>>()
        repository.searchNextPage("foo").observeForever(observer)
        verify(observer).onChanged(null)
    }

    @Test
    fun search_fromDb() {
        val ids = arrayListOf(1, 2)

        val observer = mock<Observer<Resource<List<Product>>>>()
        val dbSearchResult = MutableLiveData<PlpSearchResult>()
        val products = MutableLiveData<List<Product>>()

        `when`(dao.search("iphone")).thenReturn(dbSearchResult)
        `when`(dao.load()).thenReturn(products)

        repository.search("iphone").observeForever(observer)

        verify(observer).onChanged(Resource.loading(null))
        verifyNoMoreInteractions(service)
        reset(observer)

    }

    @Test
    fun search_fromServer() {
        val ids = arrayListOf(1, 2)
        val product1 = TestUtil.createProduct(1, "iphone")
        val product2 = TestUtil.createProduct(2, "iphone")

        val observer = mock<Observer<Resource<List<Product>>>>()
        val dbSearchResult = MutableLiveData<PlpSearchResult>()
        val repositories = MutableLiveData<List<Product>>()

        val productList = arrayListOf(product1, product2)
        val apiResponse = PlpSearchResponse(Status("OK", 20), "",
                PlpResults("",
                        PlpState("","","",0,0,0,0,""),
                        listOf<String>(),
                        listOf<String>(),
                        productList,
                        Navigation(listOf<String>(),listOf<Current>(),listOf<String>())
                )
        )

        val callLiveData = MutableLiveData<ApiResponse<PlpSearchResponse>>()
        `when`(service.searchPlp("true","iphone", 1, 10)).thenReturn(callLiveData)
        `when`(dao.load()).thenReturn(AbsentLiveData.create())
        `when`(dao.search("iphone")).thenReturn(dbSearchResult)

        repository.search("iphone").observeForever(observer)

        verify(observer).onChanged(Resource.loading(null))
        reset(observer)

        `when`(dao.load()).thenReturn(repositories)
        dbSearchResult.postValue(null)

        verify(service).searchPlp("true","iphone", 1, 10)
        val updatedResult = MutableLiveData<PlpSearchResult>()
        `when`(dao.search("iphone")).thenReturn(updatedResult)
        updatedResult.postValue(PlpSearchResult("iphone", 2, null))

        callLiveData.postValue(ApiResponse.create(Response.success(apiResponse)))
        verify(dao).insertProducts(productList)
        repositories.postValue(productList)
        verify(observer).onChanged(Resource.success(productList))
        verifyNoMoreInteractions(service)
    }

    @Test
    fun search_fromServer_error() {
        `when`(dao.search("iphoneempty")).thenReturn(AbsentLiveData.create())
        `when`(dao.load()).thenReturn(AbsentLiveData.create())
        val apiResponse = MutableLiveData<ApiResponse<PlpSearchResponse>>()
        `when`(service.searchPlp("true","iphoneempty", 1, 10)).thenReturn(apiResponse)

        val observer = mock<Observer<Resource<List<Product>>>>()
        repository.search("iphoneempty").observeForever(observer)
        verify(observer).onChanged(Resource.loading(null))

        apiResponse.postValue(ApiResponse.create(Exception("idk")))
        verify(observer).onChanged(Resource.error("idk", null))
    }
}