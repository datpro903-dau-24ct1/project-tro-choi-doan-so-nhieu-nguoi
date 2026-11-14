package doanso.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.KetQua;
import doanso.net.GameServer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientUITest {

    private GameServer server;
    private ExecutorService serverPool;
    private ClientUI clientUI;

    @BeforeEach
    void khoiTao() throws Exception {
        server = new GameServer(0);
        serverPool = Executors.newSingleThreadExecutor();
        serverPool.submit(() -> server.vongLapChapNhanKetNoi());
        clientUI = new ClientUI(false); // false: không hiện cửa sổ thực khi chạy unit test
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

    // F07: Kết nối thành công tới server và gửi lệnh JOIN
    @Test
    void ketNoiThanhCongVaGuiLenhJoin() throws Exception {
        KetQua<Void> kq = clientUI.ketNoi("127.0.0.1", server.getCong(), "Tuan");
        assertInstanceOf(KetQua.ThanhCong.class, kq);
        assertTrue(clientUI.daKetNoi());
        assertEquals("Tuan", clientUI.getTenNguoiChoi());

        // Đợi 1 chút để luồng đọc mạng nhận phản hồi OK JOIN
        TimeUnit.MILLISECONDS.sleep(150);
        assertTrue(clientUI.getNoiDungNhatKy().contains("OK JOIN Tuan"));
    }

    // F07 Trường hợp biên: Server chưa chạy
    @Test
    void baoLoiKhiServerChuaChayKhongLamSapChuongTrinh() {
        // Cố kết nối tới 1 cổng chưa mở
        int congChuaMo = 65432;
        KetQua<Void> kq = clientUI.ketNoi("127.0.0.1", congChuaMo, "Tuan");
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq);
        assertEquals("KET_NOI_THAT_BAI", thatBai.maLoi());
        assertFalse(clientUI.daKetNoi());
    }

    // F07 Trường hợp biên: Nhập chữ thay vì số vào ô đoán
    @Test
    void chanNhapChuThayViSoVaoODoan() throws Exception {
        clientUI.ketNoi("127.0.0.1", server.getCong(), "Tuan");

        KetQua<Void> kqChu = clientUI.guiSoDoan("abc");
        var thatBaiChu = assertInstanceOf(KetQua.ThatBai.class, kqChu);
        assertEquals("SO_DOAN_KHONG_HOP_LE", thatBaiChu.maLoi());

        KetQua<Void> kqHonHop = clientUI.guiSoDoan("12a3");
        var thatBaiHonHop = assertInstanceOf(KetQua.ThatBai.class, kqHonHop);
        assertEquals("SO_DOAN_KHONG_HOP_LE", thatBaiHonHop.maLoi());
    }

    // F07 Trường hợp biên: Ô nhập số đoán rỗng hoặc khoảng trắng
    @Test
    void chanODoanRongHoacKhoangTrang() throws Exception {
        clientUI.ketNoi("127.0.0.1", server.getCong(), "Tuan");

        KetQua<Void> kqRong = clientUI.guiSoDoan("");
        var thatBaiRong = assertInstanceOf(KetQua.ThatBai.class, kqRong);
        assertEquals("SO_DOAN_RONG", thatBaiRong.maLoi());

        KetQua<Void> kqTrang = clientUI.guiSoDoan("   ");
        var thatBaiTrang = assertInstanceOf(KetQua.ThatBai.class, kqTrang);
        assertEquals("SO_DOAN_RONG", thatBaiTrang.maLoi());
    }

    // F07 Trường hợp biên: Đoán khi chưa kết nối
    @Test
    void chanDoanKhiChuaKetNoiServer() {
        assertFalse(clientUI.daKetNoi());
        KetQua<Void> kq = clientUI.guiSoDoan("50");
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq);
        assertEquals("CHUA_KET_NOI", thatBai.maLoi());
    }

    // F07 Trường hợp biên: Server đóng đột ngột giữa ván (Tách luồng đọc mạng riêng không đơ)
    @Test
    void phatHienServerDongDotNgotVaCapNhatTrangThaiKhongTreoUI() throws Exception {
        clientUI.ketNoi("127.0.0.1", server.getCong(), "Tuan");
        assertTrue(clientUI.daKetNoi());

        // Đóng server
        server.close();

        // Đợi luồng đọc mạng phát hiện EOF và kích hoạt mất kết nối
        TimeUnit.MILLISECONDS.sleep(300);
        assertFalse(clientUI.daKetNoi());
        assertTrue(clientUI.getNoiDungNhatKy().contains("MẤT KẾT NỐI") ||
                   clientUI.getNoiDungNhatKy().contains("Đã ngắt kết nối"));
    }

    // F07: Nhận thông điệp broadcast từ server và hiển thị lên nhật ký
    @Test
    void nhanTinBroadcastVaHienThiLenNhatKy() throws Exception {
        clientUI.ketNoi("127.0.0.1", server.getCong(), "Tuan");
        TimeUnit.MILLISECONDS.sleep(100);

        // Server broadcast tin
        server.broadcast("INFO BAT_DAU_VAN 1 1 100");
        TimeUnit.MILLISECONDS.sleep(200);

        assertTrue(clientUI.getNoiDungNhatKy().contains("INFO BAT_DAU_VAN 1 1 100"));
    }
}
