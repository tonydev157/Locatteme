package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.databinding.FragmentPostCommentsBinding
import com.tonymen.locatteme.view.adapters.CommentAdapter
import com.tonymen.locatteme.viewmodel.PostCommentsViewModel

class PostCommentsFragment : Fragment() {

    private var _binding: FragmentPostCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PostCommentsViewModel
    private lateinit var adapter: CommentAdapter
    private lateinit var auth: FirebaseAuth

    private var postId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(PostCommentsViewModel::class.java)
        auth = FirebaseAuth.getInstance()

        postId = arguments?.getString("postId")

        adapter = CommentAdapter()
        binding.recyclerViewComments.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewComments.adapter = adapter

        binding.buttonSubmitComment.setOnClickListener {
            val commentText = binding.editTextComment.text.toString()
            if (commentText.isNotEmpty()) {
                postId?.let { id ->
                    viewModel.addComment(id, commentText, auth.currentUser?.uid.orEmpty())
                    binding.editTextComment.text.clear()
                }
            } else {
                Toast.makeText(context, "El comentario no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.comments.observe(viewLifecycleOwner, { comments ->
            adapter.submitList(comments.reversed()) // Revertir aquí para mostrar los más recientes al final
            binding.recyclerViewComments.scrollToPosition(comments.size - 1) // Scroll al último comentario
        })

        postId?.let {
            viewModel.loadComments(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
