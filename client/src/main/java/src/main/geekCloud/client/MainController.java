package src.main.geekCloud.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import src.main.geekCloud.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static src.main.geekCloud.client.KeyBoard.writeKeyCode;

public class MainController implements Initializable {

    @FXML
    TextField pathField;

    @FXML
    TableView<FileInfo> localFileTable;

    @FXML
    ListView<String> filesListServer;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    private Node rootNode;

    private final Path path = Paths.get("client_storage");
    private Path root;
    private Path fileLocal = null;
    private String fileServer = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread t = new Thread(() -> {
            try {
                TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
                filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
                filenameColumn.setPrefWidth(200);

                TableColumn<FileInfo, String> fileExtensionColumn = new TableColumn<>("Тип");
                fileExtensionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getExtension()));
                fileExtensionColumn.setPrefWidth(80);

                TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
                fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
                fileSizeColumn.setCellFactory(column -> {
                    return new TableCell<FileInfo, Long>() {
                        @Override
                        protected void updateItem(Long item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                                setStyle("");
                            } else {
                                String text = FileInfo.getStringSize(item);
                                if (item == -1L) {
                                    text = "";
                                }
                                setText(text);
                            }
                        }
                    };
                });
                fileSizeColumn.setPrefWidth(120);

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
                fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
                fileDateColumn.setPrefWidth(120);

                localFileTable.getColumns().addAll(filenameColumn, fileExtensionColumn, fileSizeColumn, fileDateColumn);
                localFileTable.getSortOrder().add(fileExtensionColumn);
                localFileTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        Path path = Paths.get(pathField.getText()).resolve(localFileTable.getSelectionModel().
                                getSelectedItem().getFilenamefull());
                        if (event.getClickCount() == 2) {
                            if (Files.isDirectory(path)) {
                                updateList(path);
                            }
                        }else {
                            fileServer = null;
                            fileLocal = path;
                            System.out.println("Выбрали файл на клиенте: " + fileLocal + " ");

                        }
                    }
                });
                //Ожидаем нажатие клавиш
                updateList(path);
                rootNode.setOnKeyPressed(new EventHandler<KeyEvent>(){
                    @Override
                    public void handle(KeyEvent event) {
                        KeyCode key = event.getCode();
                        writeKeyCode(key);
                    }

                });

                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshFilesList();
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
        setDisksBox();
        goToPath(path);
        refreshFilesList();
    }

    public  void setDisksBox(){
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
        updateList(path);
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
                Network.sendMsg(new FileMessage(Paths.get(String.valueOf(fileLocal))));
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
                Files.delete(Paths.get(String.valueOf(fileLocal)));
                refreshFilesList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            Network.sendMsg(new FileDelete(fileServer));
        }
    }

    public void moveBtnAction(ActionEvent actionEvent) throws IOException {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                Network.sendMsg(new FileMessage(Paths.get(String.valueOf(fileLocal))));
                Files.delete(Paths.get(String.valueOf(fileLocal)));
                refreshFilesList();
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

    public void renameBtnAction(ActionEvent actionEvent) {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                AtomicReference<String> newName = AlertController.inputNameDialog();
                if(!( newName == null )){
                    Files.move(Paths.get(String.valueOf(fileLocal)), Paths.get(String.valueOf(fileLocal)).resolveSibling(newName.get()));
                    refreshFilesList();
                    System.out.println("Локальный файл переименован");
                }else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Локальный файл НЕ переименован", ButtonType.OK);
                    alert.showAndWait();
                    System.out.println("Локальный файл НЕ переименован");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            AtomicReference<String> newName = AlertController.inputNameDialog();
            if(!( newName == null )) {
                Network.sendMsg(new FileRename(fileServer, newName.toString()));
                System.out.println("Файл на сервере переименован");
            }
        }

    }

    public void newFolderAction(ActionEvent actionEvent) {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Выберите место создания папки", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            try {
                AtomicReference<String> newFolderName = AlertController.inputNameDialog();

                if(!( newFolderName == null )){
                    Files.createDirectories(Paths.get(fileLocal.getParent() + "/" + newFolderName));
                    refreshFilesList();
                    System.out.println("Директория создана: "+ newFolderName);
                }else {
                    System.out.println("Директория не создана");
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Имя новой директории не выбрано", ButtonType.OK);
                    alert.showAndWait();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Выберите ЛОКАЛЬНОЕ место создания папки", ButtonType.OK);
            alert.showAndWait();
            System.out.println("Папка на сервере не создана");
        }

    }

    public void stackBtnAction(ActionEvent actionEvent) throws IOException {
        try{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open File");
            List<File> files = fileChooser.showOpenMultipleDialog(rootNode.getScene().getWindow());
            for(File file: files){
                Network.sendMsg(new FileMessage(Paths.get(String.valueOf(file))));
                System.out.println("Пересылаем файл: " + file);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось переслать файлы");
        }
    }

    public void viewBtnAction(ActionEvent actionEvent) {
        if(fileLocal == null && fileServer == null){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            File edit = new File(String.valueOf(fileLocal));
            editFile(edit);
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Загрузите файл в локальное хранилище", ButtonType.OK);
            alert.showAndWait();
            return;
        }
    }

    public boolean editFile(File file) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.EDIT)) {
            return false;
        }

        try {
            desktop.edit(file);
        } catch (IOException e) {
            // Log an error
            return false;
        }

        return true;
    }



    //TODO фикс, возврат домой после ухода с директории
    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void localBtnPathUpAction(ActionEvent actionEvent) {
        Path pathTo = root.toAbsolutePath().getParent();
        goToPath(pathTo);
    }

    public void serverListClicked(MouseEvent mouseEvent) {
        fileLocal = null;
        fileServer = getSelected(filesListServer);
        System.out.println("Выбираем \"удаленный\" файл: " + fileServer);

    }

    public void refreshFilesList() {
        goToPath(root);
        Network.sendMsg(new FileListUpdate(null));
    }

    public void updateList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            localFileTable.getItems().clear();
            localFileTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            localFileTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnExitAction(ActionEvent actionEvent) {
        AlertController.alertExitAction();
    }
}
