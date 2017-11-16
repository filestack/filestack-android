package com.filestack.android;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

// Taken from https://stackoverflow.com/a/32971704/5121687
// TODO Still needs tweaking, offsets aren't exactly correct
// In a 3 column grid, the middle items are a few pixels larger than first and last

/**
 * Adds spacing between items in a {@link RecyclerView}. Works with {@link LinearLayoutManager},
 * {@link GridLayoutManager}, and {@link StaggeredGridLayoutManager}. Only handles vertical
 * orientation.
 */
public class SpacingDecoration extends RecyclerView.ItemDecoration {
    private int hSpacing = 0;
    private int vSpacing = 0;
    private boolean includeEdge = false;

    SpacingDecoration(int hSpacing, int vSpacing, boolean includeEdge) {
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {

        super.getItemOffsets(outRect, view, parent, state);

        int position = parent.getChildAdapterPosition(view);
        RecyclerView.LayoutManager manager = parent.getLayoutManager();

        if (manager instanceof GridLayoutManager) {
            GridLayoutManager gridManager = (GridLayoutManager) manager;
            int spanCount = gridManager.getSpanCount();
            int column = position % spanCount;
            getGridItemOffsets(outRect, position, column, spanCount);

        } else if (parent.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredManager = (StaggeredGridLayoutManager) manager;
            int spanCount = staggeredManager.getSpanCount();
            StaggeredGridLayoutManager.LayoutParams params;
            params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
            int column = params.getSpanIndex();
            getGridItemOffsets(outRect, position, column, spanCount);

        } else if (manager instanceof LinearLayoutManager) {
            outRect.left = hSpacing;
            outRect.right = hSpacing;

            if (includeEdge) {
                if (position == 0) {
                    outRect.top = vSpacing;
                }
                outRect.bottom = vSpacing;
            } else {
                if (position > 0) {
                    outRect.top = vSpacing;
                }
            }
        }
    }

    private void getGridItemOffsets(Rect outRect, int position, int column, int spanCount) {
        if (includeEdge) {
            outRect.left = hSpacing * (spanCount - column) / spanCount;
            outRect.right = hSpacing * (column + 1) / spanCount;
            if (position < spanCount) {
                outRect.top = vSpacing;
            }
            outRect.bottom = vSpacing;
        } else {
            outRect.left = hSpacing * column / spanCount;
            outRect.right = hSpacing * (spanCount - 1 - column) / spanCount;
            if (position >= spanCount) {
                outRect.top = vSpacing;
            }
        }
    }
}
