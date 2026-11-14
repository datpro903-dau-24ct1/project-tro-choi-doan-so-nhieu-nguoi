package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.KetQua;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class GameServerTest {

    @Test
    void nhanNhieuDongLenhLienTiepNhoAutoFlushVaDongKhiGapQuit() throws Exception {
        try (var server = new GameServer(0)) {
            int cong = server.getCong();
            assertTrue(cong > 0);

            ExecutorService beLuong = Executors.newSingleThreadExecutor();
            try {
                Future<Integer> ketQuaPhucVu = beLuong.submit(() -> server.phucVuMotKhach());

                try (var socket = new Socket("127.0.0.1", cong);
                     var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                    socket.setSoTimeout(2000);

                    out.println("JOIN Binh");
                    String phanHoiJoin = in.readLine();
                    assertEquals("OK JOIN Binh", phanHoiJoin);

                    out.println("   ");
                    String phanHoiRong = in.readLine();
                    assertEquals("ERROR LENH_RONG", phanHoiRong);

                    out.println("HELLO");
                    String phanHoiLa = in.readLine();
                    assertEquals("ERROR LENH_KHONG_HOP_LE", phanHoiLa);

                    out.println("QUIT");
                    String phanHoiQuit = in.readLine();
                    assertEquals("BYE", phanHoiQuit);
                }

                assertEquals(4, ketQuaPhucVu.get(2, TimeUnit.SECONDS));
            } finally {
                beLuong.shutdown();
            }
        }
    }

    @Test
    void clientDongDotNgotKhiReadLineTraNullKhongLamTreoServer() throws Exception {
        try (var server = new GameServer(0)) {
            int cong = server.getCong();

            ExecutorService beLuong = Executors.newSingleThreadExecutor();
            try {
                Callable<Integer> tacVuServer = () -> server.phucVuMotKhach();
                Future<Integer> futureSoDong = beLuong.submit(tacVuServer);

                try (var socket = new Socket("127.0.0.1", cong);
                     var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                    socket.setSoTimeout(2000);
                    out.println("JOIN An");
                    assertEquals("OK JOIN An", in.readLine());
                    // Đóng đột ngột: không gửi QUIT mà thoát khối try-with-resources để đóng socket ngay
                }

                int soDongDaXuLy = futureSoDong.get(2, TimeUnit.SECONDS);
                assertEquals(1, soDongDaXuLy);
            } finally {
                beLuong.shutdown();
            }
        }
    }

    @Test
    void chanMoServerKhiCongDaBiChiemHoacCongAm() throws Exception {
        try (var socketChiemCong = new ServerSocket(0)) {
            int congDangBan = socketChiemCong.getLocalPort();

            KetQua<GameServer> kqTrungCong = GameServer.thuMoCong(congDangBan);
            var thatBai = assertInstanceOf(KetQua.ThatBai.class, kqTrungCong);
            assertEquals("CONG_DA_BI_CHIEM", thatBai.maLoi());
        }

        KetQua<GameServer> kqCongAm = GameServer.thuMoCong(-1);
        var thatBaiCongAm = assertInstanceOf(KetQua.ThatBai.class, kqCongAm);
        assertEquals("CONG_KHONG_HOP_LE", thatBaiCongAm.maLoi());
    }

    @Test
    void dongServerGiaiPhongCongChoPhepMoLaiNgayTrenCungCong() throws Exception {
        int congDaDung;
        try (var serverLan1 = new GameServer(0)) {
            congDaDung = serverLan1.getCong();
            assertTrue(congDaDung > 0);
        }

        KetQua<GameServer> kqLan2 = GameServer.thuMoCong(congDaDung);
        assertInstanceOf(KetQua.ThanhCong.class, kqLan2);
        if (kqLan2 instanceof KetQua.ThanhCong<GameServer> thanhCong) {
            try (GameServer serverLan2 = thanhCong.duLieu()) {
                assertNotNull(serverLan2);
                assertEquals(congDaDung, serverLan2.getCong());
            }
        }
    }
}
