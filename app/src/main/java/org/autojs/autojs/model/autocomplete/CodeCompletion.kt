package org.autojs.autojs.model.autocomplete

/**
 * 代码补全项
 * Created by Stardust on 2018/2/3.
 */
class CodeCompletion(
    val hint: String,
    val url: String?,
    private val mInsertText: String?,
    private val mInsertPos: Int
) {
    constructor(hint: String, url: String?, insertPos: Int) : this(hint, url, null, insertPos)
    constructor(hint: String, url: String?, insertText: String) : this(hint, url, insertText, -1)

    fun getInsertText(): String {
        return when {
            mInsertText != null -> mInsertText
            mInsertPos == 0 -> hint
            else -> hint.substring(mInsertPos)
        }
    }
}
