package doanso.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final GameServer server;

    public ClientHandler(Socket clientSocket, GameServer server) {
        if (clientSocket == null || server == null) {
            throw new IllegalArgumentException("Socket và GameServer không được null");
        }
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            phucVu();
        } catch (IOException e) {
            // Kết nối client đứt đột ngột hoặc đóng giữa chừng: bỏ qua êm ái để không làm chết luồng bể
        }
    }

    public int phucVu() throws IOException {
        try (var socket = this.clientSocket;
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            int soDongDaXuLy = 0;
            String dong;
            while ((dong = in.readLine()) != null) {
                soDongDaXuLy++;
                try {
                    var phanHoi = server.xuLyDongLenh(dong);
                    out.println(phanHoi);
                    if ("BYE".equals(phanHoi)) {
                        break;
                    }
                } catch (RuntimeException e) {
                    out.println("ERROR LOI_NOI_BO");
                    break;
                }
            }
            return soDongDaXuLy;
        }
    }
}
