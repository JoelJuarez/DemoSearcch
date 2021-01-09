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
import androidx.lifecycle.Observer
import com.android.example.liverpool.api.*
import com.android.example.liverpool.db.LiverpoolDb
import com.android.example.liverpool.db.ProductDao
import com.android.example.liverpool.util.TestUtil
import com.android.example.liverpool.util.mock
import com.android.example.liverpool.vo.PlpSearchResult
import com.android.example.liverpool.vo.Product
import com.android.example.liverpool.vo.Resource
import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

@RunWith(JUnit4::class)
class FetchNextSearchPageTaskTest {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var service: LiverpoolService

    private lateinit var db: LiverpoolDb

    private lateinit var productDao: ProductDao

    private lateinit var task: FetchNextSearchPageTask

    private val observer: Observer<Resource<Boolean>> = mock()

    @Before
    fun init() {
        service = mock(LiverpoolService::class.java)
        db = mock(LiverpoolDb::class.java)
        `when`(db.runInTransaction(any())).thenCallRealMethod()
        productDao = mock(ProductDao::class.java)
        `when`(db.productDao()).thenReturn(productDao)
        task = FetchNextSearchPageTask("true","iphone",10, service, db)
        task.liveData.observeForever(observer)
    }

    @Test
    fun withoutResult() {
        `when`(productDao.search("iphone")).thenReturn(null)
        task.run()
        verify(observer).onChanged(null)
        verifyNoMoreInteractions(observer)
        verifyNoMoreInteractions(service)
    }

    @Test
    fun noNextPage() {
        `when`(productDao.load()).thenReturn(null)
        createDbResult(null)
        task.run()
        verify(observer).onChanged(Resource.success(false))
        verifyNoMoreInteractions(observer)
        verifyNoMoreInteractions(service)
    }



    @Test
    fun nextPageWithMore() {
        createDbResult(1)
        val products = TestUtil.createProducts(10, "a",)
        val result = PlpSearchResponse(Status("OK", 20), "",
                PlpResults("",
                        PlpState("","","",0,0,0,0,""),
                        listOf<String>(),
                        listOf<String>(),
                        products,
                        Navigation(listOf<String>(),listOf<Current>(),listOf<String>())
                )
        )
        result.nextPage = 2
        val call = createCall(result, 2)
        `when`(service.searchPlpCall("true", "iphone",2, 10)).thenReturn(call)
        task.run()
        verify(productDao).insertProducts(products)
        verify(observer).onChanged(Resource.success(true))
    }

    @Test
    fun nextPageApiError() {
        createDbResult(1)
        val call = mock<Call<PlpSearchResponse>>()
        `when`(call.execute()).thenReturn(
            Response.error(
                400, ResponseBody.create(
                    "txt".toMediaTypeOrNull(), "bar"
                )
            )
        )
        `when`(service.searchPlpCall("true", "iphone",2, 10)).thenReturn(call)
        task.run()
        verify(observer)!!.onChanged(Resource.error("bar", true))
    }

    @Test
    fun nextPageIOError() {
        createDbResult(1)
        val call = mock<Call<PlpSearchResponse>>()
        `when`(call.execute()).thenThrow(IOException("bar"))
        `when`(service.searchPlpCall("true", "iphone",2, 10)).thenReturn(call)
        task.run()
        verify(observer)!!.onChanged(Resource.error("bar", true))
    }

    private fun createDbResult(nextPage: Int?) {
        val result = PlpSearchResult(
            "iphone",
            0, nextPage
        )
        `when`(productDao.findSearchResult("iphone")).thenReturn(result)
    }

    private fun createCall(body: PlpSearchResponse, nextPage: Int?): Call<PlpSearchResponse> {
        val headers = if (nextPage == null)
            null
        else
            headersOf("link",
                    "<https://api.github.com/search/repositories?q=iphone&page=" + nextPage
                + ">; rel=\"next\""
            )
        val success = if (headers == null)
            Response.success(body)
        else
            Response.success(body, headers)
        val call = mock<Call<PlpSearchResponse>>()
        `when`(call.execute()).thenReturn(success)

        return call
    }
}