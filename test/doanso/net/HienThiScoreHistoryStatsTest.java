package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.GoiY;
import doanso.domain.LuotDoan;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HienThiScoreHistoryStatsTest {

    private GameServer server;

    @BeforeEach
    void khoiTao() throws Exception {
        server = new GameServer(0);
    }

    @AfterEach
    void donDep() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    // F17 Trường hợp biên 1: Bảng điểm rỗng khi chưa có người chơi nào thắng
    @Test
    void xemScoreKhiBangDiemRong() {
        String phanHoi = server.xuLyDongLenh("SCORE", null, "NguoiChoi1");
        assertNotNull(phanHoi);
        assertEquals("OK SCORE", phanHoi, "Khi chưa ai có điểm, server phản hồi OK SCORE");
    }

    // F17: Bảng điểm có dữ liệu, sắp xếp theo điểm giảm dần và tên tăng dần
    @Test
    void xemScoreCoDuLieuDinhDangChuanVaSapXepDung() {
        server.getKhoBangDiem().congDiemThang("Binh");
        server.getKhoBangDiem().congDiemThang("An");
        server.getKhoBangDiem().congDiemThang("An"); // An có 2 điểm, Binh có 1 điểm
        server.getKhoBangDiem().congDiemThang("Cuong"); // Cuong có 1 điểm, Binh và Cuong đồng điểm thì xếp theo tên

        String phanHoi = server.xuLyDongLenh("SCORE", null, "An");
        assertTrue(phanHoi.startsWith("OK SCORE"));
        assertTrue(phanHoi.contains("An: 2"));
        assertTrue(phanHoi.contains("Binh: 1"));
        assertTrue(phanHoi.contains("Cuong: 1"));

        // Kiểm tra thứ tự sắp xếp: An (2) phải đứng trước Binh (1), Binh (1) đứng trước Cuong (1)
        int posAn = phanHoi.indexOf("An");
        int posBinh = phanHoi.indexOf("Binh");
        int posCuong = phanHoi.indexOf("Cuong");
        assertTrue(posAn < posBinh, "An nhiều điểm hơn phải đứng trước Binh");
        assertTrue(posBinh < posCuong, "Binh và Cuong cùng điểm thì xếp theo thứ tự bảng chữ cái");
    }

    // F17 Trường hợp biên 2: Xem HISTORY của tên không tồn tại hoặc chưa đoán lần nào
    @Test
    void xemHistoryCuaNguoiChuaTungDoanHoacKhongTonTai() {
        // Xem của chính mình khi chưa đoán
        String phanHoiChinhMinh = server.xuLyDongLenh("HISTORY", null, "NguoiMoi");
        assertTrue(phanHoiChinhMinh.contains("RONG") || phanHoiChinhMinh.contains("Chưa có lượt đoán nào"));

        // Xem của người khác không tồn tại
        String phanHoiKhac = server.xuLyDongLenh("HISTORY NguoiKhongTonTai", null, "NguoiMoi");
        assertTrue(phanHoiKhac.contains("RONG") || phanHoiKhac.contains("Chưa có lượt đoán nào"));
    }

    // F17: Xem HISTORY của người có lượt đoán qua nhiều ván
    @Test
    void xemHistoryCoDuLieuDinhDangChiTietTungLuot() {
        String ten = "Tuan";
        server.getKhoLichSuDoan().ghiNhanLuotDoan(new LuotDoan(ten, 50, LocalDateTime.now(), GoiY.HIGHER));
        server.getKhoLichSuDoan().ghiNhanLuotDoan(new LuotDoan(ten, 75, LocalDateTime.now(), GoiY.LOWER));
        server.getKhoLichSuDoan().ghiNhanLuotDoan(new LuotDoan(ten, 62, LocalDateTime.now(), GoiY.CORRECT));

        String phanHoi = server.xuLyDongLenh("HISTORY", null, ten);
        assertTrue(phanHoi.startsWith("OK HISTORY"));
        assertTrue(phanHoi.contains("50"));
        assertTrue(phanHoi.contains("HIGHER"));
        assertTrue(phanHoi.contains("75"));
        assertTrue(phanHoi.contains("LOWER"));
        assertTrue(phanHoi.contains("62"));
        assertTrue(phanHoi.contains("CORRECT"));

        // Người khác xem lịch sử của Tuan bằng lệnh: HISTORY Tuan
        String phanHoiNguoiKhacXem = server.xuLyDongLenh("HISTORY Tuan", null, "NguoiKhac");
        assertTrue(phanHoiNguoiKhacXem.contains("Tuan"));
        assertTrue(phanHoiNguoiKhacXem.contains("62"));
    }

    // F17 Trường hợp biên 4: Xem STATS khi chưa có dữ liệu (tránh chia cho 0)
    @Test
    void xemStatsKhiChuaCoVanVaChuaCoLuotDoanNao() {
        String phanHoi = server.xuLyDongLenh("STATS", null, "NguoiChoi1");
        assertNotNull(phanHoi);
        assertTrue(phanHoi.startsWith("OK STATS"));
        assertTrue(phanHoi.contains("0.0") || phanHoi.contains("0"), "Chưa có ván nào thì chỉ số trung bình bằng 0.0");
    }

    // F17: Xem STATS khi đã có các ván chơi hoàn tất
    @Test
    void xemStatsKhiDaCoCacVanChoiHoanTat() {
        var sw = new StringWriter();
        server.dangKyNguoiChoi("P1", new PrintWriter(sw, true));
        server.moVanMoiVoiSoBiMat(1, 10, 7);
        server.xuLyLenhGuess("P1", "5"); // sai: HIGHER
        server.xuLyLenhGuess("P1", "7"); // trúng: WIN

        String phanHoi = server.xuLyDongLenh("STATS", null, "P1");
        assertTrue(phanHoi.startsWith("OK STATS"));
        assertTrue(phanHoi.contains("trung_binh_van") || phanHoi.contains("2.00") || phanHoi.contains("2.0"));
        assertTrue(phanHoi.contains("tong_luot") || phanHoi.contains("2"));
    }
}
