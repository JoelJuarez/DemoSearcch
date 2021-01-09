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

import androidx.lifecycle.LiveData
import com.android.example.liverpool.AppExecutors
import com.android.example.liverpool.api.ApiSuccessResponse
import com.android.example.liverpool.api.LiverpoolService
import com.android.example.liverpool.api.PlpSearchResponse
import com.android.example.liverpool.db.LiverpoolDb
import com.android.example.liverpool.db.ProductDao
import com.android.example.liverpool.testing.OpenForTesting
import com.android.example.liverpool.util.RateLimiter
import com.android.example.liverpool.vo.PlpSearchResult
import com.android.example.liverpool.vo.Product
import com.android.example.liverpool.vo.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that handles Repo instances.
 *
 * unfortunate naming :/ .
 * Repo - value object name
 * Repository - type of this class.
 */
@Singleton
@OpenForTesting
class PlpRepository @Inject constructor(
        private val appExecutors: AppExecutors,
        private val db: LiverpoolDb,
        private val productDao: ProductDao,
        private val liverpoolService: LiverpoolService
) {

    private val repoListRateLimit = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun searchNextPage(search: String): LiveData<Resource<Boolean>> {
        val fetchNextSearchPageTask = FetchNextSearchPageTask(
                force = "true",
                search = search,
                itemsPerPage = 10,
                liverpoolService = liverpoolService,
                db = db
        )
        appExecutors.networkIO().execute(fetchNextSearchPageTask)
        return fetchNextSearchPageTask.liveData
    }

    fun search(search: String): LiveData<Resource<List<Product>>> {

        return object : NetworkBoundResource<List<Product>, PlpSearchResponse>(appExecutors) {

            override fun saveCallResult(item: PlpSearchResponse) {
                val repoIds = item.plpResults.records.map { it.productID }
                val plpSearchResult = PlpSearchResult(
                    query = search,
                    totalCount = item.plpResults.plpState.totalNumRecs,
                    next = item.plpResults.plpState.firstRecNum
                )
                db.runInTransaction {
                    productDao.insertProducts(item.plpResults.records)
                    productDao.insert(plpSearchResult)
                }
            }

            override fun shouldFetch(data: List<Product>?) = true

            override fun loadFromDb(): LiveData<List<Product>> {
                return productDao.load()
            }

            override fun cleanFromDb() {
                runBlocking(Dispatchers.Default) {
                    productDao.deleteAll()
                }
            }


            override fun createCall() = liverpoolService.searchPlp("true",search,1,10 )

            override fun processResponse(response: ApiSuccessResponse<PlpSearchResponse>)
                    : PlpSearchResponse {
                val body = response.body
                body.nextPage = response.nextPage
                return body
            }
        }.asLiveData()
    }
}
