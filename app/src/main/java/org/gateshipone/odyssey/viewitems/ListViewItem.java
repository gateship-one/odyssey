/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.viewitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
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
     * TextView for the title of the item.
     */
    private final TextView mTitleView;

    /**
     * TextView for the subtitle of the item.
     */
    private final TextView mSubtitleView;

    /**
     * TextView for the additional subtitle of the item.
     */
    private final TextView mAdditionalSubtitleView;

    /**
     * ImageView for the icon of the item.
     */
    private ImageView mIconView;

    /**
     * TextView for the section title of the item.
     */
    private TextView mSectionTitleView;

    public static ListViewItem createAlbumItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(
                context,
                R.layout.listview_item_image,
                R.id.item_image,
                R.id.item_image_switcher,
                false,
                false,
                true,
                adapter
        );
    }

    public static ListViewItem createArtistItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(
                context,
                R.layout.listview_item_image,
                R.id.item_image,
                R.id.item_image_switcher,
                false,
                false,
                true,
                adapter
        );
    }

    public static ListViewItem createSavedPlaylistItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(context,
                R.layout.listview_item_file,
                0,
                0,
                true,
                false,
                false,
                adapter);
    }

    public static ListViewItem createBookmarkItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(context,
                R.layout.listview_item_file,
                0,
                0,
                true,
                false,
                false,
                adapter);
    }

    public static ListViewItem createFileItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(context,
                R.layout.listview_item_file,
                0,
                0,
                true,
                false,
                false,
                adapter);
    }

    public static ListViewItem createAlbumTrackItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(context,
                R.layout.listview_item,
                0,
                0,
                false,
                false,
                false,
                adapter);
    }

    public static ListViewItem createCurrentPlaylistItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(context,
                R.layout.listview_item,
                0,
                0,
                false,
                false,
                false,
                adapter);
    }

    public static ListViewItem createCurrentPlaylistSectionItem(final Context context, final ScrollSpeedAdapter adapter) {
        return new ListViewItem(context,
                R.layout.listview_item_section,
                R.id.section_header_image,
                R.id.section_header_image_switcher,
                false,
                true,
                false,
                adapter);
    }

    private ListViewItem(final Context context,
                         @LayoutRes final int layoutID,
                         @IdRes final int imageviewID,
                         @IdRes final int switcherID,
                         final boolean hasIcon,
                         final boolean hasSection,
                         final boolean hideSubtitles,
                         final ScrollSpeedAdapter adapter) {
        super(context, layoutID, imageviewID, switcherID, adapter);

        mTitleView = findViewById(R.id.item_title);
        mSubtitleView = findViewById(R.id.item_subtitle);
        mAdditionalSubtitleView = findViewById(R.id.item_additional_subtitle);

        if (hideSubtitles) {
            mSubtitleView.setVisibility(View.GONE);
            mAdditionalSubtitleView.setVisibility(View.GONE);
        }

        if (hasIcon) {
            mIconView = findViewById(R.id.item_icon);

            ViewGroup basicItemLayout = findViewById(R.id.basic_listview_item);
            basicItemLayout.setPadding(0, 0, basicItemLayout.getPaddingRight(), 0);
        }

        if (hasSection) {
            mSectionTitleView = findViewById(R.id.section_header_text);
        }
    }

    /**
     * Extracts the information from a track model (without currently played track indicator).
     *
     * @param track The current track model.
     */
    public void setAlbumTrack(final TrackModel track, final boolean showDiscNumber) {
        // title (number + name)
        String trackTitle = "";
        int trackNumber = track.getTrackNumber();
        String formattedTrackNumber = FormatHelper.formatTrackNumber(trackNumber);
        String albumName = track.getTrackAlbumName();
        String artistName = track.getTrackArtistName();

        if (showDiscNumber) {
            String discNumber = FormatHelper.formatDiscNumber(trackNumber);

            if (!discNumber.isEmpty()) {
                trackTitle = discNumber + ".";
            }
        }

        if (!formattedTrackNumber.isEmpty()) {
            trackTitle += formattedTrackNumber + " - ";
        }

        trackTitle += track.getTrackDisplayedName();

        // subtitle (artist + album)
        String trackSubtitle = artistName;

        if (!trackSubtitle.isEmpty()) {
            trackSubtitle += " - " + albumName;
        } else {
            trackSubtitle = albumName;
        }

        // duration
        String trackDuration = FormatHelper.formatTracktimeFromMS(getContext(), track.getTrackDuration());

        setTitle(trackTitle);
        setSubtitle(trackSubtitle);
        setAddtionalSubtitle(trackDuration);
    }

    /**
     * Extracts the information from a track model.
     *
     * @param track     The current track model.
     * @param isPlaying Flag if the current item should be marked as currently played track.
     */
    public void setTrack(final TrackModel track, final boolean isPlaying) {
        setAlbumTrack(track, false);

        setPlaying(isPlaying);
    }

    /**
     * Extracts the information from a track model with section title.
     *
     * @param track        The current track model.
     * @param sectionTitle The title of the section.
     * @param isPlaying    Flag if the current item should be marked as currently played track.
     */
    public void setTrack(final TrackModel track, final String sectionTitle, final boolean isPlaying) {
        setAlbumTrack(track, false);

        setSectionTitle(sectionTitle);

        setPlaying(isPlaying);
    }

    /**
     * Extracts the information from a album model.
     *
     * @param album The current album model.
     */
    public void setAlbum(final AlbumModel album) {
        setTitle(album.getAlbumName());
    }

    /**
     * Extracts the information from an artist model.
     *
     * @param artist The current artist model.
     */
    public void setArtist(final ArtistModel artist) {
        setTitle(artist.getArtistName());
    }

    /**
     * Extracts the information from a bookmark model.
     *
     * @param bookmark The current bookmark model.
     */
    public void setBookmark(final BookmarkModel bookmark) {
        final Context context = getContext();

        // title
        String bookmarkTitle = bookmark.getTitle();

        // number of tracks
        int numberOfTracks = bookmark.getNumberOfTracks();

        String numberOfTracksString = "";

        if (numberOfTracks > 0) {
            // set number of tracks only if this bookmark contains tracks
            numberOfTracksString = numberOfTracks + " " + context.getString(R.string.fragment_bookmarks_tracks);
        }

        // get date string
        long id = bookmark.getId();

        String dateString = "";
        if (id > 0) {
            // set date string only if id of this bookmark is valid
            dateString = FormatHelper.formatTimeStampToString(bookmark.getId());
        }

        // get icon
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_bookmark_black_48dp);

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
     * @param playlist The current playlist model.
     */
    public void setPlaylist(final PlaylistModel playlist) {
        final Context context = getContext();

        // title
        String playlistTitle = playlist.getPlaylistName();

        // get icon
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_queue_music_48dp);

        if (icon != null) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(context, R.attr.odyssey_color_text_background_secondary);
            // tint the icon
            DrawableCompat.setTint(icon, tintColor);
        }

        setTitle(playlistTitle);

        switch (playlist.getPlaylistType()) {
            case MEDIASTORE:
                setSubtitle(context.getString(R.string.playlist_type_global));
                setAddtionalSubtitle("");
                break;
            case ODYSSEY_LOCAL:
                setSubtitle(context.getString(R.string.playlist_type_local));
                String numberOfTracksString = "";
                int numberOfTracks = playlist.getPlaylistTracks();

                if (numberOfTracks > 0) {
                    numberOfTracksString = numberOfTracks + " " + context.getString(R.string.fragment_bookmarks_tracks);
                }
                setAddtionalSubtitle(numberOfTracksString);
                break;
            case UNKNOWN:
            case CREATE_NEW:
            case FILE:
                setSubtitle("");
                setAddtionalSubtitle("");
                break;
        }

        setIcon(icon);
    }

    /**
     * Extracts the information from a file model.
     *
     * @param file The current file model.
     */
    public void setFile(final FileModel file) {
        final Context context = getContext();

        // title
        String title = file.getName();

        // get icon for filetype
        Drawable icon;
        if (file.isDirectory()) {
            // choose directory icon
            icon = ContextCompat.getDrawable(context, R.drawable.ic_folder_48dp);
        } else {
            // choose file icon
            icon = ContextCompat.getDrawable(context, R.drawable.ic_file_48dp);
        }

        if (icon != null) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(context, R.attr.odyssey_color_text_background_secondary);
            // tint the icon
            DrawableCompat.setTint(icon, tintColor);
        }

        // last modified
        String lastModifiedDateString = FormatHelper.formatTimeStampToString(file.getLastModified());

        setTitle(title);
        setSubtitle(lastModifiedDateString);

        setIcon(icon);
    }

    /**
     * Sets the title for the item (top line).
     *
     * @param title The title as a string (i.e. a combination of number and title of a track)
     */
    private void setTitle(final String title) {
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
