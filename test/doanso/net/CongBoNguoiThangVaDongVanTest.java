package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.TrangThaiVan;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class CongBoNguoiThangVaDongVanTest {

    @Test
    void congBoNguoiThangQuaSocketDongVanVaTuChoiLuotDoanDenMuon() throws Exception {
        try (var server = new GameServer(0, 8)) {
            int cong = server.getCong();
            ExecutorService luongAccept = Executors.newSingleThreadExecutor();
            try {
                luongAccept.submit(() -> server.vongLapChapNhanKetNoi());

                try (var socketAn = new Socket("127.0.0.1", cong);
                     var inAn = new BufferedReader(new InputStreamReader(socketAn.getInputStream(), StandardCharsets.UTF_8));
                     var outAn = new PrintWriter(socketAn.getOutputStream(), true, StandardCharsets.UTF_8);
                     var socketBinh = new Socket("127.0.0.1", cong);
                     var inBinh = new BufferedReader(new InputStreamReader(socketBinh.getInputStream(), StandardCharsets.UTF_8));
                     var outBinh = new PrintWriter(socketBinh.getOutputStream(), true, StandardCharsets.UTF_8)) {

                    socketAn.setSoTimeout(2000);
                    socketBinh.setSoTimeout(2000);

                    outAn.println("JOIN An");
                    assertEquals("OK JOIN An", inAn.readLine());

                    outBinh.println("JOIN Binh");
                    assertEquals("OK JOIN Binh", inBinh.readLine());

                    server.moVanMoiVoiSoBiMat(1, 100, 50);
                    assertEquals("INFO BAT_DAU_VAN 1 1 100", inAn.readLine());
                    assertEquals("INFO BAT_DAU_VAN 1 1 100", inBinh.readLine());

                    // An đoán trúng số bí mật 50
                    outAn.println("GUESS 50");
                    List<String> tinCuaAn = List.of(inAn.readLine(), inAn.readLine());
                    assertTrue(tinCuaAn.contains("WIN 50"));
                    assertTrue(tinCuaAn.contains("INFO NGUOI_THANG An 50"));

                    // Bình nhận được tin công bố người thắng
                    assertEquals("INFO NGUOI_THANG An 50", inBinh.readLine());

                    // Trạng thái ván đã chốt FINISHED và người thắng là An
                    assertEquals(TrangThaiVan.FINISHED, server.getVanHienTai().orElseThrow().getTrangThai());
                    assertEquals(Optional.of("An"), server.getNguoiThangVanHienTai());

                    // Bình gửi lượt đoán đến muộn bị từ chối kèm thông báo ván đã có người thắng An
                    outBinh.println("GUESS 50");
                    String phanHoiMuon = inBinh.readLine();
                    assertTrue(phanHoiMuon.startsWith("ERROR VAN_DA_KET_THUC"));
                    assertTrue(phanHoiMuon.contains("An"));
                }
            } finally {
                server.close();
                luongAccept.shutdown();
            }
        }
    }

    @Test
    void haiMuoiNguoiCungDoanTrungDongThoiChiMotNguoiThangNguoiDenSauNhanThongBaoNguoiThang() throws Exception {
        try (var server = new GameServer(0, 20)) {
            int soNguoiChoi = 20;
            List<StringWriter> dsBoGhi = new ArrayList<>();
            for (int i = 0; i < soNguoiChoi; i++) {
                var sw = new StringWriter();
                dsBoGhi.add(sw);
                server.dangKyNguoiChoi("P" + i, new PrintWriter(sw, true));
            }

            server.moVanMoiVoiSoBiMat(1, 100, 77);

            ExecutorService beLuong = Executors.newFixedThreadPool(soNguoiChoi);
            try {
                List<Callable<String>> danhSachTacVu = new ArrayList<>();
                for (int i = 0; i < soNguoiChoi; i++) {
                    final String ten = "P" + i;
                    danhSachTacVu.add(() -> server.xuLyLenhGuess(ten, "77"));
                }

                List<Future<String>> ketQua = beLuong.invokeAll(danhSachTacVu);
                String nguoiThang = server.getNguoiThangVanHienTai().orElseThrow();

                int soLuongWin = 0;
                int soLuongBiTuChoi = 0;
                for (Future<String> future : ketQua) {
                    String phanHoi = future.get();
                    if ("WIN 77".equals(phanHoi)) {
                        soLuongWin++;
                    } else {
                        assertTrue(phanHoi.startsWith("ERROR VAN_DA_KET_THUC"));
                        assertTrue(phanHoi.contains(nguoiThang));
                        soLuongBiTuChoi++;
                    }
                }

                assertEquals(1, soLuongWin);
                assertEquals(soNguoiChoi - 1, soLuongBiTuChoi);

                // Mỗi người chơi chỉ nhận đúng 1 dòng broadcast INFO NGUOI_THANG
                for (StringWriter sw : dsBoGhi) {
                    String noiDung = sw.toString();
                    assertTrue(noiDung.contains("INFO NGUOI_THANG " + nguoiThang + " 77"));
                }
            } finally {
                beLuong.shutdown();
            }
        }
    }

    @Test
    void nguoiThangRotMangNgaySauKhiThangVanGiuNguyenKetQuaVaChanDoanMuon() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(new StringWriter(), true));

            server.moVanMoiVoiSoBiMat(1, 100, 42);
            assertEquals("WIN 42", server.xuLyLenhGuess("An", "42"));

            // Người thắng An rớt mạng ngay sau khi thắng
            server.huyNguoiChoi("An");

            // Ván vẫn ở trạng thái FINISHED và vẫn giữ nguyên người thắng An
            assertEquals(TrangThaiVan.FINISHED, server.getVanHienTai().orElseThrow().getTrangThai());
            assertEquals(Optional.of("An"), server.getNguoiThangVanHienTai());

            // Bình đoán muộn vẫn nhận thông báo ván đã có người thắng An
            String phanHoiBinh = server.xuLyLenhGuess("Binh", "42");
            assertTrue(phanHoiBinh.startsWith("ERROR VAN_DA_KET_THUC"));
            assertTrue(phanHoiBinh.contains("An"));
        }
    }

    @Test
    void tinCongBoNguoiThangDuocPhatSauKhiTrangThaiVanDaChotFinished() throws Exception {
        try (var server = new GameServer(0, 4)) {
            var trangThaiDaFinishedKhiBroadcast = new AtomicBoolean(false);
            var daCoNguoiThangKhiBroadcast = new AtomicBoolean(false);

            PrintWriter kenhKiemTra = new PrintWriter(new StringWriter(), true) {
                @Override
                public void println(String x) {
                    super.println(x);
                    if (x != null && x.startsWith("INFO NGUOI_THANG ")) {
                        var van = server.getVanHienTai().orElseThrow();
                        if (van.getTrangThai() == TrangThaiVan.FINISHED) {
                            trangThaiDaFinishedKhiBroadcast.set(true);
                        }
                        if (van.getNguoiThang().equals(Optional.of("An"))) {
                            daCoNguoiThangKhiBroadcast.set(true);
                        }
                    }
                }
            };

            server.dangKyNguoiChoi("An", kenhKiemTra);
            server.moVanMoiVoiSoBiMat(1, 100, 88);

            assertEquals("WIN 88", server.xuLyLenhGuess("An", "88"));
            assertTrue(trangThaiDaFinishedKhiBroadcast.get());
            assertTrue(daCoNguoiThangKhiBroadcast.get());
        }
    }
}
