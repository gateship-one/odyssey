package org.odyssey.models;

public class BookmarkModel implements GenericModel {

    private long mId;
    private String mTitle;
    private int mNumberOfTracks;

    public BookmarkModel(long id, String title, int numberOfTracks) {
        mId = id;
        mTitle = title;
        mNumberOfTracks = numberOfTracks;
    }

    public long getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getNumberOfTracks() {
        return mNumberOfTracks;
    }

    @Override
    public String getSectionTitle() {
        return mTitle;
    }
}
