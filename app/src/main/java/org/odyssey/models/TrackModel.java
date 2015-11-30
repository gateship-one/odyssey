package org.odyssey.models;

public class TrackModel {

    private String mTrackName;
    private String mTrackArtistName;
    private String mTrackAlbumName;
    private String mTrackAlbumKey;
    private long mTrackDuration;
    private int mTrackNumber;

    public TrackModel(String name, String artistName, String albumName, String albumKey, long duration, int trackNumber) {
        mTrackName = name;
        mTrackArtistName = artistName;
        mTrackAlbumName = albumName;
        mTrackAlbumKey = albumKey;
        mTrackDuration = duration;
        mTrackNumber = trackNumber;
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
}
