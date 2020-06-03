package src.main.geekCloud.server;

import org.sqlite.JDBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBController {
    private static String DB_PATH = "users.db";
    public static Connection conn;

    public static void connectToDB(){
        try {
            conn = DriverManager.getConnection(JDBC.PREFIX + DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnectDB() throws SQLException {
        conn.close();
    }

    public static PreparedStatement getUsers() throws SQLException {
        return conn.prepareStatement("SELECT login, password FROM users;");
    }

    public static PreparedStatement createNewUser() throws SQLException {
        return conn.prepareStatement("INSERT INTO users(login, password) VALUES (?,?);");
    }

}
