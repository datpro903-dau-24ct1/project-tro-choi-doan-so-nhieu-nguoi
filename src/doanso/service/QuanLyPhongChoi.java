package doanso.service;

import doanso.domain.KetQua;
import doanso.domain.PhongChoi;
import doanso.domain.TrangThaiVan;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// F21: Hiện thực bộ quản lý nhiều phòng chơi song song (Chương 3, 4, 5, 6)
public class QuanLyPhongChoi implements QuanLyPhong {

    private final ConcurrentHashMap<String, PhongChoi> dsPhong = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> nguoiChoiThuocPhong = new ConcurrentHashMap<>();

    @Override
    public KetQua<PhongChoi> taoPhong(String maPhong) {
        return taoPhong(maPhong, maPhong);
    }

    @Override
    public KetQua<PhongChoi> taoPhong(String maPhong, String tenPhong) {
        if (maPhong == null || maPhong.isBlank()) {
            return new KetQua.ThatBai<>("MA_PHONG_KHONG_HOP_LE", "Mã phòng không được để trống");
        }
        String maChuan = maPhong.strip().toUpperCase();
        if (dsPhong.containsKey(maChuan)) {
            return new KetQua.ThatBai<>("PHONG_DA_TON_TAI", "Phòng có mã '" + maChuan + "' đã tồn tại");
        }
        PhongChoi phongMoi = new PhongChoi(maChuan, tenPhong);
        PhongChoi daCo = dsPhong.putIfAbsent(maChuan, phongMoi);
        if (daCo != null) {
            return new KetQua.ThatBai<>("PHONG_DA_TON_TAI", "Phòng có mã '" + maChuan + "' đã tồn tại");
        }
        return new KetQua.ThanhCong<>(phongMoi, "Tạo phòng thành công: " + maChuan);
    }

