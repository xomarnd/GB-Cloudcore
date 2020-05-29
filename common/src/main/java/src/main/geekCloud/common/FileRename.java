package src.main.geekCloud.common;

public class FileRename extends AbstractMessage{

    private String fileName;
    private String newFileName;

    public FileRename(String fileName, String newFileName) {
        this.fileName = fileName;
        this.newFileName = newFileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getNewFileName() {
        return newFileName;
    }
}

