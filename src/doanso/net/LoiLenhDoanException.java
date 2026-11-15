package doanso.net;

public class LoiLenhDoanException extends Exception {

    private final String maLoi;

    public LoiLenhDoanException(String maLoi, String thongDiep) {
        super(thongDiep);
        this.maLoi = maLoi;
    }

    public LoiLenhDoanException(String maLoi, String thongDiep, Throwable nguyenNhan) {
        super(thongDiep, nguyenNhan);
        this.maLoi = maLoi;
    }

    public String getMaLoi() {
        return maLoi;
    }
}
