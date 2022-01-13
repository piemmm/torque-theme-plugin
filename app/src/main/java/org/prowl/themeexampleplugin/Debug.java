package org.prowl.themeexampleplugin;

import android.util.Log;

import org.prowl.themeexampleplugin.annotations.RemovableInRelease;

public class Debug {

    private static final String LAZY = "themePlugin";

    @RemovableInRelease
    public static final void w(String message) {
        if (BuildConfig.DEBUG) {
            Log.w(LAZY, message);
        }
    }

    @RemovableInRelease
    public static final void e(String message, Throwable e) {
        if (BuildConfig.DEBUG) {
            Log.e(LAZY, message, e);
        }
    }
}
