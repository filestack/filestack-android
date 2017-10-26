package com.filestack.android;

import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.CloudItem;
import com.filestack.CloudResponse;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

class CloudListAdapter extends RecyclerView.Adapter implements
        SingleObserver<CloudResponse>, View.OnClickListener, FsActivity.BackButtonListener {

    private static final double LOAD_TRIGGER = 0.50;

    private final FsAndroidClient.Provider clientProvider;
    private final String sourceId;

    private RecyclerView recyclerView;
    private int layoutId;

    private String currentPath;
    private boolean isLoading;
    // private boolean multiSelectMode;

    private final ArrayMap<String, ArrayList<CloudItem>> folders = new ArrayMap<>();
    private final ArrayMap<String, String> nextTokens = new ArrayMap<>();
    // private final ArrayList<String> selected = new ArrayList<>();

    CloudListAdapter(FsAndroidClient.Provider clientProvider, String sourceId) {
        this.clientProvider = clientProvider;
        this.sourceId = sourceId;
        setHasStableIds(true);
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
        holder.setOnClickListener(this);

        SelectedItem.Saver itemSaver = Util.getItemSaver();
        holder.setSelected(itemSaver.isSelected(sourceId, item.getPath()));

        // holder.setOnLongClickListener(this);

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
    public int getItemCount() {
        if (currentPath == null) {
            setPath("/");
            return 0;
        }
        return folders.get(currentPath).size();
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

        SelectedItem.Saver itemSaver = Util.getItemSaver();
        boolean selected = itemSaver.toggleItem(sourceId, item.getPath());
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

//    @Override
//    public boolean onLongClick(View view) {
//        if (isLoading) {
//            return true;
//        }
//        multiSelectMode = !multiSelectMode;
//        if (multiSelectMode) {
//            selectItem(view);
//        } else {
//            selected.clear();
//            notifyItemRangeChanged(0, folders.get(currentPath).size());
//        }
//        return true;
//    }

    void setLayoutId(int layoutId) {
        this.layoutId = layoutId;
    }

    // Private helper methods (alphabetical order)

    private void loadMoreData() {
        isLoading = true;
        clientProvider
                .getClient()
                .getCloudItemsAsync(sourceId, currentPath, nextTokens.get(currentPath))
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

//    private void selectItem(View view) {
//        CloudItem item = folders.get(currentPath).get(view.getId());
//
//        if (item.isFolder()) {
//            setPath(item.getPath());
//        } else if (multiSelectMode) {
//            if (!selected.remove(item.getPath())) {
//                selected.add(item.getPath());
//            }
//            if (selected.size() == 0) {
//                multiSelectMode = false;
//            }
//            notifyItemChanged(view.getId());
//        }
//    }
}
