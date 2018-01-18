package com.filestack.android.internal;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.android.FsActivity;
import com.filestack.android.R;

public class CloudListFragment extends Fragment implements BackButtonListener {
    private final static String ARG_SOURCE = "source";
    private final static String STATE_IS_LIST_MODE = "isListMode";

    private boolean isListMode = true;
    private CloudListAdapter adapter;
    private SpacingDecoration spacer;
    private RecyclerView recyclerView;
    private SourceInfo sourceInfo;

    public static CloudListFragment create(String source) {
        CloudListFragment fragment = new CloudListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOURCE, source);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        sourceInfo = Util.getSourceInfo(args.getString(ARG_SOURCE));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.fragment_cloud_list, container, false);

        recyclerView = baseView.findViewById(R.id.recycler);
        adapter = new CloudListAdapter(sourceInfo.getId(), savedInstanceState);
        recyclerView.setAdapter(adapter);

        if (savedInstanceState != null) {
            isListMode = savedInstanceState.getBoolean(STATE_IS_LIST_MODE);
        }

        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        spacer = new SpacingDecoration(spacing, spacing, false);

        setupRecyclerLayout();

        return baseView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        adapter.saveState(outState);
        outState.putBoolean(STATE_IS_LIST_MODE, isListMode);
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
            setupRecyclerLayout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerLayout() {
        Context context = recyclerView.getContext();

        LinearLayoutManager layout = (LinearLayoutManager) recyclerView.getLayoutManager();
        int position = layout != null ? layout.findFirstCompletelyVisibleItemPosition() : 0;

        if (isListMode) {
            layout = new LinearLayoutManager((context));
            adapter.setViewType(R.layout.cloud_list_item);
            recyclerView.removeItemDecoration(spacer);
        } else  {
            int numColumns = getResources().getInteger(R.integer.grid_columns);
            layout = new GridLayoutManager(context, numColumns);
            adapter.setViewType(R.layout.cloud_grid_item);
            recyclerView.addItemDecoration(spacer);
        }

        recyclerView.setLayoutManager(layout);
        layout.scrollToPosition(position);
    }

    @Override
    public boolean onBackPressed() {
        return adapter.onBackPressed();
    }
}
