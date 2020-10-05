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

import org.gateshipone.odyssey.artwork.ArtworkManager;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.TrackModel;

import androidx.recyclerview.widget.RecyclerView;

public class GenericViewItemHolder extends RecyclerView.ViewHolder {

    public GenericViewItemHolder(final GenericImageViewItem itemView) {
        super(itemView);
    }

    public void prepareArtworkFetching(final ArtworkManager artworkManager, final AlbumModel album) {
        ((GenericImageViewItem) itemView).prepareArtworkFetching(artworkManager, album);
    }

    public void startCoverImageTask() {
        ((GenericImageViewItem) itemView).startCoverImageTask();
    }

    public void setImageDimensions(final int width, final int height) {
        ((GenericImageViewItem) itemView).setImageDimension(width, height);
    }

    public void setAlbumTrack(final TrackModel trackModel, final boolean mShowDiscNumber) {
        ((ListViewItem) itemView).setAlbumTrack(trackModel, mShowDiscNumber);
    }

    public void setAlbum(final AlbumModel album) {
        if (itemView instanceof ListViewItem) {
            ((ListViewItem) itemView).setAlbum(album);
        } else if (itemView instanceof GridViewItem) {
            ((GridViewItem) itemView).setAlbum(album);
        }
    }
}
