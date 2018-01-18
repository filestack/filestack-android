package com.filestack.android.internal;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.CloudItem;
import com.filestack.CloudResponse;
import com.filestack.android.FsActivity;
import com.filestack.android.Selection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

class CloudListAdapter extends RecyclerView.Adapter implements
        SingleObserver<CloudResponse>, View.OnClickListener, BackButtonListener {

    private static final double LOAD_TRIGGER = 0.50;
    private final static String STATE_CURRENT_PATH = "currentPath";
    private final static String STATE_FOLDERS = "folders";
    private final static String STATE_NEXT_TOKENS= "nextTokens";

    private boolean isLoading;
    private final HashMap<String, ArrayList<CloudItem>> folders;
    private final HashMap<String, String> nextTokens;
    private final String sourceId;
    private int viewType;
    private RecyclerView recyclerView;
    private String currentPath;

    CloudListAdapter(String sourceId, Bundle saveInstanceState) {
        this.sourceId = sourceId;
        setHasStableIds(true);

        if (saveInstanceState != null) {
            currentPath = saveInstanceState.getString(STATE_CURRENT_PATH);
            folders = (HashMap) saveInstanceState.getSerializable(STATE_FOLDERS);
            nextTokens = (HashMap) saveInstanceState.getSerializable(STATE_NEXT_TOKENS);
        } else {
            folders = new HashMap<>();
            nextTokens = new HashMap<>();
            setPath("/");
        }
    }

    // RecyclerView.Adapter overrides (in sequential order)

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View listItemView = inflater.inflate(viewType, viewGroup, false);
        return new CloudListViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ArrayList<CloudItem> items = folders.get(currentPath);
        CloudItem item = items.get(i);
        CloudListViewHolder holder = (CloudListViewHolder) viewHolder;

        holder.setId(i);
        holder.setName(item.getName());
        Locale locale = Locale.getDefault();
        String info = String.format(locale, "%s - %d", item.getMimetype(), item.getSize());
        holder.setInfo(info);
        holder.setIcon(item.getThumbnail());
        holder.setOnClickListener(this);

        SelectionSaver selectionSaver = Util.getSelectionSaver();
        Selection selection = new Selection(sourceId, item.getPath(), item.getMimetype(),
                item.getName());
        holder.setSelected(selectionSaver.isSelected(selection));

        String nextToken = nextTokens.get(currentPath);
        if (!isLoading) {
            if (nextToken != null && i >= (LOAD_TRIGGER * items.size())) {
                loadMoreData();
            }
        }
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

    // Interface overrides (alphabetical order)

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

        SelectionSaver selectionSaver = Util.getSelectionSaver();
        Selection selection = new Selection(sourceId, item.getPath(), item.getMimetype(),
                item.getName());
        boolean selected = selectionSaver.toggleItem(selection);
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
            notifyDataSetChanged();
        }
    }
}
