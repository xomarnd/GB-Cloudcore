package src.main.geekCloud.common;

import java.util.ArrayList;

public class FileListUpdate extends AbstractMessage {
    private ArrayList<String> list;

    public FileListUpdate(ArrayList<String> list) {
        this.list = list;
    }

    public ArrayList<String> getList() {
        return list;
    }
}
