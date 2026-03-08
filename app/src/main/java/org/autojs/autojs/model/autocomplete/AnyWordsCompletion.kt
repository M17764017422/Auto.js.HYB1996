package org.autojs.autojs.model.autocomplete

import android.text.Editable
import org.autojs.autojs.ui.widget.SimpleTextWatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

/**
 * 任意单词补全
 * Created by Stardust on 2018/2/26.
 */
class AnyWordsCompletion(private val executorService: ExecutorService) : SimpleTextWatcher() {

    companion object {
        private const val PATTERN = "[\\W]"
    }

    @Volatile
    private var dictionaryTree: DictionaryTree<String>? = null
    private val executeId = AtomicInteger()

    override fun afterTextChanged(s: Editable) {
        val str = s.toString()
        val id = executeId.incrementAndGet()
        executorService.execute { splitWords(id, str) }
    }

    private fun splitWords(id: Int, s: String) {
        if (id != executeId.get()) return
        val tree = DictionaryTree<String>()
        val words = s.split(PATTERN.toRegex())
        for (word in words) {
            if (id != executeId.get()) return
            tree.putWord(word, word)
        }
        dictionaryTree = tree
    }

    fun findCodeCompletion(completions: MutableList<CodeCompletion>, wordPrefill: String) {
        val tree = dictionaryTree ?: return
        val result = tree.searchByPrefill(wordPrefill)
        for (entry in result) {
            completions.add(CodeCompletion(entry.tag, null, wordPrefill.length))
        }
    }
}
