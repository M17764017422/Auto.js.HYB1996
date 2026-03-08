package org.autojs.autojs.ui.edit.editor

/**
 * 括号匹配工具类
 * Created by Stardust on 2018/2/25.
 */
object BracketMatching {

    const val UNMATCHED_BRACKET = -2
    const val BRACKET_NOT_FOUND = -1

    private val PAIR_LEFT = charArrayOf('(', '{', '[')
    private val PAIR_RIGHT = charArrayOf(')', '}', ']')

    @JvmStatic
    fun bracketMatching(text: CharSequence, index: Int): Int {
        val ch = text[index]
        for (i in PAIR_LEFT.indices) {
            if (PAIR_LEFT[i] == ch) {
                return findRightBracket(text, index + 1, PAIR_LEFT[i], PAIR_RIGHT[i])
            }
        }
        for (i in PAIR_RIGHT.indices) {
            if (PAIR_RIGHT[i] == ch) {
                return findLeftBracket(text, index - 1, PAIR_LEFT[i], PAIR_RIGHT[i])
            }
        }
        return BRACKET_NOT_FOUND
    }

    @JvmStatic
    fun findLeftBracket(text: CharSequence, index: Int, left: Char, right: Char): Int {
        var rightBracketCount = 0
        for (i in index downTo 0) {
            val ch = text[i]
            if (ch == left) {
                if (rightBracketCount == 0) {
                    return i
                }
                rightBracketCount--
            } else if (ch == right) {
                rightBracketCount++
            }
        }
        return UNMATCHED_BRACKET
    }

    @JvmStatic
    fun findRightBracket(text: CharSequence, index: Int, left: Char, right: Char): Int {
        var leftBracketCount = 0
        for (i in index until text.length) {
            val ch = text[i]
            if (ch == left) {
                leftBracketCount++
            } else if (ch == right) {
                if (leftBracketCount == 0) {
                    return i
                }
                leftBracketCount--
            }
        }
        return BRACKET_NOT_FOUND
    }
}
