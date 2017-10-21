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

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.TrackModel;

import java.util.ArrayList;

public abstract class PlaylistParser {

    protected ArrayList<TrackModel> createTrackModels(Context context, ArrayList<String> urls) {
        ArrayList<TrackModel> retList = new ArrayList<>();

        for (String url: urls) {
            FileModel tmpFile = new FileModel(url);

            TrackModel tmpModel = FileExplorerHelper.getInstance().getTrackModelForFile(context, tmpFile);
            retList.add(tmpModel);
        }

        return retList;
    }

    public abstract ArrayList<TrackModel> parseList(Context context);
}
