package com.filestack.android.internal;

/**
 * Communicates back button presses between activity and fragments. Needed to traverse backwards
 * in cloud files list.
 */
public interface BackButtonListener {
    boolean onBackPressed();
}
