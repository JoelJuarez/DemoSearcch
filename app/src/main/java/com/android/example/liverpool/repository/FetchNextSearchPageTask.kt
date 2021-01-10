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
import androidx.lifecycle.MutableLiveData
import com.android.example.liverpool.api.*
import com.android.example.liverpool.db.LiverpoolDb
import com.android.example.liverpool.vo.PlpSearchResult
import com.android.example.liverpool.vo.Resource
import java.io.IOException

/**
 * A task that reads the search result in the database and fetches the next page, if it has one.
 */
class FetchNextSearchPageTask constructor(
        private val force: String,
    private val search: String,
        private val itemsPerPage: Int,
    private val liverpoolService: LiverpoolService,
    private val db: LiverpoolDb
) : Runnable {
    private val _liveData = MutableLiveData<Resource<Boolean>>()
    val liveData: LiveData<Resource<Boolean>> = _liveData

    override fun run() {
        val current = db.productDao().findSearchResult(search)
        if (current == null) {
            _liveData.postValue(null)
            return
        }
        val nextPage = current.next
        if (nextPage == null) {
            _liveData.postValue(Resource.success(false))
            return
        }

        val newValue = try {
            val response = liverpoolService.searchPlpCall("true",search,nextPage + 1,10).execute()
            when (val apiResponse = ApiResponse.create(response)) {
                is ApiSuccessResponse -> {
                    // we merge all repo ids into 1 list so that it is easier to fetch the
                    // result list.

                    val merged = PlpSearchResult(
                            search,
                        10, nextPage + 1
                    )
                    db.runInTransaction {
                        db.productDao().insert(merged)
                        db.productDao().insertProducts(apiResponse.body.plpResults.records)
                    }
                    Resource.success(apiResponse.nextPage != null)
                }
                is ApiEmptyResponse -> {
                    Resource.success(null)
                }
                is ApiErrorResponse -> {
                    Resource.error(apiResponse.errorMessage, true)
                }
            }

        } catch (e: IOException) {
            Resource.error(e.message!!, true)
        }
        _liveData.postValue(newValue)
    }
}
