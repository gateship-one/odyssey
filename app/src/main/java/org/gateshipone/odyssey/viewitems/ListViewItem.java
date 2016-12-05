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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;

/**
 * Class that can be used for all list type items (albumtracks, playlist tracks, playlists, directories, etc)
 */
public class ListViewItem extends GenericImageViewItem {

    /**
     * Enum that specifies the current type of the item.
     */
    public enum LISTVIEWTYPE {
        SIMPLE_TRACK_ITEM,
        SECTION_TRACK_ITEM,
        IMAGE_ITEM,
        BOOKMARK_ITEM,
        PLAYLIST_ITEM,
        FILE_ITEM
    }

    /**
     * TextView for the title of the item.
     */
    private TextView mTitleView;

    /**
     * TextView for the subtitle of the item.
     */
    private TextView mSubtitleView;

    /**
     * TextView for the additional subtitle of the item.
     */
    private TextView mAdditionalSubtitleView;

    /**
     * ImageView for the icon of the item.
     */
    private ImageView mIconView;

    /**
     * TextView for the section title of the item.
     */
    private TextView mSectionTitleView;

    /**
     * The type of the item.
     */
    private LISTVIEWTYPE mViewType;

    /**
     * Base constructor to create a non section-type element.
     *
     * @param context  The current context.
     * @param showIcon If the icon of the item should be shown. It is not changeable after creation.
     * @param adapter  The scroll speed adapter for cover loading.
     */
    private ListViewItem(final Context context, final boolean showIcon, final ScrollSpeedAdapter adapter) {
        super(context, R.layout.listview_item_file, 0, 0, adapter);

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);
        mIconView = (ImageView) findViewById(R.id.item_icon);

