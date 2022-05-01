package com.example.reactivedemo

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun getModel(
    queryFlow: Flow<String?>,
): Model {
    var model: Model by rememberMutableState(initialValue = Blank)
    var latestQuery: String? by rememberMutableState(initialValue = null)
    LaunchedEffect(Unit) {
        queryFlow.collect { query ->
            latestQuery = query
            if (!query.isNullOrEmpty()) {
                delay(500)
                if (latestQuery == query) {
                    model = Loading
                    model = runCatching { BreedsList(searchBreeds(query)) }.getOrDefault(Failure)
                }
            } else {
                model = Blank
            }
        }
    }

    return model
}

@Composable
private fun <T> rememberMutableState(initialValue: T): MutableState<T> =
    remember { mutableStateOf(initialValue) }
