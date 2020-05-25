package src.main.geekCloud.client;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import src.main.geekCloud.common.AbstractMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;


public class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;
    private static final int MAX_OBJ_SIZE = 100 * 1024 * 1024;


    public static void start() {

        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(), MAX_OBJ_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
            stop();

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
                start();
            }else {
                reconnect.get();
            }
        }
    }

    public static void stop() {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean sendMsg(AbstractMessage msg) {
        try {
            out.writeObject(msg);
            return true;
        } catch (IOException e) {
            start();
            e.printStackTrace();
        }
        return false;
    }

    public static AbstractMessage readObject() throws ClassNotFoundException, IOException {
        Object obj = in.readObject();
        return (AbstractMessage) obj;
    }

}
