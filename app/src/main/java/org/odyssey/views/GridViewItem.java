package org.odyssey.views;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.odyssey.R;
import org.odyssey.utils.AsyncLoader;

import java.lang.ref.WeakReference;

public class GridViewItem extends RelativeLayout {

    private final AsyncLoader.CoverViewHolder mHolder;
    private final ImageView mImageView;
    private final TextView mTitleView;
    private final ViewSwitcher mSwitcher;

    private boolean mCoverDone = false;

    /**
     * Constructor that already sets the values for each view.
     */
    public GridViewItem(Context context, String title, String imageURL, ViewGroup.LayoutParams layoutParams) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gridview_item, this, true);

        setLayoutParams(layoutParams);

        mImageView = (ImageView) findViewById(R.id.grid_item_cover_image);
        mTitleView = (TextView) findViewById(R.id.grid_item_title);
        mSwitcher = (ViewSwitcher) findViewById(R.id.grid_item_view_switcher);

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.coverViewReference = new WeakReference<>(mImageView);
        mHolder.coverViewSwitcher = new WeakReference<>(mSwitcher);
        mHolder.imageDimension = new Pair<>(mImageView.getWidth(), mImageView.getHeight());

        mCoverDone = false;
        mHolder.imagePath = imageURL;
        mSwitcher.setOutAnimation(null);
        mSwitcher.setInAnimation(null);
        mImageView.setImageDrawable(null);
        mSwitcher.setDisplayedChild(0);
        mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));

        mTitleView.setText(title);
    }

    /**
     * Sets the title for the GridItem
     */
    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    /**
     * Starts the image retrieval task
     */
    public void startCoverImageTask() {
        if (mHolder.imagePath != null && mHolder.task == null && !mCoverDone) {
            mCoverDone = true;
            mHolder.task = new AsyncLoader();
            mHolder.task.execute(mHolder);
        }
    }

    /**
     * Sets the new image url for this particular gridItem. If already an image
     * getter task is running it will be cancelled. The image is reset to the
     * dummy picture.
     */
    public void setImageURL(String url) {
        // Check if image url has actually changed, otherwise there is no need to redo the image.
        if ((mHolder.imagePath == null) || (!mHolder.imagePath.equals(url))) {
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
    }

    /**
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

}
