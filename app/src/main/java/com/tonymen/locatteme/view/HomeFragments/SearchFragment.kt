package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tonymen.locatteme.databinding.FragmentSearchBinding
import com.tonymen.locatteme.view.adapters.SearchAdapter
import com.tonymen.locatteme.viewmodel.SearchViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        adapter = SearchAdapter()

        binding.recyclerViewSearchResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewSearchResults.adapter = adapter

        setupSearchBar()
        observeViewModel()
        setupHideKeyboardOnOutsideClick(binding.root)

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.searchBar.hasFocus()) {
                    binding.searchBar.clearFocus()
                    hideKeyboard()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })

        return binding.root
    }
    //Buscador Funcional solo con nombre o apellido
    private fun setupSearchBar() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().toLowerCase()
                if (query.isEmpty()) {
                    adapter.updateUsers(emptyList())
                    adapter.updatePosts(emptyList())
                } else {
                    if (query.startsWith("@")) {
                        viewModel.searchUsers(query.substring(1))
                    } else {
                        viewModel.searchPosts(query)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }



    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner, { users ->
            if (binding.searchBar.text.toString().startsWith("@")) {
                adapter.updateUsers(users)
                adapter.updatePosts(emptyList())
            }
        })

        viewModel.posts.observe(viewLifecycleOwner, { posts ->
            if (!binding.searchBar.text.toString().startsWith("@")) {
                adapter.updatePosts(posts)
                adapter.updateUsers(emptyList())
            }
        })
    }

    private fun setupHideKeyboardOnOutsideClick(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                view.clearFocus()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupHideKeyboardOnOutsideClick(innerView)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
