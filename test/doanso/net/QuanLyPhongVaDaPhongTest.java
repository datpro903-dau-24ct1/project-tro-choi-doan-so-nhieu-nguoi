package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.KetQua;
import doanso.domain.PhongChoi;
import doanso.domain.TrangThaiVan;
import doanso.service.QuanLyPhong;
import doanso.service.QuanLyPhongChoi;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// F21: Kiểm thử nhiều phòng chơi song song và các trường hợp biên
public class QuanLyPhongVaDaPhongTest {

    private QuanLyPhong quanLyPhong;
    private GameServer server;
    private ExecutorService serverPool;

    @BeforeEach
    void khoiTao() throws Exception {
        quanLyPhong = new QuanLyPhongChoi();
        server = new GameServer(0);
        serverPool = Executors.newSingleThreadExecutor();
        serverPool.submit(() -> server.vongLapChapNhanKetNoi());
    }

    @AfterEach
    void donDep() throws Exception {
        if (server != null) {
            server.close();
        }
        if (serverPool != null) {
            serverPool.shutdownNow();
        }
    }

    // F21 Trường hợp biên 1: Tạo phòng trùng tên / trùng mã phòng
    @Test
    void chanTaoPhongTrungMaVaChanMaNullHoacRong() {
        var kq1 = quanLyPhong.taoPhong("ROOM_A", "Phòng Alpha");
        assertTrue(kq1 instanceof KetQua.ThanhCong);
        assertEquals(1, quanLyPhong.getSoLuongPhong());

        // Tạo lại với cùng mã ROOM_A (kể cả khác chữ hoa/thường)
        var kqTrung = quanLyPhong.taoPhong("ROOM_A", "Phòng Alpha Khác");
        assertTrue(kqTrung instanceof KetQua.ThatBai);
        assertEquals("PHONG_DA_TON_TAI", ((KetQua.ThatBai<?>) kqTrung).maLoi());

        var kqTrungThuong = quanLyPhong.taoPhong("room_a", "Phòng Thường");
        assertTrue(kqTrungThuong instanceof KetQua.ThatBai);

        // Chặn mã null hoặc rỗng
        var kqNull = quanLyPhong.taoPhong(null, "Test");
        assertTrue(kqNull instanceof KetQua.ThatBai);
        assertEquals("MA_PHONG_KHONG_HOP_LE", ((KetQua.ThatBai<?>) kqNull).maLoi());

        var kqRong = quanLyPhong.taoPhong("   ", "Test");
        assertTrue(kqRong instanceof KetQua.ThatBai);
        assertEquals("MA_PHONG_KHONG_HOP_LE", ((KetQua.ThatBai<?>) kqRong).maLoi());
    }

    // F21 Trường hợp biên 2: Một người không được ở hai phòng cùng lúc
    @Test
    void motNguoiKhongDuocOHaiPhongCungLuc() {
        quanLyPhong.taoPhong("ROOM_1", "Phòng 1");
        quanLyPhong.taoPhong("ROOM_2", "Phòng 2");

        // Người chơi Nam vào ROOM_1
        var kqVao1 = quanLyPhong.vaoPhong("ROOM_1", "Nam");
        assertTrue(kqVao1 instanceof KetQua.ThanhCong);
        assertEquals("ROOM_1", quanLyPhong.timMaPhongCuaNguoiChoi("Nam").orElse(""));

        // Nam cố tình vào ROOM_2 khi chưa rời ROOM_1 -> Bị từ chối
        var kqVao2 = quanLyPhong.vaoPhong("ROOM_2", "Nam");
        assertTrue(kqVao2 instanceof KetQua.ThatBai);
        assertEquals("NGUOI_CHOI_DANG_O_PHONG_KHAC", ((KetQua.ThatBai<?>) kqVao2).maLoi());

        // Nếu dùng thao tác chuyenPhong thì được chấp nhận
        var kqChuyen = quanLyPhong.chuyenPhong("ROOM_1", "ROOM_2", "Nam");
        assertTrue(kqChuyen instanceof KetQua.ThanhCong);
        assertEquals("ROOM_2", quanLyPhong.timMaPhongCuaNguoiChoi("Nam").orElse(""));

        // Kiểm tra Nam không còn ở ROOM_1
        PhongChoi p1 = quanLyPhong.timPhong("ROOM_1").orElseThrow();
        PhongChoi p2 = quanLyPhong.timPhong("ROOM_2").orElseThrow();
        assertFalse(p1.coNguoiChoi("Nam"));
        assertTrue(p2.coNguoiChoi("Nam"));
    }

