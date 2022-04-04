package com.example.reactivedemo

import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {
    val client = HttpClient(Android)

    val responseCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.my_text)
        textView.text = "Cool animations"
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        rotate.duration = 1500
        rotate.repeatCount = Animation.INFINITE
        textView.startAnimation(rotate)

        val button = findViewById<Button>(R.id.my_button)
        button.setOnClickListener {
            val responseCode = makeSlowRequest()
            Toast.makeText(this, "Code: $responseCode", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeSlowRequest(): Int {
        return runBlocking {
            val response: HttpResponse = client.get("https://httpbin.org/delay/1000")
            response.status.value
        }
    }
}