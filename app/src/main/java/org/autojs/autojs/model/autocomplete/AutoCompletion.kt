package org.autojs.autojs.model.autocomplete

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.model.indices.Modules
import org.autojs.autojs.model.indices.Property
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * 自动补全
 * Created by Stardust on 2018/2/3.
 */
class AutoCompletion(context: Context, private val editText: EditText) {

    interface AutoCompleteCallback {
        fun updateCodeCompletion(codeCompletions: CodeCompletions)
    }

    companion object {
        private val STATEMENT = Pattern.compile("([A-Za-z]+\\.)?([a-zA-Z][a-zA-Z0-9_]*)?$")
    }

    private var moduleName: String? = null
    private var propertyPrefill: String? = null
    private var modules: List<org.autojs.autojs.model.indices.Module>? = null
    private val globalPropertyTree = DictionaryTree<Property>()
    private var autoCompleteCallback: AutoCompleteCallback? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private val anyWordsCompletion = AnyWordsCompletion(executorService)
    private val executeId = AtomicInteger()
    private val handler = Handler(Looper.getMainLooper())

    init {
        buildDictionaryTree(context)
        editText.addTextChangedListener(anyWordsCompletion)
    }

    fun setAutoCompleteCallback(callback: AutoCompleteCallback?) {
        autoCompleteCallback = callback
    }

    private fun buildDictionaryTree(context: Context) {
        Modules.getInstance().getModules(context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { moduleList -> buildDictionaryTree(moduleList) }
            .subscribe { moduleList -> modules = moduleList }
    }

    private fun buildDictionaryTree(moduleList: List<org.autojs.autojs.model.indices.Module>) {
        for (module in moduleList) {
            if (module.name != "globals") {
                globalPropertyTree.putWord(module.name, module.asGlobalProperty())
            }
            for (property in module.properties) {
                if (property.isGlobal) {
                    globalPropertyTree.putWord(property.key, property)
                }
            }
        }
    }

    fun onCursorChange(line: String, cursor: Int) {
        if (cursor <= 0 || line.isEmpty()) return
        if (modules == null || autoCompleteCallback == null) return

        findStatementOnCursor(line, cursor)
        val module = getModule(moduleName)
        if (propertyPrefill == null && module == null) return

        val prefill = propertyPrefill
        val id = executeId.incrementAndGet()
        executorService.execute {
            if (id != executeId.get()) return@execute
            val completions = findCodeCompletion(module, prefill)
            val codeCompletions = CodeCompletions(cursor, completions)
            if (id != executeId.get()) return@execute
            handler.post {
                if (id != executeId.get()) return@post
                autoCompleteCallback?.updateCodeCompletion(codeCompletions)
            }
        }
    }

    private fun getModule(name: String?): org.autojs.autojs.model.indices.Module? {
        if (name == null) return null
        return modules?.find { it.name == name }
    }

    private fun findStatementOnCursor(line: String, cursor: Int) {
        val matcher = STATEMENT.matcher(line.substring(0, cursor))
        if (!matcher.find()) {
            moduleName = null
            propertyPrefill = null
            return
        }
        if (matcher.groupCount() == 2) {
            val module = matcher.group(1)
            moduleName = module?.let { it.substring(0, it.length - 1) }
            propertyPrefill = matcher.group(2)
        } else {
            moduleName = null
            propertyPrefill = matcher.group(1)
        }
    }

    private fun findCodeCompletion(
        module: org.autojs.autojs.model.indices.Module?,
        prefill: String?
    ): List<CodeCompletion> {
        return if (module == null) {
            findCodeCompletionForGlobal(prefill)
        } else {
            findCodeCompletionForModule(module, prefill)
        }
    }

    private fun findCodeCompletionForModule(
        module: org.autojs.autojs.model.indices.Module,
        prefill: String?
    ): List<CodeCompletion> {
        val completions = mutableListOf<CodeCompletion>()
        val len = prefill?.length ?: 0
        for (property in module.properties) {
            if (prefill == null || property.key.startsWith(prefill)) {
                completions.add(CodeCompletion(property.key, property.url, len))
            }
        }
        return completions
    }

    private fun findCodeCompletionForGlobal(prefill: String?): List<CodeCompletion> {
        if (prefill == null) return Collections.emptyList()
        val completions = mutableListOf<CodeCompletion>()
        val result = globalPropertyTree.searchByPrefill(prefill)
        for (entry in result) {
            val property = entry.tag
            completions.add(CodeCompletion(property.key, property.url, prefill.length))
        }
        anyWordsCompletion.findCodeCompletion(completions, prefill)
        return completions
    }

    fun shutdown() {
        editText.removeTextChangedListener(anyWordsCompletion)
        executorService.shutdownNow()
    }
}
