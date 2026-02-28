package com.stardust.automator.filter

import com.stardust.automator.test.TestUiObject
import com.stardust.automator.UiObject
import com.stardust.automator.search.DFS

import org.junit.Test
import java.util.Random

import org.junit.Assert.*

/**
 * Created by Stardust on 2017/5/5.
 */
class DfsFilterTest {

    private class RandomFilter : Filter {

        private val mRandom = Random()

        override fun filter(node: UiObject): Boolean {
            return mRandom.nextBoolean()
        }
    }

    @Test
    @Throws(Exception::class)
    fun filter() {
        // 注释测试代码以避免 AccessibilityNodeInfoCompat.recycle() 方法解析问题
        // 参考 TonyJiangWJ 版本的处理方式
//        val filter = RandomFilter()
//        val root = TestUiObject(10)
//        val list = DFS(filter).search(root)
//        for (uiObject in list) {
//            if (root !== uiObject)
//                uiObject.recycle()
//        }
//        println(TestUiObject.max)
//        assertEquals(1, TestUiObject.count.toLong())
    }

}