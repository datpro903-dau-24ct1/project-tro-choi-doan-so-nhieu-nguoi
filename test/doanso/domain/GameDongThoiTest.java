package doanso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class GameDongThoiTest {

    @Test
    void motLuongDoanSaiVaDoanTrungGhiNhanDungKetQua() throws InterruptedException {
        Game vanSai = new Game(1, 1, 100, 50);
        vanSai.moVan();

        Thread luongSai = new Thread(() -> vanSai.doanSo("An", 25));
        luongSai.start();
        luongSai.join();

        assertEquals(1, vanSai.getSoLuotDoan());
        assertFalse(vanSai.daCoNguoiThang());
        assertEquals(TrangThaiVan.PLAYING, vanSai.getTrangThai());

        Game vanTrung = new Game(2, 1, 100, 50);
        vanTrung.moVan();

        Thread luongTrung = new Thread(() -> vanTrung.doanSo("Binh", 50));
        luongTrung.start();
        luongTrung.join();

        assertEquals(1, vanTrung.getSoLuotDoan());
        assertTrue(vanTrung.daCoNguoiThang());
        assertEquals("Binh", vanTrung.getNguoiThang().orElseThrow());
        assertEquals(TrangThaiVan.FINISHED, vanTrung.getTrangThai());
    }

    @Test
    void nLuongCungDoanSaiGhiNhanDuNLuotKhongMatDuLieu() throws InterruptedException {
        int soLuong = 100;
        for (int vong = 0; vong < 20; vong++) {
            Game game = new Game(vong + 1, 1, 1000, 999);
            game.moVan();

            List<Thread> danhSachLuong = new ArrayList<>(soLuong);
            for (int i = 0; i < soLuong; i++) {
                final int chiSo = i;
                Runnable tacVu = () -> game.doanSo("NguoiChoi-" + chiSo, (chiSo % 500) + 1);
                Thread luong = new Thread(tacVu);
                danhSachLuong.add(luong);
            }

            for (Thread luong : danhSachLuong) {
                luong.start();
            }
            for (Thread luong : danhSachLuong) {
                luong.join();
            }

            assertEquals(soLuong, game.getSoLuotDoan());
            assertEquals(soLuong, game.getDanhSachLuotDoan().size());
            assertFalse(game.daCoNguoiThang());
            assertEquals(TrangThaiVan.PLAYING, game.getTrangThai());
        }
    }

    @Test
    void moiLuongCungDoanTrungMotSoChiCoDungMotNguoiThangLapNhieuLan() throws Exception {
        int soVongLap = 50;
        int soLuong = 50;

        for (int vong = 0; vong < soVongLap; vong++) {
            int soBiMat = 77;
            Game game = new Game(vong + 1, 1, 100, soBiMat);
            game.moVan();

            ExecutorService beLuong = Executors.newFixedThreadPool(16);
            try {
                List<Callable<KetQua<LuotDoan>>> danhSachTacVu = new ArrayList<>(soLuong);
                for (int i = 0; i < soLuong; i++) {
                    final String tenNguoiChoi = "P-" + i;
                    danhSachTacVu.add(() -> game.doanSo(tenNguoiChoi, soBiMat));
                }

                List<Future<KetQua<LuotDoan>>> danhSachKetQua = beLuong.invokeAll(danhSachTacVu);

                int soLuongThang = 0;
                int soLuongBiTuChoiViVanDong = 0;
                String nguoiThangTuFuture = null;

                for (Future<KetQua<LuotDoan>> future : danhSachKetQua) {
                    KetQua<LuotDoan> kq = future.get();
                    if (kq instanceof KetQua.ThanhCong<LuotDoan> tc) {
                        assertEquals(GoiY.CORRECT, tc.duLieu().ketQuaGoiY());
                        soLuongThang++;
                        nguoiThangTuFuture = tc.duLieu().nguoiChoi();
                    } else if (kq instanceof KetQua.ThatBai<LuotDoan> tb) {
                        assertEquals("VAN_DA_KET_THUC", tb.maLoi());
                        soLuongBiTuChoiViVanDong++;
                    }
                }

                assertEquals(1, soLuongThang, "Mỗi ván chỉ được có đúng 1 luồng thắng ở vòng " + vong);
                assertEquals(soLuong - 1, soLuongBiTuChoiViVanDong);
                assertTrue(game.daCoNguoiThang());
                assertEquals(nguoiThangTuFuture, game.getNguoiThang().orElseThrow());
                assertEquals(1, game.getSoLuotDoan());
                assertEquals(TrangThaiVan.FINISHED, game.getTrangThai());
            } finally {
                beLuong.shutdown();
            }
        }
    }

    @Test
    void nhieuLuongVuaDoanSaiVuaDoanTrungVaDocLichSuDongThoiLapNhieuLan() throws Exception {
        int soVongLap = 30;
        int soLuongDoan = 60;

        for (int vong = 0; vong < soVongLap; vong++) {
            int soBiMat = 50;
            Game game = new Game(vong + 1, 1, 100, soBiMat);
            game.moVan();

            ExecutorService beLuong = Executors.newFixedThreadPool(16);
            try {
                List<Callable<KetQua<LuotDoan>>> danhSachTacVu = new ArrayList<>(soLuongDoan);
                for (int i = 0; i < soLuongDoan; i++) {
                    final int chiSo = i;
                    final int soDoan = (chiSo % 10 == 0) ? soBiMat : (chiSo % 40) + 1;
                    danhSachTacVu.add(() -> {
                        KetQua<LuotDoan> kq = game.doanSo("Nguoi-" + chiSo, soDoan);
                        List<LuotDoan> banSao = game.getDanhSachLuotDoan();
                        assertTrue(banSao.size() >= 0);
                        return kq;
                    });
                }

                List<Future<KetQua<LuotDoan>>> ketQuaFutures = beLuong.invokeAll(danhSachTacVu);

                int soLuotThanhCong = 0;
                int soLuotDoanTrung = 0;
                for (Future<KetQua<LuotDoan>> future : ketQuaFutures) {
                    KetQua<LuotDoan> kq = future.get();
                    if (kq instanceof KetQua.ThanhCong<LuotDoan> tc) {
                        soLuotThanhCong++;
                        if (tc.duLieu().ketQuaGoiY() == GoiY.CORRECT) {
                            soLuotDoanTrung++;
                        }
                    } else {
                        KetQua.ThatBai<LuotDoan> tb = assertInstanceOf(KetQua.ThatBai.class, kq);
                        assertEquals("VAN_DA_KET_THUC", tb.maLoi());
                    }
                }

                assertEquals(1, soLuotDoanTrung);
                assertTrue(game.daCoNguoiThang());
                assertEquals(soLuotThanhCong, game.getSoLuotDoan());
                List<LuotDoan> lichSuCuoi = game.getDanhSachLuotDoan();
                assertEquals(soLuotThanhCong, lichSuCuoi.size());
                assertEquals(GoiY.CORRECT, lichSuCuoi.get(lichSuCuoi.size() - 1).ketQuaGoiY());
            } finally {
                beLuong.shutdown();
            }
        }
    }
}
