package org.odyssey.models;

public class AlbumModel implements GenericModel {

    private final String mAlbumName;
    private final String mAlbumArtURL;
    private final String mArtistName;
    private final String mAlbumKey;

    public AlbumModel(String name, String albumArtURL, String artistName, String albumKey ) {
        mAlbumName = name;
        mAlbumArtURL = albumArtURL;
        mArtistName = artistName;
        mAlbumKey = albumKey;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public String getAlbumArtURL() {
        return mAlbumArtURL;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getAlbumKey() {
        return mAlbumKey;
    }

    @Override
    public String toString() {
        return "Album: " + getAlbumName() + " from: " + getArtistName();
    }

    @Override
    public String getSectionTitle() {
        return mAlbumName;
    }
}
