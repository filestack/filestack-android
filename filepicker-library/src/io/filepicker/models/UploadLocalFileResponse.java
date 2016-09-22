package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public class UploadLocalFileResponse {

    public ArrayList<Data> data;

    public Data getFirstData() {
        return (data != null && !data.isEmpty()) ? data.get(0) : null;
    }

    public FPFile parseToFpFile() {
        Data data = getFirstData();
        if (data != null) {
            FpFileData fileData = data.data;
            return new FPFile(fileData.container, data.url, fileData.filename, fileData.key, fileData.type, fileData.size);
        }
        return null;
    }

    // Help classes for parsing response
    private static class Data {
        private String url;
        private FpFileData data;
    }

    private static class FpFileData {
        private String container;
        private long size;
        private String type;
        private String key;
        private String filename;
    }
}