    // F21 Trường hợp biên 3: Rời phòng khi ván đang chạy
    @Test
    void roiPhongKhiVanDangChayBaoToanLichSuVaBroadcastChoNguoiConLai() {
        quanLyPhong.taoPhong("ROOM_TEST", "Phòng Test");
        PhongChoi p = quanLyPhong.timPhong("ROOM_TEST").orElseThrow();
        quanLyPhong.vaoPhong("ROOM_TEST", "Tuan");
        quanLyPhong.vaoPhong("ROOM_TEST", "Minh");

        // Mở ván với số bí mật 75
        p.moVanMoiVoiSoBiMat(1, 100, 75);
        assertEquals(TrangThaiVan.PLAYING, p.getVanHienTai().get().getTrangThai());

        // Tuan đoán số 50 -> HIGHER
        p.xuLyLuotDoan("Tuan", 50);
        assertEquals(1, p.getKhoLichSuDoan().layLichSuCua("Tuan").size());

        // Tuan rời phòng khi ván đang chạy
        var kqRoi = quanLyPhong.roiPhong("ROOM_TEST", "Tuan");
        assertTrue(kqRoi instanceof KetQua.ThanhCong);
        assertFalse(p.coNguoiChoi("Tuan"));
        assertTrue(p.coNguoiChoi("Minh"));

        // Lịch sử đoán của Tuan vẫn được bảo toàn trong phòng
        assertEquals(1, p.getKhoLichSuDoan().layLichSuCua("Tuan").size());

        // Ván vẫn tiếp tục chạy cho Minh đoán
        assertEquals(TrangThaiVan.PLAYING, p.getVanHienTai().get().getTrangThai());
        var kqMinh = p.xuLyLuotDoan("Minh", 75);
        assertTrue(kqMinh instanceof KetQua.ThanhCong);
        assertEquals(TrangThaiVan.FINISHED, p.getVanHienTai().get().getTrangThai());
        assertEquals(1, p.getKhoBangDiem().layDiem("Minh"));
    }

    // F21 Trường hợp biên 4: Phòng rỗng cần thu hồi / dọn dẹp
    @Test
    void thuHoiPhongRongKhongChuaNguoiChoiVaVanKhongChay() {
        quanLyPhong.taoPhong("ROOM_A", "Phòng A");
        quanLyPhong.taoPhong("ROOM_B", "Phòng B");
        quanLyPhong.taoPhong("ROOM_C", "Phòng C");

        // Cho người chơi vào ROOM_B
        quanLyPhong.vaoPhong("ROOM_B", "Thang");

        // ROOM_C mở ván đang chạy
        quanLyPhong.vaoPhong("ROOM_C", "Hoang");
        PhongChoi pC = quanLyPhong.timPhong("ROOM_C").orElseThrow();
        pC.moVanMoiVoiSoBiMat(1, 50, 25);
        // Hoang rời phòng nhưng ván C đang PLAYING
        quanLyPhong.roiPhong("ROOM_C", "Hoang");

        // ROOM_A hoàn toàn rỗng và không có ván
        // Khi gọi thuHoiPhongRong(), ROOM_A phải được thu hồi, ROOM_B (có người) và ROOM_C (đang chạy) được giữ
        int soPhongThuHoi = quanLyPhong.thuHoiPhongRong();
        assertEquals(1, soPhongThuHoi);
        assertFalse(quanLyPhong.timPhong("ROOM_A").isPresent());
        assertTrue(quanLyPhong.timPhong("ROOM_B").isPresent());
        assertTrue(quanLyPhong.timPhong("ROOM_C").isPresent());
    }

