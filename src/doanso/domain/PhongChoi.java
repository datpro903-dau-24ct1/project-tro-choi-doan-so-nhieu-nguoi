package doanso.domain;

import doanso.repo.KhoBangDiem;
import doanso.repo.KhoLichSuDoan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// F21: Lớp PhongChoi thuộc Tầng Nghiệp vụ - độc lập với hạ tầng mạng
public class PhongChoi {

    private final String maPhong;
    private final String tenPhong;
    private final KhoBangDiem khoBangDiem;
    private final KhoLichSuDoan khoLichSuDoan;
    private final List<Game> danhSachCacVan;
    private final Set<String> danhSachNguoiChoi;
    private final Object khoaPhong = new Object();

    private Game vanHienTai;
    private int demMaVan;

    public PhongChoi(String maPhong) {
        this(maPhong, maPhong);
    }

    public PhongChoi(String maPhong, String tenPhong) {
        if (maPhong == null || maPhong.isBlank()) {
            throw new IllegalArgumentException("Mã phòng không được để trống");
        }
        this.maPhong = maPhong.strip().toUpperCase();
        this.tenPhong = (tenPhong == null || tenPhong.isBlank()) ? this.maPhong : tenPhong.strip();
        this.khoBangDiem = new KhoBangDiem();
        this.khoLichSuDoan = new KhoLichSuDoan();
        this.danhSachCacVan = new ArrayList<>();
        this.danhSachNguoiChoi = new HashSet<>();
        this.vanHienTai = null;
        this.demMaVan = 0;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public String getTenPhong() {
        return tenPhong;
    }

    public KhoBangDiem getKhoBangDiem() {
        return khoBangDiem;
    }

    public KhoLichSuDoan getKhoLichSuDoan() {
        return khoLichSuDoan;
    }

    public List<Game> getDanhSachCacVan() {
        synchronized (khoaPhong) {
            return new ArrayList<>(danhSachCacVan);
        }
    }

    public Optional<Game> getVanHienTai() {
        synchronized (khoaPhong) {
            return Optional.ofNullable(vanHienTai);
        }
    }

    public int getSoLuongNguoiChoi() {
        synchronized (khoaPhong) {
            return danhSachNguoiChoi.size();
        }
    }

    public Set<String> getDanhSachNguoiChoi() {
        synchronized (khoaPhong) {
            return Set.copyOf(danhSachNguoiChoi);
        }
    }

    public boolean coNguoiChoi(String ten) {
        if (ten == null || ten.isBlank()) {
            return false;
        }
        synchronized (khoaPhong) {
            return danhSachNguoiChoi.contains(ten.strip());
        }
    }

    public boolean isRong() {
        synchronized (khoaPhong) {
            return danhSachNguoiChoi.isEmpty();
        }
    }

    public KetQua<Void> themNguoiChoi(String ten) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được để trống");
        }
        String tenChuan = ten.strip();
        synchronized (khoaPhong) {
            if (danhSachNguoiChoi.contains(tenChuan)) {
                return new KetQua.ThatBai<>("NGUOI_CHOI_DA_O_TRONG_PHONG", "Người chơi '" + tenChuan + "' đã ở trong phòng");
            }
            danhSachNguoiChoi.add(tenChuan);
            khoBangDiem.ghiNhanNguoiChoi(tenChuan);
            if (vanHienTai != null && vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                vanHienTai.thamGiaVan(tenChuan);
            }
            return new KetQua.ThanhCong<>(null, "Tham gia phòng thành công: " + maPhong);
        }
    }

    public boolean xoaNguoiChoi(String ten) {
        if (ten == null || ten.isBlank()) {
            return false;
        }
        String tenChuan = ten.strip();
        synchronized (khoaPhong) {
            boolean daXoa = danhSachNguoiChoi.remove(tenChuan);
            if (daXoa && vanHienTai != null) {
                vanHienTai.roiVan(tenChuan);
            }
            return daXoa;
        }
    }

    public KetQua<Game> moVanMoi(int min, int max) {
        if (min > max) {
            return new KetQua.ThatBai<>("KHOANG_SO_KHONG_HOP_LE", "min (" + min + ") không được lớn hơn max (" + max + ")");
        }
        synchronized (khoaPhong) {
            if (danhSachNguoiChoi.isEmpty()) {
                return new KetQua.ThatBai<>("CHUA_CO_NGUOI_CHOI", "Không thể mở ván khi phòng chưa có người chơi");
            }
            if (vanHienTai != null && vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                return new KetQua.ThatBai<>("VAN_DANG_CHAY", "Ván #" + vanHienTai.getMaVan() + " đang diễn ra trong phòng");
            }
            demMaVan++;
            Game gameMoi = new Game(demMaVan, min, max);
            danhSachNguoiChoi.forEach(gameMoi::thamGiaVan);
            gameMoi.moVan();
            this.vanHienTai = gameMoi;
            this.danhSachCacVan.add(gameMoi);
            return new KetQua.ThanhCong<>(gameMoi, "Đã mở ván #" + gameMoi.getMaVan() + " trong phòng " + maPhong);
        }
    }

    public KetQua<Game> moVanMoiVoiSoBiMat(int min, int max, int soBiMat) {
        if (min > max) {
            return new KetQua.ThatBai<>("KHOANG_SO_KHONG_HOP_LE", "min (" + min + ") không được lớn hơn max (" + max + ")");
        }
        synchronized (khoaPhong) {
            if (danhSachNguoiChoi.isEmpty()) {
                return new KetQua.ThatBai<>("CHUA_CO_NGUOI_CHOI", "Không thể mở ván khi phòng chưa có người chơi");
            }
            if (vanHienTai != null && vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                return new KetQua.ThatBai<>("VAN_DANG_CHAY", "Ván #" + vanHienTai.getMaVan() + " đang diễn ra trong phòng");
            }
            demMaVan++;
            Game gameMoi = new Game(demMaVan, min, max, soBiMat);
            danhSachNguoiChoi.forEach(gameMoi::thamGiaVan);
            gameMoi.moVan();
            this.vanHienTai = gameMoi;
            this.danhSachCacVan.add(gameMoi);
            return new KetQua.ThanhCong<>(gameMoi, "Đã mở ván #" + gameMoi.getMaVan() + " trong phòng " + maPhong);
        }
    }

    public KetQua<LuotDoan> xuLyLuotDoan(String ten, int so) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("CHUA_DANG_NHAP", "Tên người chơi không hợp lệ");
        }
        String tenChuan = ten.strip();
        synchronized (khoaPhong) {
            if (!danhSachNguoiChoi.contains(tenChuan)) {
                return new KetQua.ThatBai<>("KHONG_THUOC_PHONG", "Người chơi không thuộc phòng này");
            }
            if (vanHienTai == null) {
                return new KetQua.ThatBai<>("VAN_CHUA_MO", "Phòng chưa có ván nào đang mở");
            }
            KetQua<LuotDoan> kq = vanHienTai.doanSo(tenChuan, so);
            if (kq instanceof KetQua.ThanhCong<LuotDoan> tc) {
                LuotDoan luot = tc.duLieu();
                khoLichSuDoan.ghiNhanLuotDoan(luot);
                if (luot.ketQuaGoiY() == GoiY.CORRECT) {
                    khoBangDiem.congDiemThang(tenChuan);
                }
            }
            return kq;
        }
    }

    public KetQua<TrangThaiVan> dongVanDoHetGio() {
        synchronized (khoaPhong) {
            if (vanHienTai == null) {
                return new KetQua.ThatBai<>("VAN_CHUA_MO", "Phòng chưa mở ván");
            }
            return vanHienTai.dongVanDoHetGio();
        }
    }

    public Object getKhoaPhong() {
        return khoaPhong;
    }
}
