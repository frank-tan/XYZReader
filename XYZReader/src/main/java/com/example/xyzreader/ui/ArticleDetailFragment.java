package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "XYZReader";
    private static final String THUMBNAIL = "THUMBNAIL";
    private static final String FULL_SIZE = "FULL_SIZE";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ScrollView mScrollView;

    private ImageView mPhotoView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mScrollView = (ScrollView) mRootView.findViewById(R.id.scrollview);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mScrollView.setNestedScrollingEnabled(true);
        }

        mPhotoView = (ImageView) getActivity().findViewById(R.id.photo);

        getActivity().findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        if (mCursor != null) {

            Log.i(TAG,"bindViews1: "+mCursor.getString(ArticleLoader.Query.TITLE));
            Log.i(TAG, "bindViews2: " + mCursor.getString(ArticleLoader.Query.PHOTO_URL));

            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    public void setToolbarImage() {
        if (mCursor != null) {
            // load low resolution thumb nail picture first
            // It is a lot faster, so the shared element transition works well
            setImageWithUrl(mCursor.getString(ArticleLoader.Query.THUMB_URL), THUMBNAIL);
        }
    }

    private void setImageWithUrl (final String url, final String type) {
        final Context appContext = getActivity().getApplicationContext();
        final String imageUrl = url;
        Picasso.with(appContext)
                .load(imageUrl)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .noPlaceholder()
                .fit()
                .centerCrop()
                .into(mPhotoView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Picasso loaded backdrop image successfully from local cache");
                        // load high resolution picture afterwards
                        if(type == THUMBNAIL) {
                            setTitleBackgroundDarkMutedColour();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getActivity().startPostponedEnterTransition();
                            }
                            setImageWithUrl(mCursor.getString(ArticleLoader.Query.PHOTO_URL),FULL_SIZE);
                        }
                    }

                    @Override
                    public void onError() {

                        Log.e(TAG, "Picasso failed to load backdrop image");
                        Picasso.with(appContext)
                                .load(imageUrl)
                                .noPlaceholder()
                                .fit()
                                .centerCrop()
                                .into(mPhotoView, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.i(TAG, "Picasso loaded backdrop image successfully from Network");
                                        // load high resolution picture afterwards
                                        if(type == THUMBNAIL) {
                                            setTitleBackgroundDarkMutedColour();
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                getActivity().startPostponedEnterTransition();
                                            }
                                            setImageWithUrl(mCursor.getString(ArticleLoader.Query.PHOTO_URL),FULL_SIZE);
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            getActivity().startPostponedEnterTransition();
                                        }
                                    }
                                });
                    }
                });
    }

    private void setTitleBackgroundDarkMutedColour () {
        Palette.PaletteAsyncListener paletteListener = new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                mMutedColor = palette.getDarkMutedColor(0xFF333333);
                mRootView.findViewById(R.id.meta_bar)
                        .setBackgroundColor(mMutedColor);
            }
        };

        Bitmap bitmap = ((BitmapDrawable)mPhotoView.getDrawable()).getBitmap();
        if (bitmap != null && !bitmap.isRecycled()) {
            Palette.from(bitmap).generate(paletteListener);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
