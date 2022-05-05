package com.example.reactivedemo

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach

@Composable
fun update(
    queryFlow: Flow<String?>,
): Model {
    var model: Model by rememberMutableState(initialValue = Blank)
    var latestQuery: String? by rememberMutableState(initialValue = null)
    LaunchedEffect(Unit) {
        queryFlow
            .onEach {
                model = Loading
            }
            .debounce(1000)
            .collect { query ->
                latestQuery = query
                if (!query.isNullOrEmpty()) {
                    if (latestQuery == query) {
                        model = Loading
                        model =
                            runCatching { BreedsList(searchBreeds(query)) }.getOrDefault(Failure)
                    }
                } else {
                    model = Blank
                }
            }
    }

    return model
}

@Composable
fun <T> rememberMutableState(initialValue: T): MutableState<T> =
    remember { mutableStateOf(initialValue) }
