package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class NhanLuotDoanQuaMangTest {

    @Test
    void phanTichSoDoanHopLeVaNemLoiLenhDoanExceptionKhiKhongPhaiSoNguyen() throws Exception {
        assertEquals(42, GameServer.phanTichSoDoan("42"));
        assertEquals(-7, GameServer.phanTichSoDoan("  -7  "));

        LoiLenhDoanException loiRong = assertThrows(
            LoiLenhDoanException.class,
            () -> GameServer.phanTichSoDoan("   ")
        );
        assertEquals("THIEU_SO_DOAN", loiRong.getMaLoi());

        LoiLenhDoanException loiChu = assertThrows(
            LoiLenhDoanException.class,
            () -> GameServer.phanTichSoDoan("abc")
        );
        assertEquals("SO_DOAN_KHONG_HOP_LE", loiChu.getMaLoi());

        LoiLenhDoanException loiQuaDai = assertThrows(
            LoiLenhDoanException.class,
            () -> GameServer.phanTichSoDoan("999999999999999999")
        );
        assertEquals("SO_DOAN_KHONG_HOP_LE", loiQuaDai.getMaLoi());
    }

    @Test
    void doanQuaSocketTraGoiYChoNguoiDoanVaBroadcastChoCaPhong() throws Exception {
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

                    // Mở ván [1..100] với số bí mật = 50
                    server.moVanMoiVoiSoBiMat(1, 100, 50);
                    assertEquals("INFO BAT_DAU_VAN 1 1 100", inAn.readLine());
                    assertEquals("INFO BAT_DAU_VAN 1 1 100", inBinh.readLine());

                    // An đoán 30 (< 50 -> cần đoán số lớn hơn -> HIGHER)
                    outAn.println("GUESS 30");
                    List<String> tinCuaAnLan1 = List.of(inAn.readLine(), inAn.readLine());
                    assertTrue(tinCuaAnLan1.contains("HIGHER 30"));
                    assertTrue(tinCuaAnLan1.contains("GUESS An 30 HIGHER"));
                    assertEquals("GUESS An 30 HIGHER", inBinh.readLine());

                    // Bình đoán 80 (> 50 -> cần đoán số nhỏ hơn -> LOWER)
                    outBinh.println("GUESS 80");
                    List<String> tinCuaBinhLan2 = List.of(inBinh.readLine(), inBinh.readLine());
                    assertTrue(tinCuaBinhLan2.contains("LOWER 80"));
                    assertTrue(tinCuaBinhLan2.contains("GUESS Binh 80 LOWER"));
                    assertEquals("GUESS Binh 80 LOWER", inAn.readLine());
                }
            } finally {
                server.close();
                luongAccept.shutdown();
            }
        }
    }

    @Test
    void chanGuessKhiChuaJoinChuaMoVanKhongPhaiSoNguyenHoacSoAmNgoaiKhoang() throws Exception {
        try (var server = new GameServer(0, 4)) {
            var swAn = new StringWriter();
            var pwAn = new PrintWriter(swAn, true);

            // 1. Chưa JOIN mà đã GUESS
            assertEquals("ERROR CHUA_DANG_NHAP", server.xuLyDongLenh("GUESS 50", pwAn, null));

            // 2. Đã JOIN nhưng chưa có ván nào mở
            assertEquals("OK JOIN An", server.xuLyDongLenh("JOIN An", pwAn, null));
            assertEquals("ERROR VAN_CHUA_MO", server.xuLyDongLenh("GUESS 50", pwAn, "An"));

            // 3. Mở ván [1..100], số bí mật = 50
            server.moVanMoiVoiSoBiMat(1, 100, 50);

            // 4. Đầu vào không phải số nguyên
            assertEquals("ERROR SO_DOAN_KHONG_HOP_LE", server.xuLyDongLenh("GUESS khongPhaiSo", pwAn, "An"));

            // 5. Số âm hoặc ngoài khoảng [1..100]
            String phanHoiSoAm = server.xuLyDongLenh("GUESS -10", pwAn, "An");
            assertTrue(phanHoiSoAm.startsWith("ERROR NGOAI_KHOANG"));

            String phanHoiVuotMax = server.xuLyDongLenh("GUESS 101", pwAn, "An");
            assertTrue(phanHoiVuotMax.startsWith("ERROR NGOAI_KHOANG"));
        }
    }

    @Test
    void haiLenhDoanDenCungLucQuaMangDongBoChinhXacVaGhiDuLuot() throws Exception {
        try (var server = new GameServer(0, 16)) {
            int soNguoiChoi = 10;
            for (int i = 0; i < soNguoiChoi; i++) {
                server.dangKyNguoiChoi("P" + i, new PrintWriter(new StringWriter(), true));
            }
            server.moVanMoiVoiSoBiMat(1, 100, 50);

            ExecutorService beLuong = Executors.newFixedThreadPool(soNguoiChoi);
            try {
                List<Callable<String>> danhSachTacVu = new ArrayList<>();
                for (int i = 0; i < soNguoiChoi; i++) {
                    final int chiSo = i;
                    final int soDoan = (chiSo < 5) ? (10 + chiSo) : (60 + chiSo);
                    danhSachTacVu.add(() -> server.xuLyLenhGuess("P" + chiSo, String.valueOf(soDoan)));
                }

                List<Future<String>> ketQua = beLuong.invokeAll(danhSachTacVu);
                for (int i = 0; i < soNguoiChoi; i++) {
                    String phanHoi = ketQua.get(i).get();
                    if (i < 5) {
                        assertEquals("HIGHER " + (10 + i), phanHoi);
                    } else {
                        assertEquals("LOWER " + (60 + i), phanHoi);
                    }
                }

                var van = server.getVanHienTai().orElseThrow();
                assertEquals(soNguoiChoi, van.getSoLuotDoan());
            } finally {
                beLuong.shutdown();
            }
        }
    }
}
