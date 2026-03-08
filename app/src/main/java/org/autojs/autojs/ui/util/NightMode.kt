package org.autojs.autojs.ui.util

import android.content.Context
import android.content.res.Configuration

/**
 * 系统夜间模式检测扩展函数
 * 用于主题自动切换
 */

/**
 * 检测系统是否处于夜间模式
 * @return true 如果系统处于夜间模式
 */
fun Context.isSystemNightMode(): Boolean {
    val configuration = this.resources.configuration
    return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
