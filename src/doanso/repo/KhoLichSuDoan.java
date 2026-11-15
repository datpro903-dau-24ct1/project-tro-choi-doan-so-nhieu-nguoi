package doanso.repo;

import doanso.domain.KetQua;
import doanso.domain.LuotDoan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KhoLichSuDoan {

    private final Map<String, List<LuotDoan>> lichSuTheoNguoi = new HashMap<>();
    private final List<LuotDoan> tatCaLuotDoan = new ArrayList<>();

    public synchronized KetQua<Void> ghiNhanLuotDoan(LuotDoan luot) {
        if (luot == null) {
            return new KetQua.ThatBai<>("LUOT_DOAN_KHONG_HOP_LE", "Lượt đoán không được null");
        }
        String ten = luot.nguoiChoi();
        List<LuotDoan> dsCuaNguoi = lichSuTheoNguoi.get(ten);
        if (dsCuaNguoi == null) {
            dsCuaNguoi = new ArrayList<>();
            lichSuTheoNguoi.put(ten, dsCuaNguoi);
        }
        dsCuaNguoi.add(luot);
        tatCaLuotDoan.add(luot);
        return new KetQua.ThanhCong<>(null, "Đã ghi nhận lượt đoán của " + ten);
    }

    public synchronized List<LuotDoan> layLichSuCua(String tenNguoiChoi) {
        if (tenNguoiChoi == null || tenNguoiChoi.isBlank()) {
            return new ArrayList<>();
        }
        List<LuotDoan> ds = lichSuTheoNguoi.get(tenNguoiChoi.strip());
        if (ds == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ds);
    }

    public synchronized Map<String, List<LuotDoan>> layToanBoLichSu() {
        Map<String, List<LuotDoan>> banSao = new HashMap<>();
        for (var muc : lichSuTheoNguoi.entrySet()) {
            banSao.put(muc.getKey(), new ArrayList<>(muc.getValue()));
        }
        return banSao;
    }

    public synchronized List<LuotDoan> layTatCaLuotDoan() {
        return new ArrayList<>(tatCaLuotDoan);
    }
}
