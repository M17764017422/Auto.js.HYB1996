package org.autojs.autojs.model.autocomplete

/**
 * 代码补全项集合
 * Created by Stardust on 2017/9/27.
 */
class CodeCompletions @JvmOverloads constructor(
    private val from: Int,
    private val completions: List<CodeCompletion>
) {
    companion object {
        @JvmStatic
        fun just(hints: List<String>): CodeCompletions {
            val completions = hints.map { CodeCompletion(it, null, 0) }
            return CodeCompletions(-1, completions)
        }
    }

    fun getFrom(): Int = from

    fun size(): Int = completions.size

    fun getHint(position: Int): String = completions[position].hint

    operator fun get(pos: Int): CodeCompletion = completions[pos]

    fun getUrl(pos: Int): String? = completions[pos].url
}