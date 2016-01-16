package org.odyssey.models;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackModel implements GenericModel, Parcelable {

    private String mTrackName;
    private String mTrackArtistName;
    private String mTrackAlbumName;
    private String mTrackAlbumKey;
    private String mTrackURL;
    private long mTrackDuration;
    private int mTrackNumber;

    public TrackModel(String name, String artistName, String albumName, String albumKey, long duration, int trackNumber, String url) {
        mTrackName = name;
        mTrackArtistName = artistName;
        mTrackAlbumName = albumName;
        mTrackAlbumKey = albumKey;
        mTrackDuration = duration;
        mTrackNumber = trackNumber;
        mTrackURL = url;
    }

    public TrackModel() {
        mTrackName = "";
        mTrackArtistName = "";
        mTrackAlbumName = "";
        mTrackAlbumKey = "";
        mTrackDuration = 0;
        mTrackNumber = 0;
        mTrackURL = "";
    }

    public String getTrackName() {
        return mTrackName;
    }

    public String getTrackArtistName() {
        return mTrackArtistName;
    }

    public String getTrackAlbumName() {
        return mTrackAlbumName;
    }

    public String getTrackAlbumKey() {
        return mTrackAlbumKey;
    }

    public long getTrackDuration() {
        return mTrackDuration;
    }

    public int getTrackNumber() {
        return mTrackNumber;
    }

    public String getTrackURL() { return mTrackURL; }

    @Override
    public String getSectionTitle() {
        return mTrackName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTrackName);
        dest.writeString(mTrackArtistName);
        dest.writeString(mTrackAlbumName);
        dest.writeString(mTrackURL);
        dest.writeInt(mTrackNumber);
        dest.writeLong(mTrackDuration);
        dest.writeString(mTrackAlbumKey);
    }

    public static Parcelable.Creator<TrackModel> CREATOR = new Creator<TrackModel>() {

        @Override
        public TrackModel[] newArray(int size) {
            return new TrackModel[size];
        }

        @Override
        public TrackModel createFromParcel(Parcel source) {
            String trackName = source.readString();
            String artistName = source.readString();
            String albumName = source.readString();
            String url = source.readString();
            int number = source.readInt();
            long duration = source.readLong();
            String albumKey = source.readString();

            TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url);
            return item;
        }
    };
}
