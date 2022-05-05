package com.example.reactivedemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized

@JvmName("MavericksView")
@Composable
fun View(
    mavericksModel: State<MavericksModel>,
    textChanged: (String) -> Unit
) {
    val model = when (val async = mavericksModel.value.breedsState) {
        Uninitialized -> Blank
        is com.airbnb.mvrx.Loading -> Loading
        is Success -> BreedsList(async.invoke())
        is Fail -> Failure
    }
    View(derivedStateOf { model }, textChanged)
}

@Composable
fun View(
    modelState: State<Model>,
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
            AnimatedShake(
                animationStart = AnimationStarter.StartImmediately,
                repeatInfinitely = true
            ) {
                Text("Cool animations")
            }

            var text by remember { mutableStateOf("") }
            OutlinedTextField(
                value = text,
                textStyle = MaterialTheme.typography.h6,
                onValueChange = {
                    text = it
                    textChanged(it)
                }
            )

            when (val model = modelState.value) {
                Blank -> Unit
                Loading -> CircularProgressIndicator()
                is BreedsList -> LazyColumn {
                    items(model.breeds) { breed ->
                        Text(breed.name, style = MaterialTheme.typography.h6)
                    }
                }
                Failure -> Text("😫😫")
            }
        }
    }
}