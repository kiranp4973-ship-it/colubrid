package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CloudBridgeDatabase
import com.example.data.repository.CloudBridgeRepository
import com.example.ui.CloudBridgeApp
import com.example.ui.CloudBridgeViewModel
import com.example.ui.CloudBridgeViewModelFactory
import com.example.ui.theme.CloudBridgeTheme
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CloudBridgeTheme {
        CloudBridgeMainContent()
      }
    }
  }
}

/**
 * Root composable content for the CloudBridge application.
 * Initializes the repository, view model, theme, and mounts the root UI tree
 * within an opaque Surface container to guarantee solid, non-transparent rendering in all previews.
 */
@Composable
fun CloudBridgeMainContent(
  modifier: Modifier = Modifier,
  viewModel: CloudBridgeViewModel? = null
) {
  val context = LocalContext.current
  val isInspection = LocalInspectionMode.current

  val activeViewModel: CloudBridgeViewModel = viewModel ?: run {
    val repository = remember(context, isInspection) {
      val database = if (isInspection) {
        CloudBridgeDatabase.getInMemoryDatabase(context)
      } else {
        CloudBridgeDatabase.getDatabase(context)
      }
      CloudBridgeRepository(database)
    }
    viewModel(
      factory = CloudBridgeViewModelFactory(repository)
    )
  }

  CloudBridgeTheme {
    Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.onBackground
    ) {
      CloudBridgeApp(
        viewModel = activeViewModel,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Preview(name = "CloudBridge App Preview", showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun CloudBridgeAppPreview() {
  CloudBridgeMainContent()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}

