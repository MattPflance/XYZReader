package com.example.xyzreader.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

public class ArticleListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private final String CURRENT_POSITION_KEY = "Current Position";
    private final String LAST_POSITION_KEY = "Last Position";
    private final int LAST_POSITION_UNKNOWN = -1;

    private RecyclerView mRecyclerView;
    private View mRootView;

    private int mCurrentPosition;
    private int mLastPosition;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            refresh();
            mCurrentPosition = 0;
            mLastPosition = LAST_POSITION_UNKNOWN;
        } else {
            mCurrentPosition = savedInstanceState.getInt(CURRENT_POSITION_KEY);
            mLastPosition = savedInstanceState.getInt(LAST_POSITION_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_list, container, false);

        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        return mRootView;
    }

    private void refresh() {
        getActivity().startService(new Intent(getActivity(), UpdaterService.class));
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putInt(CURRENT_POSITION_KEY, mCurrentPosition);
        out.putInt(LAST_POSITION_KEY, mLastPosition);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ArticleListActivity.getTwoPane()) {

                        mLastPosition = mCurrentPosition;
                        mCurrentPosition = mRecyclerView.getChildLayoutPosition(view);

                        notifyDataSetChanged();

                        // Replace the detail fragment
                        ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(getItemId(mCurrentPosition));
                        ((Activity)view.getContext()).getFragmentManager().beginTransaction()
                                .replace(R.id.article_detail_fragment_container, fragment, ArticleListActivity.getDetailfragmentTag())
                                .commit();
                    } else {
                        Uri uri = ItemsContract.Items.buildItemUri(getItemId(mRecyclerView.getChildLayoutPosition(view)));
                        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            String subtitleText = String.format(
                    getString(R.string.subtitle_text),
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString(),
                    mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.subtitleView.setText(subtitleText);

            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(getActivity()).getImageLoader());

            if (mCurrentPosition == position) {
                holder.cardView.setBackgroundColor(ContextCompat.getColor(mRootView.getContext(), R.color.theme_accent));
            } else {
                holder.cardView.setBackgroundColor(ContextCompat.getColor(mRootView.getContext(), R.color.white));
            }
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public NetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.article_list_item_card_view);
            thumbnailView = (NetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
