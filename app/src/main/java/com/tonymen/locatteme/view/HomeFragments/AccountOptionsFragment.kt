package com.tonymen.locatteme.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tonymen.locatteme.databinding.FragmentAccountOptionsBinding

class AccountOptionsFragment : Fragment() {

    private var _binding: FragmentAccountOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountOptionsBinding.inflate(inflater, container, false)

        binding.editProfileTextView.setOnClickListener {
            (activity as HomeActivity).loadFragment(EditProfileFragment())
        }

        binding.changePasswordTextView.setOnClickListener {
            (activity as HomeActivity).loadFragment(ChangePasswordFragment())
        }

        binding.deleteuser.setOnClickListener {
            (activity as HomeActivity).loadFragment(DeleteAccountFragment())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
