package doanso.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
    private CheDoDoan cheDoDoan;
    private String nguoiThang;
    private volatile boolean biDongDoHetGio;
    private final List<LuotDoan> dsLuotDoan;
    private final Set<String> nguoiChoiTrongVan;
    private final Deque<String> hangDoiLuot;

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
        this.cheDoDoan = CheDoDoan.DONG_THOI;
        this.nguoiThang = null;
        this.biDongDoHetGio = false;
        this.dsLuotDoan = new ArrayList<>();
        this.nguoiChoiTrongVan = new HashSet<>();
        this.hangDoiLuot = new ArrayDeque<>();
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
        String tenChuan = ten.strip();
        nguoiChoiTrongVan.add(tenChuan);
        if (!hangDoiLuot.contains(tenChuan)) {
            hangDoiLuot.offer(tenChuan);
        }
        return new KetQua.ThanhCong<>(null, "Đã tham gia ván #" + maVan);
    }

    public synchronized void roiVan(String ten) {
        if (ten != null) {
            String tenChuan = ten.strip();
            nguoiChoiTrongVan.remove(tenChuan);
            hangDoiLuot.remove(tenChuan);
        }
    }

    public synchronized void batCheDoLanLuot() {
        this.cheDoDoan = CheDoDoan.LAN_LUOT;
    }

    public synchronized CheDoDoan getCheDoDoan() {
        return cheDoDoan;
    }

    public synchronized Optional<String> getNguoiDangGiuLuot() {
        if (cheDoDoan != CheDoDoan.LAN_LUOT) {
            return Optional.empty();
        }
        return Optional.ofNullable(hangDoiLuot.peek());
    }

    public synchronized Optional<String> boQuaLuotHienTai() {
        if (cheDoDoan != CheDoDoan.LAN_LUOT || hangDoiLuot.isEmpty()) {
            return Optional.empty();
        }
        String nguoiBiBoQua = hangDoiLuot.poll();
        if (nguoiBiBoQua != null) {
            hangDoiLuot.offer(nguoiBiBoQua);
        }
        return Optional.ofNullable(hangDoiLuot.peek());
    }

    public synchronized KetQua<TrangThaiVan> moVan() {
        KetQua<TrangThaiVan> kq = trangThai.chuyenSang(TrangThaiVan.PLAYING);
        if (kq instanceof KetQua.ThanhCong<TrangThaiVan> tc) {
            trangThai = tc.duLieu();
        }
        return kq;
    }

    public synchronized KetQua<TrangThaiVan> dongVanDoHetGio() {
        if (trangThai != TrangThaiVan.PLAYING) {
            return new KetQua.ThatBai<>(
                "KHONG_THE_DONG_DO_HET_GIO",
                "Không thể đóng do hết giờ khi trạng thái ván là " + trangThai
            );
        }
        KetQua<TrangThaiVan> kq = trangThai.chuyenSang(TrangThaiVan.FINISHED);
        if (kq instanceof KetQua.ThanhCong<TrangThaiVan> tc) {
            trangThai = tc.duLieu();
            biDongDoHetGio = true;
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
        if (cheDoDoan == CheDoDoan.LAN_LUOT) {
            String nguoiDenLuot = hangDoiLuot.peek();
            if (nguoiDenLuot != null && !nguoiDenLuot.equals(tenChuan)) {
                return new KetQua.ThatBai<>(
                    "CHUA_TOI_LUOT",
                    "Chưa tới lượt của " + tenChuan + ", đang là lượt của " + nguoiDenLuot
                );
            }
        }

        GoiY goiY = GoiY.soSanh(soDoan, soBiMat);
        KetQua<LuotDoan> kqLuot = LuotDoan.taoTrongKhoang(tenChuan, soDoan, min, max, goiY);
        if (kqLuot instanceof KetQua.ThanhCong<LuotDoan> tc) {
            LuotDoan luot = tc.duLieu();
            dsLuotDoan.add(luot);
            if (goiY == GoiY.CORRECT) {
                nguoiThang = tenChuan;
                trangThai = TrangThaiVan.FINISHED;
            } else if (cheDoDoan == CheDoDoan.LAN_LUOT) {
                String daDoan = hangDoiLuot.poll();
                if (daDoan != null) {
                    hangDoiLuot.offer(daDoan);
                }
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

    public synchronized boolean daCoNguoiThang() {
        return nguoiThang != null && trangThai == TrangThaiVan.FINISHED;
    }

    public synchronized boolean isBiDongDoHetGio() {
        return biDongDoHetGio;
    }

    public synchronized int getSoLuotDoan() {
        return dsLuotDoan.size();
    }

    public synchronized List<LuotDoan> getDanhSachLuotDoan() {
        return new ArrayList<>(dsLuotDoan);
    }
}
