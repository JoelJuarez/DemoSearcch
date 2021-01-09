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

package com.android.example.github.repository

import androidx.lifecycle.LiveData
import com.android.example.github.AppExecutors
import com.android.example.github.api.ApiSuccessResponse
import com.android.example.github.api.LiverpoolService
import com.android.example.github.api.PlpSearchResponse
import com.android.example.github.db.GithubDb
import com.android.example.github.db.RepoDao
import com.android.example.github.testing.OpenForTesting
import com.android.example.github.util.AbsentLiveData
import com.android.example.github.util.RateLimiter
import com.android.example.github.vo.PlpSearchResult
import com.android.example.github.vo.Resource
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
    private val db: GithubDb,
    private val repoDao: RepoDao,
    private val liverpoolService: LiverpoolService
) {

    private val repoListRateLimit = RateLimiter<String>(10, TimeUnit.MINUTES)


    fun getAllSugges() : LiveData<PlpSearchResult?> {
      val allSuggest =  db.repoDao().getAllSearchResult()
        return allSuggest
    }


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

    fun search(search: String): LiveData<Resource<PlpSearchResponse>> {
        return object : NetworkBoundResource<PlpSearchResponse, PlpSearchResponse>(appExecutors) {

            override fun saveCallResult(item: PlpSearchResponse) {
                val repoIds = item.plpResults.records.map { it.productID }
                val plpSearchResult = PlpSearchResult(
                    query = search,
                    totalCount = item.plpResults.plpState.totalNumRecs,
                    next = item.plpResults.plpState.firstRecNum
                )
                db.runInTransaction {
                    //repoDao.insertRepos(item.plpResults.records)
                    repoDao.insert(plpSearchResult)
                }
            }

            override fun shouldFetch(data: PlpSearchResponse?) = data == null

            override fun loadFromDb(): LiveData<PlpSearchResponse> {

                return AbsentLiveData.create()
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
