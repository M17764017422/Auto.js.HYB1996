package org.autojs.autojs.ui.edit.theme

import android.content.Context
import com.stardust.pio.UncheckedIOException
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.autojs.autojs.Pref
import org.autojs.autojs.ui.util.isSystemNightMode
import java.io.IOException
import java.io.InputStreamReader

/**
 * 主题管理器
 * Created by Stardust on 2018/2/22.
 */
object Themes {

    private const val ASSETS_THEMES_PATH = "editor/theme"
    private const val DEFAULT_THEME = "Quiet Light"
    private const val DARK_THEME = "Dark (Visual Studio)"

    private var themes: List<Theme>? = null
    private var defaultTheme: Theme? = null

    @JvmStatic
    fun getAllThemes(context: Context): Observable<List<Theme>> {
        themes?.let { return Observable.just(it) }

        val subject = PublishSubject.create<List<Theme>>()
        getAllThemesInner(context)
            .subscribeOn(Schedulers.io())
            .subscribe({ themeList ->
                setThemes(themeList)
                subject.onNext(themes!!)
                subject.onComplete()
            }, { it.printStackTrace() })
        return subject
    }

    @JvmStatic
    fun getDefault(context: Context): Observable<Theme> {
        defaultTheme?.let { return Observable.just(it) }
        return getAllThemes(context).map { defaultTheme!! }
    }

    @Synchronized
    private fun setThemes(themeList: List<Theme>) {
        if (themes != null) return
        themes = themeList.toList()
        for (theme in themes!!) {
            if (DEFAULT_THEME == theme.name) {
                defaultTheme = theme
                return
            }
        }
        defaultTheme = themes!![0]
    }

    private fun getAllThemesInner(context: Context): Observable<List<Theme>> {
        themes?.let { return Observable.just(it) }
        return try {
            Observable.fromArray(*context.assets.list(ASSETS_THEMES_PATH))
                .map { file -> context.assets.open("$ASSETS_THEMES_PATH/$file") }
                .map { stream -> Theme.fromJson(InputStreamReader(stream))!! }
                .toList()
                .toObservable()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    @JvmStatic
    fun getCurrent(context: Context): Observable<Theme> {
        // 检测系统夜间模式
        val currentTheme = if (Pref.isNightModeEnabled() || context.isSystemNightMode()) {
            DARK_THEME
        } else {
            Pref.getCurrentTheme()
        }

        if (currentTheme == null) return getDefault(context)

        return getAllThemes(context).map { themeList ->
            themeList.find { currentTheme == it.name } ?: themeList[0]
        }
    }

    @JvmStatic
    fun setCurrent(name: String) {
        Pref.setCurrentTheme(name)
    }
}