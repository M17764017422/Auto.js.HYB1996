package com.stardust.autojs.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.stardust.autojs.R;
import com.stardust.enhancedfloaty.util.FloatingWindowPermissionUtil;

/**
 * Created by Stardust on 2018/1/30.
 */

public class FloatingPermission {

    public static boolean ensurePermissionGranted(Context context) {
        if (!canDrawOverlays(context)) {
            Toast.makeText(context, R.string.text_no_floating_window_permission, Toast.LENGTH_SHORT).show();
            manageDrawOverlays(context);
            return false;
        }
        return true;
    }

    public static void waitForPermissionGranted(Context context) throws InterruptedException {
        if (canDrawOverlays(context)) {
            return;
        }
        Runnable r = () -> {
            manageDrawOverlays(context);
            Toast.makeText(context, R.string.text_no_floating_window_permission, Toast.LENGTH_SHORT).show();
        };
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(r);
        } else {
            r.run();
        }
        while (true) {
            if (canDrawOverlays(context))
                return;
            Thread.sleep(200);
        }
    }

    public static void manageDrawOverlays(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                // Pre-Marshmallow: try app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Exception ex) {
            FloatingWindowPermissionUtil.goToAppDetailSettings(context, context.getPackageName());
        }
    }

    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        // Pre-Marshmallow: always return true (permission is granted at install time)
        return true;
    }
}
