package src.main.geekCloud.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
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
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    private final Path defaultPath = Paths.get("client_storage"); //Директория пользователя по умолчанию
    private Path root; //Текущая локальная директория
    private Path fileLocal = null; //Ссылка на локальный файл
    private String fileServer = null;  //Ссылка на удаленный файл

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
                                getSelectedItem().getFileNameFull());
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

                updateList(defaultPath);
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
        goToPath(defaultPath);
        refreshFilesList();
    }

    public  void setDisksBox(){
        disksBox.getItems().clear();
        disksBox.getItems().add(defaultPath.toString());
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
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
            AlertController.smallAlert("Ни один файл не был выбран");
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
            AlertController.smallAlert("Ни один файл не был выбран");
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
            AlertController.smallAlert("Ни один файл не был выбран");
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
            AlertController.smallAlert("Ни один файл не был выбран");
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
                    AlertController.smallAlert("Локальный файл НЕ переименован");
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
            AlertController.smallAlert("Выберите место создания папки");
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
                    AlertController.smallAlert("Имя новой директории не выбрано");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            AlertController.smallAlert("Выберите ЛОКАЛЬНОЕ место создания папки");
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
            AlertController.smallAlert("Не удалось переслать файлы");
            System.out.println("Не удалось переслать файлы");
        }
    }

    public void viewBtnAction(ActionEvent actionEvent) {
        if(fileLocal == null && fileServer == null){
            AlertController.smallAlert("Ни один файл не был выбран");
            return;
        }
        // Проверяем фокус на локальном хранилище
        if(fileServer == null){
            File edit = new File(String.valueOf(fileLocal));
            editFile(edit);
        }
        // Проверяем фокус на сервере
        if(fileLocal == null) {
            AlertController.smallAlert("Для просмотра загрузите файл в локальное хранилище");
        }
    }

    public void editFile(File file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.EDIT)) {
            return;
        }

        try {
            desktop.edit(file);
        } catch (IOException e) {
            AlertController.smallAlert("Не удалось открыть файл");

        }

    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void localBtnPathUpAction(ActionEvent actionEvent) {
        Path pathTo = root.toAbsolutePath().getParent();
        if(pathTo.toString().contains(defaultPath.normalize().toAbsolutePath().toString())) {
            goToPath(pathTo);
        }else{
            List<String> disk = disksBox.getItems();
            for(String disks: disk){
                if(disks.equals(pathTo.getRoot().toString())){
                    disksBox.getSelectionModel().select(disk.indexOf(disks));
                    goToPath(pathTo);
                }
            }
        }
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

    public void disConnect(ActionEvent actionEvent) {
        filesListServer.getItems().clear();
        Network.stop();
    }
}
