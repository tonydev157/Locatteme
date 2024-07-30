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
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentSearchBinding
import com.tonymen.locatteme.view.adapters.SearchAdapter
import com.tonymen.locatteme.viewmodel.SearchViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: SearchAdapter
    private lateinit var auth: FirebaseAuth
    private var isFilterApplied = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        auth = FirebaseAuth.getInstance()

        val currentUserId = auth.currentUser?.uid ?: ""

        adapter = SearchAdapter(currentUserId = currentUserId)

        binding.recyclerViewSearchResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewSearchResults.adapter = adapter

        setupSearchBar()
        observeViewModel()
        setupHideKeyboardOnOutsideClick(binding.root)

        binding.filterIcon.setOnClickListener {
            if (isFilterApplied) {
                resetFilters()
            } else {
                val filterDialog = FilterDialogFragment.newInstance { startDisappearanceDate, endDisappearanceDate, startPublicationDate, endPublicationDate, status, province, city ->
                    applyFilter(startDisappearanceDate, endDisappearanceDate, startPublicationDate, endPublicationDate, status, province, city)
                }
                filterDialog.show(parentFragmentManager, "FilterDialog")
            }
        }

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

    private fun setupSearchBar() {
        binding.searchBar.setText(viewModel.currentQuery) // Restaura el estado de la búsqueda
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                viewModel.currentQuery = query // Almacena el estado de la búsqueda
                updateSearchClearIconVisibility(query)

                if (query.isNotEmpty()) {
                    if (isFilterApplied) {
                        viewModel.searchFilteredPosts(query)
                    } else {
                        if (query.startsWith("@")) {
                            if (query.length > 1) {
                                viewModel.searchUsers(query.substring(1))
                            } else {
                                // No hacer nada si solo es "@"
                                adapter.updateUsers(emptyList())
                            }
                        } else {
                            viewModel.searchPosts(query)
                        }
                    }
                } else {
                    if (isFilterApplied) {
                        viewModel.filteredPosts.value?.let { adapter.updatePosts(it) }
                    } else {
                        adapter.updatePosts(emptyList())
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchClearIcon.setOnClickListener {
            binding.searchBar.text.clear()
            if (isFilterApplied) {
                viewModel.filteredPosts.value?.let { adapter.updatePosts(it) }
            } else {
                adapter.updatePosts(emptyList())
                viewModel.clearUsers()
            }
        }
    }

    private fun updateSearchClearIconVisibility(query: String) {
        binding.searchClearIcon.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
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

        viewModel.filteredPosts.observe(viewLifecycleOwner, { filteredPosts ->
            if (isFilterApplied && binding.searchBar.text.toString().isEmpty()) {
                adapter.updatePosts(filteredPosts)
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

    private fun applyFilter(
        startDisappearanceDate: String?, endDisappearanceDate: String?,
        startPublicationDate: String?, endPublicationDate: String?,
        status: String?, province: String?, city: String?
    ) {
        val query = binding.searchBar.text.toString().lowercase()
        viewModel.filterPosts(startDisappearanceDate, endDisappearanceDate, startPublicationDate, endPublicationDate, status, province, city, query)
        isFilterApplied = true
        viewModel.isFilterApplied = true // Almacena el estado del filtro
        binding.filterIcon.setImageResource(R.drawable.ic_clear_filter) // Cambia el icono a la X
    }

    private fun resetFilters() {
        viewModel.clearFilters()
        isFilterApplied = false
        viewModel.isFilterApplied = false // Almacena el estado del filtro
        binding.filterIcon.setImageResource(R.drawable.ic_filter) // Cambia el icono de vuelta al filtro
        binding.searchBar.text.clear() // Clear the search bar text
        adapter.updatePosts(emptyList()) // Clear the displayed posts
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isFilterApplied) {
            binding.filterIcon.setImageResource(R.drawable.ic_clear_filter) // Asegura que el icono de la X se mantenga al volver a la pantalla
        } else {
            binding.filterIcon.setImageResource(R.drawable.ic_filter)
        }

        // Restaura los resultados de búsqueda
        if (viewModel.currentQuery.isNotEmpty()) {
            if (viewModel.isFilterApplied) {
                viewModel.searchFilteredPosts(viewModel.currentQuery)
            } else {
                if (viewModel.currentQuery.startsWith("@")) {
                    if (viewModel.currentQuery.length > 1) {
                        viewModel.searchUsers(viewModel.currentQuery.substring(1))
                    } else {
                        adapter.updateUsers(emptyList())
                    }
                } else {
                    viewModel.searchPosts(viewModel.currentQuery)
                }
            }
        }
    }
}
