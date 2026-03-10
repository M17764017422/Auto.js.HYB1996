package org.autojs.autojs.ui.edit

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.model.script.ScriptFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Editor ViewModel for managing editor state with Compose
 * Used by LogSheet and other Material3 components
 */
class EditorModel : ViewModel() {
    var showLog by mutableStateOf(false)

    var running by mutableStateOf(false)

    var lastScriptFile by mutableStateOf<File?>(null)

    private var currentExecution: ScriptExecution? = null

    private val scriptListener = object : ScriptExecutionListener {
        override fun onStart(execution: ScriptExecution) {
            viewModelScope.launch(Dispatchers.Main) { 
                running = true 
            }
        }

        override fun onSuccess(execution: ScriptExecution, result: Any?) {
            viewModelScope.launch(Dispatchers.Main) { 
                running = false 
                currentExecution = null
            }
        }

        override fun onException(execution: ScriptExecution, e: Throwable) {
            viewModelScope.launch(Dispatchers.Main) { 
                running = false 
                currentExecution = null
            }
        }
    }

    fun toggleLog() {
        showLog = !showLog
    }

    fun openLogActivity(context: Context) {
        try {
            val clazz = Class.forName("org.autojs.autojs.ui.log.LogActivity")
            context.startActivity(Intent(context, clazz))
        } catch (e: ClassNotFoundException) {
            // Fallback to LogActivity without underscore (non-annotated version)
            try {
                val clazz = Class.forName("org.autojs.autojs.ui.log.LogActivity")
                context.startActivity(Intent(context, clazz))
            } catch (e2: ClassNotFoundException) {
                // Ignore
            }
        }
    }

    fun rerun() {
        val file = lastScriptFile ?: return
        running = true
        try {
            currentExecution = AutoJs.getInstance().scriptEngineService.execute(
                ScriptFile(file).toSource(),
                scriptListener,
                ExecutionConfig(workingDirectory = file.parent)
            )
        } catch (e: Exception) {
            running = false
            e.printStackTrace()
        }
    }

    fun stopCurrentExecution() {
        currentExecution?.let { execution ->
            execution.engine?.forceStop()
            currentExecution = null
            running = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentExecution()
    }
}