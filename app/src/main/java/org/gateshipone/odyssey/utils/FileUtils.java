/*
 * Copyright (C) 2017 Team Gateship-One
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
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    private static final String ARTWORK_DIR = "artworks";

    public static String createSHA256HashForString(final String... inputStrings) throws NoSuchAlgorithmException {
        final StringBuilder input = new StringBuilder();

        for (String string : inputStrings) {
            if (string != null) {
                input.append(string);
            }
        }

        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input.toString().getBytes());

        final byte bytes[] = md.digest();

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

    public static void saveArtworkFile(final Context context, final String fileName, final String dirName, final byte[] image) throws IOException {
        final File artworkDir = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/");
        artworkDir.mkdirs();

        final File imageFile = new File(artworkDir, fileName);

        final FileOutputStream outputStream = new FileOutputStream(imageFile);
        outputStream.write(image);
        outputStream.close();
    }

    public static byte[] readArtworkFile(final Context context, final String fileName, final String dirName) {
        final File artworkFile = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/" + fileName);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(artworkFile);

            byte[] buffer = new byte[(int) artworkFile.length()];
            inputStream.read(buffer);
            inputStream.close();

            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void removeArtworkFile(final Context context, final String fileName, final String dirName) {
        final File artworkFile = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/" + fileName);
        artworkFile.delete();
    }

    public static void removeArtworkDirectory(final Context context, final String dirName) {
        final File artworkDir = new File(context.getFilesDir() + "/" + ARTWORK_DIR + "/" + dirName + "/");

        if (artworkDir.listFiles() != null) {
            for (File child : artworkDir.listFiles()) {
                child.delete();
            }
            artworkDir.delete();
        }
    }
}
