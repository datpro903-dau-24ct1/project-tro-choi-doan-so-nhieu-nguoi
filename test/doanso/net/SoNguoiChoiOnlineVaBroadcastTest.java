package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.KetQua;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SoNguoiChoiOnlineVaBroadcastTest {

    private SoNguoiChoiOnline soOnline;

    @BeforeEach
    void khoiTao() {
        soOnline = new SoNguoiChoiOnline();
    }

    // F09: Đăng ký thành công và truy vấn trạng thái online
    @Test
    void dangKyNguoiChoiMoiThanhCongVaTruyVanKenhGhi() {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw, true);

        KetQua<Void> kq = soOnline.dangKy("MinhAn", pw);
        assertInstanceOf(KetQua.ThanhCong.class, kq);
        assertTrue(soOnline.daOnline("MinhAn"));
        assertTrue(soOnline.daOnline("  MinhAn  ")); // Chuẩn hóa khoảng trắng
        assertEquals(1, soOnline.getSoLuongOnline());

        var optKenh = soOnline.layKenhGhi("MinhAn");
        assertTrue(optKenh.isPresent());
        assertEquals(pw, optKenh.get());
    }

    // F09 Trường hợp biên: Tên null, rỗng hoặc toàn khoảng trắng
    @Test
    void chanDangKyKhiTenNullHoacRong() {
        var pw = new PrintWriter(new StringWriter());

        KetQua<Void> kqNull = soOnline.dangKy(null, pw);
        var thatBaiNull = assertInstanceOf(KetQua.ThatBai.class, kqNull);
        assertEquals("TEN_KHONG_HOP_LE", thatBaiNull.maLoi());

        KetQua<Void> kqRong = soOnline.dangKy("", pw);
        var thatBaiRong = assertInstanceOf(KetQua.ThatBai.class, kqRong);
        assertEquals("TEN_KHONG_HOP_LE", thatBaiRong.maLoi());

        KetQua<Void> kqTrang = soOnline.dangKy("    ", pw);
        var thatBaiTrang = assertInstanceOf(KetQua.ThatBai.class, kqTrang);
        assertEquals("TEN_KHONG_HOP_LE", thatBaiTrang.maLoi());

        assertEquals(0, soOnline.getSoLuongOnline());
    }

    // F09 Trường hợp biên: Hai người đăng ký cùng tên (tuần tự)
    @Test
    void chanDangKyKhiTenDaTonTai() {
        var pw1 = new PrintWriter(new StringWriter());
        var pw2 = new PrintWriter(new StringWriter());

        KetQua<Void> kq1 = soOnline.dangKy("Binh", pw1);
        assertInstanceOf(KetQua.ThanhCong.class, kq1);

        KetQua<Void> kq2 = soOnline.dangKy("Binh", pw2);
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq2);
        assertEquals("TEN_DA_TON_TAI", thatBai.maLoi());

        // Kiểm tra với tên có khoảng trắng thừa
        KetQua<Void> kq3 = soOnline.dangKy("  Binh  ", pw2);
        var thatBaiTrang = assertInstanceOf(KetQua.ThatBai.class, kq3);
        assertEquals("TEN_DA_TON_TAI", thatBaiTrang.maLoi());

        assertEquals(1, soOnline.getSoLuongOnline());
    }

    // F09 Trường hợp biên: Hai người cùng đăng ký một tên cùng một lúc (Đồng thời)
    @Test
    void nhieuLuongCungDangKyMotTenChiDungMotLuongThanhCong() throws Exception {
        int soLuong = 20;
        ExecutorService beLuong = Executors.newFixedThreadPool(soLuong);
        try {
            CountDownLatch chotXuatPhat = new CountDownLatch(1);
            List<Callable<KetQua<Void>>> tacVu = new ArrayList<>();

            for (int i = 0; i < soLuong; i++) {
                tacVu.add(() -> {
                    chotXuatPhat.await();
                    var pw = new PrintWriter(new StringWriter());
                    return soOnline.dangKy("NguoiChoiDuyNhat", pw);
                });
            }

            chotXuatPhat.countDown();
            List<Future<KetQua<Void>>> ketQuaFutures = beLuong.invokeAll(tacVu);

            int soThanhCong = 0;
            int soThatBai = 0;
            for (var f : ketQuaFutures) {
                KetQua<Void> kq = f.get(2, TimeUnit.SECONDS);
                if (kq instanceof KetQua.ThanhCong) {
                    soThanhCong++;
                } else if (kq instanceof KetQua.ThatBai<Void> tb) {
                    assertEquals("TEN_DA_TON_TAI", tb.maLoi());
                    soThatBai++;
                }
            }

            assertEquals(1, soThanhCong, "Chỉ duy nhất 1 luồng được đăng ký tên trùng");
            assertEquals(soLuong - 1, soThatBai);
            assertEquals(1, soOnline.getSoLuongOnline());
        } finally {
            beLuong.shutdown();
        }
    }

    // F09: Hủy đăng ký khi người chơi rời phòng
    @Test
    void huyDangKyKhiNguoiChoiRoiPhong() {
        var pw = new PrintWriter(new StringWriter());
        soOnline.dangKy("Cuong", pw);
        assertEquals(1, soOnline.getSoLuongOnline());

        boolean daHuy = soOnline.huyDangKy("Cuong");
        assertTrue(daHuy);
        assertFalse(soOnline.daOnline("Cuong"));
        assertEquals(0, soOnline.getSoLuongOnline());
        assertTrue(soOnline.layKenhGhi("Cuong").isEmpty());

        // Hủy người không tồn tại
        assertFalse(soOnline.huyDangKy("KhongTonTai"));
        assertFalse(soOnline.huyDangKy(null));
    }

    // F09: Duyệt sổ khi có người vào ra đồng thời không ném ngoại lệ
    @Test
    void duyetSoDongThoiKhiCoNguoiVaoRaKhongNemNgoaiLe() throws Exception {
        ExecutorService beLuong = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch batDau = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            // 2 luồng thêm người
            for (int i = 0; i < 2; i++) {
                final int id = i;
                futures.add(beLuong.submit(() -> {
                    batDau.await();
                    for (int j = 0; j < 50; j++) {
                        soOnline.dangKy("Player_" + id + "_" + j, new PrintWriter(new StringWriter()));
                    }
                    return null;
                }));
            }

            // 1 luồng xóa người
            futures.add(beLuong.submit(() -> {
                batDau.await();
                for (int j = 0; j < 50; j++) {
                    soOnline.huyDangKy("Player_0_" + j);
                }
                return null;
            }));

            // 1 luồng đọc duyệt danh sách
            futures.add(beLuong.submit(() -> {
                batDau.await();
                for (int j = 0; j < 50; j++) {
                    Set<String> danhSach = soOnline.getDanhSachTenOnline();
                    assertNotNull(danhSach);
                }
                return null;
            }));

            batDau.countDown();
            for (var f : futures) {
                f.get(3, TimeUnit.SECONDS);
            }
        } finally {
            beLuong.shutdown();
        }
    }

    // F10: Broadcast tin nhắn tới tất cả người chơi
    @Test
    void broadcastTinNhanDenMoiNguoiChoiOnline() {
        var sw1 = new StringWriter();
        var sw2 = new StringWriter();
        var sw3 = new StringWriter();

        soOnline.dangKy("P1", new PrintWriter(sw1, true));
        soOnline.dangKy("P2", new PrintWriter(sw2, true));
        soOnline.dangKy("P3", new PrintWriter(sw3, true));

        int soNguoiNhan = soOnline.broadcast("INFO CHAO_MUNG_VAN_CHOI");
        assertEquals(3, soNguoiNhan);

        assertTrue(sw1.toString().contains("INFO CHAO_MUNG_VAN_CHOI"));
        assertTrue(sw2.toString().contains("INFO CHAO_MUNG_VAN_CHOI"));
        assertTrue(sw3.toString().contains("INFO CHAO_MUNG_VAN_CHOI"));
    }

    // F10 Trường hợp biên: Client rớt mạng/lỗi PrintWriter thì tự động gỡ khỏi sổ online
    @Test
    void clientLoiKhiBroadcastTuDongBiGoKhoiSo() {
        var swBinhThuong = new StringWriter();
        soOnline.dangKy("PlayerTot", new PrintWriter(swBinhThuong, true));

        // Client giả lập lỗi (PrintWriter có cờ setError)
        var pwLoi = new PrintWriter(new StringWriter(), true) {
            @Override
            public boolean checkError() {
                return true;
            }
        };
        soOnline.dangKy("PlayerLoi", pwLoi);
        assertEquals(2, soOnline.getSoLuongOnline());

        // Khi broadcast, phát hiện PlayerLoi bị lỗi và tự động gỡ khỏi sổ
        int soNhan = soOnline.broadcast("TEST_CANH_BAO");
        assertEquals(1, soNhan);
        assertEquals(1, soOnline.getSoLuongOnline());
        assertTrue(soOnline.daOnline("PlayerTot"));
        assertFalse(soOnline.daOnline("PlayerLoi"));
    }

    // F10: Gửi tin riêng cho từng người chơi
    @Test
    void guiTinRiengChoMotNguoiChoi() {
        var sw = new StringWriter();
        soOnline.dangKy("Lan", new PrintWriter(sw, true));

        boolean ketQua = soOnline.guiTin("Lan", "GREETING Xin chao Lan");
        assertTrue(ketQua);
        assertTrue(sw.toString().contains("GREETING Xin chao Lan"));

        assertFalse(soOnline.guiTin("KhongAi", "Tin nhắn"));
    }
}
