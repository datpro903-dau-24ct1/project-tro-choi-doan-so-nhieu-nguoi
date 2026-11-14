package doanso.net;

import doanso.domain.KetQua;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Phục vụ F09 (Sổ người chơi online) và F10 (Broadcast tin nhắn)
public class SoNguoiChoiOnline {

    private final ConcurrentHashMap<String, PrintWriter> dsOnline = new ConcurrentHashMap<>();
    private final Object khoa = new Object();

    public KetQua<Void> dangKy(String ten, PrintWriter out) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được để trống");
        }
        if (out == null) {
            throw new IllegalArgumentException("Kênh ghi PrintWriter không được null");
        }
        String tenChuan = ten.strip();
        synchronized (khoa) {
            if (dsOnline.containsKey(tenChuan)) {
                return new KetQua.ThatBai<>("TEN_DA_TON_TAI", "Tên người chơi '" + tenChuan + "' đã được sử dụng");
            }
            dsOnline.put(tenChuan, out);
            return new KetQua.ThanhCong<>(null, "Đăng ký thành công người chơi: " + tenChuan);
        }
    }

    public boolean huyDangKy(String ten) {
        if (ten == null || ten.isBlank()) {
            return false;
        }
        synchronized (khoa) {
            return dsOnline.remove(ten.strip()) != null;
        }
    }

    public Optional<PrintWriter> layKenhGhi(String ten) {
        if (ten == null || ten.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(dsOnline.get(ten.strip()));
    }

    public boolean daOnline(String ten) {
        if (ten == null || ten.isBlank()) {
            return false;
        }
        return dsOnline.containsKey(ten.strip());
    }

    public int getSoLuongOnline() {
        return dsOnline.size();
    }

    public Set<String> getDanhSachTenOnline() {
        return Set.copyOf(dsOnline.keySet());
    }

    public int broadcast(String thongDiep) {
        return broadcastNgoaiTru(null, thongDiep);
    }

    public int broadcastNgoaiTru(String nguoiBoQua, String thongDiep) {
        if (thongDiep == null) {
            return 0;
        }
        int soNguoiNhan = 0;
        List<String> dsLoi = new ArrayList<>();

        for (var entry : dsOnline.entrySet()) {
            String ten = entry.getKey();
            if (nguoiBoQua != null && ten.equalsIgnoreCase(nguoiBoQua.strip())) {
                continue;
            }
            PrintWriter out = entry.getValue();
            try {
                synchronized (out) {
                    out.println(thongDiep);
                    out.flush();
                    if (out.checkError()) {
                        dsLoi.add(ten);
                        continue;
                    }
                }
                soNguoiNhan++;
            } catch (Exception e) {
                dsLoi.add(ten);
            }
        }

        // Tự động gỡ các client gặp lỗi socket khỏi sổ online
        for (String tenLoi : dsLoi) {
            huyDangKy(tenLoi);
        }

        return soNguoiNhan;
    }

    public boolean guiTin(String tenNguoiChoi, String thongDiep) {
        var optKenh = layKenhGhi(tenNguoiChoi);
        if (optKenh.isEmpty() || thongDiep == null) {
            return false;
        }
        PrintWriter out = optKenh.get();
        try {
            synchronized (out) {
                out.println(thongDiep);
                out.flush();
                if (out.checkError()) {
                    huyDangKy(tenNguoiChoi);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            huyDangKy(tenNguoiChoi);
            return false;
        }
    }
}
