package org.autojs.autojs.ui.edit.theme

import android.content.Context
import android.graphics.Color
import android.util.SparseIntArray
import org.autojs.autojs.model.editor.EditorTheme
import org.autojs.autojs.model.editor.TokenColor
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader

/**
 * 编辑器主题
 * Created by Stardust on 2018/2/16.
 */
class Theme(private val editorTheme: EditorTheme) {

    var backgroundColor: Int = Color.WHITE
        private set
    var foregroundColor: Int = Color.BLACK
        private set
    var lineNumberColor: Int = Color.GRAY
        private set
    var imeBarBackgroundColor: Int = 0xDDFFFFFF.toInt()
    var imeBarForegroundColor: Int = Color.WHITE
    var lineHighlightBackgroundColor: Int = 0
        private set
    var breakpointColor: Int = 0
        private set
    var debuggingLineBackground: Int = 0
        private set

    private val tokenColors = SparseIntArray()

    init {
        val colors = editorTheme.editorColors
        backgroundColor = parseColor(colors.editorBackground, backgroundColor)
        foregroundColor = parseColor(colors.editorForeground, foregroundColor)
        lineNumberColor = parseColor(colors.lineNumberForeground, lineNumberColor)
        imeBarBackgroundColor = parseColor(colors.imeBackgroundColor, imeBarBackgroundColor)
        imeBarForegroundColor = parseColor(colors.imeForegroundColor, imeBarForegroundColor)
        lineHighlightBackgroundColor = parseColor(colors.lineHighlightBackground, lineHighlightBackgroundColor)
        debuggingLineBackground = parseColor(colors.debuggingLineBackground, debuggingLineBackground)
        breakpointColor = parseColor(colors.breakpointForeground, backgroundColor)

        for (tokenColor in editorTheme.tokenColors) {
            val foregroundStr = tokenColor.settings.foreground ?: continue
            val foreground = Color.parseColor(foregroundStr)
            for (scope in tokenColor.scope) {
                setTokenColor(scope, foreground)
            }
        }
    }

    private fun parseColor(color: String?, defaultValue: Int): Int {
        if (color == null) return defaultValue
        return try {
            Color.parseColor(color)
        } catch (e: Exception) {
            defaultValue
        }
    }

    val name: String
        get() = editorTheme.name

    private fun setTokenColor(scope: String, foreground: Int) {
        for (token in TokenMapping.getTokensForScope(scope)) {
            tokenColors.put(token, foreground)
        }
    }

    fun getColorForToken(token: Int): Int {
        return tokenColors.get(token, foregroundColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Theme) return false
        return editorTheme.name == other.editorTheme.name
    }

    override fun hashCode(): Int {
        return editorTheme.name?.hashCode() ?: 0
    }

    override fun toString(): String = name

    companion object {
        @JvmStatic
        fun getDefault(context: Context): Theme {
            return fromAssetsJson(context, "editor/theme/light_plus.json")!!
        }

        @JvmStatic
        fun fromJson(json: String): Theme? {
            val theme = EditorTheme.fromJson(json) ?: return null
            return Theme(theme)
        }

        @JvmStatic
        fun fromJson(reader: Reader): Theme? {
            val theme = EditorTheme.fromJson(reader) ?: return null
            return Theme(theme)
        }

        @JvmStatic
        fun fromAssetsJson(context: Context, assetsPath: String): Theme? {
            return try {
                fromJson(InputStreamReader(context.assets.open(assetsPath)))
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}
