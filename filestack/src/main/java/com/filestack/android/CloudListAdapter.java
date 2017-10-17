package com.filestack.android;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.filestack.CloudContents;
import com.filestack.CloudItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

class CloudListAdapter extends RecyclerView.Adapter implements
        SingleObserver<CloudContents>, View.OnClickListener, View.OnLongClickListener {

    private static final double PAGINATION_THRESHOLD = 0.50;

    private final ClientProvider clientProvider;
    private final String provider;

    private int layoutId;
    private RecyclerView recyclerView;

    private ArrayList<CloudItem> items;
    private ArrayList<Integer> selected;
    private Integer threshold;
    private String nextToken;
    private boolean multiSelectMode;

    CloudListAdapter(ClientProvider clientProvider, String provider) {
        this.clientProvider = clientProvider;
        this.provider = provider;
        selected = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View listItemView = inflater.inflate(layoutId, viewGroup, false);
        return new CloudListItemViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        CloudListItemViewHolder holder = (CloudListItemViewHolder) viewHolder;
        CloudItem item = items.get(i);

        holder.setId(i);
        holder.setName(item.getName());
        String info = String.format(
                Locale.getDefault(), "%s - %d", item.getMimetype(), item.getSize());
        holder.setInfo(info);
        holder.setIcon(item.getThumbnail());
        holder.setSelected(selected.contains(i));
        holder.setOnClickListener(this);
        holder.setOnLongClickListener(this);

        if (threshold != null && nextToken != null && i >= threshold ) {
            threshold = null;
            loadMoreData();
        }
    }

    @Override
    public int getItemCount() {
        if (items == null) {
            items = new ArrayList<>();
            loadMoreData();
        }
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) { }

    @Override
    public void onSuccess(@NonNull CloudContents cloudContents) {
        CloudItem[] newItems = cloudContents.getItems();
        List<CloudItem> newItemsList = Arrays.asList(newItems);

        int oldSize = items.size();
        items.addAll(newItemsList);
        int newSize = items.size();

        nextToken = cloudContents.getNextToken();
        notifyItemRangeInserted(oldSize, newSize - oldSize);

        Log.d("pagination", "Next token: " + nextToken);
        Log.d("pagination", "Items count: " + items.size());

        // This is a temporary fix for the weird Google Drive behavior
        // We get a page with just the Trash folder, which is also a duplicate
        // TODO Change this when backend is fixed
        if (nextToken != null && newSize - oldSize == 1) {
            Log.d("pagination", "Running backend fix");
            loadMoreData();
        } else {
            threshold = (int) (PAGINATION_THRESHOLD * items.size());
        }
    }

    @Override
    public void onError(@NonNull Throwable e) { }

    @Override
    public void onClick(View view) {
        selectItem(view);
    }

    @Override
    public boolean onLongClick(View view) {
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

    private void loadMoreData() {
        Log.d("pagination", "Loading more data");
        clientProvider
                .getClient()
                .getCloudContentsAsync(provider, "/", nextToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    private void selectItem(View view) {
        CloudListItemViewHolder holder;
        holder = (CloudListItemViewHolder) recyclerView.findContainingViewHolder(view);

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
        }
    }

    private void clearSelections() {
        CloudListItemViewHolder holder;
        for (int id : selected) {
            holder = (CloudListItemViewHolder) recyclerView.findViewHolderForAdapterPosition(id);
            if (holder != null) {
                holder.setSelected(false);
            }
        }
        selected.clear();
    }

    void setLayoutId(int layoutId) {
        this.layoutId = layoutId;
    }
}
