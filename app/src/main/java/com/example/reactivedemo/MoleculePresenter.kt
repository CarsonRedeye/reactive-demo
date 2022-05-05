package com.example.reactivedemo

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce

/**
 * Molecule - experimental new UI state calculation from Square Inc.
 * - Like Excel (everything can be made reactive)
 * - Kind of like a combination of ELM and free imperative coding (side effects for sure :).
 * - Possibly the most basic expression of view state (including async) using traditional imperative code.
 * - Definitely acts strangely compared to normal functions.
 * */

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun updateComposable(queryFlow: Flow<String?>): Model {
    // Whenever the model or query changes, this function will be re-run by Compose to return the model
    var model: Model by rememberMutableState(initialValue = Blank)
    val query by queryFlow.debounce(500).collectAsState(initial = null)

    if (!query.isNullOrEmpty()) {
        // This will be launched only if query changes
        LaunchedEffect(key1 = query) {
            model = Loading
            try {
                model = BreedsList(searchBreeds(query!!))
            } catch (e: Throwable) {
                model = Failure
            }
        }
    } else {
        model = Blank
    }

    return model
}

@Composable
fun <T> rememberMutableState(initialValue: T): MutableState<T> =
    remember { mutableStateOf(initialValue) }
