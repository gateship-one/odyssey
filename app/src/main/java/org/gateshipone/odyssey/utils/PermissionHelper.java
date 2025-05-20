/*
 * Copyright (C) 2023 Team Gateship-One
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
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.FileModel;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionHelper {

    /**
     * Result code for the audio permission request.
     */
    public static final int MY_PERMISSIONS_REQUEST_MEDIA_AUDIO = 0;

    /**
     * Result code for the notifications permission request.
     */
    public static final int MY_PERMISSIONS_REQUEST_NOTIFICATIONS = 1;

    /**
     * Permission to access audio files. Depends on the android version.
     */
    public static final String AUDIO_PERMISSION = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

    /**
     * Permission to show notifications. Empty if android version is below 13.
     */
    public static final String NOTIFICATION_PERMISSION = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.POST_NOTIFICATIONS : "";

    /**
     * Resource id for the image permission rationale text.
     */
    @StringRes
    public static final int AUDIO_PERMISSION_RATIONALE_TEXT = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? R.string.permission_request_audio_snackbar_explanation : R.string.permission_request_storage_snackbar_explanation;

    /**
     * Resource id for the notifications permission rationale text.
     */
    @StringRes
    public static final int NOTIFICATION_PERMISSION_RATIONALE_TEXT = R.string.permission_request_notifications_snackbar_explanation;

    /**
     * Method to check if odyssey is allowed to access audio files.
     *
     * @param context The application context for the permission check.
     * @return True if access has been granted, false otherwise.
     */
    public static boolean isAudioFilesAccessAllowed(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Method to check if odyssey is allowed to show notifications (besides the media notification).
     *
     * @param context The application context for the permission check.
     * @return True if notifications are allowed or device is running below API level 33.
     */
    public static boolean areNotificationsAllowed(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return notificationManager.areNotificationsEnabled();
        } else {
            return true;
        }
    }

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

        if (isAudioFilesAccessAllowed(context)) {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        }

        return cursor;
    }

    /**
     * Permission safe call to get all music files in a given directory.
     *
     * @param context   The application context for the permission check.
     * @param directory The {@link FileModel} representing the parent directory.
     * @return The list of {@link FileModel} of all files in the given directory.
     */
    public static List<FileModel> getFilesForDirectory(final Context context, final FileModel directory) {
        List<FileModel> files = new ArrayList<>();

        if (isAudioFilesAccessAllowed(context)) {
            files = directory.listFilesSorted();
        }

        return files;
    }

    /**
     * Permission safe call to get all files for a given directory path.
     *
     * @param context       The application context for the permission check.
     * @param directoryPath The path to the directory.
     * @param filter        A {@link FilenameFilter} to filter only specific files.
     * @return A {@link List} of {@link File} in the directory that match the given {@link FilenameFilter}.
     */
    public static List<File> getFilesForDirectory(final Context context, final String directoryPath, final FilenameFilter filter) {
        List<File> filteredFiles = new ArrayList<>();

        final File directory = new File(directoryPath);

        if (isAudioFilesAccessAllowed(context)) {
            File[] files = directory.listFiles(filter);

            if (files != null) {
                filteredFiles = Arrays.asList(files);
            }
        }

        return filteredFiles;
    }
}
