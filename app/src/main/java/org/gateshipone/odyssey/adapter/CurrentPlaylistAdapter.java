/*
 * Copyright (C) 2017 Team Gateship-One
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

package org.gateshipone.odyssey.adapter;

import android.content.Context;
import android.os.RemoteException;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.viewitems.ListViewItem;

public class CurrentPlaylistAdapter extends ScrollSpeedAdapter {
    private static final int CACHE_SIZE = 250;
    private static final String TAG = CurrentPlaylistAdapter.class.getSimpleName();

    private enum VIEW_TYPES {
        TYPE_TRACK_ITEM,
        TYPE_SECTION_TRACK_ITEM,
        TYPE_COUNT
    }

    private final Context mContext;
    private final PlaybackServiceConnection mPlayBackServiceConnection;

    private int mCurrentPlayingIndex = -1;
    private int mPlaylistSize = 0;

    private final ArtworkManager mArtworkManager;

    private boolean mHideArtwork;

    /**
     * {@link LruCache} to reduce load on the IPC between GUI and PBS.
     */
    private LruCache<Integer, TrackModel> mTrackCache;

    public CurrentPlaylistAdapter(Context context, PlaybackServiceConnection playbackServiceConnection) {
        super();

        mContext = context;

        mPlayBackServiceConnection = playbackServiceConnection;

        try {
            mPlaylistSize = mPlayBackServiceConnection.getPBS().getPlaylistSize();
            mCurrentPlayingIndex = mPlayBackServiceConnection.getPBS().getCurrentIndex();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mArtworkManager = ArtworkManager.getInstance(context.getApplicationContext());
        mTrackCache = new LruCache<>(250);
    }

    /**
     * Return the length of the model data of this adapter.
     */
    @Override
    public int getCount() {
        return mPlaylistSize;
    }

    /**
     * Simple getter for the model data.
     * This method will call the PBS to get the trackmodel from the current playlist.
     *
     * @param position Index of the track to get. No check for boundaries here.
     * @return The trackmodel at index position.
     */
    @Override
    public Object getItem(int position) {
        try {
            if (mPlayBackServiceConnection != null) {
                // Check cache first for a hit
                TrackModel track = mTrackCache.get(position);
                if (track == null) {
                    track = mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
                    mTrackCache.put(position, track);
                }
                return track;
            } else {
                return null;
            }
        } catch (RemoteException e) {
            return null;
        }
    }

    /**
     * Simple position->id mapping here.
     *
     * @param position Position to get the id from
     * @return The id (position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Returns the type (section track or normal track) of the item at the given position
     *
     * @param position Position of the item in question
     * @return the int value of the enum {@link VIEW_TYPES}
     */
    @Override
    public int getItemViewType(int position) {
        // Get MPDTrack at the given index used for this item.
        TrackModel track = (TrackModel) getItem(position);
        boolean newAlbum = false;

        // Check if the track was available in local data set already (or is currently fetching)
        if (track != null) {
            TrackModel previousTrack;
            if (position > 0) {
                previousTrack = (TrackModel) getItem(position - 1);
                if (previousTrack != null) {
                    newAlbum = !previousTrack.getTrackAlbumKey().equals(track.getTrackAlbumKey());
                }
            } else {
                return VIEW_TYPES.TYPE_SECTION_TRACK_ITEM.ordinal();
            }
        }
        return newAlbum ? VIEW_TYPES.TYPE_SECTION_TRACK_ITEM.ordinal() : VIEW_TYPES.TYPE_TRACK_ITEM.ordinal();
    }

    /**
     * @return The count of values in the enum {@link VIEW_TYPES}.
     */
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPES.TYPE_COUNT.ordinal();
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     *
     * @param position    The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent      The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // get the trackmodel for the current position from the PBS
        TrackModel currentTrack;
        try {
            if (mPlayBackServiceConnection != null) {
                currentTrack = mPlayBackServiceConnection.getPBS().getPlaylistSong(position);
            } else {
                currentTrack = new TrackModel();
            }
        } catch (RemoteException e) {
            currentTrack = new TrackModel();
        }

        // Get the view type for the current position
        VIEW_TYPES type = VIEW_TYPES.values()[getItemViewType(position)];

        ListViewItem listViewItem;
        // Check if section view type or normal track
        if (type == VIEW_TYPES.TYPE_TRACK_ITEM) {
            // Check if view is recyclable
            if (convertView != null) {
                listViewItem = (ListViewItem) convertView;
                listViewItem.setTrack(mContext, currentTrack, position == mCurrentPlayingIndex);
            } else {
                listViewItem = new ListViewItem(mContext, currentTrack, position == mCurrentPlayingIndex, this);
            }
        } else {
            // Check if view is recyclable
            if (convertView != null) {
                listViewItem = (ListViewItem) convertView;
                listViewItem.setTrack(mContext, currentTrack, currentTrack.getTrackAlbumName(), position == mCurrentPlayingIndex);
            } else {
                listViewItem = new ListViewItem(mContext, currentTrack, currentTrack.getTrackAlbumName(), position == mCurrentPlayingIndex, this);
            }
            if ( !mHideArtwork ) {
                listViewItem.prepareArtworkFetching(mArtworkManager, currentTrack);
            } else {
                // Instead reset image
                listViewItem.setImage(null);
            }

            // Check if the scroll speed currently is already 0, then start the image task right away.
            if (mScrollSpeed == 0) {
                listViewItem.startCoverImageTask();
            }
        }

        return listViewItem;
    }

    /**
     * Update the playlist size and the index of the current track.
     *
     * @param info The NowplayingInformation object containing the new playlist size and the current track index
     */
    public void updateState(NowPlayingInformation info) {
        mCurrentPlayingIndex = info.getPlayingIndex();
        mPlaylistSize = info.getPlaylistLength();
        mTrackCache.evictAll();
        notifyDataSetChanged();
    }

    public void hideArtwork(boolean enable) {
        mHideArtwork = enable;
        notifyDataSetChanged();
    }
}
