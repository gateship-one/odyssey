/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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

    /**
     * Permission safe call of the query method of the content resolver.
     *
     * @return A cursor object which will be null if the user not granted the necessary permissions.
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    /**
     * Permission safe call of the delete method of the content resolver.
     */
    public static void delete(Context context, Uri uri, String where, String[] selectionArgs) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            context.getContentResolver().delete(uri, where, selectionArgs);
        }
    }

    /**
     * Permission safe call of the insert method of the content resolver.
     *
     * @return The Url of the created row or null if the user not granted the necessary permissions.
     */
    public static Uri insert(Context context, Uri uri, ContentValues contentValues) {
        Uri row = null;

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            row = context.getContentResolver().insert(uri, contentValues);
        }

        return row;
    }

    /**
     * Permission safe call of the bulkInsert method of the content resolver.
     *
     * @return The number of inserted rows or -1 if the user not granted the necessary permissions.
     */
    public static int bulkInsert(Context context, Uri uri, ContentValues[] values) {
        int rows = -1;

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            rows = context.getContentResolver().bulkInsert(uri, values);
        }

        return rows;
    }
}
