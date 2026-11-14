package doanso.net;

import doanso.domain.KetQua;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class GameServer implements AutoCloseable {

    private final ServerSocket serverSocket;

    public GameServer(int cong) throws IOException {
        if (cong < 0 || cong > 65535) {
            throw new IllegalArgumentException("Cổng không hợp lệ: " + cong);
        }
        this.serverSocket = new ServerSocket(cong);
    }

    public static KetQua<GameServer> thuMoCong(int cong) {
        if (cong < 0 || cong > 65535) {
            return new KetQua.ThatBai<>("CONG_KHONG_HOP_LE", "Số hiệu cổng phải nằm trong [0..65535]");
        }
        try {
            var server = new GameServer(cong);
            return new KetQua.ThanhCong<>(server, "Đã mở cổng " + server.getCong());
        } catch (IOException e) {
            return new KetQua.ThatBai<>("CONG_DA_BI_CHIEM", "Không thể mở cổng " + cong + ": " + e.getMessage());
        }
    }

    public int getCong() {
        return serverSocket.getLocalPort();
    }

    public int phucVuMotKhach() throws IOException {
        try (var clientSocket = serverSocket.accept();
             var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            int soDongDaXuLy = 0;
            String dong;
            while ((dong = in.readLine()) != null) {
                soDongDaXuLy++;
                var phanHoi = xuLyDongLenh(dong);
                out.println(phanHoi);
                if ("BYE".equals(phanHoi)) {
                    break;
                }
            }
            return soDongDaXuLy;
        }
    }

    public String xuLyDongLenh(String dongTho) {
        if (dongTho == null || dongTho.isBlank()) {
            return "ERROR LENH_RONG";
        }
        var dong = dongTho.strip();
        var tach = dong.split("\\s+", 2);
        var tuKhoa = tach[0].toUpperCase();
        var thamSo = tach.length > 1 ? tach[1].strip() : "";

        return switch (tuKhoa) {
            case "JOIN" -> thamSo.isEmpty() ? "ERROR THIEU_TEN" : "OK JOIN " + thamSo;
            case "GUESS" -> thamSo.isEmpty() ? "ERROR THIEU_SO_DOAN" : "OK GUESS " + thamSo;
            case "SCORE" -> "OK SCORE";
            case "HISTORY" -> "OK HISTORY";
            case "STATS" -> "OK STATS";
            case "QUIT" -> "BYE";
            default -> "ERROR LENH_KHONG_HOP_LE";
        };
    }

    @Override
    public void close() throws IOException {
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
