package src.main.geekCloud.client;

import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import src.main.geekCloud.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;


import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    public TextField pathField;

    @FXML
    ListView<FileInfo> filesList;

    @FXML
    ListView<String> filesListServer;

    @FXML
    private TextField clientFiles;

    @FXML
    ComboBox<String> disksBox;

    private Path root;

    private FileInfo fileLocal = null;
    private String fileServer = null;
    private Path selectSendFile;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread t = new Thread(() -> {

            filesList.setCellFactory(new Callback<ListView<FileInfo>, ListCell<FileInfo>>() {
                @Override
                public ListCell<FileInfo> call(ListView<FileInfo> param) {
                    return new ListCell<FileInfo>() {
                        @Override
                        protected void updateItem(FileInfo item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                                setStyle("");
                            }else {
                                String formattedFileName = String.format("%-30s", item.getFilename());
                                String formattedFileSize = String.format(getSize(item.getSize()));
                                if(item.getSize() == -1L) {
                                    formattedFileSize = String.format("%s", "[ DIR ]");
                                }
                                if(item.getSize() == -2L) {
                                    formattedFileSize = String.format("");
                                }
                                String text = String.format("%s %-20s", formattedFileName, formattedFileSize);
                                setText(text);
                            }
                        }
                    };
                }
            });

            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                    if (am instanceof FileListUpdate) {
                        FileListUpdate flu = (FileListUpdate) am;
                        Platform.runLater(() -> {
                            filesListServer.getItems().clear();
                            flu.getList().forEach(o -> filesListServer.getItems().add(o));
                        });
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Сеть отключена");
                Network.stop();
            }


        });
        t.setDaemon(true);
        t.start();
        Path path = Paths.get("client_storage");
        goToPath(path);
        refreshLocalFilesListServer();
        disksBox.getItems().clear();
        disksBox.getItems().add(path.toString());
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            System.out.println(p);
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);
    }

    public void goToPath(Path path) {
        root = path;
        clientFiles.setText(root.toAbsolutePath().toString());
        filesList.getItems().clear();
        filesList.getItems().add(new FileInfo(FileInfo.UP_TOKEN, -2L));
        filesList.getItems().addAll(scanFiles(path));

        filesList.getItems().sort(new Comparator<FileInfo>() {
            @Override
            public int compare(FileInfo o1, FileInfo o2) {
                if (o1.getFilename().equals(FileInfo.UP_TOKEN)) {
                    return -1;
                }
                if ((int) Math.signum(o1.getSize()) == (int) Math.signum(o2.getSize())) {
                    return o1.getFilename().compareTo(o2.getFilename());
                }
                return Long.compare(o1.getSize(),o2.getSize());
            }
        });
    }

    public List<FileInfo> scanFiles(Path root) {
        try {
            return Files.list(root).map(FileInfo::new).collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Files scan exception: " + root);
        }
    }

    public void refreshLocalFilesListServer() {
        Network.sendMsg(new FileListUpdate(null));
    }


    public String getSelected(ListView<String> listView) {
        String selectFile = listView.getSelectionModel().getSelectedItem();
        listView.getSelectionModel().clearSelection();
        return selectFile;
    }

    public void copyBtnAction(ActionEvent actionEvent) throws IOException {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Network.sendMsg(new FileMessage(Paths.get("client_storage/" + fileLocal.getFilename())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            Network.sendMsg(new FileRequest(fileServer));
        }
    }
    public void delBtnAction(ActionEvent actionEvent) {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Files.delete(Paths.get("client_storage/" + fileLocal.getFilename()));
                refreshLocalFilesList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            Network.sendMsg(new FileDelete(fileServer));
        }
    }
    public void moveBtnAction(ActionEvent actionEvent) {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Network.sendMsg(new FileMessage(Paths.get("client_storage/" + fileLocal.getFilename())));
                Files.delete(Paths.get("client_storage/" + fileLocal.getFilename()));
                refreshLocalFilesList();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Отправил файл и удалил локально.");
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            Network.sendMsg(new FileRequest(fileServer));
            Network.sendMsg(new FileDelete(fileServer));
            System.out.println("Получил файл и удалил на облаке.");
            }

        }
    public void refreshLocalFilesList() {
        goToPath(root);
        refreshLocalFilesListServer();
    }

    public void newFolderAction(ActionEvent actionEvent) {

    }

    public void editBtnAction(ActionEvent actionEvent) {
    }

    public void viewBtnAction(ActionEvent actionEvent) {
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Alert alertBatal = new Alert(Alert.AlertType.CONFIRMATION);
        alertBatal.setTitle("CloudApp");
        alertBatal.setHeaderText("Закрытие программы");
        alertBatal.setContentText("Вы уверины что хотите выйти из программы?");
        Optional<ButtonType> exit = alertBatal.showAndWait();
        if(exit.get() == ButtonType.OK){
            Network.stop();
            System.exit(1);
        }
    }
    //TODO фикс, возврат домой после ухода с директории
    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        goToPath(Paths.get(element.getSelectionModel().getSelectedItem()));
        refreshLocalFilesList();
    }

    public void localBtnPathUpAction(ActionEvent actionEvent) {
        Path pathTo = root.toAbsolutePath().getParent();
        goToPath(pathTo);
    }

    //TODO rename
    public void renameBtnAction(ActionEvent actionEvent) {
    }

    public void clientListClicked(MouseEvent mouseEvent) {
        fileLocal = filesList.getSelectionModel().getSelectedItem();
        fileServer = null;
        if (mouseEvent.getClickCount() == 2) {
            if (fileLocal != null) {
                if (fileLocal.isDirectory()) { //Если fileInfo - директория
                    Path pathTo = root.resolve(fileLocal.getFilename());
                    goToPath(pathTo);
                }
                if (fileLocal.isUpElement()) {
                    Path pathTo = root.toAbsolutePath().getParent();
                    goToPath(pathTo);
                }
            }
        }
        //
        if (mouseEvent.getClickCount() == 1) {
            if (fileLocal != null) {
                if (!fileLocal.isDirectory() && !fileLocal.getFilename().equals(FileInfo.UP_TOKEN)) {
                    selectSendFile = root.resolve(fileLocal.getFilename());
                    System.out.println("Выбрали файл на клиенте: " + fileLocal + " ");
                }
            }
        }
        System.out.println("Выбираем локальный файл:" + fileLocal);
    }

    public void serverListClicked(MouseEvent mouseEvent) {
        fileLocal = null;
        fileServer = getSelected(filesListServer);
        System.out.println("Выбираем \"удаленный\" файл: " + fileServer);

    }

    public String getSize(long bytes) {
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
