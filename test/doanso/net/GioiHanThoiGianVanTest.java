package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.Game;
import doanso.domain.KetQua;
import doanso.domain.TrangThaiVan;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class GioiHanThoiGianVanTest {

    @Test
    void hetGioKhongAiTrungTuDongDongVanPhatBroadcastVaChanDoanMuon() throws Exception {
        try (var server = new GameServer(0, 4)) {
            var swAn = new StringWriter();
            var swBinh = new StringWriter();
            server.dangKyNguoiChoi("An", new PrintWriter(swAn, true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(swBinh, true));

            // Mở ván [1..100], số bí mật = 50, giới hạn 120ms
            var kqMo = server.moVanMoiCoGioiHanThoiGian(1, 100, 50, 120);
            assertInstanceOf(KetQua.ThanhCong.class, kqMo);

            // An đoán sai 1 lần trong khi còn giờ
            assertEquals("HIGHER 20", server.xuLyLenhGuess("An", "20"));

            // Chờ quá thời gian giới hạn để tác vụ đếm giờ tự động đóng ván
            Thread.sleep(260);

            Game van = server.getVanHienTai().orElseThrow();
            assertEquals(TrangThaiVan.FINISHED, van.getTrangThai());
            assertTrue(van.isBiDongDoHetGio());
            assertFalse(van.daCoNguoiThang());

            // Kiểm tra cả phòng nhận được thông báo hết giờ kèm số bí mật
            assertTrue(swAn.toString().contains("INFO HET_GIO 1 50"));
            assertTrue(swBinh.toString().contains("INFO HET_GIO 1 50"));

            // Lượt đoán đến sau khi hết giờ bị từ chối với lý do hết giờ
            String phanHoiMuon = server.xuLyLenhGuess("Binh", "50");
            assertTrue(phanHoiMuon.startsWith("ERROR VAN_DA_KET_THUC"));
            assertTrue(phanHoiMuon.contains("hết giờ"));
        }
    }

    @Test
    void huyTacVuDemGioNgayKhiCoNguoiThangSom() throws Exception {
        try (var server = new GameServer(0, 4)) {
            var swAn = new StringWriter();
            server.dangKyNguoiChoi("An", new PrintWriter(swAn, true));

            // Mở ván giới hạn 2000ms (2 giây)
            server.moVanMoiCoGioiHanThoiGian(1, 100, 75, 2000);
            assertTrue(server.dangCoTacVuHenGioChay());

            // An đoán trúng ngay lập tức
            assertEquals("WIN 75", server.xuLyLenhGuess("An", "75"));

            // Tác vụ đếm giờ phải được huỷ ngay khi có người thắng sớm
            assertFalse(server.dangCoTacVuHenGioChay());

            Game van = server.getVanHienTai().orElseThrow();
            assertTrue(van.daCoNguoiThang());
            assertFalse(van.isBiDongDoHetGio());
            assertFalse(swAn.toString().contains("INFO HET_GIO"));
        }
    }

    @Test
    void tranhChapHetGioDungLucCoLuotDoanTrungChiMotBenThangKhoaQuyetDinh() throws Exception {
        int soVong = 25;
        for (int vong = 0; vong < soVong; vong++) {
            var game = new Game(vong + 1, 1, 100, 60);
            game.moVan();

            ExecutorService beLuong = Executors.newFixedThreadPool(2);
            try {
                Callable<Boolean> luongHetGio = () -> {
                    var kq = game.dongVanDoHetGio();
                    return kq instanceof KetQua.ThanhCong;
                };
                Callable<Boolean> luongDoanTrung = () -> {
                    var kq = game.doanSo("An", 60);
                    return kq instanceof KetQua.ThanhCong;
                };

                List<Future<Boolean>> ketQua = beLuong.invokeAll(List.of(luongHetGio, luongDoanTrung));
                boolean hetGioThangKhoa = ketQua.get(0).get();
                boolean doanTrungThangKhoa = ketQua.get(1).get();

                // Chỉ đúng 1 trong 2 luồng được quyền chốt trạng thái FINISHED (ai vào khoá trước quyết định)
                assertNotEquals(hetGioThangKhoa, doanTrungThangKhoa);
                assertEquals(TrangThaiVan.FINISHED, game.getTrangThai());
                assertEquals(hetGioThangKhoa, game.isBiDongDoHetGio());
                assertEquals(doanTrungThangKhoa, game.daCoNguoiThang());
            } finally {
                beLuong.shutdown();
            }
        }
    }

    @Test
    void chanMoVanCoGioiHanThoiGianAmHoacBangKhong() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));

            var kqKhong = server.moVanMoiCoGioiHanThoiGian(1, 100, 0);
            var thatBai0 = assertInstanceOf(KetQua.ThatBai.class, kqKhong);
            assertEquals("THOI_GIAN_KHONG_HOP_LE", thatBai0.maLoi());

            var kqAm = server.moVanMoiCoGioiHanThoiGian(1, 100, 50, -500);
            var thatBaiAm = assertInstanceOf(KetQua.ThatBai.class, kqAm);
            assertEquals("THOI_GIAN_KHONG_HOP_LE", thatBaiAm.maLoi());
        }
    }
}
