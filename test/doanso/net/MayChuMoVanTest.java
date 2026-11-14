package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.Game;
import doanso.domain.KetQua;
import doanso.domain.TrangThaiVan;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MayChuMoVanTest {

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

    // F11: Mở ván thành công khi có người chơi, chuyển sang PLAYING và broadcast
    @Test
    void moVanThanhCongKhiCoNguoiChoiVaBroadcastKhoangSo() {
        var sw1 = new StringWriter();
        var sw2 = new StringWriter();
        server.getSoOnline().dangKy("An", new PrintWriter(sw1, true));
        server.getSoOnline().dangKy("Binh", new PrintWriter(sw2, true));

        KetQua<Game> kq = server.moVanMoi(1, 100);
        var thanhCong = assertInstanceOf(KetQua.ThanhCong.class, kq);
        Game game = (Game) thanhCong.duLieu();
        assertNotNull(game);
        assertEquals(TrangThaiVan.PLAYING, game.getTrangThai());
        assertEquals(1, game.getMin());
        assertEquals(100, game.getMax());

        // Kiểm tra thông điệp broadcast tới cả 2 người chơi
        String mongDoi = "INFO BAT_DAU_VAN " + game.getMaVan() + " 1 100";
        assertTrue(sw1.toString().contains(mongDoi));
        assertTrue(sw2.toString().contains(mongDoi));
    }

    // F11 Trường hợp biên: Mở ván khi chưa có người chơi nào
    @Test
    void chanMoVanKhiChuaCoNguoiChoiNaoOnline() {
        assertEquals(0, server.getSoOnline().getSoLuongOnline());

        KetQua<Game> kq = server.moVanMoi(1, 100);
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq);
        assertEquals("CHUA_CO_NGUOI_CHOI", thatBai.maLoi());
        assertTrue(thatBai.lyDo().contains("chưa có người chơi"));
    }

    // F11 Trường hợp biên: Mở ván khi đang có ván đang diễn ra (PLAYING)
    @Test
    void chanMoVanKhiDangCoVanDangChay() {
        server.getSoOnline().dangKy("An", new PrintWriter(new StringWriter(), true));

        KetQua<Game> kq1 = server.moVanMoi(1, 50);
        assertInstanceOf(KetQua.ThanhCong.class, kq1);

        // Gọi mở ván tiếp theo khi ván 1 chưa xong
        KetQua<Game> kq2 = server.moVanMoi(1, 50);
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq2);
        assertEquals("VAN_DANG_CHAY", thatBai.maLoi());
    }

    // F11 Trường hợp biên: Tham số min > max
    @Test
    void chanMoVanVoiKhoangSoSaiMinLonHonMax() {
        server.getSoOnline().dangKy("An", new PrintWriter(new StringWriter(), true));

        KetQua<Game> kq = server.moVanMoi(100, 10);
        var thatBai = assertInstanceOf(KetQua.ThatBai.class, kq);
        assertEquals("KHOANG_SO_KHONG_HOP_LE", thatBai.maLoi());
    }

    // F11 Trường hợp biên: Người mới JOIN giữa ván đang chạy
    @Test
    void nguoiMoiVaoGiuaVanDuocThamGiaVaNhanThongBaoKhoangSo() {
        var swCu = new StringWriter();
        var pwCu = new PrintWriter(swCu, true);
        server.dangKyNguoiChoi("NguoiCu", pwCu);

        KetQua<Game> kq = server.moVanMoi(10, 50);
        assertInstanceOf(KetQua.ThanhCong.class, kq);
        Game game = server.getVanHienTai().orElseThrow();

        // Người mới tham gia
        var swMoi = new StringWriter();
        var pwMoi = new PrintWriter(swMoi, true);
        server.dangKyNguoiChoi("NguoiMoi", pwMoi);

        // Kiểm tra người mới nhận được thông báo ván đang chạy
        String tinVaoSau = "INFO VAN_DANG_CHAY " + game.getMaVan() + " 10 50";
        assertTrue(swMoi.toString().contains(tinVaoSau));

        // Kiểm tra broadcast thông báo có người mới vào tới người cũ
        assertTrue(swCu.toString().contains("INFO NGUOI_MOI_VAO NguoiMoi"));
    }

    // F11: Mở ván mới sau khi ván cũ đã kết thúc (FINISHED)
    @Test
    void moVanMoiSauKhiVanCuDaKetThuc() {
        server.getSoOnline().dangKy("An", new PrintWriter(new StringWriter(), true));

        // Mở ván 1 với số bí mật xác định để đoán trúng ngay
        server.moVanMoiVoiSoBiMat(1, 10, 5);
        Game game1 = server.getVanHienTai().orElseThrow();
        assertEquals(TrangThaiVan.PLAYING, game1.getTrangThai());

        // Đoán trúng để kết thúc ván
        game1.doanSo("An", 5);
        assertEquals(TrangThaiVan.FINISHED, game1.getTrangThai());

        // Mở ván 2 thành công
        KetQua<Game> kq2 = server.moVanMoi(1, 20);
        assertInstanceOf(KetQua.ThanhCong.class, kq2);
        Game game2 = server.getVanHienTai().orElseThrow();
        assertEquals(TrangThaiVan.PLAYING, game2.getTrangThai());
        assertEquals(game1.getMaVan() + 1, game2.getMaVan());
    }
}
