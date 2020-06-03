package src.main.geekCloud.client;

import javafx.scene.control.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class AlertController {

    public static boolean alertNetworkController(){
        Alert alertReconnect = new Alert(Alert.AlertType.CONFIRMATION);
        alertReconnect.setTitle("CloudApp");
        alertReconnect.setHeaderText("Нет соеденения с сервером.");

        alertReconnect.setContentText("Переподключится?");
        ButtonType ok = new ButtonType("Reconnect");
        ButtonType cancel = new ButtonType("Cancel");
        alertReconnect.getButtonTypes().clear();

        alertReconnect.getButtonTypes().addAll(ok, cancel);
        Optional<ButtonType> reconnect = alertReconnect.showAndWait();
        if(reconnect.get() == ok) {
            return true;
        }else {
            reconnect.get();
        }
        return false;
    }

    public static boolean alertExitAction() {
        Alert alertExit = new Alert(Alert.AlertType.CONFIRMATION);
        alertExit.setTitle("CloudApp");
        alertExit.setHeaderText("Закрытие программы");
        alertExit.setContentText("Вы уверины что хотите выйти из программы?");
        Optional<ButtonType> exit = alertExit.showAndWait();
        if(exit.get() == ButtonType.OK){
            return true;
        }
        return false;
    }

    public static AtomicReference<String> inputNameDialog(){
        AtomicReference<String> newName = new AtomicReference<>(null);
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("CloudApp");
        dialog.setHeaderText("Введите имя файла: ");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (name == null){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не переименован", ButtonType.OK);
                alert.showAndWait();
            }else {
                newName.set(name);
            }
        });

        return null;
    }

    public static void smallAlert(String textAlert){
        Alert alert = new Alert(Alert.AlertType.ERROR, textAlert, ButtonType.OK);
        alert.showAndWait();
    }

    public static void inputConnectSettingDialog(){

    }


}
