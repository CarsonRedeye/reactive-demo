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
import com.airbnb.mvrx.MavericksView
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Model
sealed interface Model

object Blank : Model
object Loading : Model
object Failure : Model
data class BreedsList(val breeds: List<Breed>) : Model

@Serializable
data class Breed(val name: String)


// Update
sealed interface Msg
data class QueryUpdated(val query: String?) : Msg
data class BreedsRetrieved(val result: Result<List<Breed>>) : Msg

fun getModel(msg: Msg, model: Model): ModelAndCmd {
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

// View


@OptIn(FlowPreview::class)
class MainActivity : AppCompatActivity(), MavericksView {

    private val queryFlow = MutableStateFlow<String?>(null)

    /**
     * First-order FRP (Elm / Redux / MVI)
     * - Synchronous core (async happens outside the update function)
     * - Pure
     * - Easy to compose with new functionality
     */
    private val queryUpdatedFlow = queryFlow.map { QueryUpdated(it) }
    private val breedsRetrievedFlow = MutableStateFlow(BreedsRetrieved(Result.success(emptyList())))
    private val msgFlow = merge(queryUpdatedFlow.debounce(500), breedsRetrievedFlow)

    private val modelFlow = msgFlow.runningFold(
        initial = ModelAndCmd(
            model = Blank,
            cmd = Cmd.None
        ),
        operation = { modelAndCmd, msg ->
            getModel(msg, modelAndCmd.model)
        }
    ).onEach {
        processCmd(it.cmd)
    }.map { it.model }

    /**
     * Asynchronous data flows (Reactive extensions)
     * - Shorter
     * - More readable
     * */
    private val modelFlowRx: Flow<Model> = queryFlow.filterNotNull()
        .filter { it.isNotBlank() }
        .debounce(500)
        .flatMapLatest {
            flow {
                emit(Loading)
                emit(runCatching { BreedsList(searchBreeds(query = it)) }
                    .getOrDefault(Failure))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val viewModel =
            CoroutineScope(Main).launchMolecule {
                getModel(
                    queryFlow = queryFlow
                )
            }
        setContent {
            // val viewModel: MainViewModel = mavericksActivityViewModel()


            View(
                viewModel.collectAsState(),
                textChanged = { queryFlow.value = it }
            )
        }
    }

    // Elm runtime handles things like this
    private fun processCmd(cmd: Cmd) {
        when (cmd) {
            is Cmd.FetchBreeds -> {
                MainScope().launch {
                    val breedsResult = runCatching { searchBreeds(cmd.query) }
                    breedsRetrievedFlow.value = BreedsRetrieved(breedsResult)
                }
            }
            Cmd.None -> Unit
        }
    }

    override fun invalidate() {
        TODO("Not yet implemented")
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

sealed interface Cmd {
    object None : Cmd
    data class FetchBreeds(val query: String) : Cmd
}

data class ModelAndCmd(val model: Model, val cmd: Cmd)