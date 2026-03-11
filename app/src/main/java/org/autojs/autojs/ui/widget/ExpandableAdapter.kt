package org.autojs.autojs.ui.widget

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * 可展开分组的 RecyclerView Adapter 基类
 * 支持分组展开/折叠功能，兼容 Java 继承
 */
abstract class ExpandableAdapter<G : ExpandableGroup<C>, C, GVH : RecyclerView.ViewHolder, CVH : RecyclerView.ViewHolder> protected constructor(
    groups: List<G>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    protected val groups: MutableList<G> = groups.toMutableList()

    companion object {
        @JvmStatic
        protected val TYPE_GROUP = 0

        @JvmStatic
        protected val TYPE_CHILD = 1
    }

    private val expandedGroups = mutableSetOf<G>()

    init {
        // 初始化展开状态
        this.groups.forEach { group ->
            if (group.isInitiallyExpanded()) {
                expandedGroups.add(group)
                group.isExpanded = true
            }
        }
    }

    // ========== 公共 API ==========

    /**
     * 判断分组是否展开
     */
    fun isExpanded(group: G): Boolean = group in expandedGroups

    /**
     * 切换分组展开状态
     */
    fun toggleGroup(group: G) {
        if (group in expandedGroups) {
            expandedGroups.remove(group)
            group.isExpanded = false
        } else {
            expandedGroups.add(group)
            group.isExpanded = true
        }
        notifyDataSetChanged()
    }

    /**
     * 获取分组数量
     */
    fun getGroupCount(): Int = groups.size

    /**
     * 获取指定分组的子项数量
     */
    fun getChildCount(groupPosition: Int): Int {
        if (groupPosition < 0 || groupPosition >= groups.size) return 0
        val group = groups[groupPosition]
        return if (isExpanded(group)) group.children.size else 0
    }

    // ========== 通知方法（兼容原有 API）==========

    /**
     * 通知子项插入
     */
    fun notifyChildInserted(groupPosition: Int, childPosition: Int) {
        notifyDataSetChanged()
    }

    /**
     * 通知子项移除
     */
    fun notifyChildRemoved(groupPosition: Int, childPosition: Int) {
        notifyDataSetChanged()
    }

    /**
     * 通知子项更新
     */
    fun notifyChildChanged(groupPosition: Int, childPosition: Int) {
        notifyDataSetChanged()
    }

    /**
     * 通知分组更新
     */
    fun notifyParentChanged(groupPosition: Int) {
        notifyDataSetChanged()
    }

    // ========== RecyclerView.Adapter 实现 ==========

    override fun getItemCount(): Int {
        var count = 0
        groups.forEach { group ->
            count++ // 分组标题
            if (isExpanded(group)) {
                count += group.children.size
            }
        }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        val result = findItemPosition(position)
        return if (result.isGroup) TYPE_GROUP else TYPE_CHILD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_GROUP) {
            onCreateGroupViewHolder(parent, viewType)
        } else {
            onCreateChildViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val result = findItemPosition(position)
        if (result.isGroup) {
            @Suppress("UNCHECKED_CAST")
            onBindGroupViewHolder(holder as GVH, groups[result.groupPosition], result.groupPosition)
        } else {
            @Suppress("UNCHECKED_CAST")
            val group = groups[result.groupPosition]
            onBindChildViewHolder(
                holder as CVH,
                group.children[result.childPosition],
                result.groupPosition,
                result.childPosition
            )
        }
    }

    // ========== 抽象方法（子类实现）==========

    /**
     * 创建分组 ViewHolder
     */
    protected abstract fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): GVH

    /**
     * 创建子项 ViewHolder
     */
    protected abstract fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): CVH

    /**
     * 绑定分组数据
     */
    protected abstract fun onBindGroupViewHolder(holder: GVH, group: G, groupPosition: Int)

    /**
     * 绑定子项数据
     */
    protected abstract fun onBindChildViewHolder(holder: CVH, child: C, groupPosition: Int, childPosition: Int)

    // ========== 内部辅助类和方法 ==========

    private data class PositionResult(
        val isGroup: Boolean,
        val groupPosition: Int,
        val childPosition: Int
    )

    private fun findItemPosition(position: Int): PositionResult {
        var currentPosition = position
        for (groupIndex in groups.indices) {
            val group = groups[groupIndex]
            // 检查是否是分组标题位置
            if (currentPosition == 0) {
                return PositionResult(true, groupIndex, -1)
            }
            currentPosition--
            // 检查是否在展开的子项中
            if (isExpanded(group)) {
                if (currentPosition < group.children.size) {
                    return PositionResult(false, groupIndex, currentPosition)
                }
                currentPosition -= group.children.size
            }
        }
        return PositionResult(true, 0, -1)
    }
}
