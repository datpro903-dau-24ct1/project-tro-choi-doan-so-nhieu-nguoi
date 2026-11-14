package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class ClientHandlerVaBeLuongTest {

    @Test
    void nhieuMayKhachKetNoiVaGiaoTiepSongSongCungLuc() throws Exception {
        try (var server = new GameServer(0, 10)) {
            int cong = server.getCong();
            ExecutorService luongAccept = Executors.newSingleThreadExecutor();
            try {
                luongAccept.submit(() -> {
                    server.vongLapChapNhanKetNoi();
                    return null;
                });

                int soKhach = 5;
                List<Socket> dsSocket = new ArrayList<>();
                List<BufferedReader> dsIn = new ArrayList<>();
                List<PrintWriter> dsOut = new ArrayList<>();

                try {
                    for (int i = 0; i < soKhach; i++) {
                        var socket = new Socket("127.0.0.1", cong);
                        socket.setSoTimeout(2000);
                        dsSocket.add(socket);
                        dsIn.add(new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)));
                        dsOut.add(new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8));
                    }

                    // Cả 5 khách đều đang mở kết nối cùng lúc (chưa khách nào QUIT) và đều nhận phản hồi ngay
                    for (int i = 0; i < soKhach; i++) {
                        dsOut.get(i).println("JOIN Khach" + i);
                        assertEquals("OK JOIN Khach" + i, dsIn.get(i).readLine());
                    }

                    for (int i = 0; i < soKhach; i++) {
                        dsOut.get(i).println("QUIT");
                        assertEquals("BYE", dsIn.get(i).readLine());
                    }
                } finally {
                    for (var s : dsSocket) {
                        s.close();
                    }
                }
            } finally {
                server.close();
                luongAccept.shutdown();
            }
        }
    }

    @Test
    void vuotCoBeLuong20KhachThiKhachThu21ChoTrongHangDoiVaDuocPhucVuKhiCoLuongRanh() throws Exception {
        try (var server = new GameServer(0, 20)) {
            assertEquals(20, server.getCoBeLuong());
            int cong = server.getCong();

            ExecutorService luongAccept = Executors.newSingleThreadExecutor();
            try {
                luongAccept.submit(() -> {
                    server.vongLapChapNhanKetNoi();
                    return null;
                });

                List<Socket> dsSocket20 = new ArrayList<>();
                List<BufferedReader> dsIn20 = new ArrayList<>();
                List<PrintWriter> dsOut20 = new ArrayList<>();

                try {
                    // 20 khách đầu tiên chiếm trọn 20 luồng của bể luồng
                    for (int i = 0; i < 20; i++) {
                        var s = new Socket("127.0.0.1", cong);
                        s.setSoTimeout(2000);
                        dsSocket20.add(s);
                        dsIn20.add(new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)));
                        dsOut20.add(new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8));
                        dsOut20.get(i).println("JOIN K" + i);
                        assertEquals("OK JOIN K" + i, dsIn20.get(i).readLine());
                    }

                    // Khách thứ 21 kết nối khi bể luồng đã đầy (nằm chờ trong hàng đợi của FixedThreadPool)
                    try (var socket21 = new Socket("127.0.0.1", cong);
                         var in21 = new BufferedReader(new InputStreamReader(socket21.getInputStream(), StandardCharsets.UTF_8));
                         var out21 = new PrintWriter(socket21.getOutputStream(), true, StandardCharsets.UTF_8)) {

                        socket21.setSoTimeout(2000);
                        out21.println("JOIN KhachThu21");

                        // Giải phóng 1 luồng trong bể bằng cách cho khách 0 gửi QUIT
                        dsOut20.get(0).println("QUIT");
                        assertEquals("BYE", dsIn20.get(0).readLine());
                        dsSocket20.get(0).close();

                        // Ngay khi có luồng rảnh, khách thứ 21 trong hàng đợi được phục vụ
                        assertEquals("OK JOIN KhachThu21", in21.readLine());
                        out21.println("QUIT");
                        assertEquals("BYE", in21.readLine());
                    }
                } finally {
                    for (var s : dsSocket20) {
                        if (!s.isClosed()) {
                            s.close();
                        }
                    }
                }
            } finally {
                server.close();
                luongAccept.shutdown();
            }
        }
    }

    @Test
    void clientHandlerNemNgoaiLeKhongLamSapServerHayAnhHuongKhachKhac() throws Exception {
        GameServer serverTuyChinh = new GameServer(0, 4) {
            @Override
            public String xuLyDongLenh(String dongTho) {
                if ("BOOM".equals(dongTho != null ? dongTho.strip() : "")) {
                    throw new RuntimeException("Lỗi mô phỏng trong ClientHandler");
                }
                return super.xuLyDongLenh(dongTho);
            }
        };

        try (serverTuyChinh) {
            int cong = serverTuyChinh.getCong();
            ExecutorService luongAccept = Executors.newSingleThreadExecutor();
            try {
                luongAccept.submit(() -> {
                    serverTuyChinh.vongLapChapNhanKetNoi();
                    return null;
                });

                // Khách 1 gây ngoại lệ RuntimeException trong ClientHandler
                try (var socketLoi = new Socket("127.0.0.1", cong);
                     var inLoi = new BufferedReader(new InputStreamReader(socketLoi.getInputStream(), StandardCharsets.UTF_8));
                     var outLoi = new PrintWriter(socketLoi.getOutputStream(), true, StandardCharsets.UTF_8)) {
                    socketLoi.setSoTimeout(2000);
                    outLoi.println("BOOM");
                    assertEquals("ERROR LOI_NOI_BO", inLoi.readLine());
                }

                // Khách 2 kết nối sau đó vẫn hoạt động hoàn toàn bình thường
                try (var socketTot = new Socket("127.0.0.1", cong);
                     var inTot = new BufferedReader(new InputStreamReader(socketTot.getInputStream(), StandardCharsets.UTF_8));
                     var outTot = new PrintWriter(socketTot.getOutputStream(), true, StandardCharsets.UTF_8)) {
                    socketTot.setSoTimeout(2000);
                    outTot.println("JOIN Binh");
                    assertEquals("OK JOIN Binh", inTot.readLine());
                    outTot.println("QUIT");
                    assertEquals("BYE", inTot.readLine());
                }
            } finally {
                serverTuyChinh.close();
                luongAccept.shutdown();
            }
        }
    }

    @Test
    void tatServerKhiVongAcceptDangChayThuHoiBeLuongGiaiPhongCong() throws Exception {
        var server = new GameServer(0, 4);
        ExecutorService luongAccept = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> futureSoKhach = luongAccept.submit(() -> server.vongLapChapNhanKetNoi());

            try (var socket = new Socket("127.0.0.1", server.getCong());
                 var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                socket.setSoTimeout(2000);
                out.println("SCORE");
                assertEquals("OK SCORE", in.readLine());
            }

            server.close();
            int soKetNoiDaNhan = futureSoKhach.get(2, TimeUnit.SECONDS);
            assertEquals(1, soKetNoiDaNhan);
            assertTrue(server.daDongBeLuong());
        } finally {
            server.close();
            luongAccept.shutdown();
        }
    }
}
