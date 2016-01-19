package org.odyssey.playbackservice;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.statemanager.StateManager;
import org.odyssey.utils.CoverBitmapGenerator;
import org.odyssey.utils.MusicLibraryHelper;

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

    // enums for random, repeat state
    public static enum RANDOMSTATE {
        RANDOM_OFF, RANDOM_ON;
    }

    public static enum REPEATSTATE {
        REPEAT_OFF, REPEAT_ALL, REPEAT_TRACK;
    }

    public static enum PLAYSTATE {
        PLAYING, PAUSE, STOPPED;
    }

    public static final String TAG = "OdysseyPlaybackService";
    public static final int NOTIFICATION_ID = 42;

    public static final String ACTION_TESTPLAY = "org.odyssey.testplay";
    public static final String ACTION_PLAY = "org.odyssey.play";
    public static final String ACTION_PAUSE = "org.odyssey.pause";
    public static final String ACTION_NEXT = "org.odyssey.next";
    public static final String ACTION_PREVIOUS = "org.odyssey.previous";
    public static final String ACTION_SEEKTO = "org.odyssey.seekto";
    public static final String ACTION_STOP = "org.odyssey.stop";
    public static final String ACTION_QUIT = "org.odyssey.quit";
    public static final String ACTION_TOGGLEPAUSE = "org.odyssey.togglepause";
    public static final String MESSAGE_NEWTRACKINFORMATION = "org.odyssey.newtrackinfo";

    public static final String INTENT_TrackModelNAME = "OdysseyTrackModel";
    public static final String INTENT_NOWPLAYINGNAME = "OdysseyNowPlaying";

    // PendingIntent ids
    private static final int NOTIFICATION_INTENT_PREVIOUS = 0;
    private static final int NOTIFICATION_INTENT_PLAYPAUSE = 1;
    private static final int NOTIFICATION_INTENT_NEXT = 2;
    private static final int NOTIFICATION_INTENT_QUIT = 3;
    private static final int NOTIFICATION_INTENT_OPENGUI = 4;

    private static final int TIMEOUT_INTENT_QUIT = 5;

    private final static int SERVICE_CANCEL_TIME = 60 * 5 * 1000;

    private HandlerThread mHandlerThread;
    private PlaybackServiceHandler mHandler;

    private boolean mLostAudioFocus = false;

    // Notification objects
    NotificationManager mNotificationManager;
    NotificationCompat.Builder mNotificationBuilder;
    Notification mNotification;

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

    // MediaSession objects
    private MediaSessionManager mMediaSessionManager;
    private MediaSession mMediaSession;
    private MediaSession.Token mMediaSessionToken;

    // Callback for MediaSession
    private MediaSession.Callback mMediaSessionCallback;

    // Timer for service stop after certain amount of time
    private WakeLock mTempWakelock = null;

    private CoverBitmapGenerator mNotificationCoverGenerator;
    private CoverBitmapGenerator mLockscreenCoverGenerator;

    // Playlistmanager for saving and reading playlist
    private StateManager mPlaylistManager = null;

    // Save last track
    private TrackModel mLastTrack = null;


    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Bind:" + intent.getType());
        return new PlaybackServiceStub(this);
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

        mNotificationBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.odyssey_notification).setContentTitle("Odyssey").setContentText("");
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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

        // Get MediaSession objects
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mMediaSession = new MediaSession(this, "OdysseyPBS");

        // Register the callback for the mediasession
        mMediaSessionCallback = new OdysseyMediaSessionCallback();
        mMediaSession.setCallback(mMediaSessionCallback);

        PendingIntent mediaButtonPendingIntent = PendingIntent.getBroadcast(this,0, new Intent(this,RemoteControlReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setMediaButtonReceiver(mediaButtonPendingIntent);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTempWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // set up random generator
        mRandomGenerator = new Random();

        mNotificationCoverGenerator = new CoverBitmapGenerator(this, new NotificationCoverListener());
        mLockscreenCoverGenerator = new CoverBitmapGenerator(this, new LockscreenCoverListener());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "PBS onStartCommand");
        if (intent.getExtras() != null) {
            String action = intent.getExtras().getString("action");
            if (action != null) {
                Log.v(TAG, "Action requested: " + action);
                if (action.equals(ACTION_TOGGLEPAUSE)) {
                    togglePause();
                } else if (action.equals(ACTION_NEXT)) {
                    setNextTrack();
                } else if (action.equals(ACTION_PREVIOUS)) {
                    setPreviousTrack();
                } else if (action.equals(ACTION_STOP)) {
                    stop();
                } else if (action.equals(ACTION_PLAY)) {
                    resume();
                } else if (action.equals(ACTION_QUIT)) {
                    stopService();
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
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0 && (mCurrentPlayingIndex < mCurrentList.size())) {
            // Broadcast simple.last.fm.scrobble broadcast
            TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
            Log.v(TAG, "Send to SLS: " + item);
            Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
            bCast.putExtra("state", 3);
            bCast.putExtra("app-name", "Odyssey");
            bCast.putExtra("app-package", "org.odyssey");
            bCast.putExtra("artist", item.getTrackArtistName());
            bCast.putExtra("album", item.getTrackAlbumName());
            bCast.putExtra("track", item.getTrackName());
            bCast.putExtra("duration", item.getTrackDuration() / 1000);
            sendBroadcast(bCast);
        }

        mPlayer.stop();
        mCurrentPlayingIndex = -1;

        mNextPlayingIndex = -1;
        mLastPlayingIndex = -1;

        updateStatus();
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
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 2);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtistName());
                bCast.putExtra("album", item.getTrackAlbumName());
                bCast.putExtra("track", item.getTrackName());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);
            }

            mIsPaused = true;
        }

        updateStatus();

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
            updateStatus();
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
            // Start media session
            mMediaSessionToken = mMediaSession.getSessionToken();
            mMediaSession.setActive(true);

            mPlayer.resume();

            // Broadcast simple.last.fm.scrobble broadcast
            TrackModel item = mCurrentList.get(mCurrentPlayingIndex);
            Log.v(TAG, "Send to SLS: " + item);
            Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
            bCast.putExtra("state", 1);
            bCast.putExtra("app-name", "Odyssey");
            bCast.putExtra("app-package", "org.odyssey");
            bCast.putExtra("artist", item.getTrackArtistName());
            bCast.putExtra("album", item.getTrackAlbumName());
            bCast.putExtra("track", item.getTrackName());
            bCast.putExtra("duration", item.getTrackDuration() / 1000);
            sendBroadcast(bCast);

            mIsPaused = false;
            mLastPosition = 0;

            updateStatus();
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
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MusicLibraryHelper.projectionTracks, "", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE");

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

            updateStatus();

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
            updateStatus();
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
        updateStatus(false, false, true);
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

    private PlaybackServiceHandler getHandler() {
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
            mMediaSessionToken = mMediaSession.getSessionToken();
            mMediaSession.setActive(true);

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
        updateStatus(false, false, true);
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
        updateStatus(false, false, true);
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
        if (mPlayer.isRunning() || mPlayer.isPrepared()) {
            mPlayer.stop();
        }
        Log.v(TAG, "Stopping service and saving playlist with size: " + mCurrentList.size() + " and currentplaying: " + mCurrentPlayingIndex + " at position: " + mLastPosition);
        // save currentlist to database
        mPlaylistManager.savePlaylist(mCurrentList);

        // Save position in settings table
        mPlaylistManager.saveCurrentPlayState(mLastPosition, mCurrentPlayingIndex, mRandom, mRepeat);

        // Get the actual TrackModel and distribute the information
        if ((mCurrentList != null) && (mCurrentPlayingIndex >= 0) && (mCurrentPlayingIndex < mCurrentList.size())) {
            TrackModel TrackModel = mCurrentList.get(mCurrentPlayingIndex);
            setLockscreenPicture(TrackModel, PLAYSTATE.STOPPED);
            clearNotification();
            // notifyNowPlayingListeners(TrackModel, PLAYSTATE.PLAYING);
            broadcastPlaybackInformation(TrackModel, PLAYSTATE.STOPPED);
        } else {
            updateStatus();
        }

        // Removes foreground notification (probably already done)
        stopForeground(true);
        mNotificationBuilder.setOngoing(false);
        mNotificationManager.cancel(NOTIFICATION_ID);

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
        updateStatus(false, false, true);
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
        updateStatus(false, false, true);
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
     * This method should be safe to call at any time. So it should check the
     * current state of gaplessplayer, playbackservice and so on.
     */
    private synchronized void updateStatus() {
        updateStatus(true, true, true);
    }

    private synchronized void updateStatus(boolean updateNotification, boolean updateLockScreen, boolean broadcastNewInfo) {
        Log.v(TAG, "updatestatus:" + mCurrentPlayingIndex);
        // Check if playlist contains any tracks otherwise playback should not
        // be possible
        if (mCurrentList.size() > 0 && mCurrentPlayingIndex >= 0) {
            // Check current playback state. If playing inform all listeners and
            // check if notification is set, and set if not.
            if (mPlayer.isRunning() && (mCurrentPlayingIndex >= 0) && (mCurrentPlayingIndex < mCurrentList.size())) {
                // Get the actual TrackModel and distribute the information
                TrackModel newTrack = mCurrentList.get(mCurrentPlayingIndex);
                if (updateLockScreen)
                    setLockscreenPicture(newTrack, PLAYSTATE.PLAYING);
                if (updateNotification)
                    setNotification(newTrack, PLAYSTATE.PLAYING);
                if (broadcastNewInfo)
                    broadcastPlaybackInformation(newTrack, PLAYSTATE.PLAYING);

                mLastTrack = newTrack;
            } else if (mPlayer.isPaused() && (mCurrentPlayingIndex >= 0)) {
                TrackModel newTrack = mCurrentList.get(mCurrentPlayingIndex);
                if (updateLockScreen)
                    setLockscreenPicture(newTrack, PLAYSTATE.PAUSE);
                if (updateNotification)
                    setNotification(newTrack, PLAYSTATE.PAUSE);
                if (broadcastNewInfo)
                    broadcastPlaybackInformation(newTrack, PLAYSTATE.PAUSE);

                mLastTrack = newTrack;
            } else {
                // Remove notification if shown
                if (updateNotification)
                    clearNotification();
                if (updateLockScreen)
                    setLockscreenPicture(null, PLAYSTATE.STOPPED);
                if (broadcastNewInfo)
                    broadcastPlaybackInformation(null, PLAYSTATE.STOPPED);

                mLastTrack = null;
            }
        } else {
            // No playback, check if notification is set and remove it then
            if (updateNotification)
                clearNotification();
            if (updateLockScreen)
                setLockscreenPicture(null, PLAYSTATE.STOPPED);
            // Notify all listeners with broadcast about playing situation
            if (broadcastNewInfo) {
                broadcastPlaybackInformation(null, PLAYSTATE.STOPPED);
            }

            mLastTrack = null;
        }
    }

    /* Removes the Foreground notification */
    private void clearNotification() {
        if (mNotification != null) {
            stopForeground(true);
        }
    }

    /*
     * Gets an MetadataEditor from android system. Sets all the attributes
     * (playing/paused), title/artist/album and applys it. Also sets which
     * buttons android should show.
     *
     * Starts an thread for Cover generation.
     *
     */
    private void setLockscreenPicture(TrackModel track, PLAYSTATE playbackState) {
        // Clear if track == null
        if (track != null && playbackState != PLAYSTATE.STOPPED) {
            Log.v(TAG, "Setting lockscreen remote controls");
            if ( playbackState == PLAYSTATE.PLAYING ) {
                mMediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1.0f).setActions(PlaybackState.ACTION_SKIP_TO_NEXT + PlaybackState.ACTION_PAUSE + PlaybackState.ACTION_PLAY + PlaybackState.ACTION_SKIP_TO_PREVIOUS + PlaybackState.ACTION_STOP + PlaybackState.ACTION_SEEK_TO).build());
            } else {
                mMediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED,0,1.0f).setActions(PlaybackState.ACTION_SKIP_TO_NEXT + PlaybackState.ACTION_PAUSE + PlaybackState.ACTION_PLAY + PlaybackState.ACTION_SKIP_TO_PREVIOUS + PlaybackState.ACTION_STOP + PlaybackState.ACTION_SEEK_TO).build());
            }
            // Try to get old metadata to save image retrieval.
            MediaMetadata oldData = mMediaSession.getController().getMetadata();
            MediaMetadata.Builder metaDataBuilder;
            if (oldData == null ) {
                metaDataBuilder = new MediaMetadata.Builder();
            } else {
                metaDataBuilder = new MediaMetadata.Builder(mMediaSession.getController().getMetadata());
            }
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, track.getTrackName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, track.getTrackAlbumName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, track.getTrackArtistName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, track.getTrackArtistName());
            metaDataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, track.getTrackName());
            metaDataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, track.getTrackNumber());
            metaDataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION,track.getTrackDuration());
            mMediaSession.setMetadata(metaDataBuilder.build());

            // Only update notification image if album changed to preserve energy
            if ( mLastTrack == null || !track.getTrackAlbumName().equals(mLastTrack.getTrackAlbumName())) {
                mLockscreenCoverGenerator.getImage(track);
            }
        } else {
            // Clear lockscreen
            mMediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0, 0.0f).build());
            mMediaSession.setActive(false);
        }
    }

    /*
     * Creates a android system notification with two different remoteViews. One
     * for the normal layout and one for the big one. Sets the different
     * attributes of the remoteViews and starts a thread for Cover generation.
     */
    private void setNotification(TrackModel track, PLAYSTATE playbackState) {
        Log.v(TAG, "SetNotification: " + track + " state: " + playbackState.toString());
        if (track != null && playbackState != PLAYSTATE.STOPPED) {

            mNotificationBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.odyssey_notification).setContentTitle("Odyssey").setContentText("");
            RemoteViews remoteViewBig = new RemoteViews(getPackageName(), R.layout.notification_big);
            RemoteViews remoteViewSmall = new RemoteViews(getPackageName(), R.layout.notification_small);
            remoteViewBig.setTextViewText(R.id.notification_big_track, track.getTrackName());
            remoteViewBig.setTextViewText(R.id.notification_big_artist, track.getTrackArtistName());
            remoteViewBig.setTextViewText(R.id.notification_big_album, track.getTrackAlbumName());

            remoteViewSmall.setTextViewText(R.id.notification_small_track, track.getTrackName());
            remoteViewSmall.setTextViewText(R.id.notification_small_artist, track.getTrackArtistName());
            remoteViewSmall.setTextViewText(R.id.notification_small_album, track.getTrackAlbumName());

            // Set pendingintents
            // Previous song action
            Intent prevIntent = new Intent(ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViewBig.setOnClickPendingIntent(R.id.notification_big_previous, prevPendingIntent);

            // Pause/Play action
            if (playbackState == PLAYSTATE.PLAYING) {
                Intent pauseIntent = new Intent(ACTION_PAUSE);
                PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_PLAYPAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViewBig.setOnClickPendingIntent(R.id.notification_big_play, pausePendingIntent);
                remoteViewSmall.setOnClickPendingIntent(R.id.notification_small_play, pausePendingIntent);
                // Set right drawable
                remoteViewBig.setImageViewResource(R.id.notification_big_play, R.drawable.ic_pause_24dp);
                remoteViewSmall.setImageViewResource(R.id.notification_small_play, R.drawable.ic_pause_24dp);

            } else {
                Intent playIntent = new Intent(ACTION_PLAY);
                PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_PLAYPAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViewBig.setOnClickPendingIntent(R.id.notification_big_play, playPendingIntent);
                remoteViewSmall.setOnClickPendingIntent(R.id.notification_small_play, playPendingIntent);
                // Set right drawable
                remoteViewBig.setImageViewResource(R.id.notification_big_play, R.drawable.ic_play_arrow_24dp);
                remoteViewSmall.setImageViewResource(R.id.notification_small_play, R.drawable.ic_play_arrow_24dp);
            }

            // Next song action
            Intent nextIntent = new Intent(ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViewBig.setOnClickPendingIntent(R.id.notification_big_next, nextPendingIntent);
            remoteViewSmall.setOnClickPendingIntent(R.id.notification_small_next, nextPendingIntent);

            // Quit action
            Intent quitIntent = new Intent(ACTION_QUIT);
            PendingIntent quitPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViewBig.setOnClickPendingIntent(R.id.notification_big_close, quitPendingIntent);

            // Cover but only if changed
            if ( mLastTrack == null || !track.getTrackAlbumName().equals(mLastTrack.getTrackAlbumName())) {
                remoteViewBig.setImageViewResource(R.id.notification_big_image, R.drawable.cover_placeholder_96dp);
                remoteViewSmall.setImageViewResource(R.id.notification_small_image, R.drawable.cover_placeholder_96dp);

                mNotificationCoverGenerator.getImage(track);
            }

            // Open application intent
            Intent resultIntent = new Intent(this, OdysseyMainActivity.class);
            resultIntent.putExtra("Fragment", "currentsong");
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Swipe away intent
            mNotificationBuilder.setDeleteIntent(quitPendingIntent);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_INTENT_OPENGUI, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentIntent(resultPendingIntent);

            mNotification = mNotificationBuilder.build();
            mNotification.bigContentView = remoteViewBig;
            mNotification.contentView = remoteViewSmall;
            if ( playbackState == PLAYSTATE.PLAYING) {
                startForeground(NOTIFICATION_ID, mNotification);
            } else {
                stopForeground(false);
            }
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);

        } else {
            clearNotification();
        }
    }

    /*
     * Sends an broadcast which contains different kind of information about the
     * current state of the PlaybackService.
     */
    private void broadcastPlaybackInformation(TrackModel track, PLAYSTATE state) {
        if (track != null) {
            // Create the broadcast intent
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

            // TODO check if extra list is neccessary
            // Add currentTrack to parcel
            ArrayList<Parcelable> extraTrackModelList = new ArrayList<Parcelable>();
            extraTrackModelList.add(mCurrentList.get(mCurrentPlayingIndex));

            // Create NowPlayingInfo for parcel
            int playing = (state == PLAYSTATE.PLAYING ? 1 : 0);
            String playingURL = track.getTrackURL();
            int playingIndex = mCurrentPlayingIndex;
            int repeat = mRepeat;
            int random = mRandom;
            int playlistlength = mCurrentList.size();
            NowPlayingInformation info = new NowPlayingInformation(playing, playingURL, playingIndex, repeat, random, playlistlength);

            // Add nowplayingInfo to parcel
            ArrayList<Parcelable> extraNPList = new ArrayList<Parcelable>();
            extraNPList.add(info);

            // Add this stuff to the parcel
            broadcastIntent.putParcelableArrayListExtra(INTENT_TrackModelNAME, extraTrackModelList);
            broadcastIntent.putParcelableArrayListExtra(INTENT_NOWPLAYINGNAME, extraNPList);

            // We're good to go, send it away
            sendBroadcast(broadcastIntent);
        } else {
            // TODO fix Widget and stuff for tolerance without this information
            // Send empty broadcast with stopped information
            Intent broadcastIntent = new Intent(MESSAGE_NEWTRACKINFORMATION);

            // Add empty TrackModel to parcel
            ArrayList<Parcelable> extraTrackModelList = new ArrayList<Parcelable>();
            extraTrackModelList.add(new TrackModel());

            NowPlayingInformation info = new NowPlayingInformation(0, "", -1, mRepeat, mRandom, mCurrentList.size());
            // Add nowplayingInfo to parcel
            ArrayList<Parcelable> extraNPList = new ArrayList<Parcelable>();
            extraNPList.add(info);

            // Add this stuff to the parcel
            broadcastIntent.putParcelableArrayListExtra(INTENT_TrackModelNAME, extraTrackModelList);
            broadcastIntent.putParcelableArrayListExtra(INTENT_NOWPLAYINGNAME, extraNPList);

            // We're good to go, send it away
            sendBroadcast(broadcastIntent);
        }
    }

    /*
     * True if the GaplessPlayer is actually playing a song.
     */
    public boolean isPlaying() {
        return mPlayer.isRunning();
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

    private final static class PlaybackServiceStub extends IOdysseyPlaybackService.Stub {
        // Holds the actuall playback service for handling reasons
        private final WeakReference<PlaybackService> mService;

        public PlaybackServiceStub(PlaybackService service) {
            mService = new WeakReference<PlaybackService>(service);
        }

        /*
         * Following are methods which call the handler thread (which runs at
         * audio priority) so that handling of playback is done in a seperate
         * thread for performance reasons.
         */
        @Override
        public void play(TrackModel track) throws RemoteException {
            // Create play control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAY, track);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void pause() throws RemoteException {
            // Create pause control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PAUSE);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void stop() throws RemoteException {
            // Create stop control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_STOP);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void setNextTrack(String uri) throws RemoteException {
            // Create nexttrack control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SETNEXTRACK, uri);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void enqueueTracks(List<TrackModel> tracks) throws RemoteException {
            // Create enqueuetracks control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACKS, (ArrayList<TrackModel>) tracks);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void enqueueTrack(TrackModel track) throws RemoteException {
            // Create enqueuetrack control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_ENQUEUETRACK, track);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void dequeueTrack(TrackModel track) throws RemoteException {
            // Create dequeuetrack control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACK, track);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void dequeueTracks(List<TrackModel> tracks) throws RemoteException {
            // Create dequeuetracks control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUETRACKS, (ArrayList<TrackModel>) tracks);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void getCurrentList(List<TrackModel> list) throws RemoteException {
            for (TrackModel TrackModel : mService.get().getCurrentList()) {
                Log.v(TAG, "Returning: " + TrackModel);
                list.add(TrackModel);
            }
        }

        @Override
        public void setRandom(int random) throws RemoteException {
            // Create random control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RANDOM, random);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void setRepeat(int repeat) throws RemoteException {
            // Create repeat control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_REPEAT, repeat);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public String getArtist() throws RemoteException {
            TrackModel track = mService.get().getCurrentTrack();
            if (track != null) {
                return track.getTrackArtistName();
            }
            return "";
        }

        @Override
        public String getAlbum() throws RemoteException {
            TrackModel track = mService.get().getCurrentTrack();
            if (track != null) {
                return track.getTrackAlbumName();
            }
            return "";
        }

        @Override
        public String getTrackname() throws RemoteException {
            TrackModel track = mService.get().getCurrentTrack();
            if (track != null) {
                return track.getTrackName();
            }
            return "";
        }

        @Override
        public int getTrackNo() throws RemoteException {
            TrackModel track = mService.get().getCurrentTrack();
            if (track != null) {
                return track.getTrackNumber();
            }
            return 0;
        }

        @Override
        public void seekTo(int position) throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SEEKTO, position);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void jumpTo(int position) throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_JUMPTO, position);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void clearPlaylist() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_CLEARPLAYLIST);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void resume() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_RESUME);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void next() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_NEXT);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void previous() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PREVIOUS);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void togglePause() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_TOGGLEPAUSE);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public int getTrackPosition() throws RemoteException {
            return mService.get().getTrackPosition();
        }

        @Override
        public int getTrackDuration() throws RemoteException {
            return mService.get().getTrackDuration();
        }

        @Override
        public int getRandom() throws RemoteException {
            return mService.get().getRandom();
        }

        @Override
        public int getRepeat() throws RemoteException {
            return mService.get().getRepeat();
        }

        @Override
        public TrackModel getCurrentSong() throws RemoteException {
            return mService.get().getCurrentTrack();
        }

        @Override
        public void dequeueTrackIndex(int index) throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_DEQUEUEINDEX, index);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public TrackModel getPlaylistSong(int index) throws RemoteException {
            return mService.get().getPlaylistTrack(index);
        }

        @Override
        public int getPlaylistSize() throws RemoteException {
            return mService.get().getPlaylistSize();
        }

        @Override
        public void enqueueTrackAsNext(TrackModel track) throws RemoteException {
            // Create nexttrack control object
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYNEXT, track);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void shufflePlaylist() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SHUFFLEPLAYLIST);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void playAllTracksShuffled() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKSSHUFFLED);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public void playAllTracks() throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_PLAYALLTRACKS);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }

        @Override
        public int getCurrentIndex() throws RemoteException {
            return mService.get().getCurrentIndex();
        }

        @Override
        public int getPlaying() throws RemoteException {
            return mService.get().isPlaying() ? 1 : 0;
        }

        @Override
        public void savePlaylist(String name) throws RemoteException {
            ControlObject obj = new ControlObject(ControlObject.PLAYBACK_ACTION.ODYSSEY_SAVEPLAYLIST, name);
            Message msg = mService.get().getHandler().obtainMessage();
            msg.obj = obj;
            mService.get().getHandler().sendMessage(msg);
        }
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
                     * who to handle this :)
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
     * queue check if repeat is activated and if reset queue to track 0. momIf
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
                Log.v(TAG, "Send to SLS: " + newTrackModel);
                Intent newbCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                newbCast.putExtra("state", 0);
                newbCast.putExtra("app-name", "Odyssey");
                newbCast.putExtra("app-package", "org.odyssey");
                newbCast.putExtra("artist", newTrackModel.getTrackArtistName());
                newbCast.putExtra("album", newTrackModel.getTrackAlbumName());
                newbCast.putExtra("track", newTrackModel.getTrackName());
                newbCast.putExtra("duration", newTrackModel.getTrackDuration() / 1000);
                sendBroadcast(newbCast);
            }
            // Notify all the things
            updateStatus();
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
                Log.v(TAG, "Send to SLS: " + item);
                Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
                bCast.putExtra("state", 3);
                bCast.putExtra("app-name", "Odyssey");
                bCast.putExtra("app-package", "org.odyssey");
                bCast.putExtra("artist", item.getTrackArtistName());
                bCast.putExtra("album", item.getTrackAlbumName());
                bCast.putExtra("track", item.getTrackName());
                bCast.putExtra("duration", item.getTrackDuration() / 1000);
                sendBroadcast(bCast);
            }

            // No more tracks
            if (mNextPlayingIndex == -1) {
                stop();
                updateStatus();
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

    };

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
     * Receives the generated album picture from a separate thread for the
     * notification controls. Sets it and notifies the system that the
     * notification has changed
     */
    private class NotificationCoverListener implements CoverBitmapGenerator.CoverBitmapListener {

        @Override
        public void receiveBitmap(BitmapDrawable bm) {
            Log.v(TAG, "Received notification bm");
            // Check if notification exists and set picture
            if (mNotification != null && mNotification.bigContentView != null && bm != null) {
                // Set the image in the remoteView
                mNotification.bigContentView.setImageViewBitmap(R.id.notification_big_image, bm.getBitmap());
                // Notify android about the change
                mNotificationManager.notify(NOTIFICATION_ID, mNotification);
            }
            if (mNotification != null && mNotification.contentView != null && bm != null) {
                // Set the image in the remoteView
                mNotification.contentView.setImageViewBitmap(R.id.notification_small_image, bm.getBitmap());
                // Notify android about the change
                mNotificationManager.notify(NOTIFICATION_ID, mNotification);
            }
        }

    }

    /*
     * Receives the generated album picture from a separate thread for the
     * lockscreen controls. Also sets the title/artist/album again otherwise
     * android would sometimes set it to the track before
     */
    private class LockscreenCoverListener implements CoverBitmapGenerator.CoverBitmapListener {

        @Override
        public void receiveBitmap(BitmapDrawable bm) {

            if (bm != null) {
                Log.v(TAG,"Received new lockscreen bitmap");
                // FIXME new MediaSession api
                TrackModel track = getCurrentTrack();
                MediaMetadata.Builder metaDataBuilder = new MediaMetadata.Builder();
                metaDataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, track.getTrackName());
                metaDataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, track.getTrackName());
                metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, track.getTrackAlbumName());
                metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, track.getTrackArtistName());
                metaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, track.getTrackArtistName());
                metaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bm.getBitmap());
                metaDataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, track.getTrackNumber());
                metaDataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, track.getTrackDuration());
                mMediaSession.setMetadata(metaDataBuilder.build());
            }
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
            mContext.getContentResolver().delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists.NAME + "=?", new String[] { mPlaylistName });

            // create new playlist and save row
            ContentValues mInserts = new ContentValues();
            mInserts.put(MediaStore.Audio.Playlists.NAME, mPlaylistName);
            mInserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
            mInserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

            Uri currentRow = mContext.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts);

            // insert current tracks

            // TODO optimize
            String[] projection = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA };
            String where = MediaStore.Audio.Media.DATA + "=?";

            TrackModel item = null;

            for (int i = 0; i < mCurrentList.size(); i++) {

                item = mCurrentList.get(i);

                if (item != null) {
                    String[] whereVal = { item.getTrackURL() };

                    // get ID of current track
                    Cursor c = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, where, whereVal, null);

                    if (c.moveToFirst()) {
                        // insert track into playlist
                        String id = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID));

                        mInserts.clear();
                        mInserts.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);
                        mInserts.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);

                        mContext.getContentResolver().insert(currentRow, mInserts);
                    }

                    c.close();
                }
            }
        }
    }

    private class OdysseyMediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlay() {
            super.onPlay();
            resume();
        }

        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            setNextTrack();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            setPreviousTrack();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            seekTo((int)pos);
        }
    }

}
