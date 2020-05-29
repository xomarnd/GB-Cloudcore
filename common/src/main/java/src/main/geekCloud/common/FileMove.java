package src.main.geekCloud.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMove extends AbstractMessage{
    private String filename;
    private byte[] data;
    private String name;
    private boolean isDirectory;
    private boolean isEmpty;

    public FileMove(String fileName, boolean isDirectory, boolean isEmpty){
        this.name = fileName;
        this.isDirectory = isDirectory;
        this.isEmpty = isEmpty;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public FileMove(Path path) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }
}