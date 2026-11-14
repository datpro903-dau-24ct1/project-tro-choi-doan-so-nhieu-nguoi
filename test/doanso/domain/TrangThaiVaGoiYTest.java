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

public class TrangThaiVaGoiYTest {

    @Test
    void chiChoPhepDoanKhiTrangThaiLaPlaying() {
        assertFalse(TrangThaiVan.WAITING.coTheDoan());
        assertTrue(TrangThaiVan.PLAYING.coTheDoan());
        assertFalse(TrangThaiVan.FINISHED.coTheDoan());
    }

    @Test
    void chuyenTrangThaiHopLeTuWaitingSangPlayingVaFinished() {
        KetQua<TrangThaiVan> kq1 = TrangThaiVan.WAITING.chuyenSang(TrangThaiVan.PLAYING);
        assertTrue(kq1.isThanhCong());
        KetQua.ThanhCong<TrangThaiVan> tc1 = assertInstanceOf(KetQua.ThanhCong.class, kq1);
        assertEquals(TrangThaiVan.PLAYING, tc1.duLieu());

        KetQua<TrangThaiVan> kq2 = TrangThaiVan.PLAYING.chuyenSang(TrangThaiVan.FINISHED);
        assertTrue(kq2.isThanhCong());
        KetQua.ThanhCong<TrangThaiVan> tc2 = assertInstanceOf(KetQua.ThanhCong.class, kq2);
        assertEquals(TrangThaiVan.FINISHED, tc2.duLieu());
    }

    @Test
    void chanChuyenTrangThaiNguocHoacNhayCoc() {
        // Bien: FINISHED -> PLAYING (chuyen nguoc)
        KetQua<TrangThaiVan> nguoc1 = TrangThaiVan.FINISHED.chuyenSang(TrangThaiVan.PLAYING);
        assertFalse(nguoc1.isThanhCong());
        KetQua.ThatBai<TrangThaiVan> tb1 = assertInstanceOf(KetQua.ThatBai.class, nguoc1);
        assertEquals("CHUYEN_TRANG_THAI_SAI", tb1.maLoi());

        // Bien: FINISHED -> WAITING
        assertFalse(TrangThaiVan.FINISHED.chuyenSang(TrangThaiVan.WAITING).isThanhCong());

        // Bien: PLAYING -> WAITING
        assertFalse(TrangThaiVan.PLAYING.chuyenSang(TrangThaiVan.WAITING).isThanhCong());

        // Bien: WAITING -> FINISHED (chua mo van da dong)
        assertFalse(TrangThaiVan.WAITING.chuyenSang(TrangThaiVan.FINISHED).isThanhCong());

        // Bien: chuyen sang null
        assertFalse(TrangThaiVan.WAITING.chuyenSang(null).isThanhCong());
    }

    @Test
    void docVaDoiTrangThaiDongThoiKhongBiLoiChuyenNguoc() throws Exception {
        // Gia lap hop giu trang thai dong bo bằng synchronized (Ch4)
        class HopTrangThai {
            private TrangThaiVan hienTai = TrangThaiVan.PLAYING;

            synchronized KetQua<TrangThaiVan> thuChuyen(TrangThaiVan dich) {
                KetQua<TrangThaiVan> kq = hienTai.chuyenSang(dich);
                if (kq instanceof KetQua.ThanhCong<TrangThaiVan> tc) {
                    hienTai = tc.duLieu();
                }
                return kq;
            }

            synchronized TrangThaiVan doc() {
                return hienTai;
            }
        }

        HopTrangThai hop = new HopTrangThai();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            // 20 luong co dong van (PLAYING -> FINISHED), 20 luong co mo lai (FINISHED -> PLAYING)
            // va 20 luong doc trang thai lien tuc
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> hop.thuChuyen(TrangThaiVan.FINISHED).isThanhCong());
                tasks.add(() -> {
                    KetQua<TrangThaiVan> kq = hop.thuChuyen(TrangThaiVan.PLAYING);
                    // Khong bao gio duoc phep chuyen PLAYING -> PLAYING hay FINISHED -> PLAYING
                    return !kq.isThanhCong();
                });
                tasks.add(() -> hop.doc() != null);
            }
            List<Future<Boolean>> futures = pool.invokeAll(tasks);
            int soLanDongThanhCong = 0;
            for (int i = 0; i < futures.size(); i += 3) {
                if (futures.get(i).get()) {
                    soLanDongThanhCong++;
                }
                assertTrue(futures.get(i + 1).get(), "Khong luong nao duoc phep chuyen nguoc sang PLAYING");
                assertTrue(futures.get(i + 2).get(), "Doc trang thai luon khac null");
            }
            // Chi duy nhat 1 luong dong van thanh cong, trang thai cuoi cung la FINISHED
            assertEquals(1, soLanDongThanhCong);
            assertEquals(TrangThaiVan.FINISHED, hop.doc());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void goiYSoSanhChinhXacVaMangMoTa() {
        assertEquals(GoiY.HIGHER, GoiY.soSanh(30, 42));
        assertEquals(GoiY.LOWER, GoiY.soSanh(50, 42));
        assertEquals(GoiY.CORRECT, GoiY.soSanh(42, 42));

        assertFalse(GoiY.HIGHER. getMoTa().isBlank());
        assertFalse(GoiY.LOWER.getMoTa().isBlank());
        assertFalse(GoiY.CORRECT.getMoTa().isBlank());
    }
}
