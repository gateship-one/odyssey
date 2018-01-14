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

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.gateshipone.odyssey.models.TrackModel;

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

        HashMap<String, String> unknownTracks = new HashMap<>();

        for (TrackModel track : tracks) {
            if (TextUtils.isEmpty(track.getTrackAlbumKey())) {
                // add only tracks with an empty albumkey
                unknownTracks.put(track.getTrackURL(), track.getTrackName());
            }
        }

        Thread loaderThread = new Thread(new TrackListMetaDataExtractorRunner(context, unknownTracks));
        loaderThread.start();
    }

    /**
     * Create a {@link TrackModel} for the given url.
     * <p>
     * This method will try to retrieve the track in the mediadb or try to extract the meta data using the {@link MediaMetadataRetriever}.
     * If both methods fail a dummy {@link TrackModel} will be created.
     *
     * @param context    The {@link Context} used to open the file and access the mediadb.
     * @param trackTitle The title for the {@link TrackModel} if a dummy track is created.
     * @param trackUrl   The given url for track as a String.
     * @return A valid {@link TrackModel}.
     */
    private TrackModel readTrackMetaData(final Context context, final String trackTitle, final String trackUrl) {
        // parse the given url
        final Uri uri = FormatHelper.encodeURI(trackUrl);

        // lookup the current file in the media db
        final TrackModel track = MusicLibraryHelper.getTrackForUri(uri, context);

        if (track != null) {
            return track;
        }

        try {
            // try to read the file metadata

            final MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            retriever.setDataSource(context, uri);

            final String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            final String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            long duration = 0;

            if (durationString != null) {
                try {
                    duration = Long.valueOf(durationString);
                } catch (NumberFormatException e) {
                    duration = 0;
                }
            }

            final String noString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);

            int no = -1;

            if (noString != null) {
                try {
                    if (noString.contains("/")) {
                        // if string has the format (trackNumber / numberOfTracks)
                        String[] components = noString.split("/");
                        if (components.length > 0) {
                            no = Integer.valueOf(components[0]);
                        }
                    } else {
                        no = Integer.valueOf(noString);
                    }
                } catch (NumberFormatException e) {
                    no = -1;
                }
            }

            final String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            final String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            final String albumKey = "" + ((artist == null ? "" : artist) + (album == null ? "" : album)).hashCode();

            return new TrackModel(title, artist, album, albumKey, duration, no, trackUrl, -1);
        } catch (Exception e) {
            // something went wrong so just create a dummy track with the given title
            final String albumKey = "" + trackTitle.hashCode();
            return new TrackModel(trackTitle, null, null, albumKey, 0, -1, trackUrl, -1);
        }
    }

    private class TrackListMetaDataExtractorRunner implements Runnable {

        private final Context mContext;

        private final Map<String, String> mUnknownTracks;

        TrackListMetaDataExtractorRunner(final Context context, final Map<String, String> unknownTracks) {
            mContext = context;
            mUnknownTracks = unknownTracks;
        }

        @Override
        public void run() {
            Map<String, TrackModel> mParsedTracks = new HashMap<>();

            for (Map.Entry<String, String> unknownTrack : mUnknownTracks.entrySet()) {
                mParsedTracks.put(unknownTrack.getKey(), readTrackMetaData(mContext, unknownTrack.getValue(), unknownTrack.getKey()));
            }

            mMetaDataLoaderListener.metaDataLoaderFinished(mParsedTracks);
        }
    }
}
