package org.autojs.autojs.ui.edit.editor

/*
 * THIS CLASS IS PROVIDED TO THE PUBLIC DOMAIN FOR FREE WITHOUT ANY
 * RESTRICTIONS OR ANY WARRANTY.
 */

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.widget.TextView
import java.util.LinkedList

/**
 * A generic undo/redo implementation for TextViews.
 */
class TextViewUndoRedo(private val textView: TextView) {

    /**
     * Is undo/redo being performed? This member signals if an undo/redo
     * operation is currently being performed. Changes in the text during
     * undo/redo are not recorded because it would mess up the undo history.
     */
    private var isUndoOrRedo = false

    /**
     * The edit history.
     */
    private val editHistory = EditHistory()

    /**
     * The change listener.
     */
    private val changeListener = EditTextChangeListener()

    private var enabled = true

    private var initialHistoryStackSize = 0

    private val handler = Handler(Looper.getMainLooper())
    private var textChangeId = 0

    init {
        textView.addTextChangedListener(changeListener)
    }

    fun isEnabled(): Boolean = enabled

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun setDefaultText(text: CharSequence) {
        clearHistory()
        isUndoOrRedo = true
        (textView.text as Editable).replace(0, text.length, text)
        isUndoOrRedo = false
    }

    fun isTextChanged(): Boolean = initialHistoryStackSize != editHistory.size()

    fun markTextAsUnchanged() {
        initialHistoryStackSize = editHistory.size()
    }

    /**
     * Disconnect this undo/redo from the text view.
     */
    fun disconnect() {
        textView.removeTextChangedListener(changeListener)
    }

    /**
     * Set the maximum history size. If size is negative, then history size is
     * only limited by the device memory.
     */
    fun setMaxHistorySize(maxHistorySize: Int) {
        editHistory.maxHistorySize = maxHistorySize
    }

    /**
     * Clear history.
     */
    fun clearHistory() {
        editHistory.clear()
        initialHistoryStackSize = 0
    }

    /**
     * Can undo be performed?
     */
    fun canUndo(): Boolean = editHistory.position > 0

    /**
     * Perform undo.
     */
    fun undo() {
        val edit = editHistory.getPrevious() ?: return

        val text = textView.editableText
        val start = edit.start
        val end = start + (edit.after?.length ?: 0)

        isUndoOrRedo = true
        text.replace(start, end, edit.before)
        isUndoOrRedo = false

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (o in text.getSpans(0, text.length, UnderlineSpan::class.java)) {
            text.removeSpan(o)
        }

        Selection.setSelection(text, if (edit.before == null) start else start + edit.before.length)
    }

    /**
     * Can redo be performed?
     */
    fun canRedo(): Boolean = editHistory.position < editHistory.history.size

    /**
     * Perform redo.
     */
    fun redo() {
        val edit = editHistory.getNext() ?: return

        val text = textView.editableText
        val start = edit.start
        val end = start + (edit.before?.length ?: 0)

        isUndoOrRedo = true
        text.replace(start, end, edit.after)
        isUndoOrRedo = false

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (o in text.getSpans(0, text.length, UnderlineSpan::class.java)) {
            text.removeSpan(o)
        }

        Selection.setSelection(text, if (edit.after == null) start else start + edit.after.length)
    }

    /**
     * Store preferences.
     */
    fun storePersistentState(editor: SharedPreferences.Editor, prefix: String) {
        // Store hash code of text in the editor so that we can check if the
        // editor contents has changed.
        editor.putString(prefix + ".hash", textView.text.toString().hashCode().toString())
        editor.putInt(prefix + ".maxSize", editHistory.maxHistorySize)
        editor.putInt(prefix + ".position", editHistory.position)
        editor.putInt(prefix + ".size", editHistory.history.size)

        var i = 0
        for (ei in editHistory.history) {
            val pre = "$prefix.$i"
            editor.putInt("$pre.start", ei.start)
            editor.putString("$pre.before", ei.before.toString())
            editor.putString("$pre.after", ei.after.toString())
            i++
        }
    }

