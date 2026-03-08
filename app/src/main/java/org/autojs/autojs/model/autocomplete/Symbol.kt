package org.autojs.autojs.model.autocomplete

/**
 * 符号补全
 * Created by Stardust on 2017/9/28.
 */
object Symbol {

    @JvmStatic
    val symbols: CodeCompletions = CodeCompletions.just(
        listOf(
            "\"", "(", ")", "=", ";", "/", "{", "}", "!", "|", "&", "-",
            "[", "]", "+", "-", "<", ">", "\\", "*", "?"
        )
    )
}