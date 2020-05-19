package src.main.geekCloud.client;

import src.main.geekCloud.common.AbstractMessage;
import src.main.geekCloud.common.AuthMessage;
import src.main.geekCloud.common.AuthMessageOk;
import src.main.geekCloud.common.RegistryMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    VBox authorization;

    public void auth(ActionEvent actionEvent) {
        System.out.println(login.getText() + " " + password.getText());
        Network.sendMsg(new AuthMessage(login.getText(), password.getText()));
    }

    public void registration(ActionEvent actionEvent) {
        Network.sendMsg(new RegistryMessage(login.getText(), password.getText()));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                AbstractMessage am = Network.readObject();
                if (am instanceof AuthMessageOk) {
                    Platform.runLater(() -> fxMainWindow());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    @FXML
    private void fxMainWindow() {
        try {
            Stage stage = (Stage) authorization.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = loader.load();
            stage.setTitle("Cloud Application");
            stage.setScene(new Scene(root, 400, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}