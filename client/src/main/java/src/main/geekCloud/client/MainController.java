package src.main.geekCloud.client;

import src.main.geekCloud.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import src.main.geekCloud.common.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainController implements Initializable {

    private String focusFileName;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> filesListServer;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread t = new Thread(() -> {
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
        refreshLocalFilesList();
        refreshLocalFilesListServer();
    }

    public void refreshLocalFilesListServer() {
        Network.sendMsg(new FileListUpdate(null));
    }


    public String getSelected(ListView<String> listView) {
        String selectFile = listView.getSelectionModel().getSelectedItem();
        listView.getSelectionModel().clearSelection();
        return selectFile;
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        String fileLocal = getSelected(filesList);
        String fileServer = getSelected(filesListServer);
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Network.sendMsg(new FileMessage(Paths.get("client_storage/" + fileLocal)));
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
        String fileLocal = getSelected(filesList);
        String fileServer = getSelected(filesListServer);
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Files.delete(Paths.get("client_storage/" + fileLocal));
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
        String fileLocal = getSelected(filesList);
        String fileServer = getSelected(filesListServer);
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Network.sendMsg(new FileMessage(Paths.get("client_storage/" + fileLocal)));
                Files.delete(Paths.get("client_storage/" + fileLocal));
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
        if (Platform.isFxApplicationThread()) {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(new Function<Path, String>() {
                    @Override
                    public String apply(Path p) {
                        return p.getFileName().toString();
                    }
                }).forEach(o -> {
                    filesList.getItems().add(o);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        filesList.getItems().clear();
                        Files.list(Paths.get("client_storage")).map(new Function<Path, String>() {
                            @Override
                            public String apply(Path p) {
                                return p.getFileName().toString();
                            }
                        }).forEach(new Consumer<String>() {
                            @Override
                            public void accept(String o) {
                                filesList.getItems().add(o);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
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
            System.exit(1);
        }
    }

}
