package doanso.domain;

public sealed interface KetQua<T> permits KetQua.ThanhCong, KetQua.ThatBai {

    boolean isThanhCong();

    record ThanhCong<T>(T duLieu, String thongDiep) implements KetQua<T> {
        @Override
        public boolean isThanhCong() {
            return true;
        }
    }

    record ThatBai<T>(String maLoi, String lyDo) implements KetQua<T> {
        @Override
        public boolean isThanhCong() {
            return false;
        }
    }
}
