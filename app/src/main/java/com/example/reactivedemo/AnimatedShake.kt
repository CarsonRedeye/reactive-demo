package com.example.reactivedemo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@Composable
fun AnimatedShake(
    animationStart: AnimationStarter,
    repeatInfinitely: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    var animatedOffset by remember { mutableStateOf(Offset.Zero) }

    when (animationStart) {
        AnimationStarter.StartImmediately -> {
            LaunchedEffect(key1 = Unit) {
                animateOffset(repeatInfinitely) { value: Offset ->
                    animatedOffset = value
                }
            }
        }
        is AnimationStarter.StartLater -> animationStart.start = {
            val job = animationScope.launch(start = CoroutineStart.LAZY) {
                animateOffset(repeatInfinitely) { value: Offset ->
                    animatedOffset = value
                }
            }
            animationStart.start = {
                job.start()
            }
        }
    }
    Box(
        modifier = Modifier
            // Keeps the content within the measured area while shaking
            .padding(horizontal = 24.dp)
            .offset(x = animatedOffset.x.dp, y = animatedOffset.y.dp),
        content = content
    )
}

private suspend fun animateOffset(
    repeatInfinitely: Boolean,
    animatedOffsetChanged: (Offset) -> Unit,
) {
    while (true) {
        animate(
            Offset.VectorConverter,
            initialValue = Offset.Zero,
            targetValue = Offset.Zero,
            initialVelocity = Offset(x = 1200f, y = 0f),
            animationSpec = spring(
                dampingRatio = 0.1f,
                stiffness = Spring.StiffnessMedium,
            )
        ) { value: Offset, _: Offset ->
            animatedOffsetChanged(value)
        }
        if (!repeatInfinitely) break
    }
}

@Preview
@Composable
fun AnimatedText() {
    val animationStarter = AnimationStarter.StartLater()
    AnimatedShake(
        animationStart = animationStarter,
        repeatInfinitely = true
    ) {
        Button(
            onClick = {
                animationStarter.start?.invoke()
            }) {
            Text("Tap me")
        }
    }
}

sealed interface AnimationStarter {
    object StartImmediately : AnimationStarter
    class StartLater : AnimationStarter {
        var start: (() -> Unit)? = null
    }
}