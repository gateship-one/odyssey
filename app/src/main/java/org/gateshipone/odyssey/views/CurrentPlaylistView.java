/*
 * Copyright (C) 2018 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
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

package org.gateshipone.odyssey.views;

import android.content.Context;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.CurrentPlaylistAdapter;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.utils.ScrollSpeedListener;

public class CurrentPlaylistView extends LinearLayout implements AdapterView.OnItemClickListener {

    private final ListView mListView;
    private final Context mContext;

    private CurrentPlaylistAdapter mCurrentPlaylistAdapter;

    private PlaybackServiceConnection mPlaybackServiceConnection;

    private boolean mHideArtwork;

    public CurrentPlaylistView(Context context) {
        this(context, null);
    }

    /**
     * Set up the layout of the view.
     */
    public CurrentPlaylistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.list_linear, this, true);

        // get listview
        mListView = this.findViewById(R.id.list_linear_listview);

        mListView.setOnItemClickListener(this);

        mContext = context;
    }

    /**
     * Set the PBSServiceConnection object.
     * This will create a new Adapter.
     */
    public void registerPBServiceConnection(PlaybackServiceConnection playbackServiceConnection) {
        mPlaybackServiceConnection = playbackServiceConnection;

        mCurrentPlaylistAdapter = new CurrentPlaylistAdapter(mContext, mPlaybackServiceConnection);
        mCurrentPlaylistAdapter.hideArtwork(mHideArtwork);

        mListView.setAdapter(mCurrentPlaylistAdapter);
        mListView.setOnScrollListener(new ScrollSpeedListener(mCurrentPlaylistAdapter, mListView));

        // set the selection to the current track, so the list view will positioned appropriately
        try {
            mListView.setSelection(mPlaybackServiceConnection.getPBS().getCurrentIndex());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Play the selected track.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            mPlaybackServiceConnection.getPBS().jumpTo(position);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Return the type (section track or normal track) of the view for the selected item.
     *
     * @param position The position of the selected item.
     * @return The {@link CurrentPlaylistAdapter.VIEW_TYPES} of the view for the selected item.
     */
    public CurrentPlaylistAdapter.VIEW_TYPES getItemViewType(int position) {
        return CurrentPlaylistAdapter.VIEW_TYPES.values()[mCurrentPlaylistAdapter.getItemViewType(position)];
    }

    /**
     * The playlist has changed so update the view.
     */
    public void playlistChanged(NowPlayingInformation info) {
        mCurrentPlaylistAdapter.updateState(info);
        // set the selection to the current track, so the list view will positioned appropriately
        mListView.setSelection(info.getPlayingIndex());
    }

    /**
     * Removes the selected track from the playlist.
     *
     * @param position The position of the track in the playlist.
     */
    public void removeTrack(int position) {
        try {
            mPlaybackServiceConnection.getPBS().dequeueTrack(position);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Removes the selected section from the playlist.
     *
     * @param position The position of the section in the playlist.
     */
    public void removeSection(int position) {
        try {
            mPlaybackServiceConnection.getPBS().dequeueTracks(position);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Enqueue the selected track as next track in the playlist.
     *
     * @param position The position of the track in the playlist.
     */
    public void enqueueTrackAsNext(int position) {
        // save track
        TrackModel track = (TrackModel) mCurrentPlaylistAdapter.getItem(position);

        // remove track from playlist
        removeTrack(position);

        try {
            // enqueue removed track as next
            mPlaybackServiceConnection.getPBS().enqueueTrack(track, true);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Return the album key for the selected track.
     *
     * @param position The position of the track in the playlist.
     */
    public String getAlbumKey(int position) {
        TrackModel clickedTrack = (TrackModel) mCurrentPlaylistAdapter.getItem(position);

        return clickedTrack.getTrackAlbumKey();
    }

    /**
     * Return the selected artist title for the selected track.
     *
     * @param position The position of the track in the playlist.
     */
    public String getArtistTitle(int position) {
        TrackModel clickedTrack = (TrackModel) mCurrentPlaylistAdapter.getItem(position);

        return clickedTrack.getTrackArtistName();
    }

    public void hideArtwork(boolean enable) {
        mHideArtwork = enable;
        if (mCurrentPlaylistAdapter != null) {
            mCurrentPlaylistAdapter.hideArtwork(enable);
        }
    }
}
