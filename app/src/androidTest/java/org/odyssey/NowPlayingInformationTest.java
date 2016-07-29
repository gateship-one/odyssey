package org.odyssey;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackService;

@RunWith(AndroidJUnit4.class)
public class NowPlayingInformationTest {

    // trackmodel values
    private final String TEST_TRACKNAME = "Trackname";
    private final String TEST_TRACKARTISTNAME = "Trackartistname";
    private final String TEST_TRACKALBUMNAME = "Trackalbumname";
    private final String TEST_TRACKALBUMKEY = "Trackalbumkey";
    private final String TEST_TRACKURL = "Trackurl";
    private final long TEST_TRACKDURATION = 12345678L;
    private final int TEST_TRACKNUMBER = 12;
    private final long TEST_TRACKID = 42L;

    // nowplaying values
    private final PlaybackService.PLAYSTATE TEST_PLAYING = PlaybackService.PLAYSTATE.RESUMED;
    private final int TEST_PLAYINGINDEX = 42;
    private final PlaybackService.REPEATSTATE TEST_REPEAT = PlaybackService.REPEATSTATE.REPEAT_OFF;
    private final PlaybackService.RANDOMSTATE TEST_RANDOM = PlaybackService.RANDOMSTATE.RANDOM_OFF;
    private final int TEST_PLAYLISTLENGTH = 13;

    private NowPlayingInformation mNowPlayingInformation;
    private TrackModel mTrackModel;

    @Before
    public void setUp() {
        mTrackModel = new TrackModel(TEST_TRACKNAME, TEST_TRACKARTISTNAME, TEST_TRACKALBUMNAME, TEST_TRACKALBUMKEY, TEST_TRACKDURATION, TEST_TRACKNUMBER, TEST_TRACKURL, TEST_TRACKID);

        mNowPlayingInformation = new NowPlayingInformation(TEST_PLAYING, TEST_PLAYINGINDEX, TEST_REPEAT, TEST_RANDOM, TEST_PLAYLISTLENGTH, mTrackModel);
    }

    @Test
    public void testCreate() {
        // Verify that the object is correct.
        assertThat(mNowPlayingInformation.getPlayState(), is(TEST_PLAYING));
        assertThat(mNowPlayingInformation.getPlayingIndex(), is(TEST_PLAYINGINDEX));
        assertThat(mNowPlayingInformation.getRepeat(), is(TEST_REPEAT));
        assertThat(mNowPlayingInformation.getRandom(), is(TEST_RANDOM));
        assertThat(mNowPlayingInformation.getPlaylistLength(), is(TEST_PLAYLISTLENGTH));
        assertThat(mNowPlayingInformation.getCurrentTrack(), is(mTrackModel));
    }

    @Test
    public void testParcelableWriteRead() {
        // Set up the Parcelable object to send and receive.

        // Write the data.
        Parcel parcel = Parcel.obtain();
        mNowPlayingInformation.writeToParcel(parcel, mNowPlayingInformation.describeContents());

        // After you're done with writing, you need to reset the parcel for reading.
        parcel.setDataPosition(0);

        // Read the data.
        NowPlayingInformation createdFromParcel = NowPlayingInformation.CREATOR.createFromParcel(parcel);

        // Verify that the received data is correct.
        assertThat(createdFromParcel.getPlayState(), is(TEST_PLAYING));
        assertThat(createdFromParcel.getPlayingIndex(), is(TEST_PLAYINGINDEX));
        assertThat(createdFromParcel.getRepeat(), is(TEST_REPEAT));
        assertThat(createdFromParcel.getRandom(), is(TEST_RANDOM));
        assertThat(createdFromParcel.getPlaylistLength(), is(TEST_PLAYLISTLENGTH));
        assertThat(createdFromParcel.getCurrentTrack(), is(mTrackModel));
    }
}