    @Override
    public Optional<PhongChoi> timPhong(String maPhong) {
        if (maPhong == null || maPhong.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(dsPhong.get(maPhong.strip().toUpperCase()));
    }

    @Override
    public PhongChoi layHoacTaoPhong(String maPhong) {
        if (maPhong == null || maPhong.isBlank()) {
            throw new IllegalArgumentException("Mã phòng không được để trống");
        }
        String maChuan = maPhong.strip().toUpperCase();
        return dsPhong.computeIfAbsent(maChuan, k -> new PhongChoi(k, k));
    }

    @Override
    public KetQua<Void> xoaPhong(String maPhong) {
        if (maPhong == null || maPhong.isBlank()) {
            return new KetQua.ThatBai<>("MA_PHONG_KHONG_HOP_LE", "Mã phòng không được để trống");
        }
        String maChuan = maPhong.strip().toUpperCase();
        PhongChoi p = dsPhong.get(maChuan);
        if (p == null) {
            return new KetQua.ThatBai<>("PHONG_KHONG_TON_TAI", "Phòng không tồn tại: " + maChuan);
        }
        if (p.getSoLuongNguoiChoi() > 0) {
            return new KetQua.ThatBai<>("PHONG_CON_NGUOI_CHOI", "Không thể xoá phòng khi còn người chơi");
        }
        if (p.getVanHienTai().isPresent() && p.getVanHienTai().get().getTrangThai() == TrangThaiVan.PLAYING) {
            return new KetQua.ThatBai<>("VAN_DANG_CHAY", "Không thể xoá phòng khi ván đang diễn ra");
        }
        dsPhong.remove(maChuan);
        return new KetQua.ThanhCong<>(null, "Đã xoá phòng " + maChuan);
    }

    @Override
    public KetQua<Void> vaoPhong(String maPhong, String tenNguoiChoi) {
        if (maPhong == null || maPhong.isBlank()) {
            return new KetQua.ThatBai<>("MA_PHONG_KHONG_HOP_LE", "Mã phòng không được để trống");
        }
        if (tenNguoiChoi == null || tenNguoiChoi.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được để trống");
        }
        String maChuan = maPhong.strip().toUpperCase();
        String tenChuan = tenNguoiChoi.strip();

        // F21 Trường hợp biên 2: Một người không được ở hai phòng cùng lúc
        String phongHienTai = nguoiChoiThuocPhong.get(tenChuan);
        if (phongHienTai != null) {
            if (phongHienTai.equalsIgnoreCase(maChuan)) {
                return new KetQua.ThanhCong<>(null, "Người chơi đã ở trong phòng này");
            }
            return new KetQua.ThatBai<>(
                "NGUOI_CHOI_DANG_O_PHONG_KHAC",
                "Người chơi '" + tenChuan + "' đang ở phòng '" + phongHienTai + "', vui lòng rời phòng cũ trước"
            );
        }

        PhongChoi p = dsPhong.get(maChuan);
        if (p == null) {
            return new KetQua.ThatBai<>("PHONG_KHONG_TON_TAI", "Phòng không tồn tại: " + maChuan);
        }

        KetQua<Void> kq = p.themNguoiChoi(tenChuan);
        if (kq instanceof KetQua.ThanhCong) {
            nguoiChoiThuocPhong.put(tenChuan, maChuan);
        }
        return kq;
    }

    @Override
    public KetQua<Void> roiPhong(String maPhong, String tenNguoiChoi) {
        if (maPhong == null || maPhong.isBlank() || tenNguoiChoi == null || tenNguoiChoi.isBlank()) {
            return new KetQua.ThatBai<>("THAM_SO_KHONG_HOP_LE", "Mã phòng và tên người chơi không được để trống");
        }
        String maChuan = maPhong.strip().toUpperCase();
        String tenChuan = tenNguoiChoi.strip();

        PhongChoi p = dsPhong.get(maChuan);
        if (p != null) {
            p.xoaNguoiChoi(tenChuan);
        }
        nguoiChoiThuocPhong.remove(tenChuan);
        return new KetQua.ThanhCong<>(null, "Đã rời phòng " + maChuan);
    }

    // F21 Trường hợp biên 5: Chuyển giữa 2 phòng giành khoá theo thứ tự cố định chống DEADLOCK (Chương 4)
    @Override
    public KetQua<Void> chuyenPhong(String maPhongCu, String maPhongMoi, String tenNguoiChoi) {
        if (maPhongCu == null || maPhongMoi == null || tenNguoiChoi == null) {
            return new KetQua.ThatBai<>("THAM_SO_KHONG_HOP_LE", "Mã phòng và tên người chơi không được null");
        }
        String maCu = maPhongCu.strip().toUpperCase();
        String maMoi = maPhongMoi.strip().toUpperCase();
        String tenChuan = tenNguoiChoi.strip();

        if (maCu.equals(maMoi)) {
            return new KetQua.ThanhCong<>(null, "Người chơi đã ở phòng đích");
        }

        PhongChoi pCu = dsPhong.get(maCu);
        PhongChoi pMoi = dsPhong.get(maMoi);
        if (pCu == null || pMoi == null) {
            return new KetQua.ThatBai<>("PHONG_KHONG_TON_TAI", "Một trong hai phòng không tồn tại");
        }

        // Áp dụng Lock Ordering Rule: Sắp xếp đối tượng khoá theo thứ tự từ điển của mã phòng
        PhongChoi khoa1 = maCu.compareTo(maMoi) < 0 ? pCu : pMoi;
        PhongChoi khoa2 = maCu.compareTo(maMoi) < 0 ? pMoi : pCu;

        synchronized (khoa1) {
            synchronized (khoa2) {
                pCu.xoaNguoiChoi(tenChuan);
                KetQua<Void> kqVao = pMoi.themNguoiChoi(tenChuan);
                if (kqVao instanceof KetQua.ThanhCong) {
                    nguoiChoiThuocPhong.put(tenChuan, maMoi);
                }
                return kqVao;
            }
        }
    }

    @Override
    public Optional<String> timMaPhongCuaNguoiChoi(String tenNguoiChoi) {
        if (tenNguoiChoi == null || tenNguoiChoi.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nguoiChoiThuocPhong.get(tenNguoiChoi.strip()));
    }

    // F21 Trường hợp biên 4: Thu hồi các phòng rỗng không chứa người chơi và ván không chạy
    @Override
    public int thuHoiPhongRong() {
        int dem = 0;
        for (var entry : dsPhong.entrySet()) {
            String ma = entry.getKey();
            PhongChoi p = entry.getValue();
            if (p.isRong()) {
                boolean dangChay = p.getVanHienTai()
                    .map(v -> v.getTrangThai() == TrangThaiVan.PLAYING)
                    .orElse(false);
                if (!dangChay) {
                    if (dsPhong.remove(ma, p)) {
                        dem++;
                    }
                }
            }
        }
        return dem;
    }

    @Override
    public int getSoLuongPhong() {
        return dsPhong.size();
    }

    @Override
    public Set<String> getDanhSachMaPhong() {
        return Set.copyOf(dsPhong.keySet());
    }
}
