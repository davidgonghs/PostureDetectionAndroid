package com.posturedetection.android.ui.profile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.posturedetection.android.AccountSettingsActivity
import com.posturedetection.android.PersonalInformationActivity
import com.posturedetection.android.data.LoginUser
import com.posturedetection.android.databinding.FragmentProfileBinding
import com.posturedetection.android.util.PhotoUtils
import kotlin.math.log

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var auth : FirebaseAuth

    private var loginUser: LoginUser = LoginUser.getInstance()
    override fun onResume() {
        super.onResume()
        binding.userImg.setImageURI(loginUser.imgUrl)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        val root: View = binding.root
        loginUser.reinit()
        binding.userImg.setImageURI(loginUser.imgUrl)
        binding.profileUsername.text = loginUser.username
        binding.profileEmail.text = loginUser.email

        binding.btnPersonalInformation.setOnClickListener(View.OnClickListener {
            var intent = Intent()
            intent.setClass(requireActivity(), PersonalInformationActivity::class.java)
            startActivity(intent)
        })

        binding.btnAccountSettings.setOnClickListener(View.OnClickListener {
            var intent = Intent()
            intent.setClass(requireActivity(), AccountSettingsActivity::class.java)
            startActivity(intent)
        })

        binding.signOut.setOnClickListener {
            auth.signOut()
            //close the app
            requireActivity().finish()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}