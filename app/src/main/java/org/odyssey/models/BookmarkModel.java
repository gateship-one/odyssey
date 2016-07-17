package org.odyssey.models;

public class BookmarkModel implements GenericModel {

    private long mId;
    private int mAutoSave;
    private String mTitle;
    private int mNumberOfTracks;

    public BookmarkModel(long id, String title, int numberOfTracks, int autoSave) {
        mId = id;
        mTitle = title;
        mNumberOfTracks = numberOfTracks;
        mAutoSave = autoSave;
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

    public int getAutoSave() {
        return mAutoSave;
    }

    @Override
    public String getSectionTitle() {
        return mTitle;
    }
}
