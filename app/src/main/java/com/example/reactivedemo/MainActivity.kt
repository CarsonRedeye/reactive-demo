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

fun update(msg: Msg, model: Model): ModelAndCmd {
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

    // runningFold = foldp in Elm. This part is the the "Elm runtime" that would stay constant or be part of the framework.
    // App developers would be modifying the update() function above.
    private val elmModelFlow = msgFlow.runningFold(
        initial = ModelAndCmd(
            model = Blank,
            cmd = Cmd.None
        ),
        operation = { modelAndCmd, msg ->
            update(msg = msg, model = modelAndCmd.model)
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

    // Android's "main" function for a screen
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

// Coroutines
suspend fun searchBreeds(query: String): List<Breed> {
    return client.get("https://api.thedogapi.com/v1/breeds/search") {
        parameter("q", query)
    }.body()
}

data class ModelAndCmd(val model: Model, val cmd: Cmd)