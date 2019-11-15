/*
 * Copyright (C) 2020 Team Gateship-One
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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;

import org.gateshipone.odyssey.database.MusicDatabaseFactory;
import org.gateshipone.odyssey.models.TrackModel;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper to load meta data of tracks async.
 */
public class MetaDataLoader {

    public interface MetaDataLoaderListener {
        void metaDataLoaderFinished(Map<String, TrackModel> parsedTracks);
    }

    private final MetaDataLoaderListener mMetaDataLoaderListener;

    public MetaDataLoader(final MetaDataLoaderListener metaDataLoaderListener) {
        mMetaDataLoaderListener = metaDataLoaderListener;
    }

    /**
     * Updates the meta data of the tracks in the given list.
     *
     * @param context The {@link Context} used to open the file and access the mediadb.
     * @param tracks  The track list to check for unknown tracks.
     */
    public void getTrackListMetaData(final Context context, final List<TrackModel> tracks) {
        if (null == tracks || tracks.isEmpty()) {
            return;
        }

        HashMap<String, TrackModel> unknownTracks = new HashMap<>();

        for (TrackModel track : tracks) {
            if (track.hasNoMetaData()) {
                // add only tracks with no metadata
                unknownTracks.put(track.getTrackURL(), track);
            }
        }

        Thread loaderThread = new Thread(new TrackListMetaDataExtractorRunner(context, unknownTracks));
        loaderThread.start();
    }

    private class TrackListMetaDataExtractorRunner implements Runnable {

        private final Context mContext;

        private final Map<String, TrackModel> mUnknownTracks;

        TrackListMetaDataExtractorRunner(final Context context, final Map<String, TrackModel> unknownTracks) {
            mContext = context;
            mUnknownTracks = unknownTracks;
        }

        @Override
        public void run() {
            Map<String, TrackModel> mParsedTracks = new HashMap<>();

            for (Map.Entry<String, TrackModel> unknownTrack : mUnknownTracks.entrySet()) {
                TrackModel track = unknownTrack.getValue();
                track.fillMetadata(mContext);
                mParsedTracks.put(unknownTrack.getKey(), track);
            }

            mMetaDataLoaderListener.metaDataLoaderFinished(mParsedTracks);
        }
    }
}
