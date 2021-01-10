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

package com.android.example.liverpool.ui.search

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isGone
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.example.liverpool.AppExecutors
import com.android.example.liverpool.R
import com.android.example.liverpool.binding.FragmentDataBindingComponent
import com.android.example.liverpool.databinding.SearchFragmentBinding
import com.android.example.liverpool.di.Injectable
import com.android.example.liverpool.ui.common.ProductListAdapter
import com.android.example.liverpool.ui.common.RetryCallback
import com.android.example.liverpool.ui.common.SuggestListAdapter
import com.android.example.liverpool.ui.common.TaskListener
import com.android.example.liverpool.util.autoCleared
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class SearchFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appExecutors: AppExecutors

    var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)

    var binding by autoCleared<SearchFragmentBinding>()

    var adapter by autoCleared<ProductListAdapter>()
    var suggestAdapter by autoCleared<SuggestListAdapter>()

    val searchViewModel: SearchViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.search_fragment,
                container,
                false,
                dataBindingComponent
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        initRecyclerView()
        initRecyclerViewSuggest()
        val rvAdapter = ProductListAdapter(
                dataBindingComponent = dataBindingComponent,
                appExecutors = appExecutors,
                showFullName = true
        ) { repo ->
            /*findNavController().navigate(
                    SearchFragmentDirections.showRepo(repo.owner.login, repo.name)
            */
        }
        binding.query = searchViewModel.query
        binding.repoList.adapter = rvAdapter
        adapter = rvAdapter

        val rvSuggestAdapter = SuggestListAdapter(
                dataBindingComponent = dataBindingComponent,
                appExecutors = appExecutors,
                showFullName = true,
                object : TaskListener {
                    override fun onTaskClick(task: String) {
                        Timber.d("Search slected ${task}");
                        binding.input.setText(task)
                        searchViewModel.setQuery(task)
                    }
                },
                object : TaskListener {
                    override fun onTaskClick(task: String) {
                        Timber.d("Delete slected ${task}");
                        GlobalScope.launch(Dispatchers.IO) {
                         searchViewModel.deleteItems.deleteItem(task)
                        }
                    }
                }
        )

        binding.sugestionList.adapter = rvSuggestAdapter
        suggestAdapter = rvSuggestAdapter

        initSearchInputListener()

        binding.callback = object : RetryCallback {
            override fun retry() {
                searchViewModel.refresh()
            }
        }
        binding.delleteAll.setOnClickListener{

            GlobalScope.launch(Dispatchers.IO) {
                searchViewModel.deleteItems.deleteAllItems();
            }
        }

    }

    private fun initSearchInputListener() {
        binding.input.setOnEditorActionListener { view: View, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(view)
                true
            } else {
                false
            }
        }
        binding.input.setOnKeyListener { view: View, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                doSearch(view)
                true
            } else {
                false
            }
        }
    }

    private fun doSearch(v: View) {
        val query = binding.input.text.toString()
        // Dismiss keyboard
        dismissKeyboard(v.windowToken)
        if (query.isEmpty()) {
            binding.sugestionList.isGone = false
            binding.repoList.isGone  = true
            binding.delleteAll.isGone = false
        } else {
            binding.delleteAll.isGone = true
            searchViewModel.setQuery(query)
        }

    }

    private fun initRecyclerView() {

        binding.repoList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                if (lastPosition == adapter.itemCount - 1) {
                    searchViewModel.loadNextPage()
                }
            }
        })
        binding.searchResult = searchViewModel.results
        searchViewModel.results.observe(viewLifecycleOwner, Observer { result ->
            binding.sugestionList.isGone = true
            binding.repoList.isGone = false
            adapter.submitList(result?.data)
        })

        searchViewModel.loadMoreStatus.observe(viewLifecycleOwner, Observer { loadingMore ->
            if (loadingMore == null) {
                binding.loadingMore = false
            } else {
                binding.loadingMore = loadingMore.isRunning
                val error = loadingMore.errorMessageIfNotHandled
                if (error != null) {
                    Snackbar.make(binding.loadMoreBar, error, Snackbar.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun initRecyclerViewSuggest() {


        // binding.suggestResult = searchViewModel.listSuggest
        searchViewModel.listSuggest.observe(viewLifecycleOwner, Observer { result ->
            if (result.isEmpty()){
                binding.delleteAll.isGone = true
            }else {
                if (binding.input.text.isEmpty()) {
                    binding.delleteAll.isGone = false
                }
            }

            suggestAdapter.submitList(result)

        })


    }


    private fun dismissKeyboard(windowToken: IBinder) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }
}
