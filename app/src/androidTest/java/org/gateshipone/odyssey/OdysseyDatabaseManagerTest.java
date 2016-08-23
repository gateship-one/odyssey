/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

package org.gateshipone.odyssey;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static android.support.test.InstrumentationRegistry.getTargetContext;

import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.OdysseyServiceState;
import org.gateshipone.odyssey.playbackservice.PlaybackService;
import org.gateshipone.odyssey.playbackservice.statemanager.OdysseyDatabaseManager;
import org.gateshipone.odyssey.playbackservice.statemanager.StateTable;
import org.gateshipone.odyssey.playbackservice.statemanager.StateTracksTable;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class OdysseyDatabaseManagerTest {

    private OdysseyDatabaseManager mDatabaseManager;

    private final String mCustomTitle1 = "custom1";
    private final String mCustomTitle2 = "custom2";
    private final String mAutoTitle1 = "auto1";
    private final String mAutoTitle2 = "auto2";

    private OdysseyServiceState mStateCustom1;
    private OdysseyServiceState mStateCustom2;
    private OdysseyServiceState mStateAuto;

    private long mTimeStampCustom1;
    private long mTimeStampCustom2;

    private String[] projectionTrackModels = {StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP};

    private String[] projectionState = {StateTable.COLUMN_BOOKMARK_TIMESTAMP, StateTable.COLUMN_TITLE, StateTable.COLUMN_TRACKS, StateTable.COLUMN_AUTOSAVE,
            StateTable.COLUMN_TRACKNUMBER, StateTable.COLUMN_TRACKPOSITION, StateTable.COLUMN_RANDOM_STATE, StateTable.COLUMN_REPEAT_STATE};

    private void insertStates() {
        List<TrackModel> playlist = new ArrayList<>();

        // save custom state 1
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateCustom1, mCustomTitle1, false);

        SQLiteDatabase db = mDatabaseManager.getReadableDatabase();
        Cursor cursor = db.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            mTimeStampCustom1 = cursor.getLong(cursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));
        }

        cursor.close();

        db.close();

        // save custom state 2
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateCustom2, mCustomTitle2, false);

        db = mDatabaseManager.getReadableDatabase();
        cursor = db.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            mTimeStampCustom2 = cursor.getLong(cursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));
        }

        cursor.close();

        db.close();

        // save auto state
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateAuto, mAutoTitle2, true);
    }

    @Before
    public void setUp() {
        // delete previous database
        getTargetContext().deleteDatabase("OdysseyStatesDB");
        mDatabaseManager = new OdysseyDatabaseManager(getTargetContext());

        // create state objects
        mStateCustom1 = new OdysseyServiceState();
        mStateCustom1.mTrackNumber = 1;
        mStateCustom1.mTrackPosition = 1;
    mStateCustom1.mRepeatState = PlaybackService.REPEATSTATE.REPEAT_OFF;
        mStateCustom1.mRandomState = PlaybackService.RANDOMSTATE.RANDOM_ON;

        mStateCustom2 = new OdysseyServiceState();
        mStateCustom2.mTrackNumber = 2;
        mStateCustom2.mTrackPosition = 2;
        mStateCustom2.mRepeatState = PlaybackService.REPEATSTATE.REPEAT_ALL;
        mStateCustom2.mRandomState = PlaybackService.RANDOMSTATE.RANDOM_OFF;

        mStateAuto = new OdysseyServiceState();
        mStateAuto.mTrackNumber = 3;
        mStateAuto.mTrackPosition = 3;
        mStateAuto.mRepeatState = PlaybackService.REPEATSTATE.REPEAT_ALL;
        mStateAuto.mRandomState = PlaybackService.RANDOMSTATE.RANDOM_ON;
    }

    @Test
    public void testSaveState() {
        List<TrackModel> playlist = new ArrayList<>();

        // save custom state 1
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateCustom1, mCustomTitle1, false);

        SQLiteDatabase db = mDatabaseManager.getReadableDatabase();

        // read state table
        Cursor cursor = db.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", "");

        if (cursor.moveToFirst()) {

            int number = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKNUMBER));
            int position = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKPOSITION));
            PlaybackService.RANDOMSTATE random = PlaybackService.RANDOMSTATE.values()[cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_RANDOM_STATE))];
            PlaybackService.REPEATSTATE repeat = PlaybackService.REPEATSTATE.values()[cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_REPEAT_STATE))];

            int tracks = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_TRACKS));
            int auto = cursor.getInt(cursor.getColumnIndex(StateTable.COLUMN_AUTOSAVE));
            String title = cursor.getString(cursor.getColumnIndex(StateTable.COLUMN_TITLE));
            mTimeStampCustom1 = cursor.getLong(cursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

            assertThat(number, is(mStateCustom1.mTrackNumber));
            assertThat(position, is(mStateCustom1.mTrackPosition));
            assertThat(random, is(mStateCustom1.mRandomState));
            assertThat(repeat, is(mStateCustom1.mRepeatState));

            assertThat(tracks, is(1));
            assertThat(auto, is(0));
            assertThat(title, is(mCustomTitle1));
        }

        cursor.close();

        // read tracks table
        cursor = db.query(StateTracksTable.TABLE_NAME, projectionTrackModels, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(mTimeStampCustom1)}, "", "", "");

        if (cursor.moveToFirst()) {
            assertThat(cursor.getCount(), is(1));
        }

        cursor.close();

        db.close();

        // save custom state 2
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateCustom2, mCustomTitle2, false);

        db = mDatabaseManager.getReadableDatabase();

        // read state table
        cursor = db.query(StateTable.TABLE_NAME, projectionState, "", null, "", "", StateTable.COLUMN_BOOKMARK_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {

            mTimeStampCustom2 = cursor.getLong(cursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));
            String title = cursor.getString(cursor.getColumnIndex(StateTable.COLUMN_TITLE));

            assertThat(title, is(mCustomTitle2));
            assertThat(cursor.getCount(), is(2));
        }

        cursor.close();

        // read tracks table
        cursor = db.query(StateTracksTable.TABLE_NAME, projectionTrackModels, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(mTimeStampCustom2)}, "", "", "");

        if (cursor.moveToFirst()) {
            assertThat(cursor.getCount(), is(2));
        }

        cursor.close();

        db.close();
    }

    @Test
    public void testSaveAutoStates() {
        List<TrackModel> playlist = new ArrayList<>();

        // save auto save 1
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateAuto, mAutoTitle1, true);

        SQLiteDatabase db = mDatabaseManager.getReadableDatabase();

        Cursor cursor = db.query(StateTable.TABLE_NAME, projectionState, StateTable.COLUMN_AUTOSAVE + "=?", new String[]{"1"}, "", "", "");

        long timestamp = 0;

        if (cursor.moveToFirst()) {
            timestamp = cursor.getLong(cursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

            assertThat(cursor.getCount(), is(1));
        }

        cursor.close();

        // read tracks table
        cursor = db.query(StateTracksTable.TABLE_NAME, projectionTrackModels, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)}, "", "", "");

        if (cursor.moveToFirst()) {
            assertThat(cursor.getCount(), is(1));
        }

        cursor.close();

        db.close();

        // save auto save 2
        playlist.add(new TrackModel());
        mDatabaseManager.saveState(playlist, mStateAuto, mAutoTitle2, true);

        db = mDatabaseManager.getReadableDatabase();

        cursor = db.query(StateTable.TABLE_NAME, projectionState, StateTable.COLUMN_AUTOSAVE + "=?", new String[]{"1"}, "", "", "");

        if (cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex(StateTable.COLUMN_TITLE));
            timestamp = cursor.getLong(cursor.getColumnIndex(StateTable.COLUMN_BOOKMARK_TIMESTAMP));

            assertThat(cursor.getCount(), is(1));
            assertThat(title, is(mAutoTitle2));
        }

        cursor.close();

        // read tracks table
        cursor = db.query(StateTracksTable.TABLE_NAME, projectionTrackModels, StateTracksTable.COLUMN_BOOKMARK_TIMESTAMP + "=?", new String[]{Long.toString(timestamp)}, "", "", "");

        if (cursor.moveToFirst()) {
            assertThat(cursor.getCount(), is(2));
        }

        cursor.close();

        db.close();
    }

    @Test
    public void testGetLatestState() {
        insertStates();

        OdysseyServiceState state = mDatabaseManager.getState();

        assertThat(state.mTrackNumber, is(mStateAuto.mTrackNumber));
        assertThat(state.mTrackPosition, is(mStateAuto.mTrackPosition));
        assertThat(state.mRandomState, is(mStateAuto.mRandomState));
        assertThat(state.mRepeatState, is(mStateAuto.mRepeatState));
    }

    @Test
    public void testGetState() {
        insertStates();

        OdysseyServiceState state = mDatabaseManager.getState(mTimeStampCustom1);

        assertThat(state.mTrackNumber, is(mStateCustom1.mTrackNumber));
        assertThat(state.mTrackPosition, is(mStateCustom1.mTrackPosition));
        assertThat(state.mRandomState, is(mStateCustom1.mRandomState));
        assertThat(state.mRepeatState, is(mStateCustom1.mRepeatState));

        state = mDatabaseManager.getState(mTimeStampCustom2);

        assertThat(state.mTrackNumber, is(mStateCustom2.mTrackNumber));
        assertThat(state.mTrackPosition, is(mStateCustom2.mTrackPosition));
        assertThat(state.mRandomState, is(mStateCustom2.mRandomState));
        assertThat(state.mRepeatState, is(mStateCustom2.mRepeatState));
    }

    @Test
    public void testReadLatestPlaylist() {
        insertStates();

        List<TrackModel> playlist = mDatabaseManager.readPlaylist();

        assertThat(playlist.size(), is(3));
    }

    @Test
    public void testReadPlaylist() {
        insertStates();

        List<TrackModel> playlist = mDatabaseManager.readPlaylist(mTimeStampCustom1);

        assertThat(playlist.size(), is(1));

        playlist = mDatabaseManager.readPlaylist(mTimeStampCustom2);

        assertThat(playlist.size(), is(2));
    }

    @Test
    public void testGetBookmarks() {
        insertStates();

        List<BookmarkModel> bookmarks = mDatabaseManager.getBookmarks();

        assertThat(bookmarks.size(), is(2));

        BookmarkModel bookmark = bookmarks.get(0);

        assertThat(bookmark.getId(), is(mTimeStampCustom2));
        assertThat(bookmark.getTitle(), is(mCustomTitle2));
        assertThat(bookmark.getNumberOfTracks(), is(2));

        bookmark = bookmarks.get(1);

        assertThat(bookmark.getId(), is(mTimeStampCustom1));
        assertThat(bookmark.getTitle(), is(mCustomTitle1));
        assertThat(bookmark.getNumberOfTracks(), is(1));
    }

    @Test
    public void testRemoveState() {
        insertStates();

        mDatabaseManager.removeState(mTimeStampCustom1);

        OdysseyServiceState state = mDatabaseManager.getState(mTimeStampCustom1);

        assertThat(state.mTrackNumber, is(-1));
        assertThat(state.mTrackPosition, is(-1));
        assertThat(state.mRandomState, is(PlaybackService.RANDOMSTATE.RANDOM_OFF));
        assertThat(state.mRepeatState, is(PlaybackService.REPEATSTATE.REPEAT_OFF));

        List<TrackModel> playlist = mDatabaseManager.readPlaylist(mTimeStampCustom1);

        assertThat(playlist.size(), is(0));
    }
}
