/*
 * 文件路径监听接口
 * 提供文件/目录变化监听的抽象
 */

package com.stardust.pio.observe

import java.io.Closeable

/**
 * 路径观察者接口（函数式接口）
 * 用于接收文件变化通知
 */
fun interface PathObserver {
    /**
     * 文件变化回调
     */
    fun onChanged()
}

/**
 * 路径监听器接口
 * 用于监听文件或目录的变化
 */
interface PathObservable : Closeable {

    /**
     * 添加观察者
     * @param observer 变化回调
     */
    fun addObserver(observer: PathObserver)

    /**
     * 移除观察者
     * @param observer 变化回调
     */
    fun removeObserver(observer: PathObserver)

    /**
     * 关闭监听器，释放资源
     */
    override fun close()

    /**
     * 是否正在监听
     */
    val isObserving: Boolean
}

/**
 * 文件变化事件类型
 */
enum class FileChangeEvent {
    /**
     * 文件/目录被创建
     */
    CREATED,
    
    /**
     * 文件/目录被删除
     */
    DELETED,
    
    /**
     * 文件内容被修改
     */
    MODIFIED,
    
    /**
     * 文件/目录被重命名
     */
    RENAMED,
    
    /**
     * 未知变化
     */
    UNKNOWN
}

/**
 * 带事件类型的路径监听器接口
 */
interface TypedPathObservable : PathObservable {
    /**
     * 添加带事件类型的观察者
     * @param observer 变化回调，参数为事件类型
     */
    fun addTypedObserver(observer: (FileChangeEvent) -> Unit)
    
    /**
     * 移除带事件类型的观察者
     * @param observer 变化回调
     */
    fun removeTypedObserver(observer: (FileChangeEvent) -> Unit)
}
