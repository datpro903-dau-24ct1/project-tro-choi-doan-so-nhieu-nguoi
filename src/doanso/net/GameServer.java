package doanso.net;

import doanso.domain.CheDoDoan;
import doanso.domain.Game;
import doanso.domain.GoiY;
import doanso.domain.KetQua;
import doanso.domain.LuotDoan;
import doanso.domain.TrangThaiVan;
import doanso.repo.KhoBangDiem;
import doanso.repo.KhoLichSuDoan;
import doanso.service.ThongKeService;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

// Phục vụ F06, F08, F09, F10, F11
public class GameServer implements AutoCloseable {

    public static final int CO_BE_LUONG_MAC_DINH = 20;

    private final ServerSocket serverSocket;
    private final int coBeLuong;
    private final ExecutorService beLuong;
    private final SoNguoiChoiOnline soOnline;
    private final KhoBangDiem khoBangDiem;
    private final KhoLichSuDoan khoLichSuDoan;
    private final ThongKeService thongKeService;
    private final List<Game> danhSachCacVan;
    private final Set<Socket> dsSocketDangKetNoi;
    private volatile boolean dangChay;

    private Game vanHienTai;
    private int demMaVan;
    private final Object khoaVan = new Object();
    private final ExecutorService beHenGio;
    private Future<Boolean> tacVuHenGioHienTai;
    private volatile boolean huyHenGio;

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
        this.khoBangDiem = new KhoBangDiem();
        this.khoLichSuDoan = new KhoLichSuDoan();
        this.thongKeService = new ThongKeService();
        this.danhSachCacVan = new ArrayList<>();
        this.dsSocketDangKetNoi = ConcurrentHashMap.newKeySet();
        this.dangChay = true;
        this.vanHienTai = null;
        this.demMaVan = 0;
        this.beHenGio = Executors.newFixedThreadPool(2);
        this.tacVuHenGioHienTai = null;
        this.huyHenGio = false;
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

    public KhoBangDiem getKhoBangDiem() {
        return khoBangDiem;
    }

    public KhoLichSuDoan getKhoLichSuDoan() {
        return khoLichSuDoan;
    }

    public ThongKeService getThongKeService() {
        return thongKeService;
    }

    public List<Game> getDanhSachCacVan() {
        synchronized (khoaVan) {
            return new ArrayList<>(danhSachCacVan);
        }
    }

    public Optional<Game> getVanHienTai() {
        synchronized (khoaVan) {
            return Optional.ofNullable(vanHienTai);
        }
    }

    public Optional<String> getNguoiThangVanHienTai() {
        synchronized (khoaVan) {
            return Optional.ofNullable(vanHienTai).flatMap(Game::getNguoiThang);
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
            this.danhSachCacVan.add(gameMoi);

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
            this.danhSachCacVan.add(gameMoi);

            soOnline.broadcast("INFO BAT_DAU_VAN " + gameMoi.getMaVan() + " " + min + " " + max);
            return new KetQua.ThanhCong<>(gameMoi, "Đã mở ván #" + gameMoi.getMaVan() + " [" + min + ".." + max + "]");
        }
    }

    // F19: Mở ván theo chế độ đoán lần lượt theo vòng
    public KetQua<Game> moVanMoiTheoLuot(int min, int max) {
        return moVanMoiTheoLuot(min, max, (Integer) null, soOnline.getDanhSachTenOnline());
    }

    // F19: Mở ván theo chế độ đoán lần lượt với số bí mật và thứ tự hàng đợi định trước
    public KetQua<Game> moVanMoiTheoLuot(int min, int max, int soBiMat, List<String> thuTuNguoiChoi) {
        return moVanMoiTheoLuot(min, max, Integer.valueOf(soBiMat), thuTuNguoiChoi);
    }

