package io.filepicker.events;

/**
 * Created by Raúl Acevedo - SWEB
 * on 21/04/2017.
 */

public class GoogleDriveError {

     private Exception exception;

    public GoogleDriveError(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
