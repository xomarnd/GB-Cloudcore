package src.main.geekCloud.client;

import javafx.scene.input.KeyCode;

import java.io.File;

public class KeyBoard {
    public static void writeKeyCode(KeyCode key){
        if(key == KeyCode.F1){
            System.out.println("User press F1");
        } else if(key == KeyCode.F2){
            System.out.println("User press F2");
        } else if(key == KeyCode.F3){
            System.out.println("User press F3");
        } else if(key == KeyCode.F4){
            System.out.println("User press F4");
        } else if(key == KeyCode.F5){
            System.out.println("User press F5");
        } else if(key == KeyCode.F6){
            System.out.println("User press F6");
        }else if(key == KeyCode.F7){
            System.out.println("User press F7");
        }else if(key == KeyCode.F8){
            System.out.println("User press F8");
        }else if(key == KeyCode.F9){
            System.out.println("User press F9");
        }
    }
}
