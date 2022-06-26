package com.example.morsecode.sockets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.morsecode.R
import com.example.morsecode.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {

    lateinit var binding: ActivityTestBinding
    val viewModelFactory by lazy {
        MainViewModelFactory(
            //intent.getIntExtra("indeks_recepta", 0))

        )
        //arguments?.let { FirstFragmentArgs.fromBundle(it).idRecepta }
        //PostupakActivityArgs.confirmationAction(amount)
    }
    val viewModel: MainViewModel by viewModels() { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityTestBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_test
        )
        binding.lifecycleOwner = this//requireActivity() as PostupakActivity

        viewModel.subscribeToSocketEvents()
        binding.viewModel = viewModel
        //binding.message = viewModel.messages
    }
}