    // F21 Trường hợp biên 5: Chuyển giữa hai phòng giành khoá theo thứ tự cố định chống DEADLOCK
    @Test
    void chuyenGiuaHaiPhongGiangKhoaCoDinhKhongBiDeadlockKhiChaySongSong() throws Exception {
        quanLyPhong.taoPhong("ROOM_X", "Phòng X");
        quanLyPhong.taoPhong("ROOM_Y", "Phòng Y");

        // Chuẩn bị 20 người chơi ở ROOM_X và 20 người chơi ở ROOM_Y
        int soNguoi = 20;
        for (int i = 0; i < soNguoi; i++) {
            quanLyPhong.vaoPhong("ROOM_X", "X_Player_" + i);
            quanLyPhong.vaoPhong("ROOM_Y", "Y_Player_" + i);
        }

        int soLuong = 40;
        ExecutorService pool = Executors.newFixedThreadPool(soLuong);
        CountDownLatch latch = new CountDownLatch(soLuong);
        List<Future<Boolean>> danhSachFuture = new ArrayList<>();

        // 20 luồng chuyển từ X -> Y liên tục
        for (int i = 0; i < soNguoi; i++) {
            final String ten = "X_Player_" + i;
            danhSachFuture.add(pool.submit(() -> {
                latch.countDown();
                latch.await();
                for (int lap = 0; lap < 50; lap++) {
                    quanLyPhong.chuyenPhong("ROOM_X", "ROOM_Y", ten);
                    quanLyPhong.chuyenPhong("ROOM_Y", "ROOM_X", ten);
                }
                return true;
            }));
        }

        // 20 luồng chuyển từ Y -> X ngược chiều liên tục (dễ sinh deadlock nếu không có lock ordering)
        for (int i = 0; i < soNguoi; i++) {
            final String ten = "Y_Player_" + i;
            danhSachFuture.add(pool.submit(() -> {
                latch.countDown();
                latch.await();
                for (int lap = 0; lap < 50; lap++) {
                    quanLyPhong.chuyenPhong("ROOM_Y", "ROOM_X", ten);
                    quanLyPhong.chuyenPhong("ROOM_X", "ROOM_Y", ten);
                }
                return true;
            }));
        }

        pool.shutdown();
        boolean hoanTat = pool.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(hoanTat, "Các luồng bị DEADLOCK khi tranh chấp khoá hai phòng!");

        for (var f : danhSachFuture) {
            assertTrue(f.get());
        }
    }

    // F21: Giao thức mạng TCP hỗ trợ tạo phòng, vào phòng, danh sách phòng và cách ly ván chơi
    @Test
    void daPhongQuaMangTcpTaoPhongVaoPhongVaCachLyVan() throws Exception {
        try (var socket1 = new Socket("127.0.0.1", server.getCong());
             var in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream(), StandardCharsets.UTF_8));
             var out1 = new PrintWriter(socket1.getOutputStream(), true, StandardCharsets.UTF_8);
             var socket2 = new Socket("127.0.0.1", server.getCong());
             var in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), StandardCharsets.UTF_8));
             var out2 = new PrintWriter(socket2.getOutputStream(), true, StandardCharsets.UTF_8)) {

            // Client 1 tham gia tên Alice
            out1.println("JOIN Alice");
            assertEquals("OK JOIN Alice", in1.readLine());

            // Client 2 tham gia tên Bob
            out2.println("JOIN Bob");
            assertEquals("OK JOIN Bob", in2.readLine());

            // Alice tạo phòng VIP_A
            out1.println("CREATE_ROOM VIP_A");
            String phanHoiTao = in1.readLine();
            assertTrue(phanHoiTao.startsWith("OK CREATE_ROOM VIP_A"));

            // Alice vào phòng VIP_A
            out1.println("JOIN_ROOM VIP_A");
            assertEquals("OK JOIN_ROOM VIP_A", in1.readLine());

            // Xem danh sách phòng
            out2.println("LIST_ROOMS");
            String dsPhong = in2.readLine();
            assertTrue(dsPhong.contains("VIP_A"));

            // Bob tạo phòng VIP_B và vào phòng VIP_B
            out2.println("CREATE_ROOM VIP_B");
            assertTrue(in2.readLine().startsWith("OK CREATE_ROOM VIP_B"));
            out2.println("JOIN_ROOM VIP_B");
            assertEquals("OK JOIN_ROOM VIP_B", in2.readLine());

            // Hai phòng cách ly hoàn toàn
            // Alice ở VIP_A, Bob ở VIP_B
            out1.println("CURRENT_ROOM");
            assertEquals("OK CURRENT_ROOM VIP_A", in1.readLine());

            out2.println("CURRENT_ROOM");
            assertEquals("OK CURRENT_ROOM VIP_B", in2.readLine());
        }
    }
}
