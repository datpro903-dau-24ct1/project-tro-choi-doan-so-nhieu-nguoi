package doanso.net;

import doanso.domain.KetQua;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer implements AutoCloseable {

    public static final int CO_BE_LUONG_MAC_DINH = 20;

    private final ServerSocket serverSocket;
    private final int coBeLuong;
    private final ExecutorService beLuong;
    private volatile boolean dangChay;

    public GameServer(int cong) throws IOException {
        this(cong, CO_BE_LUONG_MAC_DINH);
    }

    public GameServer(int cong, int coBeLuong) throws IOException {
        if (cong < 0 || cong > 65535) {
            throw new IllegalArgumentException("Cổng không hợp lệ: " + cong);
        }
        if (coBeLuong <= 0) {
            throw new IllegalArgumentException("Cỡ bể luồng phải lớn hơn 0: " + coBeLuong);
        }
        this.serverSocket = new ServerSocket(cong);
        this.coBeLuong = coBeLuong;
        this.beLuong = Executors.newFixedThreadPool(coBeLuong);
        this.dangChay = true;
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

    public int getCoBeLuong() {
        return coBeLuong;
    }

    public boolean daDongBeLuong() {
        return beLuong.isShutdown();
    }

    public int phucVuMotKhach() throws IOException {
        var clientSocket = serverSocket.accept();
        var handler = new ClientHandler(clientSocket, this);
        return handler.phucVu();
    }

    public int vongLapChapNhanKetNoi() {
        int soKetNoi = 0;
        while (dangChay && !serverSocket.isClosed()) {
            try {
                var clientSocket = serverSocket.accept();
                soKetNoi++;
                beLuong.submit(new ClientHandler(clientSocket, this));
            } catch (IOException e) {
                if (!dangChay || serverSocket.isClosed()) {
                    break;
                }
            }
        }
        return soKetNoi;
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
        dangChay = false;
        beLuong.shutdown();
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
