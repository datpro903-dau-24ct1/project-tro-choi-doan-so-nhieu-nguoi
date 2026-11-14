package doanso.net;

import doanso.domain.Game;
import doanso.domain.GoiY;
import doanso.domain.KetQua;
import doanso.domain.LuotDoan;
import doanso.domain.TrangThaiVan;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Phục vụ F06, F08, F09, F10, F11
public class GameServer implements AutoCloseable {

    public static final int CO_BE_LUONG_MAC_DINH = 20;

    private final ServerSocket serverSocket;
    private final int coBeLuong;
    private final ExecutorService beLuong;
    private final SoNguoiChoiOnline soOnline;
    private final Set<Socket> dsSocketDangKetNoi;
    private volatile boolean dangChay;

    private Game vanHienTai;
    private int demMaVan;
    private final Object khoaVan = new Object();

    public GameServer(int cong) throws IOException {
        this(cong, CO_BE_LUONG_MAC_DINH);
    }

    public GameServer(int cong, int coBeLuong) throws IOException {
        if (cong < 0 || cong > 65535) {
            throw new IllegalArgumentException("Cổng không hợp lệ: " + cong);
        }
        if (coBeLuong <= 0) {
            throw new IllegalArgumentException("Cỡ bể luồng phải lớn hơn 0: " + coBeLuong);
        }
        this.serverSocket = new ServerSocket(cong);
        this.coBeLuong = coBeLuong;
        this.beLuong = Executors.newFixedThreadPool(coBeLuong);
        this.soOnline = new SoNguoiChoiOnline();
        this.dsSocketDangKetNoi = ConcurrentHashMap.newKeySet();
        this.dangChay = true;
        this.vanHienTai = null;
        this.demMaVan = 0;
    }

    public static KetQua<GameServer> thuMoCong(int cong) {
        if (cong < 0 || cong > 65535) {
            return new KetQua.ThatBai<>("CONG_KHONG_HOP_LE", "Số hiệu cổng phải nằm trong [0..65535]");
        }
        try {
            var server = new GameServer(cong);
            return new KetQua.ThanhCong<>(server, "Đã mở cổng " + server.getCong());
        } catch (IOException e) {
            return new KetQua.ThatBai<>("CONG_DA_BI_CHIEM", "Không thể mở cổng " + cong + ": " + e.getMessage());
        }
    }

    public int getCong() {
        return serverSocket.getLocalPort();
    }

    public int getCoBeLuong() {
        return coBeLuong;
    }

    public boolean daDongBeLuong() {
        return beLuong.isShutdown();
    }

    public SoNguoiChoiOnline getSoOnline() {
        return soOnline;
    }

    public Optional<Game> getVanHienTai() {
        synchronized (khoaVan) {
            return Optional.ofNullable(vanHienTai);
        }
    }

    public void dangKySocketClient(Socket s) {
        if (s != null) {
            dsSocketDangKetNoi.add(s);
        }
    }

    public void huySocketClient(Socket s) {
        if (s != null) {
            dsSocketDangKetNoi.remove(s);
        }
    }

    public int broadcast(String thongDiep) {
        return soOnline.broadcast(thongDiep);
    }

    // F11: Mở ván chơi mới
    public KetQua<Game> moVanMoi(int min, int max) {
        if (min > max) {
            return new KetQua.ThatBai<>("KHOANG_SO_KHONG_HOP_LE", "min (" + min + ") không được lớn hơn max (" + max + ")");
        }
        if (soOnline.getSoLuongOnline() == 0) {
            return new KetQua.ThatBai<>("CHUA_CO_NGUOI_CHOI", "Không thể mở ván khi chưa có người chơi nào online");
        }
        synchronized (khoaVan) {
            if (vanHienTai != null && vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                return new KetQua.ThatBai<>("VAN_DANG_CHAY", "Ván #" + vanHienTai.getMaVan() + " đang diễn ra");
            }
            demMaVan++;
            Game gameMoi = new Game(demMaVan, min, max);
            soOnline.getDanhSachTenOnline().forEach(gameMoi::thamGiaVan);
            gameMoi.moVan();
            this.vanHienTai = gameMoi;

            soOnline.broadcast("INFO BAT_DAU_VAN " + gameMoi.getMaVan() + " " + min + " " + max);
            return new KetQua.ThanhCong<>(gameMoi, "Đã mở ván #" + gameMoi.getMaVan() + " [" + min + ".." + max + "]");
        }
    }

