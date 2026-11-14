package doanso.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Game {

    private final int maVan;
    private final int min;
    private final int max;
    private final int soBiMat;
    private TrangThaiVan trangThai;
    private String nguoiThang;
    private final List<LuotDoan> dsLuotDoan;
    private final Set<String> nguoiChoiTrongVan;

    public Game(int maVan, int min, int max) {
        this(maVan, min, max, sinhSoNgauNhien(min, max));
    }

    public Game(int maVan, int min, int max, int soBiMat) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") không được lớn hơn max (" + max + ")");
        }
        if (soBiMat < min || soBiMat > max) {
            throw new IllegalArgumentException(
                "Số bí mật " + soBiMat + " phải nằm trong khoảng [" + min + ".." + max + "]"
            );
        }
        this.maVan = maVan;
        this.min = min;
        this.max = max;
        this.soBiMat = soBiMat;
        this.trangThai = TrangThaiVan.WAITING;
        this.nguoiThang = null;
        this.dsLuotDoan = new ArrayList<>();
        this.nguoiChoiTrongVan = new HashSet<>();
    }

    private static int sinhSoNgauNhien(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") không được lớn hơn max (" + max + ")");
        }
        return min + (int) (Math.random() * ((long) max - min + 1));
    }

    public synchronized KetQua<Void> thamGiaVan(String ten) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được rỗng");
        }
        nguoiChoiTrongVan.add(ten.strip());
        return new KetQua.ThanhCong<>(null, "Đã tham gia ván #" + maVan);
    }

    public synchronized void roiVan(String ten) {
        if (ten != null) {
            nguoiChoiTrongVan.remove(ten.strip());
        }
    }

    public synchronized KetQua<TrangThaiVan> moVan() {
        KetQua<TrangThaiVan> kq = trangThai.chuyenSang(TrangThaiVan.PLAYING);
        if (kq instanceof KetQua.ThanhCong<TrangThaiVan> tc) {
            trangThai = tc.duLieu();
        }
        return kq;
    }

    public synchronized KetQua<LuotDoan> doanSo(String nguoiChoi, int soDoan) {
        if (!trangThai.coTheDoan()) {
            String maLoi = switch (trangThai) {
                case WAITING -> "VAN_CHUA_MO";
                case FINISHED -> "VAN_DA_KET_THUC";
                case PLAYING -> "TRANG_THAI_KHONG_HOP_LE";
            };
            return new KetQua.ThatBai<>(maLoi, "Không thể đoán khi trạng thái ván là " + trangThai);
        }
        if (nguoiChoi == null || nguoiChoi.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được rỗng");
        }
        String tenChuan = nguoiChoi.strip();
        if (!nguoiChoiTrongVan.isEmpty() && !nguoiChoiTrongVan.contains(tenChuan)) {
            return new KetQua.ThatBai<>(
                "NGUOI_CHOI_DA_ROI_VAN",
                "Người chơi " + tenChuan + " không còn trong ván chơi"
            );
        }

        GoiY goiY = GoiY.soSanh(soDoan, soBiMat);
        KetQua<LuotDoan> kqLuot = LuotDoan.taoTrongKhoang(tenChuan, soDoan, min, max, goiY);
        if (kqLuot instanceof KetQua.ThanhCong<LuotDoan> tc) {
            LuotDoan luot = tc.duLieu();
            dsLuotDoan.add(luot);
            if (goiY == GoiY.CORRECT) {
                nguoiThang = tenChuan;
                trangThai = TrangThaiVan.FINISHED;
            }
        }
        return kqLuot;
    }

    public int getMaVan() {
        return maVan;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getSoBiMat() {
        return soBiMat;
    }

    public synchronized TrangThaiVan getTrangThai() {
        return trangThai;
    }

    public synchronized Optional<String> getNguoiThang() {
        return Optional.ofNullable(nguoiThang);
    }

    public synchronized List<LuotDoan> getDanhSachLuotDoan() {
        return new ArrayList<>(dsLuotDoan);
    }
}
