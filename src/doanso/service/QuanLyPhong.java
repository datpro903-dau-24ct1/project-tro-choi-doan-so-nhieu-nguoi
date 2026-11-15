package doanso.service;

import doanso.domain.KetQua;
import doanso.domain.PhongChoi;
import java.util.Optional;
import java.util.Set;

// F21: Giao diện quản lý các phòng chơi trong hệ thống (Kỹ thuật interface - Chương 1)
public interface QuanLyPhong {

    KetQua<PhongChoi> taoPhong(String maPhong);

    KetQua<PhongChoi> taoPhong(String maPhong, String tenPhong);

    Optional<PhongChoi> timPhong(String maPhong);

    PhongChoi layHoacTaoPhong(String maPhong);

    KetQua<Void> xoaPhong(String maPhong);

    KetQua<Void> vaoPhong(String maPhong, String tenNguoiChoi);

    KetQua<Void> roiPhong(String maPhong, String tenNguoiChoi);

    KetQua<Void> chuyenPhong(String maPhongCu, String maPhongMoi, String tenNguoiChoi);

    Optional<String> timMaPhongCuaNguoiChoi(String tenNguoiChoi);

    int thuHoiPhongRong();

    int getSoLuongPhong();

    Set<String> getDanhSachMaPhong();
}