    // F11: Mở ván với số bí mật định trước (dùng cho kiểm thử)
    public KetQua<Game> moVanMoiVoiSoBiMat(int min, int max, int soBiMat) {
        if (min > max) {
            return new KetQua.ThatBai<>("KHOANG_SO_KHONG_HOP_LE", "min (" + min + ") không được lớn hơn max (" + max + ")");
        }
        if (soOnline.getSoLuongOnline() == 0) {
            return new KetQua.ThatBai<>("CHUA_CO_NGUOI_CHOI", "Không thể mở ván khi chưa có người chơi nào online");
        }
        synchronized (khoaVan) {
            if (vanHienTai != null && vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                return new KetQua.ThatBai<>("VAN_DANG_CHAY", "Ván #" + vanHienTai.getMaVan() + " đang diễn ra");
            }
            demMaVan++;
            Game gameMoi = new Game(demMaVan, min, max, soBiMat);
            soOnline.getDanhSachTenOnline().forEach(gameMoi::thamGiaVan);
            gameMoi.moVan();
            this.vanHienTai = gameMoi;

            soOnline.broadcast("INFO BAT_DAU_VAN " + gameMoi.getMaVan() + " " + min + " " + max);
            return new KetQua.ThanhCong<>(gameMoi, "Đã mở ván #" + gameMoi.getMaVan() + " [" + min + ".." + max + "]");
        }
    }

    // F09 & F11: Đăng ký người chơi và xử lý trường hợp người mới vào giữa ván
    public KetQua<Void> dangKyNguoiChoi(String ten, PrintWriter out) {
        KetQua<Void> kq = soOnline.dangKy(ten, out);
        if (kq instanceof KetQua.ThanhCong) {
            String tenChuan = ten.strip();
            synchronized (khoaVan) {
                if (vanHienTai != null && vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                    vanHienTai.thamGiaVan(tenChuan);
                    soOnline.guiTin(tenChuan, "INFO VAN_DANG_CHAY " + vanHienTai.getMaVan() + " " + vanHienTai.getMin() + " " + vanHienTai.getMax());
                    soOnline.broadcastNgoaiTru(tenChuan, "INFO NGUOI_MOI_VAO " + tenChuan);
                }
            }
        }
        return kq;
    }

    public void huyNguoiChoi(String ten) {
        if (ten != null) {
            String tenChuan = ten.strip();
            if (soOnline.huyDangKy(tenChuan)) {
                synchronized (khoaVan) {
                    if (vanHienTai != null) {
                        vanHienTai.roiVan(tenChuan);
                        if (vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                            soOnline.broadcast("INFO NGUOI_ROI_PHONG " + tenChuan);
                        }
                    }
                }
            }
        }
    }

    public int phucVuMotKhach() throws IOException {
        var clientSocket = serverSocket.accept();
        dangKySocketClient(clientSocket);
        var handler = new ClientHandler(clientSocket, this);
        return handler.phucVu();
    }

    public int vongLapChapNhanKetNoi() {
        int soKetNoi = 0;
        while (dangChay && !serverSocket.isClosed()) {
            try {
                var clientSocket = serverSocket.accept();
                dangKySocketClient(clientSocket);
                soKetNoi++;
                beLuong.submit(new ClientHandler(clientSocket, this));
            } catch (IOException e) {
                if (!dangChay || serverSocket.isClosed()) {
                    break;
                }
            }
        }
        return soKetNoi;
    }

    public String xuLyDongLenh(String dongTho) {
        if (dongTho == null || dongTho.isBlank()) {
            return "ERROR LENH_RONG";
        }
        var dong = dongTho.strip();
        var tach = dong.split("\\s+", 2);
        var tuKhoa = tach[0].toUpperCase();
        var thamSo = tach.length > 1 ? tach[1].strip() : "";

        return switch (tuKhoa) {
            case "JOIN" -> thamSo.isEmpty() ? "ERROR THIEU_TEN" : "OK JOIN " + thamSo;
            case "GUESS" -> thamSo.isEmpty() ? "ERROR THIEU_SO_DOAN" : "OK GUESS " + thamSo;
            case "SCORE" -> "OK SCORE";
            case "HISTORY" -> "OK HISTORY";
            case "STATS" -> "OK STATS";
            case "QUIT" -> "BYE";
            default -> "ERROR LENH_KHONG_HOP_LE";
        };
    }

