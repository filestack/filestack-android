package com.filestack.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class CloudListFragment extends Fragment implements FilestackActivity.BackListener {
    private final static int MIN_GRID_WIDTH = 135;
    private final static String ARG_CLOUD_INFO_ID = "cloudInfoId";

    private boolean isListMode = true;
    private CloudListAdapter adapter;
    private DividerItemDecoration horizontalDecor, verticalDecor;
    private int gridColumns;
    private RecyclerView recyclerView;
    private SourceInfo sourceInfo;

    public static CloudListFragment create(int cloudInfoId) {
        CloudListFragment fragment = new CloudListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLOUD_INFO_ID, cloudInfoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        sourceInfo = Util.getSourceInfo(args.getInt(ARG_CLOUD_INFO_ID));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.fragment_cloud_list, container, false);

        recyclerView = baseView.findViewById(R.id.recycler);
        adapter = new CloudListAdapter(sourceInfo.getId());

        Context context = recyclerView.getContext();
        Drawable drawable = getResources().getDrawable(R.drawable.list_grid_divider);
        horizontalDecor = new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL);
        horizontalDecor.setDrawable(drawable);
        verticalDecor = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        verticalDecor.setDrawable(drawable);

        int widthPx = container.getWidth();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int widthDp = Math.round(widthPx / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        gridColumns = widthDp / MIN_GRID_WIDTH;

        setupRecyclerView();

        return baseView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle_list_grid) {
            isListMode = !isListMode;
            if (isListMode) {
                item.setIcon(R.drawable.ic_menu_grid_white);
                item.setTitle(R.string.menu_view_grid);
            } else  {
                item.setIcon(R.drawable.ic_menu_list_white);
                item.setTitle(R.string.menu_view_list);
            }
            setupRecyclerView();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        Context context = recyclerView.getContext();

        if (isListMode) {
            adapter.setLayoutId(R.layout.cloud_list_item);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager((context)));
            recyclerView.removeItemDecoration(horizontalDecor);
            recyclerView.removeItemDecoration(verticalDecor);
        } else  {
            adapter.setLayoutId(R.layout.cloud_grid_item);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new GridLayoutManager(context, gridColumns));
            recyclerView.addItemDecoration(horizontalDecor);
            recyclerView.addItemDecoration(verticalDecor);
        }
    }

    @Override
    public boolean onBackPressed() {
        return adapter.onBackPressed();
    }
}
