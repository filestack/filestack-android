package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public class UploadLocalFileResponse {
    public ArrayList<Data> data;

    public Data getFirstData() {
        return data.get(0);
    }

    public FPFile parseToFpFile() {
        Data data = getFirstData();

        String url = data.url;

        FpFileData fpFileData = data.data;

        return new FPFile(
                fpFileData.container,
                url,
                fpFileData.filename,
                fpFileData.key,
                fpFileData.type,
                fpFileData.size);
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
