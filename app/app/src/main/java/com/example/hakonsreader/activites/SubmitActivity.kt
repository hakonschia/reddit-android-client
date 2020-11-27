package com.example.hakonsreader.activites

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.ActivitySubmitBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubmitActivity : AppCompatActivity() {

    private val api = App.get().api
    private lateinit var binding:  ActivitySubmitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.submitPost.setOnClickListener {
            CoroutineScope(IO).launch {
                api.subredditKt("hakonschia").submit(
                        "crosspost",
                        "Hello Reddit",
                        url = "t3_k1ykx4",
                        text = "Hello, Reddit!"
                )

                withContext(Main) {
                    binding.postSubmittedText.text = "Post submitted!"
                }
            }
        }
    }
}