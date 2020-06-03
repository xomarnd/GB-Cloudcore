package src.main.geekCloud.common;

public class FileRequest extends AbstractMessage {
    private boolean move;
    private String filename;

    public String getFilename() {
        return filename;
    }
    public boolean getMove() {
        return move;
    }

    public FileRequest(String filename, boolean move) {
        this.move = move;
        this.filename = filename;
    }
    public FileRequest(String filename) {
        this.filename = filename;
    }
}