    /**
     * Restore preferences.
     *
     * @param prefix The preference key prefix used when state was stored.
     * @return did restore succeed? If this is false, the undo history will be
     * empty.
     */
    fun restorePersistentState(sp: SharedPreferences, prefix: String): Boolean {
        val ok = doRestorePersistentState(sp, prefix)
        if (!ok) {
            editHistory.clear()
        }
        return ok
    }

    private fun doRestorePersistentState(sp: SharedPreferences, prefix: String): Boolean {
        val hash = sp.getString(prefix + ".hash", null) ?: return true

        if (hash.toInt() != textView.text.toString().hashCode()) {
            return false
        }

        editHistory.clear()
        editHistory.maxHistorySize = sp.getInt(prefix + ".maxSize", -1)

        val count = sp.getInt(prefix + ".size", -1)
        if (count == -1) {
            return false
        }

        for (i in 0 until count) {
            val pre = "$prefix.$i"
            val start = sp.getInt("$pre.start", -1)
            val before = sp.getString("$pre.before", null)
            val after = sp.getString("$pre.after", null)

            if (start == -1 || before == null || after == null) {
                return false
            }
            editHistory.add(EditItem(start, before, after))
        }

        editHistory.position = sp.getInt(prefix + ".position", -1)
        if (editHistory.position == -1) {
            return false
        }

        return true
    }

    /**
     * Represents the changes performed by a single edit operation.
     */
    private inner class EditItem(
        val start: Int,
        val before: CharSequence,
        val after: CharSequence
    )

    /**
     * Keeps track of all the edit history of a text.
     */
    private inner class EditHistory {
        /**
         * The position from which an EditItem will be retrieved when getNext()
         * is called. If getPrevious() has not been called, this has the same
         * value as mmHistory.size().
         */
        var position = 0

        /**
         * Maximum undo history size.
         */
        var maxHistorySize = -1
            set(value) {
                field = value
                if (value >= 0) {
                    trimHistory()
                }
            }

        /**
         * The list of edits in chronological order.
         */
        val history: LinkedList<EditItem> = LinkedList()

        /**
         * Clear history.
         */
        fun clear() {
            position = 0
            history.clear()
        }

        /**
         * Adds a new edit operation to the history at the current position. If
         * executed after a call to getPrevious() removes all the future history
         * (elements with positions >= current history position).
         */
        fun add(item: EditItem) {
            while (history.size > position) {
                history.removeLast()
            }
            history.add(item)
            position++

            if (maxHistorySize >= 0) {
                trimHistory()
            }
        }

        fun size(): Int = history.size

        /**
         * Trim history when it exceeds max history size.
         */
        private fun trimHistory() {
            while (history.size > maxHistorySize) {
                history.removeFirst()
                position--
            }
            if (position < 0) {
                position = 0
            }
        }

        /**
         * Traverses the history backward by one position, returns and item at
         * that position.
         */
        fun getPrevious(): EditItem? {
            if (position == 0) {
                return null
            }
            position--
            return history[position]
        }

        /**
         * Traverses the history forward by one position, returns and item at
         * that position.
         */
        fun getNext(): EditItem? {
            if (position >= history.size) {
                return null
            }
            val item = history[position]
            position++
            return item
        }
    }

    /**
     * Class that listens to changes in the text.
     */
    private inner class EditTextChangeListener : TextWatcher {
        /**
         * The text that will be removed by the change event.
         */
        private var beforeChange: CharSequence? = null

        /**
         * The text that was inserted by the change event.
         */
        private var afterChange: CharSequence? = null

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (isUndoOrRedo || !enabled) return
            beforeChange = s.subSequence(start, start + count)
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (isUndoOrRedo || !enabled) return
            afterChange = s.subSequence(start, start + count)
            textChangeId++
            editHistory.add(EditItem(start, beforeChange ?: "", afterChange ?: ""))
        }

        override fun afterTextChanged(s: Editable) {
            if (isUndoOrRedo || !enabled) return
            if (editHistory.size() < initialHistoryStackSize) {
                initialHistoryStackSize = 0
            }
        }
    }
}
