package vn.dau.cntt.doanso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class LuotDoanTest {

    @Test
    void khoiTaoHopLeVaTuDongCatKhoangTrangTen() {
        LocalDateTime moc = LocalDateTime.of(2026, 11, 14, 10, 0, 0);
        LuotDoan luot = new LuotDoan("  An  ", 42, moc, GoiY.CORRECT);

        assertEquals("An", luot.nguoiChoi());
        assertEquals(42, luot.soDoan());
        assertEquals(moc, luot.thoiDiem());
        assertEquals(GoiY.CORRECT, luot.ketQuaGoiY());
    }

    @Test
    void nemNgoaiLeKhiTenNullHoacRongTrongCompactConstructor() {
        LocalDateTime moc = LocalDateTime.of(2026, 11, 14, 10, 0, 0);

        assertThrows(IllegalArgumentException.class,
            () -> new LuotDoan(null, 50, moc, GoiY.LOWER));
        assertThrows(IllegalArgumentException.class,
            () -> new LuotDoan("", 50, moc, GoiY.LOWER));
        assertThrows(IllegalArgumentException.class,
            () -> new LuotDoan("   ", 50, moc, GoiY.LOWER));
    }

    @Test
    void nemNgoaiLeKhiThoiDiemHoacGoiYBiNull() {
        LocalDateTime moc = LocalDateTime.of(2026, 11, 14, 10, 0, 0);

        assertThrows(IllegalArgumentException.class,
            () -> new LuotDoan("An", 50, null, GoiY.LOWER));
        assertThrows(IllegalArgumentException.class,
            () -> new LuotDoan("An", 50, moc, null));
    }

    @Test
    void taoTrongKhoangTraVeThatBaiKhiNgoaiKhoangHoacThamSoSai() {
        // Gia tri nho hon min
        KetQua<LuotDoan> nhoHonMin = LuotDoan.taoTrongKhoang("An", 0, 1, 100, GoiY.HIGHER);
        assertFalse(nhoHonMin.isThanhCong());
        KetQua.ThatBai<LuotDoan> tb1 = assertInstanceOf(KetQua.ThatBai.class, nhoHonMin);
        assertEquals("NGOAI_KHOANG", tb1.maLoi());

        // Gia tri lon hon max
        KetQua<LuotDoan> lonHonMax = LuotDoan.taoTrongKhoang("An", 101, 1, 100, GoiY.LOWER);
        assertFalse(lonHonMax.isThanhCong());
        KetQua.ThatBai<LuotDoan> tb2 = assertInstanceOf(KetQua.ThatBai.class, lonHonMax);
        assertEquals("NGOAI_KHOANG", tb2.maLoi());

        // Khoang min > max
        assertFalse(LuotDoan.taoTrongKhoang("An", 50, 100, 1, GoiY.LOWER).isThanhCong());

        // Ten rong khi goi taoTrongKhoang
        assertFalse(LuotDoan.taoTrongKhoang("  ", 50, 1, 100, GoiY.LOWER).isThanhCong());
    }

    @Test
    void taoTrongKhoangHopLeTaiBienMinMaxGanThoiDiemHienTai() {
        KetQua<LuotDoan> taiMin = LuotDoan.taoTrongKhoang("Binh", 1, 1, 100, GoiY.HIGHER);
        assertTrue(taiMin.isThanhCong());
        KetQua.ThanhCong<LuotDoan> tcMin = assertInstanceOf(KetQua.ThanhCong.class, taiMin);
        assertEquals(1, tcMin.duLieu().soDoan());
        assertNotNull(tcMin.duLieu().thoiDiem());

        KetQua<LuotDoan> taiMax = LuotDoan.taoTrongKhoang("Binh", 100, 1, 100, GoiY.LOWER);
        assertTrue(taiMax.isThanhCong());
        KetQua.ThanhCong<LuotDoan> tcMax = assertInstanceOf(KetQua.ThanhCong.class, taiMax);
        assertEquals(100, tcMax.duLieu().soDoan());
    }

    @Test
    void haiLuotGiongHetNhauThiBangNhauVaCungHashCodeNhungKhacThoiDiemThiKhacNhau() {
        LocalDateTime t1 = LocalDateTime.of(2026, 11, 14, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 11, 14, 10, 0, 5);

        LuotDoan a1 = new LuotDoan("An", 50, t1, GoiY.LOWER);
        LuotDoan a2 = new LuotDoan("An", 50, t1, GoiY.LOWER);
        LuotDoan aKhacGio = new LuotDoan("An", 50, t2, GoiY.LOWER);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, aKhacGio);

        Set<LuotDoan> tapHop = new HashSet<>();
        tapHop.add(a1);
        tapHop.add(a2);
        tapHop.add(aKhacGio);
        assertEquals(2, tapHop.size());
    }
}
