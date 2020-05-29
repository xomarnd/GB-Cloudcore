package src.main.geekCloud.server;

import src.main.geekCloud.common.AuthMessage;
import src.main.geekCloud.common.AuthMessageOk;
import src.main.geekCloud.common.RegistryMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.sqlite.JDBC;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;


public class AuthGatewayHandler extends ChannelInboundHandlerAdapter {

    private boolean authorized;

    private Map<String, User> users = new HashMap<>();
    private final String DB_PATH = "users.db";
    private Connection conn;

    public AuthGatewayHandler() {
        try {
            this.conn = DriverManager.getConnection(JDBC.PREFIX + DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }
        if (!authorized) {
            if (msg instanceof AuthMessage) {
                AuthMessage fd = (AuthMessage) msg;
                if (authByLoginAndPassword(fd.getLogin(), fd.getPassword()) != null) {
                    String username = fd.getLogin();
                    authorized = true;
                    AuthMessageOk amo = new AuthMessageOk();
                    ctx.writeAndFlush(amo).await();
                    ctx.pipeline().addLast(new MainHandler(username));
                    System.out.println(fd.getLogin() + "/" + fd.getPassword());
                    conn.close();
                }
            } else if (msg instanceof RegistryMessage) {
                RegistryMessage rm = (RegistryMessage) msg;
                if (loginExist(rm.getLogin())) {
                    System.out.println("User with nick" + rm.getLogin() + "already exist");
                    return;
                } else {
                    createOrActivateUser(rm.getLogin(), rm.getPassword());
                    String username = rm.getLogin();
                    authorized = true;
                    Files.createDirectory(Paths.get("server_storage/" + username));
                    AuthMessageOk amo = new AuthMessageOk();
                    ctx.writeAndFlush(amo).await();
                    ctx.pipeline().addLast(new MainHandler(username));
                    conn.close();
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private String authByLoginAndPassword(String login, String password) {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT login, password FROM users;")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String log = rs.getString(1);
                String pass = rs.getString(2);
                users.put(log, new User(log, pass));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (User user : users.values()) {
            if (login.equals(user.getLogin()) && password.equals(user.getPassword())) {
                return user.getLogin();
            }
        }
        return null;
    }

    private void createOrActivateUser(String login, String password) {
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users(login, password) VALUES (?,?);")) {

            pstmt.setString(1, login);
            pstmt.setString(2, password);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean loginExist(String login) {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT login, password FROM users;")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String log = rs.getString(1);
                String pass = rs.getString(2);
                users.put(log, new User(log, pass));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (users.containsKey(login)) {
            return true;
        }
        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
