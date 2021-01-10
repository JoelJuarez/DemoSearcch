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

package com.android.example.liverpool.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.example.liverpool.testing.OpenForTesting
import com.android.example.liverpool.vo.PlpSearchResult
import com.android.example.liverpool.vo.Product

/**
 * Interface for database access on Repo related operations.
 */
@Dao
@OpenForTesting
abstract class ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg repos: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertProducts(repositories: List<Product>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun createProductIfNotExists(repo: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(result: PlpSearchResult)

    @Query("SELECT * FROM PlpSearchResult WHERE `query` = :query")
    abstract fun search(query: String): LiveData<PlpSearchResult?>

    @Query("SELECT * FROM Product WHERE productID in (:productIDs)")
    abstract fun loadById(productIDs: List<Int>): LiveData<List<Product>>

    @Query("SELECT * FROM Product")
    abstract fun load(): LiveData<List<Product>>

    @Query("SELECT * FROM PlpSearchResult WHERE `query` = :query")
    abstract fun findSearchResult(query: String): PlpSearchResult?

    @Query("DELETE FROM Product")
    abstract fun deleteAll()

    @Query("SELECT * FROM PlpSearchResult")
    abstract fun getAllResults(): LiveData<List<PlpSearchResult>>

    @Query("DELETE FROM PlpSearchResult WHERE `query` = :query ")
    abstract fun deleteItemResult(query: String)

    @Query("DELETE FROM PlpSearchResult ")
    abstract fun deleteAllItems()

}
