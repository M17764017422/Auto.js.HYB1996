/*
 * SAF 路径监听实现
 * 基于 ContentObserver 实现文件变化监听
 */

package com.stardust.pio.observe

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.util.Log
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/**
 * SAF 路径监听器
 * 通过 ContentObserver 监听 SAF 目录变化
 * 
 * @param path 监听的路径
 * @param treeUri SAF 授权的树 URI
 * @param documentId 路径对应的 documentId
 * @param context Android Context
 * @param intervalMillis 最小通知间隔（毫秒），用于防抖
 */
class SafPathObservable(
    private val path: String,
    private val treeUri: Uri,
    private val documentId: String,
    private val context: Context,
    private val intervalMillis: Long = 1000
) : PathObservable, Closeable {

    companion object {
        private const val TAG = "SafPathObservable"
    }

    private val observers = CopyOnWriteArrayList<PathObserver>()
    private val typedObservers = CopyOnWriteArrayList<(FileChangeEvent) -> Unit>()
    
    private var cursor: Cursor? = null
    private var lastNotifyTime = 0L
    private var closed = false

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (closed) return
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotifyTime < intervalMillis) {
                return  // 防抖
            }
            lastNotifyTime = currentTime
            
            Log.d(TAG, "Path changed: $path")
            notifyObservers()
        }
    }

    override val isObserving: Boolean
        get() = !closed && cursor != null

    init {
        startObserving()
    }

    private fun startObserving() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "SAF observation requires API 21+")
            return
        }

        try {
            // 构建子文档 URI 用于监听
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            
            cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null
            )
            
            cursor?.registerContentObserver(contentObserver)
            Log.i(TAG, "Started observing: $path")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start observing: $path", e)
        }
    }

    override fun addObserver(observer: PathObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PathObserver) {
        observers.remove(observer)
    }

    fun addTypedObserver(observer: (FileChangeEvent) -> Unit) {
        typedObservers.add(observer)
    }

    fun removeTypedObserver(observer: (FileChangeEvent) -> Unit) {
        typedObservers.remove(observer)
    }

    private fun notifyObservers() {
        // 通知简单观察者
        observers.forEach { observer ->
            try {
                observer.onChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Observer callback error", e)
            }
        }
        
        // 通知带类型观察者（SAF 无法区分事件类型，统一返回 UNKNOWN）
        typedObservers.forEach { observer ->
            try {
                observer.invoke(FileChangeEvent.UNKNOWN)
            } catch (e: Exception) {
                Log.e(TAG, "Typed observer callback error", e)
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        
        try {
            cursor?.unregisterContentObserver(contentObserver)
            cursor?.close()
            cursor = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing cursor", e)
        }
        
        observers.clear()
        typedObservers.clear()
        Log.i(TAG, "Stopped observing: $path")
    }
}

/**
 * 传统文件路径监听器
 * 基于 FileObserver 实现（用于非 SAF 路径）
 * 
 * 注意：这是一个简化实现，完整实现需要使用 android.os.FileObserver
 */
class TraditionalPathObservable(
    private val path: String
) : PathObservable, Closeable {

    companion object {
        private const val TAG = "TraditionalPathObserver"
    }

    private val observers = CopyOnWriteArrayList<PathObserver>()
    private var closed = false
    private var fileObserver: Any? = null // 实际使用时为 FileObserver

    override val isObserving: Boolean
        get() = !closed && fileObserver != null

    init {
        // FileObserver 需要 API 级别支持
        // 这里提供占位实现，实际使用时需要创建 FileObserver 子类
        Log.d(TAG, "TraditionalPathObservable created for: $path")
    }

    override fun addObserver(observer: PathObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PathObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { observer ->
            try {
                observer.onChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Observer callback error", e)
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        
        // 关闭 FileObserver
        try {
            val method = fileObserver?.javaClass?.getMethod("stopWatching")
            method?.invoke(fileObserver)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping FileObserver", e)
        }
        
        fileObserver = null
        observers.clear()
        Log.i(TAG, "Stopped observing: $path")
    }
}

/**
 * 复合路径监听器
 * 管理多个路径的监听
 */
class CompositePathObservable : PathObservable, Closeable {

    private val observables = mutableListOf<PathObservable>()
    private val observers = CopyOnWriteArrayList<PathObserver>()
    private var closed = false

    override val isObserving: Boolean
        get() = !closed && observables.isNotEmpty()

    /**
     * 添加路径监听
     */
    fun addObservable(observable: PathObservable) {
        if (closed) return
        observables.add(observable)
    }

    /**
     * 移除路径监听
     */
    fun removeObservable(observable: PathObservable) {
        observables.remove(observable)
        try {
            observable.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun addObserver(observer: PathObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: PathObserver) {
        observers.remove(observer)
    }

    internal fun notifyObservers() {
        observers.forEach { it.onChanged() }
    }

    override fun close() {
        if (closed) return
        closed = true
        
        observables.forEach { observable ->
            try {
                observable.close()
            } catch (e: Exception) {
                // ignore
            }
        }
        observables.clear()
        observers.clear()
    }
}
