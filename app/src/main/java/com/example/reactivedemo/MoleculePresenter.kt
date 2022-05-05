package com.example.reactivedemo

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce

@Composable
fun updateComposable(queryFlow: Flow<String?>): Model {
    var model: Model by rememberMutableState(initialValue = Blank)
    LaunchedEffect(Unit) {
        queryFlow
            .debounce(1000)
            .collect { query ->
                if (!query.isNullOrEmpty()) {
                    model = Loading
                    model = try {
                        BreedsList(searchBreeds(query))
                    } catch (e: Throwable) {
                        Failure
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
