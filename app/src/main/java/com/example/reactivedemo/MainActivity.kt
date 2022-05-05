@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.reactivedemo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import app.cash.molecule.AndroidUiDispatcher.Companion.Main
import app.cash.molecule.launchMolecule
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Model **/
sealed interface Model

object Blank : Model
object Loading : Model
object Failure : Model
data class BreedsList(val breeds: List<Breed>) : Model

@Serializable
data class Breed(val name: String)

/** View **/
// See View.kt

/** Update **/
sealed interface Msg
data class QueryUpdated(val query: String?) : Msg
data class BreedsRetrieved(val result: Result<List<Breed>>) : Msg

sealed interface Cmd {
    object None : Cmd
    data class FetchBreeds(val query: String) : Cmd
}

fun updateComposable(msg: Msg, model: Model): ModelAndCmd {
    return when (msg) {
        is QueryUpdated -> {
            if (msg.query.isNullOrBlank()) {
                ModelAndCmd(model = Blank, Cmd.None)
            } else {
                ModelAndCmd(
                    model = Loading,
                    cmd = Cmd.FetchBreeds(msg.query)
                )
            }
        }
        is BreedsRetrieved -> {
            val newModel = msg.result.map { BreedsList(it) }.getOrNull() ?: model
            ModelAndCmd(model = newModel, Cmd.None)
        }
    }
}


@OptIn(FlowPreview::class)
class MainActivity : AppCompatActivity() {

    private val querySignal = MutableStateFlow<String?>(null)

    /**
     * First-order FRP (Elm / Redux / MVI)
     * - Synchronous core (async happens outside the update function)
     * - Pure - should be more predictable - one place for inputs, one place for outputs.
     * - Easy to compose with new functionality
     */
    private val queryUpdatedSignal = querySignal.map { QueryUpdated(it) }
    private val breedsRetrievedSignal =
        MutableStateFlow(BreedsRetrieved(Result.success(emptyList())))
    private val msgFlow = merge(queryUpdatedSignal, breedsRetrievedSignal)

    // runningFold = foldp in Elm.
    private val elmModelFlow = msgFlow.runningFold(
        initial = ModelAndCmd(
            model = Blank,
            cmd = Cmd.None
        ),
        operation = { modelAndCmd, msg ->
            updateComposable(msg = msg, model = modelAndCmd.model)
        }
    ).onEach {
        processCmd(it.cmd)
    }.map {
        it.model // return just the model, not the Cmd
    }

    /**
     * Asynchronous data flows (Reactive extensions)
     * - Shorter
     * - More readable because each function chain deals with one concept, running top to bottom
     * - Common async scenarios for free - with standard library operators like Flow.debounce
     *     - Debounce with Elm is tricky - either pre-process the query signal, or deal with it manually in update().
     * */
    private val rXModelFlow: Flow<Model> = querySignal
        .filterNotNull()
        .filter { it.isNotBlank() }
        .debounce(500)
        .flatMapLatest {
            flow {
                emit(Loading)
                emit(
                    try {
                        BreedsList(searchBreeds(query = it))
                    } catch (e: Throwable) {
                        Failure
                    }
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** Molecule **/
        val moleculeModelFlow = CoroutineScope(Main).launchMolecule {
            updateComposable(queryFlow = querySignal)
        }
        setContent {
//            View(
//                elmModelFlow.collectAsState(initial = Blank),
//                textChanged = { querySignal.value = it }
//            )

            View(
                moleculeModelFlow.collectAsState(),
                textChanged = { querySignal.value = it }
            )
        }
    }

    // Elm runtime handles things like this
    private fun processCmd(cmd: Cmd) {
        when (cmd) {
            is Cmd.FetchBreeds -> {
                MainScope().launch {
                    val breedsResult = runCatching { searchBreeds(cmd.query) }
                    breedsRetrievedSignal.value = BreedsRetrieved(breedsResult)
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
suspend fun searchBreeds(query: String): List<Breed> {
    return client.get("https://api.thedogapi.com/v1/breeds/search") {
        parameter("q", query)
    }.body()
}

suspend fun fetchImage(url: String): Bitmap {
    val imageResponse: HttpResponse = client.get(url)
    val bytes = imageResponse.readBytes()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
}

suspend fun decodeImageSlow(url: String): Bitmap {
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

@Serializable
data class DogResponse(@SerialName("message") val url: String, val status: String)

sealed class ViewState {
    object Placeholder : ViewState()
    object Loading : ViewState()
    data class DogView(val bitmap: Bitmap, val caption: String) : ViewState()
}

data class ModelAndCmd(val model: Model, val cmd: Cmd)