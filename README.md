# Đề 30: Trò chơi đoán số nhiều người qua mạng TCP

## Cấu trúc thư mục

- `src/`: Mã nguồn chính của dự án (`doanso.domain`, `doanso.repo`, `doanso.service`, `doanso.net`, `doanso.ui`)
- `test/`: Mã nguồn kiểm thử tự động JUnit 5 (25 lớp kiểm thử, 103 ca kiểm thử)
- `lib/`: Thư viện kiểm thử (`junit-platform-console-standalone.jar`)
- `bin/`: Thư mục chứa các tệp `.class` sau khi biên dịch
- `docs/`: Danh sách chức năng (`danh-sach-chuc-nang.md`), các sơ đồ thiết kế (`so-do/`), báo cáo (`bao-cao/`) và kết quả kiểm thử tự động (`ket-qua-kiem-thu.txt`)
- `kiemthu.bat`: Kịch bản tự động xóa `bin/`, biên dịch toàn bộ bằng Java 21, chạy lặp 5 lần toàn bộ 103 ca kiểm thử đa luồng JUnit 5 và xuất báo cáo ra `docs/ket-qua-kiem-thu.txt`

## 1. Chạy toàn bộ kiểm thử bằng một lệnh duy nhất (`kiemthu.bat`)

Mở Terminal (PowerShell hoặc Command Prompt) tại thư mục gốc dự án `d:\Hoc\JaVa\DoanSo` và chạy:

```powershell
.\kiemthu.bat
```

Kịch bản `.\kiemthu.bat` thực hiện tự động:
1. Xoá thư mục `bin/` cũ, biên dịch lại toàn bộ `src/` và `test/` bằng Java 21 với `-encoding UTF-8` và classpath gồm `lib/junit-platform-console-standalone.jar`.
2. Chạy toàn bộ 103 ca kiểm thử JUnit 5 trong `test/`.
3. Lặp bước chạy kiểm thử **5 lần liên tiếp** để kiểm chứng độ ổn định đa luồng (Chương 4 & Chương 5), in kết quả từng lần.
4. In dòng tổng kết cuối cùng `TONG KET: DAT` (mã thoát `0`) hoặc `TONG KET: HONG` (mã thoát khác `0`).
5. Ghi toàn bộ nhật ký và kết quả kiểm thử ra `docs/ket-qua-kiem-thu.txt`.

## 2. Biên dịch và chạy thủ công từng bước

### Biên dịch toàn bộ mã nguồn (`src/`) và kiểm thử (`test/`)
```powershell
$files = (Get-ChildItem -Path src,test -Recurse -Filter *.java).FullName
javac -encoding UTF-8 -d bin -cp "lib/junit-platform-console-standalone.jar;src;test" $files
```

### Chạy kiểm thử JUnit 5 Console Launcher (1 lần)
```powershell
java -jar lib/junit-platform-console-standalone.jar execute --class-path bin --scan-class-path
```

### Khởi chạy Máy chủ (`GameServer`) và Giao diện Máy khách (`ClientUI`)
```powershell
# Mở 2 cửa sổ giao diện người chơi (Swing)
Start-Process java -ArgumentList "-cp bin doanso.ui.ClientUI"
Start-Process java -ArgumentList "-cp bin doanso.ui.ClientUI"

# Chạy máy chủ TCP lắng nghe tại cổng 8888
java -cp bin doanso.net.GameServer
```
