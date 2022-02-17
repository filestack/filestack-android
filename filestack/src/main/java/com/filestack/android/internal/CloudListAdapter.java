package com.filestack.android.internal;

import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.CloudItem;
import com.filestack.CloudResponse;
import com.filestack.android.R;
import com.filestack.android.Selection;
import com.filestack.android.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Loads metadata for a user's cloud contents from Cloud API (cloudrouter) into the UI of
 * {{@link CloudListFragment}}. {{@link CloudListFragment}}, {{@link CloudListAdapter}}, and
 * {{@link CloudListViewHolder}} work together to create the cloud sources list. This works like an
 * "infinite scroll" list; We don't have an upfront count or a concept of pages. When the user
 * scrolls past a certain threshold count of items relative to what we have data for, we
 * automatically load more items. We know we've loaded all the items when the Cloud API doesn't
 * return a "next" token.
 *
 * As we load items, we save them into a hash map. We map path strings to lists of cloud items in
 * that path. We have to combine folder navigation with the fact that we don't know everything
 * that's inside a folder until we reach the end, so we also keep a hash map of path strings to
 * "next" token strings. For example if the user goes back a directory, and scrolls further than
 * before, we need to remember the "next" token from the last request made for that path. We could
 * simplify this and just reload items with directory changes, but this was done to make navigating
 * back and forth faster.
 *
 * @see <a href="https://developer.android.com/guide/topics/ui/layout/recyclerview">
 *     https://developer.android.com/guide/topics/ui/layout/recyclerview</a>
 */
class CloudListAdapter extends RecyclerView.Adapter<CloudListViewHolder> implements
        SingleObserver<CloudResponse>, View.OnClickListener, BackButtonListener {

    private static final double LOAD_TRIGGER = 0.50;
    private final static String STATE_CURRENT_PATH = "currentPath";
    private final static String STATE_FOLDERS = "folders";
    private final static String STATE_NEXT_TOKENS= "nextTokens";

    private boolean isLoading;
    private final HashMap<String, ArrayList<CloudItem>> folders;
    private final HashMap<String, String> nextTokens;
    private final String sourceId;
    private final String[] mimeTypes;
    private final Selector selector;
    private final Theme theme;
    private int viewType;
    private RecyclerView recyclerView;
    private String currentPath;


    CloudListAdapter(String sourceId, String[] mimeTypes, Bundle saveInstanceState,
                     Selector selector, Theme theme) {
        this.sourceId = sourceId;
        this.mimeTypes = mimeTypes;
        this.selector = selector;
        this.theme = theme;
        setHasStableIds(true);
        if (saveInstanceState != null) {
            currentPath = saveInstanceState.getString(STATE_CURRENT_PATH);
            folders = (HashMap) saveInstanceState.getSerializable(STATE_FOLDERS);
            nextTokens = (HashMap) saveInstanceState.getSerializable(STATE_NEXT_TOKENS);
            if (folders == null || folders.get(currentPath) == null) {
                folders.put(currentPath, new ArrayList<CloudItem>());
            }
        } else {
            folders = new HashMap<>();
            nextTokens = new HashMap<>();
            setPath("/");
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public CloudListViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View listItemView = inflater.inflate(viewType, viewGroup, false);
        return new CloudListViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(CloudListViewHolder holder, int i) {
        ArrayList<CloudItem> items = folders.get(currentPath);
        CloudItem item = items.get(i);

        holder.apply(theme);
        holder.setId(i);
        holder.setName(item.getName());
        String info = formatInfo(item);
        holder.setInfo(info);
        holder.setInfoVisible(!TextUtils.isEmpty(info));
        holder.setIcon(item.getThumbnail());
        holder.setOnClickListener(this);
        holder.setEnabled(item.isFolder() || Util.mimeAllowed(mimeTypes, item.getMimetype()));
        Selection selection = SelectionFactory.from(sourceId, item);
        holder.setSelected(selector.isSelected(selection));
        String nextToken = nextTokens.get(currentPath);
        if (!isLoading) {
            if (nextToken != null && i >= (LOAD_TRIGGER * items.size())) {
                loadMoreData();
            }
        }
    }

    private String formatInfo(CloudItem item) {
        if (item.getSize() == 0 && item.getMimetype() == null) {
            return "";
        }
        String fileSize = Formatter.formatShortFileSize(recyclerView.getContext(), item.getSize());
        if (item.getMimetype() == null) {
            return String.format(Locale.getDefault(), "%s", fileSize);
        }
        return String.format(Locale.getDefault(), "%s - %s", item.getMimetype(), fileSize);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @Override
    public int getItemCount() {
        ArrayList<CloudItem> folder = folders.get(currentPath);
        return folder != null ? folder.size(): 0;
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) { }

    @Override
    public void onSuccess(@NonNull CloudResponse cloudContents) {
        ArrayList<CloudItem> items = folders.get(currentPath);
        CloudItem[] newItems = cloudContents.getItems();

        int oldSize = items.size();
        for (CloudItem item : newItems) {
            if (!items.contains(item)) {
                items.add(item);
            }
        }
        int newSize = items.size();

        String nextToken = cloudContents.getNextToken();
        nextTokens.put(currentPath, nextToken);
        if (oldSize == 0) {
            // The new path may have fewer items than the old path
            // If we don't clear all item views, we could end up displaying items from the old path
            notifyDataSetChanged();
        } else {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        }

        // This is a temporary fix for the weird Google Drive behavior
        // We get a page with just the Trash folder, which is also a duplicate
        // TODO Change this when backend is fixed
        if (newSize == oldSize && nextToken != null) {
            loadMoreData();
        } else {
            isLoading = false;
        }
    }

    @Override
    public void onError(@NonNull Throwable e) { }

    @Override
    public void onClick(View view) {
        if (isLoading) {
            return;
        }

        int id = view.getId();
        CloudItem item = folders.get(currentPath).get(id);

        if (item.isFolder()) {
            setPath(item.getPath());
            return;
        }

        if (!Util.mimeAllowed(mimeTypes, item.getMimetype())) {
            return;
        }


        Selection selection = SelectionFactory.from(sourceId, item);
        boolean selected = selector.toggle(selection);
        CloudListViewHolder holder = (CloudListViewHolder) recyclerView.findViewHolderForItemId(id);

        holder.setSelected(selected);
    }

    @Override
    public boolean onBackPressed() {
        if (currentPath.equals("/")) {
            return false;
        }
        String newPath = Util.trimLastPathSection(currentPath);
        setPath(newPath);
        return true;
    }

    // Save the items we've loaded on orientation changes etc
    void saveState(Bundle outState) {
        outState.putString(STATE_CURRENT_PATH, currentPath);
        outState.putSerializable(STATE_FOLDERS, folders);
        outState.putSerializable(STATE_NEXT_TOKENS, nextTokens);
    }

    void setViewType(int viewType) {
        this.viewType = viewType;
    }

    // Private helper methods (alphabetical order)

    private void loadMoreData() {
        isLoading = true;
        Util.getClient()
                .getCloudItemsAsync(sourceId, currentPath, nextTokens.get(currentPath))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    private void setPath(String path) {
        currentPath = path;
        if (!folders.containsKey(path)) {
            folders.put(path, new ArrayList<CloudItem>());
            loadMoreData();
        } else {
            // Causes all item views to be rebound with data from the new path
            // We don't need this in the other case because it gets called in loadMoreData()
            notifyDataSetChanged();
        }
    }
}
