package com.example.reactivedemo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Model
sealed interface Model
object Loading : Model
object Failure : Model
data class ShowBreeds(val breeds: List<Breed>) : Model

@Serializable
data class Breed(val name: String)

// Update
sealed interface Msg
data class QueryUpdated(val query: String?) : Msg
data class BreedsRetrieved(val result: Result<List<Breed>>) : Msg

fun update(msg: Msg, model: Model): ModelAndCmd {
    return when (msg) {
        is QueryUpdated -> {
            if (msg.query.isNullOrBlank()) {
                ModelAndCmd(model = Loading, Cmd.None)
            } else {
                ModelAndCmd(
                    model = Loading,
                    cmd = Cmd.FetchBreeds(msg.query)
                )
            }
        }
        is BreedsRetrieved -> {
            val newModel = msg.result.map { ShowBreeds(it) }.getOrNull() ?: model
            ModelAndCmd(model = newModel, Cmd.None)
        }
    }
}

// View


@OptIn(FlowPreview::class)
class MainActivity : AppCompatActivity() {

    //private val requestBreedsSignal = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val model = MutableStateFlow<Model>(ShowBreeds(emptyList()))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Elm runtime
        //textSignal.onEach { update(QueryUpdated(it), model.value) }
//        requestBreedsSignal.onEach {
//            val breeds = runCatching { searchBreeds(it) }
//            updateAndRender(BreedsRetrieved(breeds), model.value)
//        }.launchIn(MainScope())

        // This reads like a sequential recipe, but it's agnostic to time. You could say it's "reactive"
//        val breeds = textSignal
//            .filterNotNull()
//            .filter { it.isNotBlank() }
//            .debounce(500)
//
//            // In traditional sequential code, this would necessarily block the cpu from executing other instructions.
//            .map { searchBreedsBlocking(query = it) }
//            .catch { it.printStackTrace() }

        setContent {
            View(
                model,
                textChanged = { updateAndRender(QueryUpdated(it), model.value) }
            )
        }
    }

    private fun updateAndRender(msg: Msg, model: Model) {
        val modelAndCmd = update(msg, model)
        this.model.value = modelAndCmd.model
        processCmd(modelAndCmd.cmd)
    }

    private fun processCmd(cmd: Cmd) {
        when (cmd) {
            is Cmd.FetchBreeds -> {
                MainScope().launch {
                    val breeds = runCatching { searchBreeds(cmd.query) }
                    updateAndRender(BreedsRetrieved(breeds), model.value)
                }
            }
            Cmd.None -> Unit
        }
    }
}

// Blocking
private fun searchBreedsBlocking(query: String): List<Breed> {
    return runBlocking {
        client.get("https://api.thedogapi.com/v1/breeds/search") {
            parameter("q", query)
        }.body()
    }
}

// Callbacks
private fun searchBreedsCallback(query: String, callback: (Breed) -> Unit) {
    MainScope().launch {
        val breedResponse: Breed =
            client.get("https://api.thedogapi.com/v1/breeds/search") {
                parameter("q", query)
            }.body()
        callback(breedResponse)
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
private suspend fun searchBreeds(query: String): List<Breed> {
    return client.get("https://api.thedogapi.com/v1/breeds/search") {
        parameter("q", query)
    }.body()
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

//private fun randomDogsFlow(): Flow<DogResponse> {
//    return flow {
//        emit(searchDogs())
//    }
//}

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

@Serializable
data class DogResponse(@SerialName("message") val url: String, val status: String)


sealed class ViewState {
    object Placeholder : ViewState()
    object Loading : ViewState()
    data class DogView(val bitmap: Bitmap, val caption: String) : ViewState()
}

sealed interface Cmd {
    object None : Cmd
    data class FetchBreeds(val query: String) : Cmd
}

data class ModelAndCmd(val model: Model, val cmd: Cmd)