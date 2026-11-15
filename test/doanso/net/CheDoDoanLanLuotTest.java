package doanso.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import doanso.domain.CheDoDoan;
import doanso.domain.Game;
import doanso.domain.KetQua;
import doanso.domain.TrangThaiVan;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class CheDoDoanLanLuotTest {

    @Test
    void doanLanLuotTheoVongXoayDauHangDoiBangPollVaOfferVaChanDoanKhiChuaToiLuot() throws Exception {
        try (var server = new GameServer(0, 4)) {
            var swAn = new StringWriter();
            var swBinh = new StringWriter();
            var swCuong = new StringWriter();

            server.dangKyNguoiChoi("An", new PrintWriter(swAn, true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(swBinh, true));
            server.dangKyNguoiChoi("Cuong", new PrintWriter(swCuong, true));

            KetQua<Game> kqMo = server.moVanMoiTheoLuot(1, 100, 50, List.of("An", "Binh", "Cuong"));
            assertInstanceOf(KetQua.ThanhCong.class, kqMo);

            Game van = server.getVanHienTai().orElseThrow();
            assertEquals(CheDoDoan.LAN_LUOT, van.getCheDoDoan());
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());

            // 1. Đang là lượt của An: Bình hoặc Cường đoán sẽ bị chặn bởi CHUA_TOI_LUOT
            String phanHoiBinhSom = server.xuLyLenhGuess("Binh", "30");
            assertTrue(phanHoiBinhSom.startsWith("ERROR CHUA_TOI_LUOT"));
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());

            // 2. An đoán 20 -> hợp lệ, xoay vòng sang Bình
            assertEquals("HIGHER 20", server.xuLyLenhGuess("An", "20"));
            assertEquals(Optional.of("Binh"), van.getNguoiDangGiuLuot());
            assertTrue(swBinh.toString().contains("INFO LUOT_TIEP_THEO Binh"));

            // 3. Bình đoán 80 -> hợp lệ, xoay vòng sang Cường
            assertEquals("LOWER 80", server.xuLyLenhGuess("Binh", "80"));
            assertEquals(Optional.of("Cuong"), van.getNguoiDangGiuLuot());
            assertTrue(swCuong.toString().contains("INFO LUOT_TIEP_THEO Cuong"));

            // 4. Cường đoán 40 -> hợp lệ, xoay vòng quay lại An
            assertEquals("HIGHER 40", server.xuLyLenhGuess("Cuong", "40"));
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());
            assertEquals(3, van.getSoLuotDoan());
        }
    }

    @Test
    void nguoiDangGiuLuotRotMangHoacKhongTraLoiTuDongChuyenLuotTiepTheo() throws Exception {
        try (var server = new GameServer(0, 4)) {
            var swAn = new StringWriter();
            var swBinh = new StringWriter();
            var swCuong = new StringWriter();

            server.dangKyNguoiChoi("An", new PrintWriter(swAn, true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(swBinh, true));
            server.dangKyNguoiChoi("Cuong", new PrintWriter(swCuong, true));

            server.moVanMoiTheoLuot(1, 100, 50, List.of("An", "Binh", "Cuong"));
            Game van = server.getVanHienTai().orElseThrow();
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());

            // 1. An đang giữ lượt nhưng không trả lời -> máy chủ chuyển lượt sang Bình (An xuống cuối hàng đợi)
            Optional<String> luotSauBoQua = server.chuyenLuotTiepTheo();
            assertEquals(Optional.of("Binh"), luotSauBoQua);
            assertEquals(Optional.of("Binh"), van.getNguoiDangGiuLuot());
            assertTrue(swBinh.toString().contains("INFO LUOT_TIEP_THEO Binh"));

            // 2. Bình đang giữ lượt thì rớt mạng / rời phòng -> tự động gỡ Bình và chuyển lượt ngay cho Cường
            server.huyNguoiChoi("Binh");
            assertEquals(Optional.of("Cuong"), van.getNguoiDangGiuLuot());
            assertTrue(swCuong.toString().contains("INFO LUOT_TIEP_THEO Cuong"));

            // 3. Cường đoán xong thì vòng quay về An (vì Bình đã rời)
            assertEquals("HIGHER 10", server.xuLyLenhGuess("Cuong", "10"));
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());
        }
    }

    @Test
    void nguoiMoiVaoGiuaVongDuocXepVaoCuoiHangDoiArrayDeque() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(new StringWriter(), true));

            server.moVanMoiTheoLuot(1, 100, 50, List.of("An", "Binh"));
            Game van = server.getVanHienTai().orElseThrow();
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());

            // Dũng mới JOIN vào giữa vòng -> được offer vào cuối hàng đợi (An -> Binh -> Dung)
            var swDung = new StringWriter();
            server.dangKyNguoiChoi("Dung", new PrintWriter(swDung, true));
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());

            // An đoán -> tới Bình
            assertEquals("HIGHER 15", server.xuLyLenhGuess("An", "15"));
            assertEquals(Optional.of("Binh"), van.getNguoiDangGiuLuot());

            // Bình đoán -> tới người mới vào là Dũng
            assertEquals("LOWER 85", server.xuLyLenhGuess("Binh", "85"));
            assertEquals(Optional.of("Dung"), van.getNguoiDangGiuLuot());

            // Dũng đoán -> quay lại An
            assertEquals("HIGHER 25", server.xuLyLenhGuess("Dung", "25"));
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());
        }
    }

    @Test
    void doanSaiKhoangSoKhongLamMatLuotVaDoanTrungKetThucVanNgay() throws Exception {
        try (var server = new GameServer(0, 4)) {
            server.dangKyNguoiChoi("An", new PrintWriter(new StringWriter(), true));
            server.dangKyNguoiChoi("Binh", new PrintWriter(new StringWriter(), true));

            server.moVanMoiTheoLuot(1, 100, 65, List.of("An", "Binh"));
            Game van = server.getVanHienTai().orElseThrow();

            // An gửi số ngoài khoảng [1..100] -> báo lỗi NGOAI_KHOANG và An vẫn giữ lượt để đoán lại
            String loiNgoaiKhoang = server.xuLyLenhGuess("An", "999");
            assertTrue(loiNgoaiKhoang.startsWith("ERROR NGOAI_KHOANG"));
            assertEquals(Optional.of("An"), van.getNguoiDangGiuLuot());

            // An đoán trúng 65 -> thắng ván và kết thúc FINISHED
            assertEquals("WIN 65", server.xuLyLenhGuess("An", "65"));
            assertEquals(TrangThaiVan.FINISHED, van.getTrangThai());
            assertTrue(van.daCoNguoiThang());
        }
    }
}
