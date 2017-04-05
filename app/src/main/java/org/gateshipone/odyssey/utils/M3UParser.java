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

package org.gateshipone.odyssey.utils;


import android.content.Context;
import android.util.Log;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class M3UParser extends PlaylistParser {
    private static final String TAG = M3UParser.class.getSimpleName();

    FileModel mFile;

    String mPathPrefix = "";

    public M3UParser(FileModel playlistFile) {
        mFile = playlistFile;
    }

    @Override
    public ArrayList<TrackModel> parseList(Context context) {
        FileReader fileReader;
        try {
            fileReader = new FileReader(mFile.getPath());
        } catch (FileNotFoundException e) {
            return null;
        }

        BufferedReader bufReader = new BufferedReader(fileReader);

        // Try to check if file paths in playlist are relativ or absolute
        String line = "";
        try {
            line = bufReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File tmpFile = new File(line);
        if ( !tmpFile.exists()) {
            String plPath = mFile.getPath();
            plPath = plPath.substring(0,plPath.lastIndexOf('/') );
            while(!plPath.isEmpty()) {
                tmpFile = new File (plPath + '/' + line);
                if ( !tmpFile.exists()) {
                    plPath = plPath.substring(0,plPath.lastIndexOf('/') );
                } else {
                    mPathPrefix = plPath;
                    break;
                }
            }

        }

        ArrayList<String> urls = new ArrayList<>();
        while(line != null) {
            String tmpUrl;
            if (!mPathPrefix.isEmpty()) {
                tmpUrl = mPathPrefix + '/' + line;
            } else {
                tmpUrl = line;
            }
            if (new File(tmpUrl).exists()) {
                urls.add(tmpUrl);
            }
            try {
                line = bufReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return createTrackModels(context,urls);
    }
}
