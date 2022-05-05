package com.example.reactivedemo

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

@Preview
@Composable
fun mainWithCompose() {
    println("Start program")
    var a by rememberMutableState(initialValue = 1)
    val b by derivedStateOf { a + 1 }

    LaunchedEffect(key1 = Unit) {
        delay(timeMillis = 2000)
        a = 5
    }

    Text("Result: $b")
}
