package doanso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.Game;
import doanso.net.GameServer;
import doanso.repo.KhoLichSuDoan;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ThongKeServiceTest {

    @Test
    void traVeKhongKhiChuaCoVanHoacChuaCoLuotDoanNaoKhongBiChiaChoKhong() {
        var service = new ThongKeService();
        var khoRong = new KhoLichSuDoan();

        assertEquals(0.0, service.tinhTrungBinhSoLuotMoiVan(List.of()), 1e-6);
        assertEquals(0.0, service.tinhTrungBinhSoLuotMoiVan(null), 1e-6);
        assertEquals(0.0, service.tinhTrungBinhTongLuotMoiNguoiCoDoan(khoRong), 1e-6);
        assertEquals(0.0, service.tinhTrungBinhSoLuotCuaNguoiMoiVan("An", List.of()), 1e-6);
        assertEquals(0.0, service.tinhTrungBinhSoLuotCuaNguoiMoiVan("   ", List.of()), 1e-6);
        assertTrue(service.demTongLuotTheoNguoi(List.of()).isEmpty());
    }

    @Test
    void loaiBoVanChuaHoanTatHoacKhongAiThangKhoiThongKeTrungBinhVan() {
        var service = new ThongKeService();

        // Ván 1: Hoàn tất có người thắng sau 4 lượt đoán
        var van1 = new Game(1, 1, 100, 50);
        van1.moVan();
        van1.doanSo("An", 10);
        van1.doanSo("Binh", 90);
        van1.doanSo("An", 30);
        van1.doanSo("Binh", 50);

        // Ván 2: Hoàn tất có người thắng sau 2 lượt đoán
        var van2 = new Game(2, 1, 100, 20);
        van2.moVan();
        van2.doanSo("An", 40);
        van2.doanSo("An", 20);

        // Ván 3: Đang chơi dở (10 lượt đoán sai, chưa ai thắng) -> không được tính vào trung bình
        var van3DangChoi = new Game(3, 1, 100, 99);
        van3DangChoi.moVan();
        for (int i = 1; i <= 10; i++) {
            van3DangChoi.doanSo("An", i);
        }

        // Ván 4: Chưa mở (WAITING)
        var van4ChuaMo = new Game(4, 1, 100, 70);

        double trungBinhVan = service.tinhTrungBinhSoLuotMoiVan(
            List.of(van1, van2, van3DangChoi, van4ChuaMo)
        );
        assertEquals(3.0, trungBinhVan, 1e-6);
    }

    @Test
    void nguoiVaoChiXemKhongDoanKhongLamLechTrungBinhVaThongKeTheoNguoiChinhXac() {
        var service = new ThongKeService();
        var kho = new KhoLichSuDoan();

        // Ván 1: An đoán 3 lần, Bình đoán 1 lần (trúng)
        var van1 = new Game(1, 1, 100, 50);
        van1.thamGiaVan("An");
        van1.thamGiaVan("Binh");
        van1.thamGiaVan("CuongChiXem");
        van1.moVan();
        van1.doanSo("An", 10);
        van1.doanSo("An", 20);
        van1.doanSo("An", 30);
        van1.doanSo("Binh", 50);
        van1.getDanhSachLuotDoan().forEach(kho::ghiNhanLuotDoan);

        // Ván 2: An đoán 1 lần (trúng), Bình không đoán, Cường vẫn chỉ xem
        var van2 = new Game(2, 1, 100, 80);
        van2.thamGiaVan("An");
        van2.thamGiaVan("Binh");
        van2.thamGiaVan("CuongChiXem");
        van2.moVan();
        van2.doanSo("An", 80);
        van2.getDanhSachLuotDoan().forEach(kho::ghiNhanLuotDoan);

        List<Game> dsVan = List.of(van1, van2);

        // Đếm tổng lượt đoán của từng người bằng groupingBy + counting
        Map<String, Long> tongLuot = service.demTongLuotTheoNguoi(kho.layTatCaLuotDoan());
        assertEquals(4L, tongLuot.get("An"));
        assertEquals(1L, tongLuot.get("Binh"));
        assertFalse(tongLuot.containsKey("CuongChiXem"));

        // Trung bình tổng lượt đoán mỗi người có tham gia đoán: (4 + 1) / 2.0 = 2.5
        assertEquals(2.5, service.tinhTrungBinhTongLuotMoiNguoiCoDoan(kho), 1e-6);

        // Trung bình số lượt mỗi ván của An: (3 + 1) / 2.0 = 2.0
        assertEquals(2.0, service.tinhTrungBinhSoLuotCuaNguoiMoiVan("An", dsVan), 1e-6);

        // Trung bình số lượt mỗi ván của Bình (chỉ tính ván 1 mà Bình có đoán): 1 / 1.0 = 1.0
        assertEquals(1.0, service.tinhTrungBinhSoLuotCuaNguoiMoiVan("Binh", dsVan), 1e-6);

        // Cường chỉ vào xem, không đoán lượt nào -> trả về 0.0
        assertEquals(0.0, service.tinhTrungBinhSoLuotCuaNguoiMoiVan("CuongChiXem", dsVan), 1e-6);
    }

    @Test
    void tichHopThongKeTuGameServerQuaNhieuVan() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("KhachXem", new PrintWriter(new StringWriter(), true));

            // Ván 1: 3 lượt đoán (An đoán 20, Bình đoán 60, An đoán trúng 40)
            server.moVanMoiVoiSoBiMat(1, 100, 40);
            server.xuLyLenhGuess("An", "20");
            server.xuLyLenhGuess("Binh", "60");
            server.xuLyLenhGuess("An", "40");

            // Ván 2: 1 lượt đoán (Bình đoán trúng 90 ngay lượt đầu)
            server.moVanMoiVoiSoBiMat(1, 100, 90);
            server.xuLyLenhGuess("Binh", "90");

            ThongKeService tk = server.getThongKeService();
            List<Game> dsVan = server.getDanhSachCacVan();
            assertEquals(2, dsVan.size());

            // Trung bình số lượt mỗi ván hoàn tất: (3 + 1) / 2.0 = 2.0
            assertEquals(2.0, tk.tinhTrungBinhSoLuotMoiVan(dsVan), 1e-6);

            // Trung bình mỗi ván của An (2 lượt ở ván 1) = 2.0; của Bình (1 lượt ván 1, 1 lượt ván 2) = 1.0
            assertEquals(2.0, tk.tinhTrungBinhSoLuotCuaNguoiMoiVan("An", dsVan), 1e-6);
            assertEquals(1.0, tk.tinhTrungBinhSoLuotCuaNguoiMoiVan("Binh", dsVan), 1e-6);
            assertEquals(0.0, tk.tinhTrungBinhSoLuotCuaNguoiMoiVan("KhachXem", dsVan), 1e-6);
        }
    }
}
