package org.odyssey.views;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.odyssey.utils.AsyncLoader;

import java.lang.ref.WeakReference;

public abstract class GenericGridItem extends RelativeLayout {

    protected AsyncLoader.CoverViewHolder mHolder;
    protected boolean mCoverDone = false;

    protected ImageView mImageView;
    protected TextView mTitleView;
    protected ViewSwitcher mSwitcher;

    public GenericGridItem(Context context, String imageURL, ViewGroup.LayoutParams layoutParams) {
        super(context);

        setLayoutParams(layoutParams);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(provideLayout(), this, true);
        setLayoutParams(layoutParams);

        mImageView = provideImageView();
        mTitleView = provideTitleView();

        mSwitcher = provideViewSwitcher();

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.coverViewReference = new WeakReference<ImageView>(provideImageView());
        mHolder.coverViewSwitcher = new WeakReference<ViewSwitcher>(provideViewSwitcher());
        mHolder.imagePath = imageURL;
        mHolder.imageDimension = new Pair<Integer,Integer>(mImageView.getWidth(),mImageView.getHeight());
    }

    /* Methods needed to provide generic imageview, generic and textview
    viewswitcher and layout to inflate.
     */
    abstract ImageView provideImageView();

    abstract TextView provideTitleView();

    abstract ViewSwitcher provideViewSwitcher();

    abstract int provideLayout();

    /*
    * Sets the title for the GridItem
     */
    public void setTitle(String text) {
        mTitleView.setText(text);
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

}
