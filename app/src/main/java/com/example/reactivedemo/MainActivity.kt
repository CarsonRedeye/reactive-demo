package com.example.reactivedemo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import reactivecircus.flowbinding.android.view.clicks

class MainActivity : AppCompatActivity() {

    private lateinit var headingTextView: TextView
    private lateinit var dogButton: Button
    private lateinit var image: ImageView
    private lateinit var caption: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

        }
        setContentView(R.layout.activity_main)
        setupViewReferences()
        animateText(headingTextView)

        MainScope().launch {
            val imageFlow = dogButton.clicks()
                .flatMapConcat {
                    randomDogsFlow()
                }
                .flatMapConcat {
                    imageFlow(it.url)
                }
                .collect {

                }

            val viewStateFlow: Flow<ViewState> = flow {
                emit(ViewState.Placeholder)
                val imageFlow = asdf()
                emitAll(imageFlow)
            }

            viewStateFlow.collect { viewState ->
                when (viewState) {
                    ViewState.Placeholder -> image.setImageDrawable(resources.getDrawable(R.drawable.placeholder))
                    ViewState.Loading -> {
                        setImageLoadingSpinner()
                        caption.setText("")
                    }
                    is ViewState.DogView -> {
                        image.setImageBitmap(viewState.bitmap)
                        caption.setText(viewState.caption)
                    }
                }
            }
        }
    }

    private fun asdf(): Flow<ViewState> = dogButton.clicks()
        .transform {
            emit(ViewState.Loading)
            val dog = fetchRandomDog()
            val bitmap = fetchImage(dog.url)
            emit(ViewState.DogView(bitmap, caption = dog.url))
        }

    private fun setupViewReferences() {
        headingTextView = findViewById(R.id.animated_text)
        dogButton = findViewById(R.id.request_dog_button)
        image = findViewById(R.id.dog_image)
        caption = findViewById(R.id.caption_text)
    }

    private fun setImageLoadingSpinner() {
        val avd: Drawable? = AnimatedVectorDrawableCompat.create(this, R.drawable.download_spinner)
        image.setImageDrawable(avd)
        (avd as Animatable?)?.start()
    }
}

// Blocking
private fun fetchRandomDogBlocking(): DogResponse {
    return runBlocking {
        client.get("https://dog.ceo/api/breeds/image/random")
    }
}

private fun fetchImageBlocking(url: String): Bitmap {
    return runBlocking {
        val imageResponse: HttpResponse = client.get(url)
        val bytes = imageResponse.readBytes()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
    }
}

// Callbacks
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

// Coroutines
private suspend fun fetchRandomDog(): DogResponse {
    return client.get("https://dog.ceo/api/breeds/image/random")
}

private suspend fun fetchImage(url: String): Bitmap {
    val imageResponse: HttpResponse = client.get(url)
    val bytes = imageResponse.readBytes()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
}

private suspend fun decodeImageSlow(url: String): Bitmap {
    val imageResponse: HttpResponse = client.get(url)
    val bytes = imageResponse.readBytes()
    repeat(100) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
}

private fun randomDogsFlow(): Flow<DogResponse> {
    return flow {
        emit(fetchRandomDog())
    }
}

private fun imageFlow(url: String): Flow<Bitmap> {
    return flow {
        emit(fetchImage(url))
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

@kotlinx.serialization.Serializable
data class DogResponse(@SerialName("message") val url: String, val status: String)

sealed class ViewState {
    object Placeholder : ViewState()
    object Loading : ViewState()
    data class DogView(val bitmap: Bitmap, val caption: String) : ViewState()
}
