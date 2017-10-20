package com.filestack.android;

import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.CloudContents;
import com.filestack.CloudItem;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

class CloudListAdapter extends RecyclerView.Adapter implements
        SingleObserver<CloudContents>, View.OnClickListener, View.OnLongClickListener,
        FsActivity.BackListener {

    private static final double LOAD_TRIGGER = 0.50;

    private final FsAndroidClient.Provider clientProvider;
    private final String sourceId;

    private RecyclerView recyclerView;
    private int layoutId;

    private String currentPath;
    private boolean isLoading;
    private boolean multiSelectMode;

    private ArrayMap<String, ArrayList<CloudItem>> folders;
    private ArrayMap<String, String> nextTokens;
    private ArrayList<Integer> selected;

    CloudListAdapter(FsAndroidClient.Provider clientProvider, String sourceId) {
        this.clientProvider = clientProvider;
        this.sourceId = sourceId;

        folders = new ArrayMap<>();
        nextTokens = new ArrayMap<>();
        selected = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View listItemView = inflater.inflate(layoutId, viewGroup, false);
        return new CloudListViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ArrayList<CloudItem> items = folders.get(currentPath);
        CloudItem item = items.get(i);
        CloudListViewHolder holder = (CloudListViewHolder) viewHolder;

        holder.setId(i);
        holder.setName(item.getName());
        String info = String.format("%s - %d", item.getMimetype(), item.getSize());
        holder.setInfo(info);
        holder.setIcon(item.getThumbnail());
        holder.setSelected(selected.contains(i));
        holder.setOnClickListener(this);
        holder.setOnLongClickListener(this);

        String nextToken = nextTokens.get(currentPath);
        if (!isLoading && nextToken != null && i >= LOAD_TRIGGER * items.size()) {
            loadMoreData();
        }
    }

    @Override
    public int getItemCount() {
        if (currentPath == null) {
            setPath("/");
            return 0;
        }
        return folders.get(currentPath).size();
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) { }

    @Override
    public void onSuccess(@NonNull CloudContents cloudContents) {
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
        selectItem(view);
    }

    @Override
    public boolean onLongClick(View view) {
        if (isLoading) {
            return true;
        }
        multiSelectMode = !multiSelectMode;
        if (multiSelectMode) {
            selectItem(view);
        } else {
            clearSelections();
        }
        return true;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
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

    private void loadMoreData() {
        isLoading = true;
        clientProvider
                .getClient()
                .getCloudContentsAsync(sourceId, currentPath, nextTokens.get(currentPath))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    private void selectItem(View view) {
        ArrayList<CloudItem> items = folders.get(currentPath);
        CloudListViewHolder holder =
                (CloudListViewHolder) recyclerView.findContainingViewHolder(view);
        CloudItem item = items.get(holder.getId());

        if (multiSelectMode) {
            if (selected.contains(holder.getId())) {
                // view.setActivated(false);
                selected.remove(holder.getId());
                holder.setSelected(false);
            } else {
                // view.setActivated(true);
                selected.add(holder.getId());
                holder.setSelected(true);
            }
        } else {
            if (item.isFolder()) {
                setPath(item.getPath());
            }
        }
    }

    private void clearSelections() {
        CloudListViewHolder holder;
        for (int id : selected) {
            holder = (CloudListViewHolder) recyclerView.findViewHolderForAdapterPosition(id);
            if (holder != null) {
                holder.setSelected(false);
            }
        }
        selected.clear();
    }

    void setLayoutId(int layoutId) {
        this.layoutId = layoutId;
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
