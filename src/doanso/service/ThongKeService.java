package doanso.service;

import doanso.domain.Game;
import doanso.domain.LuotDoan;
import doanso.repo.KhoLichSuDoan;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ThongKeService {

    public Map<String, Long> demTongLuotTheoNguoi(List<LuotDoan> danhSachLuotDoan) {
        if (danhSachLuotDoan == null || danhSachLuotDoan.isEmpty()) {
            return Map.of();
        }
        return danhSachLuotDoan.stream()
            .collect(Collectors.groupingBy(LuotDoan::nguoiChoi, Collectors.counting()));
    }

    public double tinhTrungBinhTongLuotMoiNguoiCoDoan(KhoLichSuDoan khoLichSu) {
        if (khoLichSu == null) {
            return 0.0;
        }
        Map<String, List<LuotDoan>> toanBo = khoLichSu.layToanBoLichSu();
        List<List<LuotDoan>> dsCoDoan = toanBo.values().stream()
            .filter(ds -> !ds.isEmpty())
            .collect(Collectors.toList());
        if (dsCoDoan.isEmpty()) {
            return 0.0;
        }
        Double ketQua = dsCoDoan.stream()
            .collect(Collectors.averagingDouble(List::size));
        return Optional.ofNullable(ketQua).orElse(0.0);
    }

    public double tinhTrungBinhSoLuotMoiVan(List<Game> danhSachVan) {
        if (danhSachVan == null || danhSachVan.isEmpty()) {
            return 0.0;
        }
        return danhSachVan.stream()
            .filter(Game::daCoNguoiThang)
            .mapToInt(Game::getSoLuotDoan)
            .average()
            .orElse(0.0);
    }

    public double tinhTrungBinhSoLuotCuaNguoiMoiVan(String tenNguoiChoi, List<Game> danhSachVan) {
        if (tenNguoiChoi == null || tenNguoiChoi.isBlank() || danhSachVan == null || danhSachVan.isEmpty()) {
            return 0.0;
        }
        String tenChuan = tenNguoiChoi.strip();
        return danhSachVan.stream()
            .filter(Game::daCoNguoiThang)
            .map(van -> van.getDanhSachLuotDoan().stream()
                .filter(ld -> tenChuan.equals(ld.nguoiChoi()))
                .count())
            .filter(soLuot -> soLuot > 0)
            .mapToInt(Long::intValue)
            .average()
            .orElse(0.0);
    }
}
