package de.ajoklar.makepadlibrarysample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.ajoklar.makepadlibrarysample.ui.theme.MakepadLibrarySampleTheme
import dev.makepad.android.MakepadActivity
import dev.makepad.android.MakepadView

class MainActivity : MakepadActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MakepadLibrarySampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootView()
                }
            }
        }
    }
}

@Composable
fun RootView() {
    var buttonTapped by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (buttonTapped) {
            Text("Native Jetpack Compose Text")
            Spacer(modifier = Modifier.size(30.dp))
            MakepadView()
        } else {
            Button(onClick = { buttonTapped = true }) {
                Text("Show the Makepad Example")
            }
        }
    }
}
