package doanso.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.GoiY;
import doanso.domain.KetQua;
import doanso.domain.LuotDoan;
import doanso.net.GameServer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class KhoLichSuDoanTest {

    @Test
    void ghiNhanLichSuTheoTungNguoiQuaNhieuVanVaNguoiChuaDoanTraDanhSachRong() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Cuong", new PrintWriter(new StringWriter(), true));

            // Ván 1: số bí mật 50
            server.moVanMoiVoiSoBiMat(1, 100, 50);
            server.xuLyLenhGuess("An", "20");
            server.xuLyLenhGuess("Binh", "80");
            server.xuLyLenhGuess("An", "50");

            // Ván 2: số bí mật 10
            server.moVanMoiVoiSoBiMat(1, 100, 10);
            server.xuLyLenhGuess("An", "10");

            KhoLichSuDoan kho = server.getKhoLichSuDoan();
            List<LuotDoan> lichSuAn = kho.layLichSuCua("An");
            assertEquals(3, lichSuAn.size());
            assertEquals(20, lichSuAn.get(0).soDoan());
            assertEquals(GoiY.HIGHER, lichSuAn.get(0).ketQuaGoiY());
            assertEquals(50, lichSuAn.get(1).soDoan());
            assertEquals(GoiY.CORRECT, lichSuAn.get(1).ketQuaGoiY());
            assertEquals(10, lichSuAn.get(2).soDoan());
            assertEquals(GoiY.CORRECT, lichSuAn.get(2).ketQuaGoiY());

            List<LuotDoan> lichSuBinh = kho.layLichSuCua("Binh");
            assertEquals(1, lichSuBinh.size());
            assertEquals(80, lichSuBinh.get(0).soDoan());
            assertEquals(GoiY.LOWER, lichSuBinh.get(0).ketQuaGoiY());

            // Người chơi Cường chỉ vào xem, chưa đoán lần nào -> danh sách rỗng
            assertTrue(kho.layLichSuCua("Cuong").isEmpty());
            assertTrue(kho.layLichSuCua("KhachLa").isEmpty());
            assertTrue(kho.layLichSuCua(null).isEmpty());
            assertTrue(kho.layLichSuCua("   ").isEmpty());
            assertEquals(4, kho.layTatCaLuotDoan().size());
        }
    }

    @Test
    void traBanSaoPhongThuGiuTinhBatBienKhongBiSuaDoiTuBenNgoai() {
        var kho = new KhoLichSuDoan();
        var luot1 = new LuotDoan("An", 30, LocalDateTime.now(), GoiY.HIGHER);
        kho.ghiNhanLuotDoan(luot1);

        // Sửa danh sách trả về từ layLichSuCua
        List<LuotDoan> banSaoList = kho.layLichSuCua("An");
        banSaoList.clear();
        assertEquals(1, kho.layLichSuCua("An").size());

        // Sửa Map trả về từ layToanBoLichSu
        Map<String, List<LuotDoan>> banSaoMap = kho.layToanBoLichSu();
        banSaoMap.get("An").clear();
        banSaoMap.clear();
        assertEquals(1, kho.layLichSuCua("An").size());
    }

    @Test
    void nhieuLuongVuaGhiVuaDuyetLichSuDongThoiKhongNemConcurrentModificationException() throws Exception {
        var kho = new KhoLichSuDoan();
        int soLuong = 20;
        int soLanMoiLuong = 50;

        ExecutorService beLuong = Executors.newFixedThreadPool(soLuong);
        try {
            List<Callable<Integer>> danhSachTacVu = new ArrayList<>();
            for (int i = 0; i < soLuong; i++) {
                final int chiSoLuong = i;
                danhSachTacVu.add(() -> {
                    int tongDuyet = 0;
                    for (int j = 0; j < soLanMoiLuong; j++) {
                        kho.ghiNhanLuotDoan(
                            new LuotDoan("An", (chiSoLuong * 100) + j + 1, LocalDateTime.now(), GoiY.HIGHER)
                        );
                        List<LuotDoan> danhSachHienTai = kho.layLichSuCua("An");
                        for (LuotDoan ld : danhSachHienTai) {
                            if (ld.soDoan() > 0) {
                                tongDuyet++;
                            }
                        }
                    }
                    return tongDuyet;
                });
            }

            List<Future<Integer>> ketQua = beLuong.invokeAll(danhSachTacVu);
            for (Future<Integer> f : ketQua) {
                assertTrue(f.get() > 0);
            }
            assertEquals(soLuong * soLanMoiLuong, kho.layLichSuCua("An").size());
        } finally {
            beLuong.shutdown();
        }
    }

    @Test
    void chanGhiNhanKhiLuotDoanBiNull() {
        var kho = new KhoLichSuDoan();
        var kq = kho.ghiNhanLuotDoan(null);
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq);
        assertEquals("LUOT_DOAN_KHONG_HOP_LE", thatBai.maLoi());
    }
}
