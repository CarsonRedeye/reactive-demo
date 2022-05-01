package com.example.reactivedemo

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce

@Composable
fun MoleculePresenter(
    queryFlow: Flow<String?>,
): Model {
    var model: Model by remember { mutableStateOf(Blank) }

    LaunchedEffect(Unit) {
        queryFlow.debounce(timeoutMillis = 500)
            .collect { query ->
                if (!query.isNullOrEmpty()) {
                    model = Loading
                    model = runCatching { BreedsList(searchBreeds(query)) }.getOrDefault(Failure)
                }
            }
    }

    return model
}
