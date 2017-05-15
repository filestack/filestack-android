package io.filepicker.events;
public class GoogleDriveError {

     private Exception exception;

    public GoogleDriveError(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
