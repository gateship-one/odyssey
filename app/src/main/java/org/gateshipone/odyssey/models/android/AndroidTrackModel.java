/*
 * Copyright (C) 2019 Team Gateship-One
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

package org.gateshipone.odyssey.models.android;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.gateshipone.odyssey.database.MusicDatabaseFactory;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.FormatHelper;

import java.io.FileInputStream;

public class AndroidTrackModel extends TrackModel {
    /**
     * The unique key of the album of the track
     */
    private String mTrackAlbumKey;

    /**
     * The unique id of the track in the mediastore
     */
    private long mTrackId;

    public AndroidTrackModel(@NonNull String name, @NonNull String artistName, @NonNull String albumName, long duration, int trackNumber, @NonNull String url, int dateAdded,
                             @NonNull String trackKey, long trackId) {
        super(name, artistName, albumName, duration, trackNumber, url, dateAdded);

        mTrackAlbumKey = trackKey;
        mTrackId = trackId;
    }

    @Override
    public boolean sameAlbum(AlbumModel albumModel) {
        if (!(albumModel instanceof AndroidAlbumModel)) {
            return false;
        }
        return mTrackAlbumKey.equals(((AndroidAlbumModel)albumModel).getAlbumKey());
    }

    @Override
    public boolean sameAlbum(TrackModel trackModel) {
        if (!(trackModel instanceof AndroidTrackModel)) {
            return false;
        }
        return mTrackAlbumKey.equals(((AndroidTrackModel)trackModel).getTrackAlbumKey());
    }


    /**
     * Constructs a TrackModel from a Parcel.
     * <p>
     * see {@link Parcelable}
     */
    protected AndroidTrackModel(Parcel in) {
        super(in);

        mTrackAlbumKey = in.readString();
        mTrackId = in.readLong();
    }

    /**
     * Flatten this object in to a Parcel.
     * <p/>
     * see {@link Parcelable}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(mTrackAlbumKey);
        dest.writeLong(mTrackId);
    }

    /**
     * Return the unique album key
     */
    public String getTrackAlbumKey() {
        return mTrackAlbumKey;
    }

    /**
     * Return the unique id of the track
     */
    public long getTrackId() {
        return mTrackId;
    }

    /**
     * Equals method for the TrackModel
     * <p/>
     * TrackModel instances are equal if they have the same id
     */
    @Override
    public boolean equals(Object model) {
        if (model == null) {
            return false;
        }
        if (model == this) {
            return true;
        }
        if (!(model instanceof TrackModel)) {
            return false;
        }
        AndroidTrackModel track = (AndroidTrackModel) model;

        return (this.mTrackId == track.mTrackId);
    }

    @Override
    public boolean hasAlbum() {
        return !mTrackAlbumKey.isEmpty();
    }

    @Override
    public void fillMetadata(Context context) {
        // parse the given url
        final Uri uri = FormatHelper.encodeURI(mTrackURL);

        // lookup the current file in the media db
        final AndroidTrackModel track = (AndroidTrackModel)MusicDatabaseFactory.getDatabase(context).getTrackForUri(uri, context);

        if (track != null) {
            mTrackAlbumName = track.mTrackAlbumName;
            mTrackArtistName = track.mTrackArtistName;
            mTrackDuration = track.mTrackDuration;
            mTrackName = track.mTrackName;
            mTrackNumber = track.mTrackNumber;
            mTrackURL = track.mTrackURL;
            mTrackId = track.mTrackId;
            mTrackAlbumKey = track.mTrackAlbumKey;
        }

        try {
            // try to read the file metadata
            final MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            FileInputStream fileInputStream = new FileInputStream(mTrackURL);
            retriever.setDataSource(fileInputStream.getFD());
            fileInputStream.close();

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

            mTrackAlbumName = album;
            mTrackArtistName = artist;
            mTrackDuration = duration;
            mTrackName = title;
            mTrackNumber = no;
            mTrackAlbumKey = albumKey;
        } catch (Exception e) {
        }
        mNoMetaData = false;
    }
}
