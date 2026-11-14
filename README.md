# Đề 30: Trò chơi đoán số nhiều người

## Cấu trúc thư mục

- `src/`: Mã nguồn chính của dự án
- `test/`: Mã nguồn kiểm thử JUnit 5 (`AppTest.java`)
- `lib/`: Thư viện phụ thuộc (`junit-platform-console-standalone.jar`)
- `bin/`: Thư mục chứa các tệp `.class` sau khi biên dịch
- `docs/`: Danh sách chức năng (`danh-sach-chuc-nang.md`) và các sơ đồ (`so-do/*.drawio.png`)

## Biên dịch và chạy kiểm thử JUnit 5 bằng dòng lệnh

Mở Terminal (PowerShell) tại thư mục gốc `d:\Hoc\JaVa\DoanSo` và chạy:

### 1. Biên dịch mã nguồn (`src/`) và kiểm thử (`test/`)
```powershell
javac -encoding UTF-8 -d bin -cp "lib/junit-platform-console-standalone.jar;src;test" src/App.java test/AppTest.java
```

### 2. Chạy toàn bộ test với JUnit 5 Console Launcher
```powershell
java -jar lib/junit-platform-console-standalone.jar execute --class-path bin --scan-class-path
```

> **Lưu ý trên máy hiện tại:** Bản cài JDK tại `D:\C\JAVA\lib\` đang bị thiếu tệp `tzdb.dat` khiến `D:\C\JAVA\bin\javac.exe` báo lỗi `FileNotFoundException: D:\C\JAVA\lib\tzdb.dat` khi đọc file `.jar`. Để sửa dứt điểm cho `D:\C\JAVA`, bạn chạy 1 lệnh sau trong PowerShell:
> ```powershell
> Copy-Item "$env:USERPROFILE\.vscode\extensions\redhat.java-*\jre\*\lib\tzdb.dat" "D:\C\JAVA\lib\tzdb.dat"
> ```
