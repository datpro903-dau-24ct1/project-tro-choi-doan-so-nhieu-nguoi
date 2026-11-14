package vn.dau.cntt.doanso.domain;

public enum TrangThaiVan {
    WAITING(false),
    PLAYING(true),
    FINISHED(false);

    private final boolean choPhepDoan;

    TrangThaiVan(boolean choPhepDoan) {
        this.choPhepDoan = choPhepDoan;
    }

    public boolean coTheDoan() {
        return choPhepDoan;
    }

    public KetQua<TrangThaiVan> chuyenSang(TrangThaiVan dich) {
        if (dich == null) {
            return new KetQua.ThatBai<>("TRANG_THAI_NULL", "Trạng thái đích không được null");
        }
        boolean hopLe = switch (this) {
            case WAITING -> dich == PLAYING;
            case PLAYING -> dich == FINISHED;
            case FINISHED -> false;
        };
        if (!hopLe) {
            return new KetQua.ThatBai<>(
                "CHUYEN_TRANG_THAI_SAI",
                "Không thể chuyển trạng thái từ " + this + " sang " + dich
            );
        }
        return new KetQua.ThanhCong<>(dich, "Đã chuyển sang " + dich);
    }
}
