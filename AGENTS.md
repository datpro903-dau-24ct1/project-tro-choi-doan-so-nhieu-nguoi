# AGENTS.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## Dự án

- **Đề tài & Tài liệu:** Đề 30 "Trò chơi đoán số nhiều người" trong `De_30_Tro_choi_doan_so_nhieu_nguoi.md` (`de30.md`); bảng chức năng tại `docs/danh-sach-chuc-nang.md`; các sơ đồ thiết kế tại `docs/so-do/`.
- **Cấu trúc package và kiến trúc các tầng:** Tổ chức mã nguồn trong `src/` (gói gốc `doanso`), kiểm thử trong `test/`, chia làm 3 tầng:
  1. **Tầng Giao diện & Mạng (`ui`, `net`):** `ClientUI`, `GameServer`, `ClientHandler`.
  2. **Tầng Nghiệp vụ (`domain`, `service`):** `Game`, `PhongChoi`, `ThongKeService`, các `record`/`enum` (`LuotDoan`, `TrangThaiVan`, `GoiY`). **Tầng nghiệp vụ hoàn toàn không phụ thuộc vào giao diện hay mạng** để kiểm thử độc lập.
  3. **Tầng Lưu trữ (`repo`):** `KhoDuLieu` (`KhoBangDiem`, `KhoLichSuDoan`, `SoNguoiChoiOnline`).
- **Cách diễn đạt thất bại và kết quả dùng chung:** Dùng `sealed interface KetQua<T> permits KetQua.ThanhCong, KetQua.ThatBai` (với `record ThanhCong<T>(T duLieu, String thongDiep)` và `record ThatBai<T>(String maLoi, String lyDo)`).
- **Nền tảng & Thư viện:** Java 21, không thêm bất kỳ thư viện ngoài nào khác ngoài JUnit 5 (`lib/junit-platform-console-standalone.jar`).
- **Phạm vi kỹ thuật:** Chỉ dùng các kỹ thuật có trong slide các chương đã học (`chuong-01-slide.pdf` … `chuong-06-slide.pdf`).
- **Quản lý tài nguyên & Trạng thái:** Đóng mọi tệp, `Socket`, `ServerSocket`, luồng đọc/ghi (`BufferedReader`, `PrintWriter`) bằng `try-with-resources`; không dùng biến toàn cục (trạng thái phải được đóng gói trong đối tượng và truyền tường minh).
- **Quy trình triển khai & Kiểm thử:** Mỗi lần chỉ làm đúng **một chức năng** trong bảng `docs/danh-sach-chuc-nang.md` (`F01` → `F21`); tuyệt đối không sửa test chỉ để ép test xanh.
- **Lệnh biên dịch và chạy kiểm thử (từ `README.md`):**
  - Biên dịch:
    ```powershell
    javac -encoding UTF-8 -d bin -cp "lib/junit-platform-console-standalone.jar;src;test" src/*.java test/*.java
    ```
  - Chạy kiểm thử JUnit 5:
    ```powershell
    java -jar lib/junit-platform-console-standalone.jar execute --class-path bin --scan-class-path
    ```
- **Ngôn ngữ giao tiếp:** Luôn trả lời bằng tiếng Việt.
