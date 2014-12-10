package io.filepicker.events;

import retrofit.RetrofitError;

/**
 * Created by maciejwitowski on 10/28/14.
 */
public final class ApiErrorEvent {

    public final RetrofitError error;

    public ApiErrorEvent (RetrofitError error) {
        this.error = error;
    }
}