        if (showIcon) {
            mIconView.setVisibility(VISIBLE);
            ViewGroup basicItemLayout = (ViewGroup) findViewById(R.id.basic_listview_item);
            basicItemLayout.setPadding(0, 0, basicItemLayout.getPaddingRight(), 0);
        } else {
            mIconView.setVisibility(GONE);
        }
    }

    /**
     * Base constructor to create a section-type element.
     *
     * @param context The current context.
     * @param adapter The scroll speed adapter for cover loading.
     */
    private ListViewItem(final Context context, final ScrollSpeedAdapter adapter) {
        super(context, R.layout.listview_item_section, R.id.section_header_image, R.id.section_header_image_switcher, adapter);

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);
        mSectionTitleView = (TextView) findViewById(R.id.section_header_text);
    }

    /**
     * Constructor to create a image item element (i.e. for artists or albums).
     *
     * @param context The current context.
     * @param title   The title of the item.
     * @param adapter The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final String title, final ScrollSpeedAdapter adapter) {
        super(context, R.layout.listview_item_image, R.id.item_image, R.id.item_image_switcher, adapter);

        mViewType = LISTVIEWTYPE.IMAGE_ITEM;

        mTitleView = (TextView) findViewById(R.id.item_title);
        mSubtitleView = (TextView) findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = (TextView) findViewById(R.id.item_additional_subtitle);

        // hide subtitles
        mSubtitleView.setVisibility(GONE);
        mAdditionalSubtitleView.setVisibility(GONE);

        setTitle(title);
    }

    /**
     * Derived constructor to increase speed by directly selecting the right methods for the need of the using adapter.
     * <p>
     * Constructor that creates a section item for the given track.
     *
     * @param context      The current context.
     * @param track        The current track model.
     * @param sectionTitle The title of the section.
     * @param isPlaying    Flag if the current item should be marked as currently played track.
     * @param adapter      The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final TrackModel track, final String sectionTitle, final boolean isPlaying, final ScrollSpeedAdapter adapter) {
        this(context, adapter);

        mViewType = LISTVIEWTYPE.SECTION_TRACK_ITEM;

        setTrack(context, track, sectionTitle, isPlaying);
    }

    /**
     * Derived constructor to increase speed by directly selecting the right methods for the need of the using adapter.
     * <p>
     * Constructor that creates a basic item for the given track (without currently played track indicator).
     *
     * @param context The current context.
     * @param track   The current track model.
     * @param adapter The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final TrackModel track, final ScrollSpeedAdapter adapter) {
        this(context, false, adapter);

        mViewType = LISTVIEWTYPE.SIMPLE_TRACK_ITEM;

        setTrack(context, track);
    }

    /**
     * Derived constructor to increase speed by directly selecting the right methods for the need of the using adapter.
     * <p>
     * Constructor that creates a basic item for the given track.
     *
     * @param context   The current context.
     * @param track     The current track model.
     * @param isPlaying Flag if the current item should be marked as currently played track.
     * @param adapter   The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final TrackModel track, final boolean isPlaying, final ScrollSpeedAdapter adapter) {
        this(context, false, adapter);

        mViewType = LISTVIEWTYPE.SIMPLE_TRACK_ITEM;

        setTrack(context, track, isPlaying);
    }

    /**
     * Derived constructor to increase speed by directly selecting the right methods for the need of the using adapter.
     * <p>
     * Constructor that creates a basic item for the given file.
     *
     * @param context The current context.
     * @param file    The current file model.
     * @param adapter The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final FileModel file, final ScrollSpeedAdapter adapter) {
        this(context, true, adapter);

        mViewType = LISTVIEWTYPE.FILE_ITEM;

        setFile(context, file);
    }

    /**
     * Derived constructor to increase speed by directly selecting the right methods for the need of the using adapter.
     * <p>
     * Constructor that creates a basic item for the given playlist. The subtitle views will be hidden.
     *
     * @param context  The current context.
     * @param playlist The current playlist model.
     * @param adapter  The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final PlaylistModel playlist, final ScrollSpeedAdapter adapter) {
        this(context, true, adapter);

        mViewType = LISTVIEWTYPE.PLAYLIST_ITEM;

        setPlaylist(context, playlist);

        // hide subtitles
        mSubtitleView.setVisibility(GONE);
        mAdditionalSubtitleView.setVisibility(GONE);
    }

    /**
     * Derived constructor to increase speed by directly selecting the right methods for the need of the using adapter.
     * <p>
     * Constructor that creates a basic item for the given bookmark.
     *
     * @param context  The current context.
     * @param bookmark The current bookmark model.
     * @param adapter  The scroll speed adapter for cover loading.
     */
    public ListViewItem(final Context context, final BookmarkModel bookmark, final ScrollSpeedAdapter adapter) {
        this(context, true, adapter);

        mViewType = LISTVIEWTYPE.BOOKMARK_ITEM;

        setBookmark(context, bookmark);
    }

    /**
     * Extracts the information from a track model (without currently played track indicator).
     *
     * @param context The current context.
     * @param track   The current track model.
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
     * Extracts the information from a track model.
     *
     * @param context   The current context.
     * @param track     The current track model.
     * @param isPlaying Flag if the current item should be marked as currently played track.
     */
    public void setTrack(final Context context, final TrackModel track, final boolean isPlaying) {
        setTrack(context, track);

        setPlaying(isPlaying);
    }

    /**
     * Extracts the information from a track model with section title.
     *
     * @param context      The current context.
     * @param track        The current track model.
     * @param sectionTitle The title of the section.
     * @param isPlaying    Flag if the current item should be marked as currently played track.
     */
    public void setTrack(final Context context, final TrackModel track, final String sectionTitle, final boolean isPlaying) {
        setTrack(context, track);

        setSectionTitle(sectionTitle);

        setPlaying(isPlaying);
    }

    /**
     * Extracts the information from a bookmark model.
     *
     * @param context  The current context.
     * @param bookmark The current bookmark model.
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
     * Extracts the information from a playlist model.
     *
     * @param context  The current context.
     * @param playlist The current playlist model.
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
     * Extracts the information from a file model.
     *
     * @param context The current context.
     * @param file    The current file model.
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

    /**
     * Getter for the view type of the item.
     *
     * @return The view type of the item as a LISTVIEWTYPE
     */
    public LISTVIEWTYPE getViewType() {
        return mViewType;
    }

    /**
     * Sets the title for the item (top line).
     *
     * @param title The title as a string (i.e. a combination of number and title of a track)
     */
    public void setTitle(final String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the subtitle for the item (left side).
     *
     * @param subtitle The subtitle as a string (i.e. a combination of artist and album name of a track)
     */
    private void setSubtitle(final String subtitle) {
        mSubtitleView.setText(subtitle);
    }

    /**
     * Sets the additional subtitle for the item (right side).
     *
     * @param additionalSubtitle The additional subtitle as a string (i.e. the duration of a track)
     */
    private void setAddtionalSubtitle(final String additionalSubtitle) {
        mAdditionalSubtitleView.setText(additionalSubtitle);
    }

    /**
     * Sets the icon for the item.
     *
     * @param icon The icon as a drawable (i.e. used for files, playlists, bookmarks etc)
     */
    private void setIcon(final Drawable icon) {
        mIconView.setImageDrawable(icon);
    }

    /**
     * Sets the title of the section for the item.
     *
     * @param sectionTitle The section title as a string (i.e. the title of the album of a track)
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
