package com.example.cornell

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cornell.ui.AppNavigation
import com.example.cornell.ui.theme.CORNELLTheme
import com.example.cornell.ui.theme.CornellBackground

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CORNELLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CornellBackground
                ) {
                    AppNavigation(
                        onShowToast = { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
