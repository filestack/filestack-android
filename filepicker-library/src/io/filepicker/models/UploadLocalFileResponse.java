package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public class UploadLocalFileResponse {
    public ArrayList<Data> data;

    public Data getFirstData() {
        Data firstData = null;

        if(data != null && data.size() > 0) {
            firstData = data.get(0);
        }

        return firstData;
    }

    public FPFile parseToFpFile() {
        Data data = getFirstData();
        FPFile fpFile = null;

        if(data != null) {
            String url = data.url;

            FpFileData fpFileData = data.data;

            fpFile = new FPFile(fpFileData.container, url, fpFileData.filename, fpFileData.key,
                                fpFileData.type, fpFileData.size);
        }

        return fpFile;
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
