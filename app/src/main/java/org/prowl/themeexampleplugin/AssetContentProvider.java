package org.prowl.themeexampleplugin;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;

public class AssetContentProvider extends ContentProvider {

    private static final String ASSET = "content://"+BuildConfig.APPLICATION_ID+"/assets/";

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        Debug.w("Remote requesting uri:"+ uri.toString() + "  access mode:" + mode);

        AssetManager am = getContext().getAssets();
        String fileName = uri.toString();
        fileName = fileName.substring(ASSET.length());
        if (fileName == null) {
            throw new FileNotFoundException();
        }
        AssetFileDescriptor afd = null;
        try {
            afd = am.openFd(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return afd;
    }

}