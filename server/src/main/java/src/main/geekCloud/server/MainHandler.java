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
            //Отправка файла
            if (msg instanceof FileRequest) {
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
            }

            //Получение файла
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                if (!Files.exists(Paths.get("server_storage" + userName + "/" + fm.getFilename()))) {
                    Files.write(Paths.get("server_storage/" + userName + "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                    FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                    ctx.writeAndFlush(flu);
                    System.out.println("Получил");
                }
            }

            //Удаление файла
            if (msg instanceof FileDelete) {
                FileDelete fd = (FileDelete) msg;
                Files.delete(Paths.get("server_storage/" + userName + "/" + fd.getFilename()));
                FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                ctx.writeAndFlush(flu);
                System.out.println("Файл " + fd.getFilename() + " удален");
            }

            //Переименование файла
            if (msg instanceof FileRename) {
                FileRename fd = (FileRename) msg;
                Files.move(Paths.get("server_storage/" + userName + "/" + fd.getFileName()),
                        Paths.get("server_storage/" + userName + "/" + fd.getFileName()).resolveSibling(fd.getNewFileName()));

                FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                ctx.writeAndFlush(flu);
                System.out.println("Файл " + fd.getFileName() + " удален");
            }

            //Перемещение файла
            if (msg instanceof FileRequest) {
                FileRequest message = (FileRequest) msg;
                if(message.getMove()) System.out.println("work!");
                new Thread(() -> {
                    try {
                        if (Files.exists(Paths.get("server_storage/" + userName + "/" + message.getFilename()))) {
                            FileMessage fm = new FileMessage(Paths.get("server_storage/" + userName + "/" + message.getFilename()));
                            ctx.writeAndFlush(fm);
                            System.out.println("Отправил");
                        }
                        Files.delete(Paths.get("server_storage/" + userName + "/" + message.getFilename()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                FileListUpdate flu = new FileListUpdate(getFileServerList(userName));
                ctx.writeAndFlush(flu);
                System.out.println("Файл " + message.getFilename() + " удален");
            }

            //Формирование и отсылка списка файлов клиента
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