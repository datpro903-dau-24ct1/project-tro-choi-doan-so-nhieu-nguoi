package doanso.domain;

public enum GoiY {
    LOWER("Nhỏ hơn (Số bí mật nhỏ hơn số bạn đoán)"),
    HIGHER("Lớn hơn (Số bí mật lớn hơn số bạn đoán)"),
    CORRECT("Chính xác! Bạn đã đoán trúng số bí mật");

    private final String moTa;

    GoiY(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    public static GoiY soSanh(int soDoan, int soBiMat) {
        if (soDoan < soBiMat) {
            return HIGHER;
        }
        if (soDoan > soBiMat) {
            return LOWER;
        }
        return CORRECT;
    }
}
