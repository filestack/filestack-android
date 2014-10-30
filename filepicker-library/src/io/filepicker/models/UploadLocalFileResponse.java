package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/27/14.
 */
public class UploadLocalFileResponse {
    ArrayList<Data> data;

    public Data getFirstData() {
        return data.get(0);
    }

    public FPFile parseToFpFile() {
        Data data = getFirstData();


        String url = data.url;

        FpFileData fpFileData = data.getFpFileData();

        return new FPFile(
                fpFileData.container,
                url,
                fpFileData.filename,
                fpFileData.key,
                fpFileData.type,
                fpFileData.size);
    }

    // Help classes for parsing response
    class Data {
        String url;
        FpFileData data;

        public FpFileData getFpFileData() {
            return data;
        }
    }

    class FpFileData {
        String container;
        long size;
        String type;
        String key;
        String filename;
    }
}
