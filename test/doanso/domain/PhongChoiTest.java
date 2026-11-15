package doanso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// F21: Kiểm thử đơn vị cho lớp PhongChoi (Tầng Nghiệp vụ - độc lập với mạng)
public class PhongChoiTest {

    private PhongChoi phongA;
    private PhongChoi phongB;

    @BeforeEach
    void khoiTao() {
        phongA = new PhongChoi("PHONG_01", "Phòng VIP 1");
        phongB = new PhongChoi("PHONG_02", "Phòng VIP 2");
    }

    // Kiểm tra khởi tạo phòng hợp lệ với mã phòng và tên phòng
    @Test
    void khoiTaoPhongHopLeVaThuocTinhBanDau() {
        assertEquals("PHONG_01", phongA.getMaPhong());
        assertEquals("Phòng VIP 1", phongA.getTenPhong());
        assertTrue(phongA.isRong());
        assertEquals(0, phongA.getSoLuongNguoiChoi());
        assertNotNull(phongA.getKhoBangDiem());
        assertNotNull(phongA.getKhoLichSuDoan());
        assertTrue(phongA.getVanHienTai().isEmpty());
    }

    // Kiểm tra chặn mã phòng null hoặc khoảng trắng
    @Test
    void chanKhoiTaoPhongVoiMaNullHoacRong() {
        assertThrows(IllegalArgumentException.class, () -> new PhongChoi(null, "Test"));
        assertThrows(IllegalArgumentException.class, () -> new PhongChoi("   ", "Test"));
    }

    // Thêm và xoá người chơi trong phòng
    @Test
    void themVaXoaNguoiChoiTrongPhong() {
        var kq1 = phongA.themNguoiChoi("An");
        assertTrue(kq1 instanceof KetQua.ThanhCong);
        assertEquals(1, phongA.getSoLuongNguoiChoi());
        assertTrue(phongA.coNguoiChoi("An"));
        assertFalse(phongA.isRong());

        // Chặn thêm trùng tên trong cùng một phòng
        var kqTrung = phongA.themNguoiChoi("An");
        assertTrue(kqTrung instanceof KetQua.ThatBai);

        // Xoá người chơi
        assertTrue(phongA.xoaNguoiChoi("An"));
        assertEquals(0, phongA.getSoLuongNguoiChoi());
        assertTrue(phongA.isRong());
        assertFalse(phongA.coNguoiChoi("An"));
    }

    // Mở ván và đoán số trong phòng chơi
    @Test
    void moVanVaDoanSoTrongPhongChoi() {
        phongA.themNguoiChoi("Binh");
        phongA.themNguoiChoi("Cuong");

        // Mở ván với số bí mật định trước là 50
        var kqMo = phongA.moVanMoiVoiSoBiMat(1, 100, 50);
        assertTrue(kqMo instanceof KetQua.ThanhCong);
        assertTrue(phongA.getVanHienTai().isPresent());
        assertEquals(TrangThaiVan.PLAYING, phongA.getVanHienTai().get().getTrangThai());

        // Binh đoán 30 -> HIGHER
        var kqDoan1 = phongA.xuLyLuotDoan("Binh", 30);
        assertTrue(kqDoan1 instanceof KetQua.ThanhCong);
        assertEquals(GoiY.HIGHER, ((KetQua.ThanhCong<LuotDoan>) kqDoan1).duLieu().ketQuaGoiY());

        // Cuong đoán 50 -> CORRECT (Thắng ván)
        var kqDoan2 = phongA.xuLyLuotDoan("Cuong", 50);
        assertTrue(kqDoan2 instanceof KetQua.ThanhCong);
        assertEquals(GoiY.CORRECT, ((KetQua.ThanhCong<LuotDoan>) kqDoan2).duLieu().ketQuaGoiY());
        assertEquals(TrangThaiVan.FINISHED, phongA.getVanHienTai().get().getTrangThai());

        // Điểm số được cộng vào bảng điểm riêng của phòng A
        assertEquals(1, phongA.getKhoBangDiem().layDiem("Cuong"));
        assertEquals(0, phongA.getKhoBangDiem().layDiem("Binh"));
        assertEquals(2, phongA.getKhoLichSuDoan().layTatCaLuotDoan().size());
    }

    // F21: Hai phòng hoạt động hoàn toàn độc lập, ván và bảng điểm tách biệt
    @Test
    void haiPhongHoatDongDocLapTachBietVanVaBangDiem() {
        phongA.themNguoiChoi("Hai");
        phongB.themNguoiChoi("Hai"); // Cùng tên nhưng ở 2 phòng khác nhau

        // Phòng A mở ván số 40, Phòng B mở ván số 80
        phongA.moVanMoiVoiSoBiMat(1, 100, 40);
        phongB.moVanMoiVoiSoBiMat(1, 100, 80);

        // Hai đoán 40 ở phòng A -> Thắng phòng A
        var kqA = phongA.xuLyLuotDoan("Hai", 40);
        assertEquals(GoiY.CORRECT, ((KetQua.ThanhCong<LuotDoan>) kqA).duLieu().ketQuaGoiY());

        // Nhưng ở phòng B, số bí mật là 80 nên đoán 40 sẽ là HIGHER, ván B vẫn PLAYING
        var kqB = phongA.xuLyLuotDoan("Hai", 40); // Ở phòng A đã kết thúc, đoán tiếp báo lỗi
        assertTrue(kqB instanceof KetQua.ThatBai);

        var kqBThuc = phongB.xuLyLuotDoan("Hai", 40);
        assertEquals(GoiY.HIGHER, ((KetQua.ThanhCong<LuotDoan>) kqBThuc).duLieu().ketQuaGoiY());
        assertEquals(TrangThaiVan.PLAYING, phongB.getVanHienTai().get().getTrangThai());

        // Bảng điểm phòng A có 1 điểm cho Hai, bảng điểm phòng B Hai có 0 điểm
        assertEquals(1, phongA.getKhoBangDiem().layDiem("Hai"));
        assertEquals(0, phongB.getKhoBangDiem().layDiem("Hai"));
    }
}
