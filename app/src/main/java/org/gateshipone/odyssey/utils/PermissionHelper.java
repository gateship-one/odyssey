/*
 * Copyright (C) 2018 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
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

package org.gateshipone.odyssey.utils;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import org.gateshipone.odyssey.models.FileModel;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

public class PermissionHelper {

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

    /**
     * Permission safe call of the query method of the content resolver.
     *
     * @param context       The application context for the permission check and the access of the content resolver.
     * @param uri           The URI, using the content:// scheme, for the content to
     *                      retrieve.
     * @param projection    A list of which columns to return. Passing null will
     *                      return all columns, which is inefficient.
     * @param selection     A filter declaring which rows to return, formatted as an
     *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
     *                      return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *                      replaced by the values from selectionArgs, in the order that they
     *                      appear in the selection. The values will be bound as Strings.
     * @param sortOrder     How to order the rows, formatted as an SQL ORDER BY
     *                      clause (excluding the ORDER BY itself). Passing null will use the
     *                      default sort order, which may be unordered.
     * @return A {@link Cursor} which is positioned before the first entry, or null if the user not granted the necessary permissions.
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    /**
     * Permission safe call of the delete method of the content resolver.
     *
     * @param context       The application context for the permission check and the access of the content resolver.
     * @param uri           The URL of the row to delete.
     * @param where         A filter to apply to rows before deleting, formatted as an SQL WHERE clause
     *                      (excluding the WHERE itself).
     * @param selectionArgs Additional values for the where clause.
     * @return The number of deleted rows.
     */
    public static int delete(final Context context, final Uri uri, final String where, final String[] selectionArgs) {
        int rows = -1;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            rows = context.getContentResolver().delete(uri, where, selectionArgs);
        }

        return rows;
    }

    /**
     * Permission safe call of the insert method of the content resolver.
     *
     * @param context       The application context for the permission check and the access of the content resolver.
     * @param uri           The {@link Uri} of the table to insert into.
     * @param contentValues The values for the row.
     * @return The Url of the created row or null if the user not granted the necessary permissions.
     */
    public static Uri insert(final Context context, final Uri uri, final ContentValues contentValues) {
        Uri row = null;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            row = context.getContentResolver().insert(uri, contentValues);
        }

        return row;
    }

    /**
     * Permission safe call of the bulkInsert method of the content resolver.
     *
     * @param context The application context for the permission check and the access of the content resolver.
     * @param uri     The {@link Uri} of the table to insert into.
     * @param values  The values for the rows.
     * @return The number of created rows.
     */
    public static int bulkInsert(final Context context, final Uri uri, final ContentValues[] values) {
        int rows = -1;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            rows = context.getContentResolver().bulkInsert(uri, values);
        }

        return rows;
    }

    /**
     * Permission safe call to get all files in a given directory.
     *
     * @param context   The application context for the permission check.
     * @param directory The {@link FileModel} representing the parent directory.
     * @return The list of {@link FileModel} of all files in the given directory.
     */
    public static List<FileModel> getFilesForDirectory(final Context context, final FileModel directory) {
        List<FileModel> files = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            files = directory.listFilesSorted();
        }

        return files;
    }
}
