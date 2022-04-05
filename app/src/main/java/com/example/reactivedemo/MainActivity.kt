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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val headingTextView = findViewById<TextView>(R.id.animated_text)
        val button = findViewById<Button>(R.id.my_button)
        val image = findViewById<ImageView>(R.id.my_image)
        val urlTextView = findViewById<TextView>(R.id.image_url_text)

        animateText(headingTextView)

        button.setOnClickListener {
            fetchRandomDog { dog ->
                urlTextView.text = dog.message
                fetchImage(dog.message) { bitmap ->
                    image.setImageBitmap(bitmap)
                }
            }
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


//    private fun makeSlowRequestCallback(responseCallback: (Int) -> Unit) {
//        GlobalScope.launch {
//            val response: HttpResponse = client.get(randomDogUrl)
//            responseCallback(response.status.value)
//        }
//    }
}


private fun fetchRandomDog(dogCallback: (DogResponse) -> Unit) {
    MainScope().launch {
        val dog = client.get<DogResponse>("https://dog.ceo/api/breeds/image/random")
        dogCallback(dog)
    }
}

private fun fetchImage(url: String, imageCallback: (Bitmap) -> Unit) {
    MainScope().launch {
        val imageResponse: HttpResponse = client.get(url)
        val bytes = imageResponse.readBytes()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
        imageCallback(bitmap)
    }
}

@kotlinx.serialization.Serializable
data class DogResponse(val message: String, val status: String)