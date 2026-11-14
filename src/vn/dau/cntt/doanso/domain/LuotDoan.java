package vn.dau.cntt.doanso.domain;

import java.time.LocalDateTime;

public record LuotDoan(String nguoiChoi, int soDoan, LocalDateTime thoiDiem, GoiY ketQuaGoiY) {

    public LuotDoan {
        if (nguoiChoi == null || nguoiChoi.isBlank()) {
            throw new IllegalArgumentException("Tên người chơi không được rỗng hoặc null");
        }
        if (thoiDiem == null) {
            throw new IllegalArgumentException("Thời điểm đoán không được null");
        }
        if (ketQuaGoiY == null) {
            throw new IllegalArgumentException("Kết quả gợi ý không được null");
        }
        nguoiChoi = nguoiChoi.strip();
    }

    public static KetQua<LuotDoan> taoTrongKhoang(
            String nguoiChoi, int soDoan, int min, int max, GoiY ketQuaGoiY) {
        if (min > max) {
            return new KetQua.ThatBai<>(
                "KHOANG_KHONG_HOP_LE",
                "Giá trị min (" + min + ") không được lớn hơn max (" + max + ")"
            );
        }
        if (soDoan < min || soDoan > max) {
            return new KetQua.ThatBai<>(
                "NGOAI_KHOANG",
                "Số đoán " + soDoan + " nằm ngoài khoảng [" + min + ".." + max + "]"
            );
        }
        try {
            LuotDoan luot = new LuotDoan(nguoiChoi, soDoan, LocalDateTime.now(), ketQuaGoiY);
            return new KetQua.ThanhCong<>(luot, "Tạo lượt đoán hợp lệ");
        } catch (IllegalArgumentException e) {
            return new KetQua.ThatBai<>("THAM_SO_KHONG_HOP_LE", e.getMessage());
        }
    }
}
