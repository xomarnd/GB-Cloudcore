package src.main.geekCloud.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.commons.io.FilenameUtils;

public class FileInfo {
    public static final String UP_TOKEN = "[..]";

    public String getFilenamefull() {
        return filenamefull;
    }

    public void setFilenamefull(String filenamefull) {
        this.filenamefull = filenamefull;
    }


    public enum FileType {
        FILE("F"), DIRECTORY("D");
        private String name;
        public String getName() {
            return name;
        }
        FileType(String name) {
            this.name = name;
        }
    }

    private String filename;
    private long size;
    private FileType type;
    private LocalDateTime lastModified;
    private String extension;
    private String filenamefull;


    public String getFileName() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getExtension() { return extension; }

    public FileInfo(Path path) {
        try {
            this.filenamefull = path.getFileName().toString();
            this.filename = getFileNameRemoveExtension(path);
            this.extension = getExtensionNotFileName(path);

            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
        } catch (IOException e) {
            throw new RuntimeException("Нет данных о файле");
        }
    }

    public String getFileNameRemoveExtension(Path path) {
        String fullFileName = path.getFileName().toString();
        if (FilenameUtils.removeExtension(fullFileName) == null){
            return null;
        }
        if(Files.isDirectory(path)){
            return fullFileName;
        }
        return FilenameUtils.removeExtension(fullFileName);
    }

    public String getExtensionNotFileName(Path path) {
        String fullFileName = path.getFileName().toString();
        if(Files.isDirectory(path)){
            return "[ DIR ]";
        }
        return FilenameUtils.getExtension(fullFileName);
    }

    public static String getStringSize(long bytes) {
        if(bytes < 1000){
            return String.format ("%,d bytes", bytes);
        }else if (bytes < 1000 * Math.pow(2, 10)) {
            return String.format ("%,d KB", (long)(bytes / Math.pow(2, 10)));
        }else if (bytes < 1000 * Math.pow(2, 20) ) {
            return String.format ("%,d MB", (long)(bytes / Math.pow(2, 20)));
        }else if (bytes < 1000 * Math.pow(2, 30) ) {
            return String.format ("%,d GB", (long)(bytes / Math.pow(2, 30)));
        }else if (bytes < 1000 * Math.pow(2, 40) ) {
            return String.format ("%,d TB", (long)(bytes / Math.pow(2, 40)));
        }
        return "n/a";
    }

}