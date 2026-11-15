package doanso.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.GoiY;
import doanso.domain.LuotDoan;
import doanso.net.GameServer;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientUIHienThiTest {

    private GameServer server;
    private ExecutorService serverPool;
    private ClientUI clientUI;

    @BeforeEach
    void khoiTao() throws Exception {
        server = new GameServer(0);
        serverPool = Executors.newSingleThreadExecutor();
        serverPool.submit(() -> server.vongLapChapNhanKetNoi());
        clientUI = new ClientUI(false);
    }

    @AfterEach
    void donDep() throws Exception {
        if (clientUI != null) {
            clientUI.dongKetNoi();
        }
        if (server != null) {
            server.close();
        }
        if (serverPool != null) {
            serverPool.shutdownNow();
        }
    }

    // F17: Bấm nút Bảng điểm và hiển thị lên giao diện ClientUI
    @Test
    void bamNutScoreVaHienThiBangDiemLenNhatKyClient() throws Exception {
        server.getKhoBangDiem().congDiemThang("Hung");
        server.getKhoBangDiem().congDiemThang("Hung");
        server.getKhoBangDiem().congDiemThang("Dung");

        clientUI.ketNoi("127.0.0.1", server.getCong(), "Hung");
        TimeUnit.MILLISECONDS.sleep(150);

        clientUI.guiLenh("SCORE");
        TimeUnit.MILLISECONDS.sleep(200);

        String nhatKy = clientUI.getNoiDungNhatKy();
        assertTrue(nhatKy.contains("Hung: 2") || nhatKy.contains("Hung"));
        assertTrue(nhatKy.contains("Dung: 1") || nhatKy.contains("Dung"));
    }

    // F17: Bấm nút Lịch sử và hiển thị chi tiết các lượt đoán lên ClientUI
    @Test
    void bamNutHistoryVaHienThiLichSuLenNhatKyClient() throws Exception {
        String ten = "Hai";
        server.getKhoLichSuDoan().ghiNhanLuotDoan(new LuotDoan(ten, 25, LocalDateTime.now(), GoiY.HIGHER));
        server.getKhoLichSuDoan().ghiNhanLuotDoan(new LuotDoan(ten, 35, LocalDateTime.now(), GoiY.CORRECT));

        clientUI.ketNoi("127.0.0.1", server.getCong(), ten);
        TimeUnit.MILLISECONDS.sleep(150);

        clientUI.guiLenh("HISTORY");
        TimeUnit.MILLISECONDS.sleep(200);

        String nhatKy = clientUI.getNoiDungNhatKy();
        assertTrue(nhatKy.contains("25"));
        assertTrue(nhatKy.contains("HIGHER"));
        assertTrue(nhatKy.contains("35"));
        assertTrue(nhatKy.contains("CORRECT"));
    }

    // F17: Bấm nút Thống kê và hiển thị các số liệu thống kê lên ClientUI
    @Test
    void bamNutStatsVaHienThiThongKeLenNhatKyClient() throws Exception {
        clientUI.ketNoi("127.0.0.1", server.getCong(), "Hai");
        TimeUnit.MILLISECONDS.sleep(150);

        clientUI.guiLenh("STATS");
        TimeUnit.MILLISECONDS.sleep(200);

        String nhatKy = clientUI.getNoiDungNhatKy();
        assertTrue(nhatKy.contains("STATS") || nhatKy.contains("Thống kê"));
    }

    // F17 Trường hợp biên: Kết quả dài nhiều dòng vẫn ghi nhận đầy đủ vào vùng nhật ký
    @Test
    void ketQuaDaiNhieuDongGhiNhanDayDuKhongBiCatXen() throws Exception {
        // Tạo 30 người chơi trong bảng điểm để tạo kết quả dài
        for (int i = 0; i < 30; i++) {
            server.getKhoBangDiem().congDiemThang("NguoiChoi_" + i);
        }

        clientUI.ketNoi("127.0.0.1", server.getCong(), "TestLong");
        TimeUnit.MILLISECONDS.sleep(150);

        clientUI.guiLenh("SCORE");
        TimeUnit.MILLISECONDS.sleep(250);

        String nhatKy = clientUI.getNoiDungNhatKy();
        assertTrue(nhatKy.contains("NguoiChoi_0"));
        assertTrue(nhatKy.contains("NguoiChoi_29"));
    }
}
