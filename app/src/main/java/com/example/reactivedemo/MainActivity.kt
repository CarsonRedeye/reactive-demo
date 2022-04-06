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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks

class MainActivity : AppCompatActivity() {

    private lateinit var headingTextView: TextView
    private lateinit var dogButton: Button
    private lateinit var image: ImageView
    private lateinit var urlTextView: TextView

    private var imageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        headingTextView = findViewById(R.id.animated_text)
        dogButton = findViewById(R.id.request_dog_button)
        image = findViewById(R.id.my_image)
        urlTextView = findViewById(R.id.image_url_text)

        animateText(headingTextView)

        dogButton.clicks().map { }
        dogButton.setOnClickListener {
            fetchRandomDog { dog ->
                imageUrl = dog.message
                fetchImage(imageUrl) { bitmap ->
                    urlTextView.text = imageUrl
                    image.setImageBitmap(bitmap)
                }
            }
        }
    }
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

private suspend fun fetchRandomDog(): DogResponse {
    return client.get("https://dog.ceo/api/breeds/image/random")
}

private suspend fun fetchImage(url: String): Bitmap {
    val imageResponse: HttpResponse = client.get(url)
    val bytes = imageResponse.readBytes()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
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

@kotlinx.serialization.Serializable
data class DogResponse(val message: String, val status: String)

data class DogView(val bitmap: Bitmap, val caption: String)