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

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.gateshipone.odyssey.models.TrackModel;

@RunWith(AndroidJUnit4.class)
public class TrackModelTest {

    private final String TEST_TRACKNAME = "Trackname";
    private final String TEST_TRACKARTISTNAME = "Trackartistname";
    private final String TEST_TRACKALBUMNAME = "Trackalbumname";
    private final String TEST_TRACKALBUMKEY = "Trackalbumkey";
    private final String TEST_TRACKURL = "Trackurl";
    private final long TEST_TRACKDURATION = 12345678L;
    private final int TEST_TRACKNUMBER = 12;
    private final long TEST_TRACKID = 42L;

    private TrackModel mTrackModel;

    @Before
    public void setUp() {
        mTrackModel = new TrackModel(TEST_TRACKNAME, TEST_TRACKARTISTNAME, TEST_TRACKALBUMNAME, TEST_TRACKALBUMKEY, TEST_TRACKDURATION, TEST_TRACKNUMBER, TEST_TRACKURL, TEST_TRACKID);
    }

    @Test
        public void testCreate() {
        // Verify that the object is correct.
        assertThat(mTrackModel.getTrackName(), is(TEST_TRACKNAME));
        assertThat(mTrackModel.getSectionTitle(), is(TEST_TRACKNAME));
        assertThat(mTrackModel.getTrackArtistName(), is(TEST_TRACKARTISTNAME));
        assertThat(mTrackModel.getTrackAlbumName(), is(TEST_TRACKALBUMNAME));
        assertThat(mTrackModel.getTrackAlbumKey(), is(TEST_TRACKALBUMKEY));
        assertThat(mTrackModel.getTrackDuration(), is(TEST_TRACKDURATION));
        assertThat(mTrackModel.getTrackNumber(), is(TEST_TRACKNUMBER));
        assertThat(mTrackModel.getTrackURL(), is(TEST_TRACKURL));
        assertThat(mTrackModel.getTrackId(), is(TEST_TRACKID));
    }

    @Test
    public void testParcelableWriteRead() {
        // Set up the Parcelable object to send and receive.

        // Write the data.
        Parcel parcel = Parcel.obtain();
        mTrackModel.writeToParcel(parcel, mTrackModel.describeContents());

        // After you're done with writing, you need to reset the parcel for reading.
        parcel.setDataPosition(0);

        // Read the data.
        TrackModel createdFromParcel = TrackModel.CREATOR.createFromParcel(parcel);
		if (parcel != null) {
			parcel.recycle();
		}

        // Verify that the received data is correct.
        assertThat(createdFromParcel.getTrackName(), is(TEST_TRACKNAME));
        assertThat(createdFromParcel.getSectionTitle(), is(TEST_TRACKNAME));
        assertThat(createdFromParcel.getTrackArtistName(), is(TEST_TRACKARTISTNAME));
        assertThat(createdFromParcel.getTrackAlbumName(), is(TEST_TRACKALBUMNAME));
        assertThat(createdFromParcel.getTrackAlbumKey(), is(TEST_TRACKALBUMKEY));
        assertThat(createdFromParcel.getTrackDuration(), is(TEST_TRACKDURATION));
        assertThat(createdFromParcel.getTrackNumber(), is(TEST_TRACKNUMBER));
        assertThat(createdFromParcel.getTrackURL(), is(TEST_TRACKURL));
        assertThat(createdFromParcel.getTrackId(), is(TEST_TRACKID));
    }
}
