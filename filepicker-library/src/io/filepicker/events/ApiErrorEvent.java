package io.filepicker.events;

import retrofit.RetrofitError;

/**
 * Created by maciejwitowski on 10/28/14.
 */
public class ApiErrorEvent {

    RetrofitError error;

    public ApiErrorEvent (RetrofitError error) {
        this.error = error;
    }

    public RetrofitError getError() {
        return error;
    }
}
