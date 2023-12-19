package com.filestack.android.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.android.FsConstants;
import com.filestack.android.R;
import com.filestack.android.Theme;

/**
 * Mostly configures a contained {{@link RecyclerView}}. {{@link CloudListFragment}},
 * {{@link CloudListAdapter}}, and {{@link CloudListViewHolder}} work together to create the cloud
 * sources list.
 *
 * @see <a href="https://developer.android.com/guide/topics/ui/layout/recyclerview">
 *     https://developer.android.com/guide/topics/ui/layout/recyclerview</a>
 */
public class CloudListFragment extends Fragment implements BackButtonListener {
    private static final String ARG_SOURCE = "source";
    private static final String ARG_ALLOW_MULTIPLE_FILES = "multipleFiles";
    private static final String STATE_IS_LIST_MODE = "isListMode";
    private static final String ARG_THEME = "theme";

    private boolean isListMode = true;
    private CloudListAdapter adapter;
    private SpacingDecoration spacer;
    private RecyclerView recyclerView;
    private SourceInfo sourceInfo;
    private Theme theme;

    public static CloudListFragment create(String source, boolean allowMultipleFiles, Theme theme) {
        CloudListFragment fragment = new CloudListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOURCE, source);
        args.putBoolean(ARG_ALLOW_MULTIPLE_FILES, allowMultipleFiles);
        args.putParcelable(ARG_THEME, theme);
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.filestack__fragment_cloud_list, container, false);
        theme = getArguments().getParcelable(ARG_THEME);
        baseView.setBackgroundColor(theme.getBackgroundColor());
        recyclerView = baseView.findViewById(R.id.recycler);
        Intent intent = requireActivity().getIntent();
        String[] mimeTypes = intent.getStringArrayExtra(FsConstants.EXTRA_MIME_TYPES);
        boolean allowMultipleFiles = getArguments().getBoolean(ARG_ALLOW_MULTIPLE_FILES);
        final Selector selector = allowMultipleFiles ?
                new Selector.Multi(Util.getSelectionSaver()) :
                new Selector.Single(Util.getSelectionSaver());

        adapter = new CloudListAdapter(sourceInfo.getId(), mimeTypes, savedInstanceState, selector, theme);
        recyclerView.setAdapter(adapter);

        if (savedInstanceState != null) {
            isListMode = savedInstanceState.getBoolean(STATE_IS_LIST_MODE);
        }

        int spacing = getResources().getDimensionPixelSize(R.dimen.filestack__grid_spacing);
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
                item.setIcon(R.drawable.filestack__ic_menu_grid_white);
                item.setTitle(R.string.filestack__menu_view_grid);
            } else  {
                item.setIcon(R.drawable.filestack__ic_menu_list_white);
                item.setTitle(R.string.filestack__menu_view_list);
            }
            setupRecyclerLayout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(true);
        menu.findItem(R.id.action_toggle_list_grid).setVisible(true);
    }

    private void setupRecyclerLayout() {
        Context context = recyclerView.getContext();

        LinearLayoutManager layout = (LinearLayoutManager) recyclerView.getLayoutManager();
        int position = layout != null ? layout.findFirstCompletelyVisibleItemPosition() : 0;

        if (isListMode) {
            layout = new LinearLayoutManager((context));
            adapter.setViewType(R.layout.filestack__cloud_list_item);
            recyclerView.removeItemDecoration(spacer);
        } else  {
            int numColumns = getResources().getInteger(R.integer.filestack__grid_columns);
            layout = new GridLayoutManager(context, numColumns);
            adapter.setViewType(R.layout.filestack__cloud_grid_item);
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
