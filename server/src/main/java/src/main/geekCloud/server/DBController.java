package src.main.geekCloud.server;

import org.sqlite.JDBC;
import java.sql.*;

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

    public static ResultSet getUsers() throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("SELECT login, password FROM users;");
        ResultSet rs = pstmt.executeQuery();
        return rs;
    }

    static void createOrActivateUser(String login, String password) {
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users(login, password) VALUES (?,?);")) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
