package vn.dau.cntt.doanso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class GameTest {

    @Test
    void sinhSoBiMatNgauNhienNamTrongKhoangMinMaxVaChanThamSoSai() {
        for (int i = 0; i < 50; i++) {
            Game g = new Game(1, 10, 20);
            assertTrue(g.getSoBiMat() >= 10 && g.getSoBiMat() <= 20);
            assertEquals(TrangThaiVan.WAITING, g.getTrangThai());
            assertEquals(Optional.empty(), g.getNguoiThang());
        }

        // min > max hoac soBiMat ngoai khoang -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> new Game(1, 50, 10));
        assertThrows(IllegalArgumentException.class, () -> new Game(1, 1, 100, 101));
        assertThrows(IllegalArgumentException.class, () -> new Game(1, 1, 100, 0));
    }

    @Test
    void chanDoanKhiVanChuaMoHoacDaKetThuc() {
        Game game = new Game(1, 1, 100, 42);
        game.thamGiaVan("An");

        // Doan khi van con WAITING
        KetQua<LuotDoan> chuaMo = game.doanSo("An", 50);
        assertFalse(chuaMo.isThanhCong());
        KetQua.ThatBai<LuotDoan> tb1 = assertInstanceOf(KetQua.ThatBai.class, chuaMo);
        assertEquals("VAN_CHUA_MO", tb1.maLoi());

        // Mo van -> doan trung -> van FINISHED
        assertTrue(game.moVan().isThanhCong());
        assertTrue(game.doanSo("An", 42).isThanhCong());
        assertEquals(TrangThaiVan.FINISHED, game.getTrangThai());

        // Doan tiep khi van da FINISHED
        KetQua<LuotDoan> daDong = game.doanSo("An", 42);
        assertFalse(daDong.isThanhCong());
        KetQua.ThatBai<LuotDoan> tb2 = assertInstanceOf(KetQua.ThatBai.class, daDong);
        assertEquals("VAN_DA_KET_THUC", tb2.maLoi());

        // Khong cho mo lai van da FINISHED
        assertFalse(game.moVan().isThanhCong());
    }

    @Test
    void chanSoDoanNgoaiKhoangVaNguoiChoiDaRoiVan() {
        Game game = new Game(1, 1, 100, 42);
        game.thamGiaVan("An");
        game.thamGiaVan("Binh");
        game.moVan();

        // So ngoai khoang
        KetQua<LuotDoan> ngoaiKhoang = game.doanSo("An", 150);
        assertFalse(ngoaiKhoang.isThanhCong());
        KetQua.ThatBai<LuotDoan> tbNgoai = assertInstanceOf(KetQua.ThatBai.class, ngoaiKhoang);
        assertEquals("NGOAI_KHOANG", tbNgoai.maLoi());
        assertTrue(game.getDanhSachLuotDoan().isEmpty());

        // Nguoi choi roi van hoac chua tham gia van
        game.roiVan("Binh");
        KetQua<LuotDoan> daRoi = game.doanSo("Binh", 42);
        assertFalse(daRoi.isThanhCong());
        KetQua.ThatBai<LuotDoan> tbRoi = assertInstanceOf(KetQua.ThatBai.class, daRoi);
        assertEquals("NGUOI_CHOI_DA_ROI_VAN", tbRoi.maLoi());
        assertEquals(TrangThaiVan.PLAYING, game.getTrangThai());
    }

    @Test
    void traGoiYLonHonNhoHonChotNguoiThangDauTienVaSaoChepPhongThuLichSu() {
        Game game = new Game(1, 1, 100, 42);
        game.thamGiaVan("An");
        game.thamGiaVan("Binh");
        game.moVan();

        // An doan 20 -> HIGHER
        KetQua.ThanhCong<LuotDoan> l1 = assertInstanceOf(KetQua.ThanhCong.class, game.doanSo("An", 20));
        assertEquals(GoiY.HIGHER, l1.duLieu().ketQuaGoiY());

        // Binh doan 80 -> LOWER
        KetQua.ThanhCong<LuotDoan> l2 = assertInstanceOf(KetQua.ThanhCong.class, game.doanSo("Binh", 80));
        assertEquals(GoiY.LOWER, l2.duLieu().ketQuaGoiY());

        // Binh doan 42 truoc -> CORRECT & thang
        KetQua.ThanhCong<LuotDoan> l3 = assertInstanceOf(KetQua.ThanhCong.class, game.doanSo("Binh", 42));
        assertEquals(GoiY.CORRECT, l3.duLieu().ketQuaGoiY());
        assertEquals(Optional.of("Binh"), game.getNguoiThang());
        assertEquals(TrangThaiVan.FINISHED, game.getTrangThai());

        // An cung doan 42 ngay sau do -> ThatBai vi Binh da thang truoc
        KetQua<LuotDoan> l4 = game.doanSo("An", 42);
        assertFalse(l4.isThanhCong());
        assertEquals(Optional.of("Binh"), game.getNguoiThang());
        assertEquals(3, game.getDanhSachLuotDoan().size());

        // Kiem tra sao chep phong thu: sua list tra ve khong anh huong ben trong Game
        List<LuotDoan> banSao = game.getDanhSachLuotDoan();
        banSao.clear();
        assertEquals(3, game.getDanhSachLuotDoan().size());
    }
}
