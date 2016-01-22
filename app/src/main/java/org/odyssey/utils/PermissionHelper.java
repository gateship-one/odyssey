package org.odyssey.utils;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;

public class PermissionHelper {

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

    public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    public static void delete(Context context, Uri uri, String where, String[] selectionArgs) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            context.getContentResolver().delete(uri, where, selectionArgs);
        }
    }

    public static Uri insert(Context context, Uri uri, ContentValues contentValues) {
        Uri row = null;

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            row = context.getContentResolver().insert(uri, contentValues);
        }

        return row;
    }
}
