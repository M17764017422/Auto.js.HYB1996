package org.autojs.autojs.ui.material3.components

import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.ConsoleView
import com.stardust.util.UiHandler
import org.autojs.autojs.R
import org.autojs.autojs.ui.edit.EditorModel
import org.autojs.autojs.ui.material3.theme.AppTheme

/**
 * Material3 LogSheet component for displaying script logs
 * This is a ModalBottomSheet that can be shown/hidden to display console output
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    consoleImpl: ConsoleImpl,
    viewModel: EditorModel = viewModel()
) {
    if (!viewModel.showLog) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { viewModel.showLog = false },
        sheetState = sheetState,
        dragHandle = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.lastScriptFile?.name ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Run/Rerun button with rotation animation
            IconButton(
                onClick = {
                    if (!viewModel.running) {
                        viewModel.rerun()
                    }
                },
                enabled = viewModel.lastScriptFile != null
            ) {
                if (viewModel.running) {
                    val infiniteTransition = rememberInfiniteTransition(label = "running")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing)
                        ),
                        label = "rotation"
                    )
                    Icon(
                        modifier = Modifier.rotate(rotation),
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Running",
                        tint = Color(0xFF161145)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = Color(0xFF3F51B5)
                    )
                }
            }

            // Clear button
            IconButton(onClick = { consoleImpl.clear() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Color(0xFF462566)
                )
            }

            // Open full log activity button
            IconButton(onClick = { viewModel.openLogActivity(context) }) {
                Icon(
                    imageVector = Icons.Default.Close, // Using Close as placeholder, should be log icon
                    contentDescription = "Open Log",
                    tint = Color(0xFF155465)
                )
            }
        }

        Column(
            modifier = Modifier
                .height(500.dp)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    ConsoleView(ctx).apply {
                        findViewById<View>(R.id.input_container).visibility = View.GONE
                        setConsole(consoleImpl)
                        setEnableStackFrameLinks(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Wrapper composable that provides the LogSheet with AppTheme
 */
@Composable
fun LogSheetWithTheme(
    consoleImpl: ConsoleImpl,
    viewModel: EditorModel = viewModel()
) {
    AppTheme {
        LogSheet(consoleImpl, viewModel)
    }
}

/**
 * Helper method for Java interoperability
 * Sets up ComposeView content with LogSheet
 */
fun setupLogSheetComposeView(
    composeView: androidx.compose.ui.platform.ComposeView,
    consoleImpl: ConsoleImpl,
    viewModel: EditorModel
) {
    composeView.setContent {
        LogSheetWithTheme(consoleImpl, viewModel)
    }
}