    public String xuLyDongLenh(String dongTho, PrintWriter out, String tenNguoiChoiHienTai) {
        // Luôn gọi xuLyDongLenh(dongTho) để đảm bảo các lớp con override (ví dụ ném ngoại lệ kiểm thử) được thực thi
        String ketQuaCoBan = xuLyDongLenh(dongTho);

        if (dongTho == null || dongTho.isBlank()) {
            return ketQuaCoBan;
        }
        var dong = dongTho.strip();
        var tach = dong.split("\\s+", 2);
        var tuKhoa = tach[0].toUpperCase();
        var thamSo = tach.length > 1 ? tach[1].strip() : "";

        return switch (tuKhoa) {
            case "JOIN" -> {
                if (thamSo.isEmpty()) {
                    yield ketQuaCoBan;
                }
                if (out != null) {
                    KetQua<Void> kq = dangKyNguoiChoi(thamSo, out);
                    if (kq instanceof KetQua.ThatBai<Void> tb) {
                        yield "ERROR " + tb.maLoi() + " " + tb.lyDo();
                    }
                }
                yield "OK JOIN " + thamSo;
            }
            case "GUESS" -> {
                if (thamSo.isEmpty()) {
                    yield ketQuaCoBan;
                }
                int so;
                try {
                    so = Integer.parseInt(thamSo);
                } catch (NumberFormatException e) {
                    yield "ERROR SO_DOAN_KHONG_HOP_LE";
                }
                if (tenNguoiChoiHienTai == null || !soOnline.daOnline(tenNguoiChoiHienTai)) {
                    yield "ERROR CHUA_DANG_NHAP";
                }
                synchronized (khoaVan) {
                    if (vanHienTai == null || vanHienTai.getTrangThai() != TrangThaiVan.PLAYING) {
                        yield "ERROR VAN_CHUA_MO";
                    }
                    KetQua<LuotDoan> kq = vanHienTai.doanSo(tenNguoiChoiHienTai, so);
                    if (kq instanceof KetQua.ThatBai<LuotDoan> tb) {
                        yield "ERROR " + tb.maLoi() + " " + tb.lyDo();
                    }
                    LuotDoan luot = ((KetQua.ThanhCong<LuotDoan>) kq).duLieu();
                    GoiY gy = luot.ketQuaGoiY();
                    if (gy == GoiY.CORRECT) {
                        soOnline.broadcast("INFO NGUOI_THANG " + tenNguoiChoiHienTai + " " + so);
                        yield "WIN " + so;
                    } else if (gy == GoiY.HIGHER) {
                        soOnline.broadcast("GUESS " + tenNguoiChoiHienTai + " " + so + " HIGHER");
                        yield "HIGHER " + so;
                    } else {
                        soOnline.broadcast("GUESS " + tenNguoiChoiHienTai + " " + so + " LOWER");
                        yield "LOWER " + so;
                    }
                }
            }
            case "START" -> {
                int min = 1;
                int max = 100;
                if (!thamSo.isEmpty()) {
                    String[] cacKhoang = thamSo.split("\\s+");
                    if (cacKhoang.length >= 2) {
                        try {
                            min = Integer.parseInt(cacKhoang[0]);
                            max = Integer.parseInt(cacKhoang[1]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                KetQua<Game> kqMo = moVanMoi(min, max);
                if (kqMo instanceof KetQua.ThanhCong<Game> tc) {
                    yield "OK START " + tc.duLieu().getMaVan() + " " + min + " " + max;
                } else {
                    var tb = (KetQua.ThatBai<Game>) kqMo;
                    yield "ERROR " + tb.maLoi() + " " + tb.lyDo();
                }
            }
            default -> ketQuaCoBan;
        };
    }

    @Override
    public void close() throws IOException {
        dangChay = false;
        beLuong.shutdownNow();
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
        for (Socket s : dsSocketDangKetNoi) {
            try {
                if (!s.isClosed()) {
                    s.close();
                }
            } catch (IOException ignored) {
            }
        }
        dsSocketDangKetNoi.clear();
    }
}
