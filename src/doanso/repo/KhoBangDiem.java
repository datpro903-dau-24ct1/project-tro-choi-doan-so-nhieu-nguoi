package doanso.repo;

import doanso.domain.KetQua;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KhoBangDiem {

    public record HangBangDiem(String tenNguoiChoi, int diem) {
        public HangBangDiem {
            if (tenNguoiChoi == null || tenNguoiChoi.isBlank()) {
                throw new IllegalArgumentException("Tên người chơi không được rỗng");
            }
            tenNguoiChoi = tenNguoiChoi.strip();
        }
    }

    private final Map<String, Integer> bangDiem = new ConcurrentHashMap<>();

    public synchronized KetQua<Integer> ghiNhanNguoiChoi(String ten) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được rỗng");
        }
        String tenChuan = ten.strip();
        bangDiem.putIfAbsent(tenChuan, 0);
        return new KetQua.ThanhCong<>(bangDiem.getOrDefault(tenChuan, 0), "Đã ghi nhận người chơi " + tenChuan);
    }

    public synchronized KetQua<Integer> congDiemThang(String ten) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được rỗng");
        }
        String tenChuan = ten.strip();
        int diemMoi = bangDiem.merge(tenChuan, 1, Integer::sum);
        return new KetQua.ThanhCong<>(diemMoi, "Đã cộng 1 điểm cho " + tenChuan);
    }

    public synchronized int layDiem(String ten) {
        if (ten == null || ten.isBlank()) {
            return 0;
        }
        return bangDiem.getOrDefault(ten.strip(), 0);
    }

    public synchronized List<HangBangDiem> layBangXepHang() {
        List<HangBangDiem> danhSach = new ArrayList<>(bangDiem.size());
        for (var muc : bangDiem.entrySet()) {
            danhSach.add(new HangBangDiem(muc.getKey(), muc.getValue()));
        }
        Comparator<HangBangDiem> boSoSanh = Comparator
            .comparing(HangBangDiem::diem)
            .reversed()
            .thenComparing(HangBangDiem::tenNguoiChoi);
        danhSach.sort(boSoSanh);
        return danhSach;
    }
}
