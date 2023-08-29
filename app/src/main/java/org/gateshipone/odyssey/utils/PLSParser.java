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
import android.net.Uri;

import org.gateshipone.odyssey.models.FileModel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PLSParser extends PlaylistParser {
    private static final String TAG = PLSParser.class.getSimpleName();


    public PLSParser(FileModel file) {
        super(file);
    }

    @Override
    public ArrayList<String> getFileURLsFromFile(Context context) {
        Uri uri = FormatHelper.encodeURI(mFile.getPath());
        InputStream inputStream;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        if (null == inputStream) {
            return new ArrayList<>();
        }

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = "";

        ArrayList<String> urls = new ArrayList<>();
        do {
            try {
                line = bufReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (line == null || !line.startsWith("File")) {
                // ignore those lines
                continue;
            }

            urls.add(line.substring(line.indexOf('=') + 1));
        } while (line != null);


        return urls;
    }
}
