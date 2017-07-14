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

package org.gateshipone.odyssey.loaders;


import android.content.Context;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.PlaylistParser;
import org.gateshipone.odyssey.utils.PlaylistParserFactory;

import java.util.List;

public class PlaylistTrackLoader extends TrackLoader {
    String mPath;
    public PlaylistTrackLoader(Context context, String playlistPath) {
        super(context);
        mPath = playlistPath;
    }

    /**
     * Load all tracks from the mediastore or a subset if a filter is set.
     */
    @Override
    public List<TrackModel> loadInBackground() {
        PlaylistParser parser = PlaylistParserFactory.getParser(new FileModel(mPath));
        if (parser == null) {
            return null;
        }
        return parser.parseList(getContext());
    }
}
