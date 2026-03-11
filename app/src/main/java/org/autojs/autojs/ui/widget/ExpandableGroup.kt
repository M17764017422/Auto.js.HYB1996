package org.autojs.autojs.ui.widget

/**
 * 可展开分组接口，用于 RecyclerView 的分组列表
 * 支持 Kotlin 和 Java 互操作
 */
interface ExpandableGroup<C> {

    /**
     * 获取子项列表
     */
    val children: List<C>

    /**
     * 是否展开状态
     */
    var isExpanded: Boolean

    /**
     * 初始是否展开（用于首次显示）
     */
    fun isInitiallyExpanded(): Boolean = true
}
