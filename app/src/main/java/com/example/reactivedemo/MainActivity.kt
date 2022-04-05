package com.example.reactivedemo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    val randomDogUrl = "https://dog.ceo/api/breeds/image/random"

    val responseCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val headingTextView = findViewById<TextView>(R.id.animated_text)
        val button = findViewById<Button>(R.id.my_button)
        val image = findViewById<ImageView>(R.id.my_image)
        val urlTextView = findViewById<TextView>(R.id.image_url_text)

        animateText(headingTextView)

        button.setOnClickListener {
            val dog = fetchRandomDog()
            urlTextView.text = dog.message
            val bitmap = fetchImage(dog.message)
            image.setImageBitmap(bitmap)
        }
    }

    private fun animateText(textView: TextView) {
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        rotate.duration = 1500
        rotate.repeatCount = Animation.INFINITE
        textView.startAnimation(rotate)
    }

    fun fetchRandomDog(): DogResponse {
        return runBlocking {
            client.get(randomDogUrl)
        }
    }

    private fun fetchImage(url: String): Bitmap {
        return runBlocking {
            val dogResponse = client.get<DogResponse>(randomDogUrl)
            val imageResponse: HttpResponse = client.get(dogResponse.message)
            val bytes = imageResponse.readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
        }
    }

//    private fun makeSlowRequestCallback(responseCallback: (Int) -> Unit) {
//        GlobalScope.launch {
//            val response: HttpResponse = client.get(randomDogUrl)
//            responseCallback(response.status.value)
//        }
//    }
}

@kotlinx.serialization.Serializable
data class DogResponse(val message: String, val status: String)