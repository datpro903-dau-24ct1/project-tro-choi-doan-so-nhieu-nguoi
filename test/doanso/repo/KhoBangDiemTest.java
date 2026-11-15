package doanso.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.KetQua;
import doanso.net.GameServer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class KhoBangDiemTest {

    @Test
    void congDiemQuaNhieuVanVaXepHangDongDiemTheoTenTangDan() {
        var kho = new KhoBangDiem();

        kho.ghiNhanNguoiChoi("Dung");
        kho.ghiNhanNguoiChoi("Binh");
        kho.ghiNhanNguoiChoi("An");
        kho.ghiNhanNguoiChoi("Cuong");
        kho.ghiNhanNguoiChoi("Em");

        // An và Bình cùng thắng 2 ván (2 điểm); Cường và Dũng cùng thắng 1 ván (1 điểm); Em 0 điểm
        kho.congDiemThang("Binh");
        kho.congDiemThang("Binh");
        kho.congDiemThang("An");
        kho.congDiemThang("An");
        kho.congDiemThang("Dung");
        kho.congDiemThang("Cuong");

        assertEquals(2, kho.layDiem("An"));
        assertEquals(2, kho.layDiem("Binh"));
        assertEquals(1, kho.layDiem("Cuong"));
        assertEquals(1, kho.layDiem("Dung"));
        assertEquals(0, kho.layDiem("Em"));
        assertEquals(0, kho.layDiem("KhachChuaTungVao"));

        List<KhoBangDiem.HangBangDiem> bangXepHang = kho.layBangXepHang();
        assertEquals(5, bangXepHang.size());

        // Điểm giảm dần; khi đồng điểm thì tên tăng dần theo thứ tự từ điển
        assertEquals(new KhoBangDiem.HangBangDiem("An", 2), bangXepHang.get(0));
        assertEquals(new KhoBangDiem.HangBangDiem("Binh", 2), bangXepHang.get(1));
        assertEquals(new KhoBangDiem.HangBangDiem("Cuong", 1), bangXepHang.get(2));
        assertEquals(new KhoBangDiem.HangBangDiem("Dung", 1), bangXepHang.get(3));
        assertEquals(new KhoBangDiem.HangBangDiem("Em", 0), bangXepHang.get(4));
    }

    @Test
    void nguoiThangRoiMangVanGiuDiemVaCungTenVaoLaiKhongBiResetDiem() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(new StringWriter(), true));

            // Ván 1: An đoán trúng và được cộng 1 điểm
            server.moVanMoiVoiSoBiMat(1, 100, 35);
            assertEquals("WIN 35", server.xuLyLenhGuess("An", "35"));
            assertEquals(1, server.getKhoBangDiem().layDiem("An"));
            assertEquals(0, server.getKhoBangDiem().layDiem("Binh"));

            // An rời mạng: vẫn phải giữ nguyên 1 điểm trên bảng xếp hạng
            server.huyNguoiChoi("An");
            assertEquals(1, server.getKhoBangDiem().layDiem("An"));

            // An vào lại bằng đúng tên cũ ("An"): không bị reset điểm về 0
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            assertEquals(1, server.getKhoBangDiem().layDiem("An"));

            // Ván 2: An thắng tiếp -> điểm tăng lên 2
            server.moVanMoiVoiSoBiMat(1, 100, 80);
            assertEquals("WIN 80", server.xuLyLenhGuess("An", "80"));
            assertEquals(2, server.getKhoBangDiem().layDiem("An"));
        }
    }

    @Test
    void nhieuLuongCungCapNhatMotKhoaKhongBiMatDiem() throws Exception {
        var kho = new KhoBangDiem();
        int soLuong = 20;
        int soLanMoiLuong = 50;

        ExecutorService beLuong = Executors.newFixedThreadPool(soLuong);
        try {
            List<Callable<Void>> danhSachTacVu = new ArrayList<>();
            for (int i = 0; i < soLuong; i++) {
                danhSachTacVu.add(() -> {
                    for (int j = 0; j < soLanMoiLuong; j++) {
                        kho.congDiemThang("An");
                    }
                    return null;
                });
            }
            beLuong.invokeAll(danhSachTacVu);
            assertEquals(soLuong * soLanMoiLuong, kho.layDiem("An"));
        } finally {
            beLuong.shutdown();
        }
    }

    @Test
    void chanTenNullHoacRongKhiGhiNhanVaCongDiem() {
        var kho = new KhoBangDiem();

        var kqGhiNull = kho.ghiNhanNguoiChoi(null);
        assertInstanceOf(KetQua.ThatBai.class, kqGhiNull);

        var kqCongRong = kho.congDiemThang("   ");
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kqCongRong);
        assertEquals("TEN_KHONG_HOP_LE", thatBai.maLoi());
        assertTrue(kho.layBangXepHang().isEmpty());
    }
}
