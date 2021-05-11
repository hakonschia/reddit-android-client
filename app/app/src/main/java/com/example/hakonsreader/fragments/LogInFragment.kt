package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.persistence.RedditUserInfoDao
import com.example.hakonsreader.databinding.FragmentLogInBinding
import com.example.hakonsreader.misc.startLoginIntent
import com.example.hakonsreader.views.util.showAccountManagement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment for logging in
 */
@AndroidEntryPoint
class LogInFragment : Fragment() {
    companion object {
        /**
         * @return A new instance of this fragment
         */
        fun newInstance() = LogInFragment()
    }

    private var _binding: FragmentLogInBinding? = null
    private val binding get() = _binding!!


    @Inject
    lateinit var userInfoDao: RedditUserInfoDao
    @Inject
    lateinit var api: RedditApi

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLogInBinding.inflate(LayoutInflater.from(requireActivity())).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        CoroutineScope(Dispatchers.IO).launch {
            binding.hasUsers = userInfoDao.getAllUsers().isNotEmpty()
        }

        binding.btnLogIn.setOnClickListener {
            startLoginIntent(requireContext())
        }

        binding.btnAccountManagement.setOnClickListener {
            showAccountManagement(requireActivity(), api, userInfoDao)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}