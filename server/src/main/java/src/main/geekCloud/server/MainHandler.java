package src.main.geekCloud.server;

import src.main.geekCloud.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String userName;

    public MainHandler(String userName) {
        this.userName = userName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg == null) {
                return;
            }
//TODO single thread executor and stack
            if (msg instanceof FileRequest) {
                new Thread(() -> {
                    try {
                        FileRequest message = (FileRequest) msg;

                        if (Files.exists(Paths.get("server_storage/" + userName + "/" + message.getFilename()))) {
                            FileMessage fm = new FileMessage(Paths.get("server_storage/" + userName + "/" + message.getFilename()));
                            ctx.writeAndFlush(fm);
                            System.out.println("Отправил");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                if (!Files.exists(Paths.get("server_storage" + userName + "/" + fm.getFilename()))) {
                    Files.write(Paths.get("server_storage/" + userName + "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                    FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                    ctx.writeAndFlush(flu);
                    System.out.println("Получил");
                }
            }

            if (msg instanceof FileMove) {
                FileMove message = (FileMove) msg;
                new Thread(() -> {
                    try {

                        if (Files.exists(Paths.get("server_storage/" + userName + "/" + message.getFilename()))) {
                            FileMessage fm = new FileMessage(Paths.get("server_storage/" + userName + "/" + message.getFilename()));
                            ctx.writeAndFlush(fm);
                            System.out.println("Отправил");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                Files.delete(Paths.get("server_storage/" + userName + "/" + message.getFilename()));
                FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                ctx.writeAndFlush(flu);
                System.out.println("Файл " + message.getFilename() + " удален");
            }

            if (msg instanceof FileDelete) {
                FileDelete fd = (FileDelete) msg;
                Files.delete(Paths.get("server_storage/" + userName + "/" + fd.getFilename()));
                FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                ctx.writeAndFlush(flu);
                System.out.println("Файл " + fd.getFilename() + " удален");
            }
            if (msg instanceof FileListUpdate) {
                FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                ctx.writeAndFlush(flu);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private ArrayList<String> getFileServerList(String userName) {
        ArrayList<String> list = new ArrayList<>();
        try {
            Files.list(Paths.get("server_storage/" + userName)).map(p -> p.getFileName().toString()).forEach(o -> list.add(o));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}