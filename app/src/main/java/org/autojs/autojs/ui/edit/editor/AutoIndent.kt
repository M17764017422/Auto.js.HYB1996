package org.autojs.autojs.ui.edit.editor

import android.text.Editable
import android.text.TextWatcher

/**
 * 自动缩进处理
 * Created by Stardust on 2018/2/25.
 */
class AutoIndent(private val editText: CodeEditText) : TextWatcher {

    private var indent = "    "
    private var insertingIndent = false
    private var extraIndent = false
    private var autoIndent = false
    private var cursor = 0

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    /**
     * 判断是否是在光标处插入一个换行符的情况，是的话在下一个afterTextChanged回调中将调整缩进
     */
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (insertingIndent) return

        // 不是插入一个字符的情况
        if (count != 1 || before != 0) return

        // 边界检查
        if (start - 1 < 0 || start >= s.length) return

        val charInserted = s[start]
        // 不是插入换行符的情况
        if (charInserted != '\n') return

        cursor = editText.selectionStart
        // 不是在光标处插入字符的情况
        if (cursor != editText.selectionEnd || cursor != start + 1) return

        // 到这里已经可以判断为当前的字符变化为"在光标处插入一个换行符"的情况
        autoIndent = true
        // 我们再做一点额外判断。判断换行符之前的字符是否是括号，是的话下行将额外增加空格用于缩进
        val charBefore = s[start - 1]
        if (charBefore == '{' || charBefore == '(') {
            extraIndent = true
        }
    }

    override fun afterTextChanged(s: Editable) {
        if (insertingIndent || !autoIndent) return

        var indentStr = getLastLineIndent()
        if (extraIndent) {
            indentStr = indent + indentStr
        }
        insertingIndent = true
        s.insert(cursor, indentStr)
        insertingIndent = false
        extraIndent = false
        autoIndent = false
        cursor = -1
    }

    private fun getLastLineIndent(): CharSequence {
        if (cursor < 0 || cursor > editText.length()) {
            return ""
        }
        val line = LayoutHelper.getLineOfChar(editText.layout, cursor)
        if (line == 0) {
            return ""
        }
        val lastLineStart = editText.layout.getLineStart(line - 1)
        val lastLineEnd = editText.layout.getLineEnd(line)
        val text = editText.text!!
        for (i in lastLineStart until lastLineEnd) {
            if (text[i] != ' ') {
                return if (i == lastLineStart) {
                    ""
                } else {
                    text.subSequence(lastLineStart, i)
                }
            }
        }
        return text.subSequence(lastLineStart, lastLineEnd)
    }
}
