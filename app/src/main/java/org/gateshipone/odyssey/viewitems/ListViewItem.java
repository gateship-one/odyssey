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

package org.gateshipone.odyssey.viewitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.widget.ImageView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;

public class ListViewItem extends GenericImageViewItem {

    /**
     *
     */
    public enum LISTVIEWTYPE {
        SIMPLE_TRACK_ITEM,
        SECTION_TRACK_ITEM,
        BOOKMARK_ITEM,
        PLAYLIST_ITEM,
        FILE_ITEM
    }

    /**
     *
     */
    private TextView mTitleView;

    /**
     *
     */
    private TextView mSubtitleView;

    /**
     *
     */
    private TextView mAdditionalSubtitleView;

    /**
     *
     */
    private ImageView mIconView;

    /**
     *
     */
    private TextView mSectionTitleView;

    /**
     *
     */
    private LISTVIEWTYPE mViewType;

    /**
     * @param context
     * @param showIcon
     */
    private ListViewItem(final Context context, final boolean showIcon) {
        super(context, R.layout.listview_item_file, 0, 0, null);

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);
        mIconView = (ImageView) findViewById(R.id.item_icon);

        if (showIcon) {
            mIconView.setVisibility(VISIBLE);
        } else {
            mIconView.setVisibility(GONE);
        }
    }

    /**
     * @param context
     */
    private ListViewItem(final Context context) {
        super(context, R.layout.listview_item_section, R.id.section_header_image, R.id.section_header_image_switcher, null);

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);
        mSectionTitleView = (TextView) findViewById(R.id.section_header_text);
    }

    /**
     * @param context
     * @param track
     * @param sectionTitle
     */
    public ListViewItem(final Context context, final TrackModel track, final String sectionTitle) {
        this(context);

        mViewType = LISTVIEWTYPE.SECTION_TRACK_ITEM;

        setTrack(context, track, sectionTitle);
    }

    /**
     * @param context
     * @param track
     * @param sectionTitle
     * @param isPlaying
     */
    public ListViewItem(final Context context, final TrackModel track, final String sectionTitle, final boolean isPlaying) {
        this(context);

        mViewType = LISTVIEWTYPE.SECTION_TRACK_ITEM;

        setTrack(context, track, sectionTitle, isPlaying);
    }

    /**
     * @param context
     * @param track
     */
    public ListViewItem(final Context context, final TrackModel track) {
        this(context, false);

        mViewType = LISTVIEWTYPE.SIMPLE_TRACK_ITEM;

        setTrack(context, track);
    }

    /**
     * @param context
     * @param track
     * @param isPlaying
     */
    public ListViewItem(final Context context, final TrackModel track, final boolean isPlaying) {
        this(context, false);

        mViewType = LISTVIEWTYPE.SIMPLE_TRACK_ITEM;

        setTrack(context, track, isPlaying);
    }

    /**
     * @param context
     * @param file
     */
    public ListViewItem(final Context context, final FileModel file) {
        this(context, true);

        mViewType = LISTVIEWTYPE.FILE_ITEM;

        setFile(context, file);
    }

    /**
     * @param context
     * @param playlist
     */
    public ListViewItem(final Context context, final PlaylistModel playlist) {
        this(context, true);

        mViewType = LISTVIEWTYPE.PLAYLIST_ITEM;

        setPlaylist(context, playlist);
    }

    /**
     * @param context
     * @param bookmark
     */
    public ListViewItem(final Context context, final BookmarkModel bookmark) {
        this(context, true);

        mViewType = LISTVIEWTYPE.BOOKMARK_ITEM;

        setBookmark(context, bookmark);
    }

    /**
     * @param context
     * @param track
     */
    public void setTrack(final Context context, final TrackModel track) {
        // title (number + name)
        String trackTitle = track.getTrackName();
        String trackNumber = FormatHelper.formatTrackNumber(track.getTrackNumber());
        if (!trackTitle.isEmpty() && !trackNumber.isEmpty()) {
            trackTitle = context.getString(R.string.track_title_template, trackNumber, trackTitle);
        } else if (!trackNumber.isEmpty()) {
            trackTitle = trackNumber;
        }

        // subtitle (artist + album)
        String trackSubtitle = track.getTrackAlbumName();
        if (!track.getTrackArtistName().isEmpty() && !trackSubtitle.isEmpty()) {
            trackSubtitle = context.getString(R.string.track_title_template, track.getTrackArtistName(), trackSubtitle);
        } else if (!track.getTrackArtistName().isEmpty()) {
            trackSubtitle = track.getTrackArtistName();
        }

        // duration
        String trackDuration = FormatHelper.formatTracktimeFromMS(context, track.getTrackDuration());

        setTitle(trackTitle);
        setSubtitle(trackSubtitle);
        setAddtionalSubtitle(trackDuration);
    }

    /**
     * @param context
     * @param track
     * @param isPlaying
     */
    public void setTrack(final Context context, final TrackModel track, final boolean isPlaying) {
        setTrack(context, track);

        setPlaying(isPlaying);
    }

    /**
     * @param context
     * @param track
     * @param sectionTitle
     */
    public void setTrack(final Context context, final TrackModel track, final String sectionTitle) {
        setTrack(context, track);

        setSectionTitle(sectionTitle);
    }

    /**
     * @param context
     * @param track
     * @param sectionTitle
     * @param isPlaying
     */
    public void setTrack(final Context context, final TrackModel track, final String sectionTitle, final boolean isPlaying) {
        setTrack(context, track, sectionTitle);

        setPlaying(isPlaying);
    }

    /**
     * @param context
     * @param bookmark
     */
    public void setBookmark(final Context context, final BookmarkModel bookmark) {
        // title
        String bookmarkTitle = bookmark.getTitle();

        // number of tracks
        int numberOfTracks = bookmark.getNumberOfTracks();

        String numberOfTracksString = "";

        if (numberOfTracks > 0) {
            // set number of tracks only if this bookmark contains tracks
            numberOfTracksString = Integer.toString(bookmark.getNumberOfTracks()) + " " + context.getString(R.string.fragment_bookmarks_tracks);
        }

        // get date string
        long id = bookmark.getId();

        String dateString = "";
        if (id > 0) {
            // set date string only if id of this bookmark is valid
            dateString = FormatHelper.formatTimeStampToString(context, bookmark.getId());
        }

        // get icon
        Drawable icon = context.getDrawable(R.drawable.ic_bookmark_black_48dp);

        if (icon != null) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(context, R.attr.odyssey_color_text_background_secondary);
            // tint the icon
            DrawableCompat.setTint(icon, tintColor);
        }

        setTitle(bookmarkTitle);
        setSubtitle(dateString);
        setAddtionalSubtitle(numberOfTracksString);
        setIcon(icon);
    }

    /**
     * @param context
     * @param playlist
     */
    public void setPlaylist(final Context context, final PlaylistModel playlist) {
        // title
        String playlistTitle = playlist.getPlaylistName();

        // get icon
        Drawable icon = context.getDrawable(R.drawable.ic_queue_music_48dp);

        if (icon != null) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(context, R.attr.odyssey_color_text_background_secondary);
            // tint the icon
            DrawableCompat.setTint(icon, tintColor);
        }

        setTitle(playlistTitle);
        setIcon(icon);
    }

    /**
     * @param context
     * @param file
     */
    public void setFile(final Context context, final FileModel file) {
        // title
        String title = file.getName();

        // get icon for filetype
        Drawable icon;
        if (file.isDirectory()) {
            // choose directory icon
            icon = context.getDrawable(R.drawable.ic_folder_48dp);
        } else {
            // choose file icon
            icon = context.getDrawable(R.drawable.ic_file_48dp);
        }

        if (icon != null) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(context, R.attr.odyssey_color_text_background_secondary);
            // tint the icon
            DrawableCompat.setTint(icon, tintColor);
        }

        // last modified
        String lastModifiedDateString = FormatHelper.formatTimeStampToString(context, file.getLastModified());

        setTitle(title);
        setSubtitle(lastModifiedDateString);

        setIcon(icon);
    }

    public LISTVIEWTYPE getViewType() {
        return mViewType;
    }

    /**
     * Sets the title for the item.
     *
     * @param title The title as a string (i.e. a combination of number and title of a track)
     */
    private void setTitle(final String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the subtitle for the item.
     *
     * @param subtitle The subtitle as a string (i.e. a combination of artist and album name of a track)
     */
    private void setSubtitle(final String subtitle) {
        mSubtitleView.setText(subtitle);
    }

    /**
     * Sets the additional subtitle for the item.
     *
     * @param additionalSubtitle The additional subtitle as a string (i.e. the duration of a track)
     */
    private void setAddtionalSubtitle(final String additionalSubtitle) {
        mAdditionalSubtitleView.setText(additionalSubtitle);
    }

    /**
     * @param icon
     */
    private void setIcon(final Drawable icon) {
        mIconView.setImageDrawable(icon);
    }

    /**
     * @param sectionTitle
     */
    private void setSectionTitle(final String sectionTitle) {
        mSectionTitleView.setText(sectionTitle);
    }

    /**
     * Method that tint the title view according to the state.
     *
     * @param state flag indicates if the representing track is currently marked as played by the playbackservice
     */
    private void setPlaying(final boolean state) {
        if (state) {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
            mTitleView.setTextColor(color);
        } else {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_background_primary);
            mTitleView.setTextColor(color);
        }
    }

}
