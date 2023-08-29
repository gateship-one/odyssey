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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class FileUtils {

    /**
     * The directory name for all artwork images and subfolders
     */
    private static final String ARTWORK_DIR = "artworks";

    private static final String MEDIA_AUTHORITY = "com.android.providers.media.documents";

    private static final String DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents";

    private static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

    private static final String CONTENT_SCHEME = "content";

    private static final String FILE_SCHEME = "file";

    private static final String AUDIO_MEDIA_TYPE = "audio";

    private static final String RAW_TYPE = "raw";

    /**
     * Create a SHA256 Hash for the given input strings.
     *
     * @param inputStrings The input that will be used as a concatenated string to create the hashed value.
     * @return The result as a hex string.
     * @throws NoSuchAlgorithmException If SHA-256 is not available.
     */
    public static String createSHA256HashForString(final String... inputStrings) throws NoSuchAlgorithmException {
        final StringBuilder input = new StringBuilder();

        for (String string : inputStrings) {
            if (string != null) {
                input.append(string);
            }
        }

        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input.toString().getBytes());

        final byte[] bytes = md.digest();

        final StringBuilder hexString = new StringBuilder();
        for (byte oneByte : bytes) {
            final String hex = Integer.toHexString(0xff & oneByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Saves an image byte array in a file in the given directory.
     *
     * @param context  The application context to get the files directory of the app.
     * @param fileName The name that will be used to save the file.
     * @param dirName  The directory name in which the file is saved.
     * @param image    The image byte array that will be saved in a file.
     * @throws IOException If the file couldn't be written.
     */
    public static void saveArtworkFile(final Context context, final String fileName, final String dirName, final byte[] image) throws IOException {
        final File artworkDir = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/");
        artworkDir.mkdirs();

        final File imageFile = new File(artworkDir, fileName);

        final FileOutputStream outputStream = new FileOutputStream(imageFile);
        outputStream.write(image);
        outputStream.close();
    }

    /**
     * Generates the full absolute file path for an artwork image
     *
     * @param context  Context used for directory resolving
     * @param fileName Filename used as a basis
     * @param dirName  Directory suffix
     * @return Full absolute file path
     */
    public static String getFullArtworkFilePath(final Context context, final String fileName, final String dirName) {
        return context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/" + fileName;
    }

    /**
     * Removes a file from the given directory.
     *
     * @param context  The application context to get the files directory of the app.
     * @param fileName The name of the file.
     * @param dirName  The name of the parent directory of the file.
     */
    public static void removeArtworkFile(final Context context, final String fileName, final String dirName) {
        final File artworkFile = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/" + fileName);
        artworkFile.delete();
    }

    /**
     * Removes the given directory.
     *
     * @param context The application context to get the files directory of the app.
     * @param dirName The name of the directory that should be removed.
     */
    public static void removeArtworkDirectory(final Context context, final String dirName) {
        final File artworkDir = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/");

        final File[] files = artworkDir.listFiles();
        if (files != null) {
            for (File child : files) {
                child.delete();
            }
            artworkDir.delete();
        }
    }

    /**
     * Currently only this solutions seems to work properly we should investigate this more and change this.
     *
     * @param context The application context.
     * @param uri     The given {@link Uri}.
     * @return The extracted file path or null if the given {@link Uri} is not supported.
     */
    public static String getFilePathFromUri(final Context context, final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // handle document uri

            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":", 2);

            if (split.length < 2) {
                return null;
            }

            final String type = split[0];

            if (isExternalStorageDocument(uri)) {
                // handle external storage uri

                final List<String> storageLocations = FileExplorerHelper.getInstance().getStorageVolumes(context);

                for (final String storageLocation : storageLocations) {
                    final String potentialFilePath = storageLocation + "/" + split[1];

                    final File file = new File(potentialFilePath);

                    if (file.exists()) {
                        return potentialFilePath;
                    }
                }

                return null;
            } else if (isDownloadsDocument(uri)) {
                // handle downloads uri

                if (RAW_TYPE.equalsIgnoreCase(type)) {
                    // handle raw uri

                    final String potentialFilePath = split[1];

                    final File file = new File(potentialFilePath);

                    if (file.exists()) {
                        return potentialFilePath;
                    } else {
                        return null;
                    }
                } else {
                    // TODO any other case is currently not supported
                    return null;
                }
            } else if (isMediaDocument(uri)) {
                // handle media uri

                if (AUDIO_MEDIA_TYPE.equalsIgnoreCase(type)) {
                    // extract path with audio content uri
                    return extractPathFromUri(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "_id=?", new String[]{split[1]});
                } else {
                    // we only support audio uri

                    return null;
                }
            }
        }

        return extractPathFromUri(context, uri, null, null);
    }

    /**
     * Method to extract a path from a given uri with optional arguments.
     *
     * @param context       The application context.
     * @param uri           The given {@link Uri}.
     * @param selection     Optional selection statement.
     * @param selectionArgs Optional selection arguments.
     * @return The extracted path or null if the scheme of the uri is not supported.
     */
    private static String extractPathFromUri(final Context context, final Uri uri, final String selection, final String[] selectionArgs) {
        if (CONTENT_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            // handle content uri

            String[] projection = {MediaStore.Audio.Media.DATA};

            final Cursor cursor = PermissionHelper.query(context, uri, projection, selection, selectionArgs, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    final int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                    final String filePath = cursor.getString(columnIndex);

                    cursor.close();

                    return filePath;
                }
            }

            return null;
        } else if (FILE_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            // handle file uri

            return uri.getPath();
        } else {
            // currently we don't support any other uri scheme

            return null;
        }
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return EXTERNAL_STORAGE_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return DOWNLOADS_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return MEDIA_AUTHORITY.equals(uri.getAuthority());
    }
}
