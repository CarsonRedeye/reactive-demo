package com.example.reactivedemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun View(
    model: Model,
    textChanged: (String) -> Unit
) {
    View(flowOf(model), textChanged)
}

@Composable
fun View(
    modelFlow: Flow<Model>,
    textChanged: (String) -> Unit
) {
    Scaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
//            AnimatedShake(
//                animationStart = AnimationStarter.StartImmediately,
//                repeatInfinitely = true
//            ) {
//                Text("Cool animations")
//            }

            var text by remember { mutableStateOf("") }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    textChanged(it)
                }
            )

            val modelState = modelFlow.collectAsState(initial = BreedsList(emptyList()))
            when (val model = modelState.value) {
                Blank ->
                Loading -> CircularProgressIndicator()
                is BreedsList -> LazyColumn {
                    items(model.breeds) { breed ->
                        Text(breed.name)
                    }
                }
                Failure -> Text("😫😫")
            }
        }
    }
}