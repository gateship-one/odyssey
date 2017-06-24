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
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.gateshipone.odyssey.models.TrackModel;

import java.util.List;
import java.util.ListIterator;

/**
 * Helper to load meta data of tracks async.
 */
public class MetaDataLoader {

    public interface MetaDataLoaderListener {
        void metaDataLoaderFinished();
    }

    private final MetaDataLoaderListener mMetaDataLoaderListener;

    public MetaDataLoader(final MetaDataLoaderListener metaDataLoaderListener) {
        mMetaDataLoaderListener = metaDataLoaderListener;
    }

    /**
     * Updates the meta data of the tracks in the given list.
     *
     * @param context The {@link Context} used to open the file and access the mediadb.
     * @param tracks  The track list. The elements of the list will be replaced.
     */
    public void getTrackListMetaData(final Context context, final List<TrackModel> tracks) {
        if (null == tracks || tracks.isEmpty()) {
            return;
        }

        Thread loaderThread = new Thread(new TrackListMetaDataExtractorRunner(context, tracks));
        loaderThread.start();
    }

    private class TrackListMetaDataExtractorRunner implements Runnable {

        private final Context mContext;

        private final List<TrackModel> mTracks;

        TrackListMetaDataExtractorRunner(final Context context, final List<TrackModel> tracks) {
            mContext = context;
            mTracks = tracks;
        }

        @Override
        public void run() {
            // TODO for testing purposes
            // prevent the ui from receiving to many update calls
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean tracksChanged = false;

            ListIterator<TrackModel> iterator = mTracks.listIterator();

            while (iterator.hasNext()) {
                final TrackModel track = iterator.next();
                if (TextUtils.isEmpty(track.getTrackAlbumKey())) {
                    iterator.set(readTrackMetaData(mContext, track.getTrackName(), track.getTrackURL()));
                    tracksChanged = true;
                }
            }

            if (tracksChanged) {
                mMetaDataLoaderListener.metaDataLoaderFinished();
            }
        }
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
        Uri uri = FormatHelper.encodeURI(trackUrl);

        String whereVal[] = {uri.getPath()};

        String where = MediaStore.Audio.Media.DATA + "=?";

        if (uri.getScheme().equals("content")) {
            // special handling for content urls
            final String parts[] = uri.getLastPathSegment().split(":");

            if (parts.length > 1) {
                if (parts[0].equals("audio")) {
                    whereVal = new String[]{parts[1]};
                    where = MediaStore.Audio.Media._ID + "=?";
                } else {
                    whereVal = new String[]{"%" + parts[1]};
                    where = MediaStore.Audio.Media.DATA + " LIKE ?";
                }
            }
        }

        // lookup the current file in the media db
        Cursor cursor = PermissionHelper.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, where, whereVal, MediaStore.Audio.Media.TRACK);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                int no = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                cursor.close();

                return new TrackModel(title, artist, album, albumKey, duration, no, url, id);
            }

            cursor.close();
        }

        try {
            // try to read the file metadata

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            retriever.setDataSource(context, uri);

            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            long duration = 0;

            if (durationString != null) {
                try {
                    duration = Long.valueOf(durationString);
                } catch (NumberFormatException e) {
                    duration = 0;
                }
            }

            String noString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);

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

            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            String albumKey = "" + ((artist == null ? "" : artist) + (album == null ? "" : album)).hashCode();

            return new TrackModel(title, artist, album, albumKey, duration, no, trackUrl, -1);
        } catch (Exception e) {
            // something went wrong so just create a dummy track with the given title
            String albumKey = "" + trackTitle.hashCode();
            return new TrackModel(trackTitle, null, null, albumKey, 0, -1, trackUrl, -1);
        }
    }
}
