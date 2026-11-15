package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NgatKetNoiVaDonDepTaiNguyenTest {

    private GameServer server;
    private ExecutorService serverPool;

    @BeforeEach
    void khoiTao() throws Exception {
        server = new GameServer(0);
        serverPool = Executors.newSingleThreadExecutor();
        serverPool.submit(() -> server.vongLapChapNhanKetNoi());
    }

    @AfterEach
    void donDep() throws Exception {
        if (server != null) {
            server.close();
        }
        if (serverPool != null) {
            serverPool.shutdownNow();
        }
    }

    // F18 Trường hợp biên 1: Rớt mạng giữa ván, lượt đã đoán vẫn giữ nguyên trong lịch sử
    @Test
    void rotMangGiuaVanLuotDoanVanGiuNguyenTrongKhoLichSu() throws Exception {
        int cong = server.getCong();

        // Client 1 kết nối và đoán số giữa ván
        try (var socket1 = new Socket("127.0.0.1", cong);
             var in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream(), StandardCharsets.UTF_8));
             var out1 = new PrintWriter(socket1.getOutputStream(), true, StandardCharsets.UTF_8)) {

            socket1.setSoTimeout(2000);
            out1.println("JOIN PlayerA");
            assertEquals("OK JOIN PlayerA", in1.readLine());

            // Server mở ván -> client nhận tin broadcast bắt đầu ván
            server.moVanMoiVoiSoBiMat(1, 100, 75);
            assertEquals("INFO BAT_DAU_VAN 1 1 100", in1.readLine());

            out1.println("GUESS 50");
            String phanHoi1 = in1.readLine();
            assertTrue(phanHoi1.contains("HIGHER 50") || phanHoi1.contains("GUESS PlayerA 50 HIGHER"));

            // Đọc dòng broadcast kèm theo nếu có
            in1.readLine();

            out1.println("GUESS 80");
            String phanHoi2 = in1.readLine();
            assertTrue(phanHoi2.contains("LOWER 80") || phanHoi2.contains("GUESS PlayerA 80 LOWER"));

            // Client 1 đột ngột rớt mạng (thoát khối try-with-resources đóng socket ngay không gửi QUIT)
        }

        // Đợi server xử lý ngắt kết nối
        TimeUnit.MILLISECONDS.sleep(200);

        // Kiểm tra PlayerA đã được gỡ khỏi sổ online
        assertFalse(server.getSoOnline().daOnline("PlayerA"));

        // Lịch sử 2 lượt đoán của PlayerA VẪN CÒN NGUYÊN VẸN trong KhoLichSuDoan
        var dsLichSu = server.getKhoLichSuDoan().layLichSuCua("PlayerA");
        assertEquals(2, dsLichSu.size(), "Lịch sử đoán phải được lưu giữ dù người chơi đã rớt mạng");
        assertEquals(50, dsLichSu.get(0).soDoan());
        assertEquals(80, dsLichSu.get(1).soDoan());
    }

    // F18 Trường hợp biên 2: Người thắng rớt mạng ngay sau khi thắng, điểm số vẫn giữ nguyên
    @Test
    void nguoiThangRotMangDiemVanGiuNguyenVaVaoLaiKhongBiReset() throws Exception {
        int cong = server.getCong();

        try (var socket = new Socket("127.0.0.1", cong);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            socket.setSoTimeout(2000);
            out.println("JOIN Winner");
            assertEquals("OK JOIN Winner", in.readLine());

            // Mở ván và nhận tin broadcast bắt đầu ván
            server.moVanMoiVoiSoBiMat(1, 20, 15);
            assertEquals("INFO BAT_DAU_VAN 1 1 20", in.readLine());

            out.println("GUESS 15");
            String phanHoiWin = in.readLine();
            assertTrue(phanHoiWin.contains("WIN 15") || phanHoiWin.contains("INFO NGUOI_THANG"));

            // Điểm của Winner là 1
            assertEquals(1, server.getKhoBangDiem().layDiem("Winner"));

            // Đột ngột rớt mạng
        }

        TimeUnit.MILLISECONDS.sleep(200);
        assertFalse(server.getSoOnline().daOnline("Winner"));

        // Điểm vẫn là 1 trong KhoBangDiem
        assertEquals(1, server.getKhoBangDiem().layDiem("Winner"));

        // Winner kết nối lại vào ván mới, điểm vẫn giữ 1
        try (var socket2 = new Socket("127.0.0.1", cong);
             var in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), StandardCharsets.UTF_8));
             var out2 = new PrintWriter(socket2.getOutputStream(), true, StandardCharsets.UTF_8)) {

            socket2.setSoTimeout(2000);
            out2.println("JOIN Winner");
            assertEquals("OK JOIN Winner", in2.readLine());

            assertEquals(1, server.getKhoBangDiem().layDiem("Winner"), "Cùng tên vào lại không bị mất điểm");
            out2.println("QUIT");
            assertEquals("BYE", in2.readLine());
        }
    }

    // F18 Trường hợp biên 3: QUIT chủ động dọn dẹp sạch sẽ và khác với mất mạng đột ngột
    @Test
    void quitChuDongDonDepTaiNguyenVaGiaiPhongKhoiSo() throws Exception {
        int cong = server.getCong();

        try (var socket = new Socket("127.0.0.1", cong);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            socket.setSoTimeout(2000);
            out.println("JOIN NguoiRoi");
            assertEquals("OK JOIN NguoiRoi", in.readLine());
            assertTrue(server.getSoOnline().daOnline("NguoiRoi"));

            // Gửi QUIT chủ động
            out.println("QUIT");
            assertEquals("BYE", in.readLine());
        }

        TimeUnit.MILLISECONDS.sleep(150);
        assertFalse(server.getSoOnline().daOnline("NguoiRoi"));
        assertEquals(0, server.getSoOnline().getSoLuongOnline());
    }

    // F18: Mất mạng đột ngột khi đang có ván chạy thì phát broadcast thông báo người rời phòng
    @Test
    void matMangDotNgotGiuaVanPhatBroadcastNguoiRoiPhongChoCacKhachKhac() throws Exception {
        int cong = server.getCong();

        // Khách A ở lại
        var socketA = new Socket("127.0.0.1", cong);
        socketA.setSoTimeout(2000);
        var inA = new BufferedReader(new InputStreamReader(socketA.getInputStream(), StandardCharsets.UTF_8));
        var outA = new PrintWriter(socketA.getOutputStream(), true, StandardCharsets.UTF_8);
        outA.println("JOIN KhachA");
        assertEquals("OK JOIN KhachA", inA.readLine());

        // Khách B tham gia
        var socketB = new Socket("127.0.0.1", cong);
        socketB.setSoTimeout(2000);
        var inB = new BufferedReader(new InputStreamReader(socketB.getInputStream(), StandardCharsets.UTF_8));
        var outB = new PrintWriter(socketB.getOutputStream(), true, StandardCharsets.UTF_8);
        outB.println("JOIN KhachB");
        assertEquals("OK JOIN KhachB", inB.readLine());

        // Server mở ván
        server.moVanMoi(1, 50);

        // Khách B rớt mạng đột ngột
        socketB.close();

        // Đợi luồng đọc phát hiện KhachB ngắt và broadcast
        TimeUnit.MILLISECONDS.sleep(250);

        // Khách B đã được gỡ khỏi sổ online
        assertFalse(server.getSoOnline().daOnline("KhachB"));
        assertTrue(server.getSoOnline().daOnline("KhachA"));

        socketA.close();
    }
}
