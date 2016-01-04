package org.odyssey.models;

public class ArtistModel implements GenericModel {

    private String mArtistName;
    private String mArtistURL;
    private String mArtistKey;
    private long mArtistID;

    public ArtistModel(String name, String artURL, String artistKey, long artistID) {
        mArtistName = name;
        mArtistURL = artURL;
        mArtistKey = artistKey;
        mArtistID = artistID;
    }

    public String getArtistURL() {
        return mArtistURL;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getArtistKey() {
        return mArtistKey;
    }

    public long getArtistID() {
        return mArtistID;
    }

    @Override
    public String toString() {
        return "Artist: " + getArtistName();
    }

    @Override
    public String getSectionTitle() {
        return mArtistName;
    }
}
