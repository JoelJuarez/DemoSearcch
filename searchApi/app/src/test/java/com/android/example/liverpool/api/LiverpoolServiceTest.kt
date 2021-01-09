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

package com.android.example.liverpool.api

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.example.liverpool.util.LiveDataCallAdapterFactory
import com.android.example.liverpool.util.getOrAwaitValue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Okio
import okio.buffer
import okio.source
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.core.IsNull.notNullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(JUnit4::class)
class LiverpoolServiceTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var service: LiverpoolService

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun createService() {
        mockWebServer = MockWebServer()
        service = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
            .create(LiverpoolService::class.java)
    }

    @After
    fun stopService() {
        mockWebServer.shutdown()
    }


    @Test
    fun getProducts() {
        enqueueResponse("search.json")
        val apiResponse = (service.searchPlp("true", "iphone",1, 10).getOrAwaitValue() as ApiSuccessResponse).body

        val request = mockWebServer.takeRequest()
        assertThat(apiResponse.plpResults.records.size, `is`(10))

        val repo = apiResponse.plpResults.records[0]
        assertThat(repo.productDisplayName, `is`("Consola Xbox Series S 512 GB blanco"))
    }


    @Test
    fun search() {
        val next = """<https://shoppapp.liverpool.com.mx/appclienteservices/services/v3/plp?force-plp=true&search-string=xbox&page-number=2&number-of-items-per-page=10>; rel="next""""
        val last = """<https://shoppapp.liverpool.com.mx/appclienteservices/services/v3/plp?force-plp=true&search-string=xbox&page-number=200&number-of-items-per-page=10>; rel="last""""
        enqueueResponse(
            "search.json", mapOf(
                "link" to "$next,$last"
            )
        )
        val response = service.searchPlp("true", "iphone",1, 10).getOrAwaitValue() as ApiSuccessResponse

        assertThat(response, notNullValue())
        assertThat(response.body.plpResults.plpState.totalNumRecs, `is`(1178))
        assertThat(response.body.plpResults.records.size, `is`(10))
        assertThat<String>(
            response.links["next"],
            `is`("https://shoppapp.liverpool.com.mx/appclienteservices/services/v3/plp?force-plp=true&search-string=xbox&page-number=2&number-of-items-per-page=10")
        )
        assertThat<Int>(response.nextPage, `is`(10))
    }

    private fun enqueueResponse(fileName: String, headers: Map<String, String> = emptyMap()) {
        val inputStream = javaClass.classLoader!!
            .getResourceAsStream("api-response/$fileName")
        val source = inputStream.source().buffer()
        val mockResponse = MockResponse()
        for ((key, value) in headers) {
            mockResponse.addHeader(key, value)
        }
        mockWebServer.enqueue(
            mockResponse
                .setBody(source.readString(Charsets.UTF_8))
        )
    }
}
