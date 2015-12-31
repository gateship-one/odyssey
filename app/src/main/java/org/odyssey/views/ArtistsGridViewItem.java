package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.odyssey.R;
import org.odyssey.utils.AsyncLoader;

import java.lang.ref.WeakReference;

public class ArtistsGridViewItem extends RelativeLayout implements GenericGridItem{

    private TextView mTitleView;
    private ImageView mImageView;
    private ViewSwitcher mSwitcher;

    private AsyncLoader.CoverViewHolder mHolder;
    private boolean mCoverDone = false;

    public ArtistsGridViewItem(Context context, String title, String imageURL, ViewGroup.LayoutParams layoutParams) {
        super(context);

        setLayoutParams(layoutParams);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gridview_item_artists, this, true);
        setLayoutParams(layoutParams);
        mTitleView = ((TextView) this.findViewById(R.id.item_artists_title));
        mTitleView.setText(title);

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.coverViewReference = new WeakReference<ImageView>((ImageView) this.findViewById(R.id.item_artists_cover_image));
        mHolder.coverViewSwitcher = new WeakReference<ViewSwitcher>((ViewSwitcher) this.findViewById(R.id.item_artists_view_switcher));
        mHolder.imagePath = imageURL;

        mSwitcher = (ViewSwitcher) this.findViewById(R.id.item_artists_view_switcher);
        mImageView = (ImageView) this.findViewById(R.id.item_artists_cover_image);

    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    /*
    * Sets the new image url for this particular gridItem. If already an image
    * getter task is running it will be cancelled. The image is reset to the
    * dummy picture.
    */
    public void setImageURL(String url) {
        // Cancel old task
        if (mHolder.task != null) {
            mHolder.task.cancel(true);
            mHolder.task = null;
        }
        mCoverDone = false;
        mHolder.imagePath = url;
        mSwitcher.setOutAnimation(null);
        mSwitcher.setInAnimation(null);
        mImageView.setImageDrawable(null);
        mSwitcher.setDisplayedChild(0);
        mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
    }

    /*
     * If this GridItem gets detached from the parent it makes no sense to let
     * the task for image retrieval runnig. (non-Javadoc)
     *
     * @see android.view.View#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHolder.task != null) {
            mHolder.task.cancel(true);
            mHolder.task = null;
        }
    }

    /*
     * Starts the image retrieval task
     */
    public void startCoverImageTask() {
        if (mHolder.imagePath != null && mHolder.task == null && !mCoverDone) {
            mCoverDone = true;
            mHolder.task = new AsyncLoader();
            mHolder.task.execute(mHolder);
        }
    }
}