    private KetQua<Game> moVanMoiTheoLuot(int min, int max, Integer soBiMat, Collection<String> thuTuNguoiChoi) {
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
            Game gameMoi = (soBiMat == null)
                ? new Game(demMaVan, min, max)
                : new Game(demMaVan, min, max, soBiMat);
            gameMoi.batCheDoLanLuot();
            if (thuTuNguoiChoi != null) {
                for (String ten : thuTuNguoiChoi) {
                    if (ten != null && soOnline.daOnline(ten.strip())) {
                        gameMoi.thamGiaVan(ten.strip());
                    }
                }
            }
            soOnline.getDanhSachTenOnline().forEach(gameMoi::thamGiaVan);
            gameMoi.moVan();
            this.vanHienTai = gameMoi;
            this.danhSachCacVan.add(gameMoi);

            soOnline.broadcast("INFO BAT_DAU_VAN " + gameMoi.getMaVan() + " " + min + " " + max);
            gameMoi.getNguoiDangGiuLuot().ifPresent(
                nguoiDau -> soOnline.broadcast("INFO LUOT_TIEP_THEO " + nguoiDau)
            );
            return new KetQua.ThanhCong<>(gameMoi, "Đã mở ván theo lượt #" + gameMoi.getMaVan() + " [" + min + ".." + max + "]");
        }
    }

    // F19: Bỏ qua lượt của người chơi hiện tại (khi không trả lời) và chuyển sang người tiếp theo
    public Optional<String> chuyenLuotTiepTheo() {
        synchronized (khoaVan) {
            if (vanHienTai == null || vanHienTai.getTrangThai() != TrangThaiVan.PLAYING) {
                return Optional.empty();
            }
            Optional<String> luotMoi = vanHienTai.boQuaLuotHienTai();
            luotMoi.ifPresent(nguoiTiep -> soOnline.broadcast("INFO LUOT_TIEP_THEO " + nguoiTiep));
            return luotMoi;
        }
    }

    // F20: Mở ván có giới hạn thời gian (mili-giây)
    public KetQua<Game> moVanMoiCoGioiHanThoiGian(int min, int max, long thoiGianGioiHanMs) {
        if (thoiGianGioiHanMs <= 0) {
            return new KetQua.ThatBai<>("THOI_GIAN_KHONG_HOP_LE", "Thời gian giới hạn mỗi ván phải lớn hơn 0");
        }
        KetQua<Game> kqMo = moVanMoi(min, max);
        if (kqMo instanceof KetQua.ThanhCong<Game> tc) {
            batDauHenGioChoVan(tc.duLieu(), thoiGianGioiHanMs);
        }
        return kqMo;
    }

    // F20: Mở ván với số bí mật định trước và giới hạn thời gian (dùng cho kiểm thử)
    public KetQua<Game> moVanMoiCoGioiHanThoiGian(int min, int max, int soBiMat, long thoiGianGioiHanMs) {
        if (thoiGianGioiHanMs <= 0) {
            return new KetQua.ThatBai<>("THOI_GIAN_KHONG_HOP_LE", "Thời gian giới hạn mỗi ván phải lớn hơn 0");
        }
        KetQua<Game> kqMo = moVanMoiVoiSoBiMat(min, max, soBiMat);
        if (kqMo instanceof KetQua.ThanhCong<Game> tc) {
            batDauHenGioChoVan(tc.duLieu(), thoiGianGioiHanMs);
        }
        return kqMo;
    }

    private void batDauHenGioChoVan(Game vanCanHenGio, long thoiGianGioiHanMs) {
        synchronized (khoaVan) {
            huyTacVuHenGioHienTai();
            this.huyHenGio = false;
            Callable<Boolean> tacVuDemGio = () -> {
                try {
                    Thread.sleep(thoiGianGioiHanMs);
                } catch (InterruptedException e) {
                    return false;
                }
                if (huyHenGio) {
                    return false;
                }
                synchronized (khoaVan) {
                    if (huyHenGio || vanHienTai != vanCanHenGio) {
                        return false;
                    }
                    KetQua<TrangThaiVan> kqDong = vanCanHenGio.dongVanDoHetGio();
                    if (kqDong instanceof KetQua.ThanhCong) {
                        soOnline.broadcast(
                            "INFO HET_GIO " + vanCanHenGio.getMaVan() + " " + vanCanHenGio.getSoBiMat()
                        );
                        return true;
                    }
                    return false;
                }
            };
            this.tacVuHenGioHienTai = beHenGio.submit(tacVuDemGio);
        }
    }

    public void huyTacVuHenGioHienTai() {
        synchronized (khoaVan) {
            this.huyHenGio = true;
            if (this.tacVuHenGioHienTai != null && !this.tacVuHenGioHienTai.isDone()) {
                this.tacVuHenGioHienTai.cancel(true);
            }
        }
    }

    public boolean dangCoTacVuHenGioChay() {
        synchronized (khoaVan) {
            return tacVuHenGioHienTai != null && !tacVuHenGioHienTai.isDone() && !tacVuHenGioHienTai.isCancelled();
        }
    }

    // F09 & F11: Đăng ký người chơi và xử lý trường hợp người mới vào giữa ván
    public KetQua<Void> dangKyNguoiChoi(String ten, PrintWriter out) {
        KetQua<Void> kq = soOnline.dangKy(ten, out);
        if (kq instanceof KetQua.ThanhCong) {
            String tenChuan = ten.strip();
            khoBangDiem.ghiNhanNguoiChoi(tenChuan);
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
                        Optional<String> luotTruocKhiRoi = vanHienTai.getNguoiDangGiuLuot();
                        vanHienTai.roiVan(tenChuan);
                        if (vanHienTai.getTrangThai() == TrangThaiVan.PLAYING) {
                            soOnline.broadcast("INFO NGUOI_ROI_PHONG " + tenChuan);
                            if (vanHienTai.getCheDoDoan() == CheDoDoan.LAN_LUOT
                                    && luotTruocKhiRoi.isPresent()
                                    && luotTruocKhiRoi.get().equals(tenChuan)) {
                                vanHienTai.getNguoiDangGiuLuot().ifPresent(
                                    nguoiTiep -> soOnline.broadcast("INFO LUOT_TIEP_THEO " + nguoiTiep)
                                );
                            }
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
            case "SCORE" -> dinhDangBangDiem();
            case "HISTORY" -> dinhDangLichSu(thamSo);
            case "STATS" -> dinhDangThongKe();
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
            case "GUESS" -> xuLyLenhGuess(tenNguoiChoiHienTai, thamSo);
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
            case "SCORE" -> dinhDangBangDiem();
            case "HISTORY" -> {
                String tenXem = thamSo.isEmpty() ? tenNguoiChoiHienTai : thamSo;
                if (tenXem == null || tenXem.isBlank()) {
                    yield "ERROR CHUA_DANG_NHAP";
                }
                yield dinhDangLichSu(tenXem);
            }
            case "STATS" -> dinhDangThongKe();
            default -> ketQuaCoBan;
        };
    }

    // F17: Định dạng dữ liệu SCORE (Bảng điểm nhiều ván)
    public String dinhDangBangDiem() {
        var danhSach = khoBangDiem.layBangXepHang();
        if (danhSach.isEmpty()) {
            return "OK SCORE";
        }
        String noiDung = danhSach.stream()
            .map(h -> h.tenNguoiChoi() + ": " + h.diem())
            .collect(Collectors.joining(", "));
        return "OK SCORE " + noiDung;
    }

    // F17: Định dạng dữ liệu HISTORY (Lịch sử đoán mỗi người)
    public String dinhDangLichSu(String tenCanXem) {
        if (tenCanXem == null || tenCanXem.isBlank()) {
            return "OK HISTORY RONG (Chưa có lượt đoán nào)";
        }
        String tenChuan = tenCanXem.strip();
        var dsLuot = khoLichSuDoan.layLichSuCua(tenChuan);
        if (dsLuot.isEmpty()) {
            return "OK HISTORY " + tenChuan + " RONG (Chưa có lượt đoán nào)";
        }
        String chiTiet = dsLuot.stream()
            .map(ld -> ld.soDoan() + "->" + ld.ketQuaGoiY().name())
            .collect(Collectors.joining(", "));
        return "OK HISTORY " + tenChuan + ": " + chiTiet;
    }

    // F17: Định dạng dữ liệu STATS (Thống kê hệ thống bằng Streams)
    public String dinhDangThongKe() {
        double avgVan;
        double avgNguoi;
        int tongLuot;
        int tongVan;
        synchronized (khoaVan) {
            avgVan = thongKeService.tinhTrungBinhSoLuotMoiVan(danhSachCacVan);
            avgNguoi = thongKeService.tinhTrungBinhTongLuotMoiNguoiCoDoan(khoLichSuDoan);
            tongLuot = khoLichSuDoan.layTatCaLuotDoan().size();
            tongVan = danhSachCacVan.size();
        }
        return String.format(
            java.util.Locale.US,
            "OK STATS trung_binh_van=%.2f trung_binh_nguoi=%.2f tong_luot=%d tong_van=%d",
            avgVan, avgNguoi, tongLuot, tongVan
        );
    }

    public static int phanTichSoDoan(String thamSo) throws LoiLenhDoanException {
        if (thamSo == null || thamSo.isBlank()) {
            throw new LoiLenhDoanException("THIEU_SO_DOAN", "Thiếu số đoán");
        }
        Function<String, Integer> boChuyenDoi = Integer::parseInt;
        try {
            return boChuyenDoi.apply(thamSo.strip());
        } catch (NumberFormatException e) {
            throw new LoiLenhDoanException("SO_DOAN_KHONG_HOP_LE", "Số đoán không phải số nguyên hợp lệ: " + thamSo, e);
        }
    }

    public String xuLyLenhGuess(String tenNguoiChoiHienTai, String thamSo) {
        final int so;
        try {
            so = phanTichSoDoan(thamSo);
        } catch (LoiLenhDoanException e) {
            return "ERROR " + e.getMaLoi();
        }

        if (tenNguoiChoiHienTai == null || !soOnline.daOnline(tenNguoiChoiHienTai)) {
            return "ERROR CHUA_DANG_NHAP";
        }

        synchronized (khoaVan) {
            if (vanHienTai == null) {
                return "ERROR VAN_CHUA_MO";
            }
            KetQua<LuotDoan> kq = vanHienTai.doanSo(tenNguoiChoiHienTai, so);
            if (kq instanceof KetQua.ThatBai<LuotDoan> tb) {
                if ("VAN_DA_KET_THUC".equals(tb.maLoi())) {
                    if (vanHienTai.isBiDongDoHetGio()) {
                        return "ERROR VAN_DA_KET_THUC Ván đã kết thúc do hết giờ";
                    }
                    String thongTinThang = vanHienTai.getNguoiThang()
                        .map(ten -> "Ván đã có người thắng: " + ten)
                        .orElse(tb.lyDo());
                    return "ERROR VAN_DA_KET_THUC " + thongTinThang;
                }
                return "ERROR " + tb.maLoi() + " " + tb.lyDo();
            }
            LuotDoan luot = ((KetQua.ThanhCong<LuotDoan>) kq).duLieu();
            khoLichSuDoan.ghiNhanLuotDoan(luot);
            GoiY gy = luot.ketQuaGoiY();
            return switch (gy) {
                case CORRECT -> {
                    huyTacVuHenGioHienTai();
                    khoBangDiem.congDiemThang(luot.nguoiChoi());
                    soOnline.broadcast("INFO NGUOI_THANG " + luot.nguoiChoi() + " " + luot.soDoan());
                    yield "WIN " + luot.soDoan();
                }
                case HIGHER -> {
                    soOnline.broadcast("GUESS " + luot.nguoiChoi() + " " + luot.soDoan() + " HIGHER");
                    if (vanHienTai.getCheDoDoan() == CheDoDoan.LAN_LUOT) {
                        vanHienTai.getNguoiDangGiuLuot().ifPresent(
                            nguoiTiep -> soOnline.broadcast("INFO LUOT_TIEP_THEO " + nguoiTiep)
                        );
                    }
                    yield "HIGHER " + luot.soDoan();
                }
                case LOWER -> {
                    soOnline.broadcast("GUESS " + luot.nguoiChoi() + " " + luot.soDoan() + " LOWER");
                    if (vanHienTai.getCheDoDoan() == CheDoDoan.LAN_LUOT) {
                        vanHienTai.getNguoiDangGiuLuot().ifPresent(
                            nguoiTiep -> soOnline.broadcast("INFO LUOT_TIEP_THEO " + nguoiTiep)
                        );
                    }
                    yield "LOWER " + luot.soDoan();
                }
            };
        }
    }

    @Override
    public void close() throws IOException {
        dangChay = false;
        huyTacVuHenGioHienTai();
        beHenGio.shutdownNow();
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
