package org.autojs.autojs.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatDelegate;

import com.stardust.app.GlobalAppContext;

import org.autojs.autojs.R;

import com.stardust.theme.ThemeColor;
import com.stardust.theme.ThemeColorManager;

/**
 * Created by Stardust on 2017/3/12.
 */

public class ThemeColorManagerCompat {

    private static SharedPreferences sSharedPreferences;
    private static Context sContext;
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferenceChangeListener = (sharedPreferences, key) -> {
        if (key.equals(sContext.getString(R.string.key_night_mode))) {
            setNightModeEnabled(sharedPreferences.getBoolean(key, false));
        }
    };

    public static int getColorPrimary() {
        int color = ThemeColorManager.getColorPrimary();
        if (color == 0) {
            return GlobalAppContext.get().getResources().getColor(R.color.colorPrimary);
        } else {
            return color;
        }
    }

    public static void setNightModeEnabled(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            // 保持用户选择的主题色，不再强制切换为黑色主题
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static void init(Context context, ThemeColor defaultThemeColor) {
        sContext = context;
        sSharedPreferences = context.getSharedPreferences("theme_color", Context.MODE_PRIVATE);
        ThemeColorManager.setDefaultThemeColor(defaultThemeColor);
        ThemeColorManager.init(context);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
    }
}
