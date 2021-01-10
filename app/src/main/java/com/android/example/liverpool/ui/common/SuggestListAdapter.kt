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

package com.android.example.liverpool.ui.common

import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import android.view.LayoutInflater
import android.view.ViewGroup
import com.android.example.liverpool.AppExecutors
import com.android.example.liverpool.R
import com.android.example.liverpool.databinding.SugestItemBinding
import com.android.example.liverpool.vo.PlpSearchResult
import timber.log.Timber


/**
 * A RecyclerView adapter for [PlpSearchResult] class.
 */
class SuggestListAdapter (
    private val dataBindingComponent: DataBindingComponent,
    appExecutors: AppExecutors,
    private val showFullName: Boolean,
    //private val repoClickCallback: ((PlpSearchResult) -> Unit)?
    private val taskListener: TaskListener?,
    private val deleteListener: TaskListener?
) : DataBoundListAdapter<PlpSearchResult, SugestItemBinding>(
    appExecutors = appExecutors,
    diffCallback = object : DiffUtil.ItemCallback<PlpSearchResult>() {
        override fun areItemsTheSame(oldItem: PlpSearchResult, newItem: PlpSearchResult): Boolean {
            return oldItem.query == newItem.query
        }

        override fun areContentsTheSame(oldItem: PlpSearchResult, newItem: PlpSearchResult): Boolean {
            return oldItem.query == newItem.query
        }
    }
) {

    override fun createBinding(parent: ViewGroup): SugestItemBinding {
        val binding = DataBindingUtil.inflate<SugestItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.sugest_item,
            parent,
            false,
            dataBindingComponent
        )
        //binding.showFullName = showFullNames
        binding.root.setOnClickListener {

            binding.plpSuggest?.let {
                taskListener?.onTaskClick(it.query)
                //repoClickCallback?.invoke(it)
            }
        }
        binding.deleteButton.setOnClickListener{
            binding.plpSuggest?.let {
                Timber.d("delete ${it.query}")
                deleteListener?.onTaskClick(it.query)
            }

        }


        return binding
    }

    override fun bind(binding: SugestItemBinding, item: PlpSearchResult) {
        binding.plpSuggest = item
    }
}

interface TaskListener {
    fun onTaskClick(task: String)
}