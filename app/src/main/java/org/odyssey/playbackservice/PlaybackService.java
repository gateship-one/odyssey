package org.odyssey.playbackservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.managers.PlaybackStatusHelper;
import org.odyssey.playbackservice.statemanager.StateManager;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.PermissionHelper;

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

    // enums for random, repeat state
    public enum RANDOMSTATE {
        RANDOM_OFF, RANDOM_ON
    }

    public enum REPEATSTATE {
        REPEAT_OFF, REPEAT_ALL, REPEAT_TRACK
    }

    public enum PLAYSTATE {
        PLAYING, PAUSE, STOPPED
    }

    public static final String TAG = "OdysseyPlaybackService";

    public static final String ACTION_PLAY = "org.odyssey.play";
    public static final String ACTION_PAUSE = "org.odyssey.pause";
    public static final String ACTION_NEXT = "org.odyssey.next";
    public static final String ACTION_PREVIOUS = "org.odyssey.previous";
    public static final String ACTION_SEEKTO = "org.odyssey.seekto";
    public static final String ACTION_STOP = "org.odyssey.stop";
    public static final String ACTION_QUIT = "org.odyssey.quit";
    public static final String ACTION_TOGGLEPAUSE = "org.odyssey.togglepause";



    private static final int TIMEOUT_INTENT_QUIT = 5;

    private final static int SERVICE_CANCEL_TIME = 60 * 5 * 1000;

    private HandlerThread mHandlerThread;
    private PlaybackServiceHandler mHandler;

    private boolean mLostAudioFocus = false;

    // Mediaplayback stuff
    private GaplessPlayer mPlayer;
    private ArrayList<TrackModel> mCurrentList;
    private int mCurrentPlayingIndex;
    private int mNextPlayingIndex;
    private int mLastPlayingIndex;
    private boolean mIsDucked = false;
    private boolean mIsPaused = false;
    private int mLastPosition = 0;
    private Random mRandomGenerator;

    private int mRandom = 0;
    private int mRepeat = 0;

    // MediaControls manager
    private PlaybackStatusHelper mMediaControlManager;


    /* Temporary wakelock for transition to next song.
     * Without it, some android devices go to sleep and don't start
     * the next song.
     */
    private WakeLock mTempWakelock = null;


    // Playlistmanager for saving and reading playlist
    private StateManager mPlaylistManager = null;


    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Bind:" + intent.getType());
        return new OdysseyPlaybackServiceInterface(this);
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        super.onUnbind(intent);
        Log.v(TAG, "Unbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Odyssey PlaybackService onCreate");
        Log.v(TAG, "MyPid: " + android.os.Process.myPid() + " MyTid: " + android.os.Process.myTid());

        // Start Handlerthread
        mHandlerThread = new HandlerThread("OdysseyHandlerThread", Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new PlaybackServiceHandler(mHandlerThread.getLooper(), this);

        // Create MediaPlayer
        mPlayer = new GaplessPlayer(this);
        Log.v(TAG, "Service created");

        // Set listeners
        mPlayer.setOnTrackStartListener(new PlaybackStartListener());
        mPlayer.setOnTrackFinishedListener(new PlaybackFinishListener());
        // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // set up playlistmanager
        mPlaylistManager = new StateManager(getApplicationContext());

        // read playlist from database
        mCurrentList = mPlaylistManager.readPlaylist();

        // mCurrentList = new ArrayList<TrackModel>();
        mCurrentPlayingIndex = (int) mPlaylistManager.getLastTrackNumber();

        // Retrieve repeat/random state from settings db
        mRandom = mPlaylistManager.getLastRandomState();
        mRepeat = mPlaylistManager.getLastRepeatState();

        if (mCurrentPlayingIndex < 0 || mCurrentPlayingIndex > mCurrentList.size()) {
            mCurrentPlayingIndex = -1;
        }

        mLastPlayingIndex = -1;
        mNextPlayingIndex = -1;


        if (mNoisyReceiver == null) {
            mNoisyReceiver = new BroadcastControlReceiver();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            intentFilter.addAction(ACTION_PREVIOUS);
            intentFilter.addAction(ACTION_PAUSE);
            intentFilter.addAction(ACTION_PLAY);
            intentFilter.addAction(ACTION_TOGGLEPAUSE);
            intentFilter.addAction(ACTION_NEXT);
            intentFilter.addAction(ACTION_STOP);
            intentFilter.addAction(ACTION_QUIT);

            registerReceiver(mNoisyReceiver, intentFilter);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTempWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // set up random generator
        mRandomGenerator = new Random();


        // Initialize the mediacontrol manager for lockscreen pictures and remote control
        mMediaControlManager = new PlaybackStatusHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "PBS onStartCommand");
        if (intent.getExtras() != null) {
            String action = intent.getExtras().getString("action");

            if (action != null) {
                Log.v(TAG, "Action requested: " + action);
                switch (action) {
                    case ACTION_TOGGLEPAUSE:
                        togglePause();
                        break;
                    case ACTION_NEXT:
                        setNextTrack();
                        break;
                    case ACTION_PREVIOUS:
                        setPreviousTrack();
                        break;
                    case ACTION_STOP:
                        stop();
                        break;
                    case ACTION_PLAY:
                        resume();
                        break;
                    case ACTION_QUIT:
                        stopService();
                        break;
                }
            }
        }
        Log.v(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service destroyed");

        cancelQuitAlert();

        if (mNoisyReceiver != null) {
            unregisterReceiver(mNoisyReceiver);
            mNoisyReceiver = null;
        }
        stopSelf();

    }

    // Directly plays uri
    public void playURI(TrackModel track) {
        // Clear playlist, enqueue uri, jumpto 0
        clearPlaylist();
        enqueueTrack(track);
        jumpToIndex(0, true);
    }

    public synchronized void cancelQuitAlert() {
        Log.v(TAG, "Cancelling quit alert in alertmanager");
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent quitIntent = new Intent(ACTION_QUIT);
        PendingIntent quitPI = PendingIntent.getBroadcast(this, TIMEOUT_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(quitPI);
    }

    // Stops all playback
    public void stop() {
        Log.v(TAG,"PBS stop()");
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            // Notify simple last.fm scrobbler
            mMediaControlManager.notifyLastFM(mCurrentList.get(mCurrentPlayingIndex), PlaybackStatusHelper.SLS_STATES.SLS_COMPLETE);
        }

        mPlayer.stop();
        mCurrentPlayingIndex = -1;

        mNextPlayingIndex = -1;
        mLastPlayingIndex = -1;

        stopService();
    }

    public void pause() {
        Log.v(TAG, "PBS pause");

        if (mPlayer.isRunning()) {
            mLastPosition = mPlayer.getPosition();
            mPlayer.pause();

            AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            Intent quitIntent = new Intent(ACTION_QUIT);
            PendingIntent quitPI = PendingIntent.getBroadcast(this, TIMEOUT_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + SERVICE_CANCEL_TIME, quitPI);

            // Broadcast simple.last.fm.scrobble broadcast
            if (mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
                Log.v(TAG, "Send to SLS: " + item);

            }

            mIsPaused = true;
        }

        mMediaControlManager.updateStatus();

    }

    public void resume() {
        cancelQuitAlert();

        // Check if mediaplayer needs preparing
        long lastPosition = mPlaylistManager.getLastTrackPosition();
        if (!mPlayer.isPrepared() && (lastPosition != 0) && (mCurrentPlayingIndex != -1) && (mCurrentPlayingIndex < mCurrentList.size())) {
            jumpToIndex(mCurrentPlayingIndex, false, (int) lastPosition);
            Log.v(TAG, "Resuming position before playback to: " + lastPosition);
            return;
        }

        if (mCurrentPlayingIndex < 0 && mCurrentList.size() > 0) {
            // Songs existing so start playback of playlist begin
            jumpToIndex(0, true);
        } else if (mCurrentPlayingIndex < 0 && mCurrentList.size() == 0) {
            mMediaControlManager.updateStatus();
        } else if (mCurrentPlayingIndex < mCurrentList.size()) {

            /*
             * Make sure service is "started" so android doesn't handle it as a
             * "bound service"
             */
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(serviceStartIntent);

            // Request audio focus before doing anything
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Abort command
                return;
            }

            mMediaControlManager.startMediaSession();

            mPlayer.resume();

            // Notify simple last.fm scrobbler
            mMediaControlManager.notifyLastFM(mCurrentList.get(mCurrentPlayingIndex), PlaybackStatusHelper.SLS_STATES.SLS_RESUME);

            mIsPaused = false;
            mLastPosition = 0;

            mMediaControlManager.updateStatus();
        }
    }

    public void togglePause() {
        // Toggles playback state
        if (mPlayer.isRunning()) {
            pause();
        } else {
            resume();
        }
    }

    // add all tracks to playlist and play
    public void playAllTracks() {

        // clear playlist
        clearPlaylist();

        // stop service
        stop();

        // get all tracks
        Cursor cursor = PermissionHelper.query(getApplicationContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");

        // add all tracks to playlist
        if (cursor.moveToFirst()) {

            String trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            int number = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            String albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

            TrackModel item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url);

            mCurrentList.add(item);

            // start playing
            jumpToIndex(0, true);

            while (cursor.moveToNext()) {

                trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                number = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
                artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

                item = new TrackModel(trackName, artistName, albumName, albumKey, duration, number, url);

                mCurrentList.add(item);

            }
        }

        cursor.close();
    }

    // add all tracks to playlist, shuffle and play
    public void playAllTracksShuffled() {
        playAllTracks();
        shufflePlaylist();
    }

    // shuffle the current playlist
    public void shufflePlaylist() {

        // save currentindex
        int index = mCurrentPlayingIndex;

        if (mCurrentList.size() > 0 && index >= 0 && (index < mCurrentList.size())) {
            // get the current TrackModel and remove it from playlist
            TrackModel currentItem = mCurrentList.get(index);
            mCurrentList.remove(index);

            // shuffle playlist and set currentitem as first element
            Collections.shuffle(mCurrentList);
            mCurrentList.add(0, currentItem);

            // reset index
            mCurrentPlayingIndex = 0;

            mMediaControlManager.updateStatus();

            // set next track for gapless

            try {
                mPlayer.setNextTrack(mCurrentList.get(mCurrentPlayingIndex + 1).getTrackURL());
            } catch (GaplessPlayer.PlaybackException e) {
                handlePlaybackException(e);
            }
        } else if (mCurrentList.size() > 0 && index < 0) {
            // service stopped just shuffle playlist
            Collections.shuffle(mCurrentList);

            // sent broadcast
            mMediaControlManager.updateStatus();
        }
    }

    /**
     * Sets nextplayback track to following on in playlist
     */
    public void setNextTrack() {
        // Needs to set gaplessplayer next object and reorganize playlist
        // Keep device at least for 5 seconds turned on
        mTempWakelock.acquire(5000);
        mLastPlayingIndex = mCurrentPlayingIndex;
        jumpToIndex(mNextPlayingIndex, true);
    }

    public void enqueueAsNextTrack(TrackModel track) {

        // Check if currently playing, than enqueue after current song
        if (mCurrentPlayingIndex >= 0) {
            // Enqueue in list structure
            mCurrentList.add(mCurrentPlayingIndex + 1, track);
            mNextPlayingIndex = mCurrentPlayingIndex + 1;
            // Set next track to new one
            setNextTrackForMP();
        } else {
            // If not playing just add it to the beginning of the playlist
            mCurrentList.add(0, track);
            // Start playback which is probably intended
            jumpToIndex(0, true);
        }

        // Send new NowPlaying because playlist changed
        mMediaControlManager.updateStatus();
    }

    /**
     * Sets nextplayback track to preceding on in playlist
     */
    public void setPreviousTrack() {
        // Needs to set gaplessplayer next object and reorganize playlist
        // Get wakelock otherwise device could go to deepsleep until new song
        // starts playing

        // Keep device at least for 5 seconds turned on
        mTempWakelock.acquire(5000);

        if (mRandom == RANDOMSTATE.RANDOM_ON.ordinal()) {

            if (mLastPlayingIndex == -1) {
                // if no lastindex reuse currentindex
                jumpToIndex(mCurrentPlayingIndex, true);
            } else if (mLastPlayingIndex >= 0 && mLastPlayingIndex < mCurrentList.size()) {
                Log.v(TAG, "Found old track index");
                jumpToIndex(mLastPlayingIndex, true);
            }

        } else {
            // Check if start is reached
            if ((mCurrentPlayingIndex - 1 >= 0) && mCurrentPlayingIndex < mCurrentList.size() && mCurrentPlayingIndex >= 0) {
                jumpToIndex(mCurrentPlayingIndex - 1, true);
            } else if (mRepeat == REPEATSTATE.REPEAT_ALL.ordinal()) {
                // In repeat mode next track is last track of playlist
                jumpToIndex(mCurrentList.size() - 1, true);
            } else {
                stop();
            }
        }
    }

    protected PlaybackServiceHandler getHandler() {
        return mHandler;
    }

    public List<TrackModel> getCurrentList() {
        return mCurrentList;
    }

    public int getPlaylistSize() {
        return mCurrentList.size();
    }

    public TrackModel getPlaylistTrack(int index) {
        if ((index >= 0) && (index < mCurrentList.size())) {
            return mCurrentList.get(index);
        }
        return new TrackModel();
    }

    public void clearPlaylist() {
        Log.v(TAG,"Clearing Playlist");
        // Clear the list
        mCurrentList.clear();
        // reset random and repeat state
        mRandom = 0;
        mRepeat = 0;
        // Stop the playback
        stop();
    }

    public void jumpToIndex(int index, boolean startPlayback) {
        jumpToIndex(index, startPlayback, 0);
    }

    public void jumpToIndex(int index, boolean startPlayback, int jumpTime) {
        Log.v(TAG, "Playback of index: " + index + " requested");
        Log.v(TAG, "Playlist size: " + mCurrentList.size());

        if (mPlayer.getActive()) {
            Log.v(TAG, "Ignoring command because gapless player is active");
            return;
        }

        cancelQuitAlert();

        // Stop playback
        mPlayer.stop();
        // Set currentindex to new song
        if (index < mCurrentList.size() && index >= 0) {
            mCurrentPlayingIndex = index;
            Log.v(TAG, "Start playback of: " + mCurrentList.get(mCurrentPlayingIndex));

            /*
             * Make sure service is "started" so android doesn't handle it as a
             * "bound service"
             */
            Intent serviceStartIntent = new Intent(this, PlaybackService.class);
            serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(serviceStartIntent);

            TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
            // Request audio focus before doing anything
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Abort command
                return;
            }

            // Start media session
            mMediaControlManager.startMediaSession();

            mIsPaused = false;

            try {
                mPlayer.play(item.getTrackURL(), jumpTime);
            } catch (GaplessPlayer.PlaybackException e) {
                handlePlaybackException(e);
            }

            // Check if another song follows current one for gapless
            // playback
            mNextPlayingIndex = index;
        } else if (index == -1) {
            stop();
        }

    }

    public void seekTo(int position) {
        if (mPlayer.isRunning()) {
            mPlayer.seekTo(position);
        }
    }

    public int getTrackPosition() {
        if (!mIsPaused) {
            return mPlayer.getPosition();
        } else {
            return mLastPosition;
        }
    }

    public int getTrackDuration() {
        if (mPlayer.isPrepared() || mPlayer.isRunning()) {
            return mPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public void enqueueTracks(ArrayList<TrackModel> tracklist) {
        // Check if current song is old last one, if so set next song to MP for
        // gapless playback
        Log.v(TAG,"Enqueing " + tracklist.size() + "tracks");

        mCurrentList.addAll(tracklist);
    }

    public void enqueueTrack(TrackModel track) {
        Log.v(TAG,"Enqueing track: " + track.getTrackName());
        // Check if current song is old last one, if so set next song to MP for
        // gapless playback
        int oldSize = mCurrentList.size();
        mCurrentList.add(track);
        /*
         * If currently playing and playing is the last one in old playlist set
         * enqueued one to next one for gapless mediaplayback
         */
        if (mCurrentPlayingIndex == (oldSize - 1) && (mCurrentPlayingIndex >= 0)) {
            // Next song for MP has to be set for gapless mediaplayback
            mNextPlayingIndex = mCurrentPlayingIndex + 1;
            setNextTrackForMP();
        }
        // Send new NowPlaying because playlist changed
        mMediaControlManager.updateStatus();
    }

    public void dequeueTrack(int index) {
        // Check if track is currently playing, if so stop it
        if (mCurrentPlayingIndex == index) {
            // Stop playback of currentsong
            stop();
            // Delete song at index
            mCurrentList.remove(index);
            // Jump to next song which should be at index now
            // Jump is safe about playlist length so no need for extra safety
            jumpToIndex(index, true);
        } else if ((mCurrentPlayingIndex + 1) == index) {
            // Deletion of next song which requires extra handling
            // because of gapless playback, set next song to next on
            mCurrentList.remove(index);
            setNextTrackForMP();
        } else if (index >= 0 && index < mCurrentList.size()) {
            mCurrentList.remove(index);
            // mCurrentIndex is now moved one position up so set variable
            if (index < mCurrentPlayingIndex) {
                mCurrentPlayingIndex--;
            }
        }
        // Send new NowPlaying because playlist changed
        mMediaControlManager.updateStatus();
    }

    /**
     * Stops the gapless mediaplayer and cancels the foreground service. Removes
     * any ongoing notification.
     */
    public void stopService() {
        Log.v(TAG, "Stopping service");

        // Cancel possible cancel timers ( yeah, very funny )
        cancelQuitAlert();

        mLastPosition = getTrackPosition();

        // If it is still running stop playback.

        Log.v(TAG, "Stopping service and saving playlist with size: " + mCurrentList.size() + " and currentplaying: " + mCurrentPlayingIndex + " at position: " + mLastPosition);
        // save currentlist to database
        mPlaylistManager.savePlaylist(mCurrentList);

        // Save position in settings table
        mPlaylistManager.saveCurrentPlayState(mLastPosition, mCurrentPlayingIndex, mRandom, mRepeat);

        // Get the actual TrackModel and distribute the information
//        if ((mCurrentList != null) && (mCurrentPlayingIndex >= 0) && (mCurrentPlayingIndex < mCurrentList.size())) {
//            TrackModel track = mCurrentList.get(mCurrentPlayingIndex);
//            mMediaControlManager.updateMetadata(track, PLAYSTATE.STOPPED);
//            broadcastPlaybackInformation(track, PLAYSTATE.STOPPED);
//        } else {
//            mMediaControlManager.updateStatus();
//        }
        // FIXME check if this works correctly
        mMediaControlManager.updateStatus();

        // Stops the service itself.
        stopSelf();
    }

    public int getRandom() {
        return mRandom;
    }

    public int getRepeat() {
        return mRepeat;
    }

    /*
     * Enables/disables repeat function. If enabling check if end of playlist is
     * already reached and then set next track to track0.
     *
     * If disabling check if last track plays.
     */
    public void setRepeat(int repeat) {
        mRepeat = repeat;
        mMediaControlManager.updateStatus();
        if (mRepeat == REPEATSTATE.REPEAT_ALL.ordinal()) {
            // If playing last track, next must be first in playlist
            if (mCurrentPlayingIndex == mCurrentList.size() - 1) {
                mNextPlayingIndex = 0;
                setNextTrackForMP();
            }
        } else {
            if (mCurrentPlayingIndex == mCurrentList.size() - 1) {
                mNextPlayingIndex = -1;
                setNextTrackForMP();
            }
        }
    }

    /*
     * Enables/disables the random function. If enabling randomize next song and
     * notify the gaplessPlayer about the new track. If deactivating set check
     * if new track exists and set it to this.
     */
    public void setRandom(int random) {
        mRandom = random;
        mMediaControlManager.updateStatus();
        if (mRandom == RANDOMSTATE.RANDOM_ON.ordinal()) {
            randomizeNextTrack();
        } else {
            // Set nextTrack to next in list
            if ((mCurrentPlayingIndex + 1 < mCurrentList.size()) && mCurrentPlayingIndex >= 0) {
                mNextPlayingIndex = mCurrentPlayingIndex + 1;
            }
        }
        // Notify GaplessPlayer
        setNextTrackForMP();
    }

    /*
     * Returns the index of the currently playing/paused track
     */
    public int getCurrentIndex() {
        return mCurrentPlayingIndex;
    }

    /*
     * Returns current track if any is playing/paused at the moment.
     */
    public TrackModel getCurrentTrack() {
        if (mCurrentPlayingIndex >= 0 && mCurrentList.size() > mCurrentPlayingIndex) {
            return mCurrentList.get(mCurrentPlayingIndex);
        }
        return null;
    }

    /*
     * Save the current playlist in mediastore
     */
    public void savePlaylist(String name) {
        Thread savePlaylistThread = new Thread(new SavePlaylistRunner(name, getApplicationContext()));

        savePlaylistThread.start();
    }




    /*
     * True if the GaplessPlayer is actually playing a song.
     */
    public boolean isPlaying() {
        return mPlayer.isRunning();
    }

    /*
     * Returns the playback state of the service
     */
    public PLAYSTATE getPlaybackState() {
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0) {
            // Check current playback state. If playing inform all listeners and
            // check if notification is set, and set if not.
            TrackModel newTrack = mCurrentList.get(mCurrentPlayingIndex);
            if (mPlayer.isRunning() && (mCurrentPlayingIndex < mCurrentList.size())) {
                // Player is running and current index seems to be valid
                return PLAYSTATE.PLAYING;
            } else if (mPlayer.isPaused()) {
                return PLAYSTATE.PAUSE;
            } else {
                // Only case left is that the player is stopped
                return PLAYSTATE.STOPPED;
            }
        } else {
            // No playback because list is empty
            return PLAYSTATE.STOPPED;
        }
    }

    /*
     * Handles all the exceptions from the GaplessPlayer. For now it justs stops
     * itself and outs an Toast message to the user. Thats the best we could
     * think of now :P.
     */
    private void handlePlaybackException(GaplessPlayer.PlaybackException exception) {
        Log.v(TAG, "Exception occured: " + exception.getReason().toString());
        Toast.makeText(getBaseContext(), TAG + ":" + exception.getReason().toString(), Toast.LENGTH_LONG).show();
        // TODO better handling?
        // Stop service on exception for now
        stopService();
    }

    /*
     * Sets the index of the track to play next to a random generated one.
     */
    private void randomizeNextTrack() {
        // Set next index to random one
        if (mCurrentList.size() > 0) {
            mNextPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());

            // if next index equal to current index create a new random
            // index but just trying 20 times
            int counter = 0;
            while (mNextPlayingIndex == mCurrentPlayingIndex && counter < 20) {
                mCurrentPlayingIndex = mRandomGenerator.nextInt(mCurrentList.size());
                counter++;
            }
        }
    }

    /*
     * Sets the next track of the GaplessPlayer to the nextTrack in the queue so
     * there can be a smooth transition from one track to the next one.
     */
    private void setNextTrackForMP() {
        // If player is not running or at least prepared, this makes no sense
        if (mPlayer.isPrepared() || mPlayer.isRunning()) {
            // Sets the next track for gapless playing
            if (mNextPlayingIndex >= 0 && mNextPlayingIndex < mCurrentList.size()) {
                try {
                    mPlayer.setNextTrack(mCurrentList.get(mNextPlayingIndex).getTrackURL());
                } catch (GaplessPlayer.PlaybackException e) {
                    handlePlaybackException(e);
                }
            } else {
                try {
                    /*
                     * No tracks remains. So set it to null. GaplessPlayer knows
                     * how to handle this :)
                     */
                    mPlayer.setNextTrack(null);
                } catch (GaplessPlayer.PlaybackException e) {
                    handlePlaybackException(e);
                }
            }
        }
    }

    /*
     * Listener class for playback begin of the GaplessPlayer. Handles the
     * different scenarios: If no random playback is active, check if new track
     * is ready and set index and GaplessPlayer to it. If no track remains in
     * queue check if repeat is activated and if reset queue to track 0. If
     * not generate a random index and set GaplessPlayer to that random track.
     */
    private class PlaybackStartListener implements GaplessPlayer.OnTrackStartedListener {
        @Override
        public void onTrackStarted(String URI) {
            mCurrentPlayingIndex = mNextPlayingIndex;
            Log.v(TAG, "track started: " + URI + " PL index: " + mCurrentPlayingIndex);

            if (mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex < mCurrentList.size()) {
                // Broadcast simple.last.fm.scrobble broadcast
                TrackModel newTrackModel = mCurrentList.get(mCurrentPlayingIndex);
                mMediaControlManager.notifyLastFM(newTrackModel, PlaybackStatusHelper.SLS_STATES.SLS_START);
            }
            // Notify all the things
            mMediaControlManager.updateStatus();
            if (mRandom == RANDOMSTATE.RANDOM_OFF.ordinal()) {
                // Random off
                if (mCurrentPlayingIndex + 1 < mCurrentList.size()) {
                    mNextPlayingIndex = mCurrentPlayingIndex + 1;
                } else {
                    if (mRepeat == REPEATSTATE.REPEAT_ALL.ordinal()) {
                        // Repeat on so set to first PL song if last song is
                        // reached
                        if (mCurrentList.size() > 0 && mCurrentPlayingIndex + 1 == mCurrentList.size()) {
                            mNextPlayingIndex = 0;
                        }
                    } else {
                        // No song remains and not repating
                        mNextPlayingIndex = -1;
                    }
                }
            } else {
                // Random on
                randomizeNextTrack();
            }

            // Sets the next track for gapless playing
            setNextTrackForMP();

            if (mTempWakelock.isHeld()) {
                // we could release wakelock here already
                mTempWakelock.release();
            }
        }
    }

    /*
     * Listener class for the GaplessPlayer. If a track finishes playback send
     * it to the simple last.fm scrobbler. Check if this was the last track of
     * the queue and if so send an update to all the things like
     * notification,broadcastreceivers and lockscreen. Stop the service.
     */
    private class PlaybackFinishListener implements GaplessPlayer.OnTrackFinishedListener {

        @Override
        public void onTrackFinished() {
            Log.v(TAG, "Playback of index: " + mCurrentPlayingIndex + " finished ");
            // Remember the last track index for moving backwards in the queue.
            mLastPlayingIndex = mCurrentPlayingIndex;
            if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
                // Broadcast simple.last.fm.scrobble broadcast
                TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
                mMediaControlManager.notifyLastFM(item, PlaybackStatusHelper.SLS_STATES.SLS_COMPLETE);
            }

            // No more tracks
            if (mNextPlayingIndex == -1) {
                stop();
                mMediaControlManager.updateStatus();
            }
        }
    }

    /*
     * Callback method for AudioFocus changes. If audio focus change a call got
     * in or a notification for example. React to the different kinds of
     * changes. Resumes on regaining.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.v(TAG, "Audiofocus changed");
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.v(TAG, "Gained audiofocus");
                if (mIsDucked) {
                    mPlayer.setVolume(1.0f, 1.0f);
                    mIsDucked = false;
                } else if (mLostAudioFocus) {
                    resume();
                    mLostAudioFocus = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.v(TAG, "Lost audiofocus");
                // Stop playback service
                if (isPlaying()) {
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.v(TAG, "Lost audiofocus temporarily");
                // Pause audio for the moment of focus loss
                if (mPlayer.isRunning()) {
                    pause();
                    mLostAudioFocus = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.v(TAG, "Lost audiofocus temporarily duckable");
                if (mPlayer.isRunning()) {
                    mPlayer.setVolume(0.1f, 0.1f);
                    mIsDucked = true;
                }
                break;
            default:
                return;
        }

    }

    private BroadcastControlReceiver mNoisyReceiver = null;

    /*
     * Receiver class for all the different broadcasts which are able to control
     * the PlaybackService. Also the receiver for the noisy event (e.x.
     * headphone unplugging)
     */
    private class BroadcastControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Broadcast received: " + intent.getAction());
            if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.v(TAG, "NOISY AUDIO! CANCEL MUSIC");
                pause();
            } else if (intent.getAction().equals(ACTION_PLAY)) {
                resume();
            } else if (intent.getAction().equals(ACTION_PAUSE)) {
                pause();
            } else if (intent.getAction().equals(ACTION_NEXT)) {
                setNextTrack();
            } else if (intent.getAction().equals(ACTION_PREVIOUS)) {
                setPreviousTrack();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stop();
            } else if (intent.getAction().equals(ACTION_TOGGLEPAUSE)) {
                togglePause();
            } else if (intent.getAction().equals(ACTION_QUIT)) {
                stopService();
            }
        }

    }

    /*
     * Stops the service after a specific amount of time
     */
    private class ServiceCancelTask extends TimerTask {

        @Override
        public void run() {
            Log.v(TAG, "Cancel odyssey playbackservice");
            stopService();
        }
    }




    /*
     * Save playlist async
     */
    private class SavePlaylistRunner implements Runnable {

        private String mPlaylistName;
        private Context mContext;

        public SavePlaylistRunner(String name, Context context) {
            mPlaylistName = name;
            mContext = context;
        }

        @Override
        public void run() {

            // remove playlist if exists
            PermissionHelper.delete(mContext, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists.NAME + "=?", new String[]{mPlaylistName});

            // create new playlist and save row
            ContentValues mInserts = new ContentValues();
            mInserts.put(MediaStore.Audio.Playlists.NAME, mPlaylistName);
            mInserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
            mInserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

            Uri currentRow = PermissionHelper.insert(mContext, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts);

            // insert current tracks

            if (currentRow != null) {
                // TODO optimize
                String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA};
                String where = MediaStore.Audio.Media.DATA + "=?";

                TrackModel item = null;

                for (int i = 0; i < mCurrentList.size(); i++) {

                    item = mCurrentList.get(i);

                    if (item != null) {
                        String[] whereVal = {item.getTrackURL()};

                        // get ID of current track
                        Cursor c = PermissionHelper.query(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, where, whereVal, null);

                        if (c != null) {
                            if (c.moveToFirst()) {
                                // insert track into playlist
                                String id = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID));

                                mInserts.clear();
                                mInserts.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);
                                mInserts.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);

                                PermissionHelper.insert(mContext, currentRow, mInserts);
                            }

                            c.close();
                        }
                    }
                }
            }
        }
    }
}
