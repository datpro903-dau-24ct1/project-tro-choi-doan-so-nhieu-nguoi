# Đề 30 – Trò chơi đoán số nhiều người: Bảng chức năng

Căn cứ: đề `De_30_Tro_choi_doan_so_nhieu_nguoi.md` và slide **Chương 1–6** (`Chuong_1.md` … `Chuong_6.md`).
Các hàng được sắp theo **thứ tự nên làm** (logic thuần trước, mạng sau, đồng bộ rồi dữ liệu, mở rộng cuối).

## Quy ước

**Cách gán cột Nguồn:**

| Nhãn | Lấy từ mục nào của đề |
| :--- | :--- |
| **Phần lõi** | Mô tả + Ứng dụng mạng (máy chủ mở ván, đoán lần lượt/đồng thời, gợi ý, công bố người thắng) |
| **Phải làm** | Đồng thời + Dữ liệu và Streams + Kỹ thuật bắt buộc |
| **Mở rộng** | Mở rộng (giới hạn thời gian mỗi ván, nhiều phòng) |

**Ký hiệu trong cột Kỹ thuật Java:** Chỉ dùng các kỹ thuật có mặt trong slide Chương 1–6, kèm `(ChN)` là chương dạy kỹ thuật đó.

**Phân công 2 người (song song, ít đụng file):**

| Thành viên | Vai trò chính | Các mục phụ trách | Khối lượng |
| :--- | :--- | :--- | :--- |
| **Huỳnh Tiến Đạt, Trần Thanh Bình** | Chốt giao thức lệnh văn bản đầu tiên để làm hợp đồng chung | `F01` | 1 mục chung |
| **Huỳnh Tiến Đạt** | **Logic ván chơi, Đồng bộ ván & Thống kê dữ liệu (Streams):** Viết `enum`, `record`, lớp `Game` đồng bộ, kiểm thử đa luồng, xử lý đoán/thắng, bảng điểm, lịch sử, Streams, chế độ lần lượt và hẹn giờ ván | `F02`, `F03`, `F04`, `F05`, `F12`, `F13`, `F14`, `F15`, `F16`, `F19`, `F20` | 11 mục |
| **Trần Thanh Bình** | **Hạ tầng mạng TCP, Giao diện màn hình Client/Server, Broadcast & Đa phòng:** Viết `ServerSocket`, màn hình giao diện người chơi (`Client`), bể luồng `ClientHandler`, sổ người chơi trực tuyến, broadcast, mở ván, hiển thị bảng điểm/lịch sử/thống kê lên màn hình, xử lý rớt mạng và nhiều phòng | `F06`, `F07`, `F08`, `F09`, `F10`, `F11`, `F17`, `F18`, `F21` | 9 mục (nặng về I/O mạng & giao diện) |

## Bảng chức năng

| Mã | Chức năng | Nguồn | Trường hợp biên | Kỹ thuật Java (chương) | Phụ thuộc | Người phụ trách |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **F01** | Chốt giao thức văn bản theo dòng: `JOIN <tên>`, `GUESS <số>`, `SCORE`, `HISTORY`, `STATS`, `QUIT`; phản hồi `HIGHER`, `LOWER`, `WIN`... | Phần lõi | Lệnh lạ; thiếu tham số; dòng rỗng; tên có khoảng trắng; số quá dài | Biểu thức `switch` (Ch1, Ch6), so sánh chuỗi bằng `equals` không dùng `==` (Ch1, Ch3), `String::strip`, `String::toUpperCase` (Ch2) | — | Huỳnh Tiến Đạt, Trần Thanh Bình |
| **F02** | `enum` trạng thái ván (`WAITING`, `PLAYING`, `FINISHED`) và kết quả gợi ý (`LOWER`, `HIGHER`, `CORRECT`), kèm dữ liệu và phương thức kiểm tra | Phải làm | Chuyển trạng thái ngược (`FINISHED` → `PLAYING`); đọc trạng thái khi luồng khác đang đổi | `enum` mang trường và hành vi (Ch4, Ch6), `synchronized` (Ch4) | — | Huỳnh Tiến Đạt |
| **F03** | `record` lượt đoán bất biến: người đoán, giá trị đoán, thời điểm, kết quả gợi ý; kiểm tra hợp lệ ngay khi tạo | Phải làm | Tên `null`/rỗng; giá trị ngoài khoảng cho phép; hai lượt giống hệt nhau có bằng nhau không (`record` tự sinh `equals`/`hashCode`) | `record` (Ch2, Ch3, Ch6), hàm dựng gọn compact constructor (Ch6), `IllegalArgumentException` (Ch6), `equals`/`hashCode` (Ch3, Ch6), `LocalDateTime.now()` (Ch5), tính bất biến `immutability` (Ch6) | F02 | Huỳnh Tiến Đạt |
| **F04** | Lớp `Game` (ván): sinh số bí mật, nhận lượt đoán, trả gợi ý lớn hơn/nhỏ hơn/đúng, chốt người thắng đầu tiên; đồng bộ trạng thái ván | Phải làm | Đoán khi ván chưa mở hoặc đã kết thúc; số ngoài khoảng; hai người cùng trúng một lúc (chỉ một người thắng: ai vào khoá trước); người đoán đã rời ván | Lớp, trường `private`/`final`, `this`, hàm dựng (Ch1, Ch6), `synchronized` phương thức/khối (Ch4), `Math.random()` (Ch2), `List`/`ArrayList` (Ch3), biểu thức `switch` (Ch6), `Optional` (Ch6) | F02, F03 | Huỳnh Tiến Đạt |
| **F05** | Kiểm thử đồng thời cho `Game` (không qua mạng): N luồng đoán cùng lúc, kiểm đúng một người thắng và ghi nhận đủ N lượt | Phải làm | N = 1; mọi luồng cùng đoán đúng một số; phải chạy lặp nhiều lần vì lỗi tranh chấp `race condition` không phải lần nào cũng lộ | `Thread`, `Runnable`, `start`/`join` (Ch4), `ExecutorService`, `Callable`, `Future` (Ch4), `Lambda` (Ch2) | F04 | Huỳnh Tiến Đạt |
| **F06** | Máy chủ TCP một máy khách: lắng nghe cổng, nhận dòng lệnh, trả dòng phản hồi | Phần lõi | Cổng đã bị chiếm; client đóng đột ngột (`readLine` trả `null`); quên bật `auto-flush`; quên đóng socket làm cạn cổng | `ServerSocket`, `Socket`, `BufferedReader`, `InputStreamReader`, `PrintWriter` auto-flush (Ch5), `try-with-resources`, `IOException` (Ch5, Ch6), `var` (Ch6) | F01 | Trần Thanh Bình |
| **F07** | Màn hình giao diện máy khách (Client): cửa sổ nhập tên kết nối, ô nhập số và nút bấm đoán, khung hiển thị gợi ý, bảng xếp hạng và tin phát từ server | Phần lõi | Server chưa chạy; nhập chữ thay vì số vào ô đoán; ô nhập rỗng; server đóng giữa ván; tách luồng đọc mạng riêng để giao diện cửa sổ không bị treo khi chờ tin từ server | `Socket`, `BufferedReader`, `InputStreamReader`, `PrintWriter` (Ch5), `Thread`/`Runnable` tách luồng phụ giữ tính đáp ứng giao diện (Ch4), `Lambda` / lớp ẩn danh bắt sự kiện nút bấm (Ch1, Ch2), `try-with-resources` (Ch5, Ch6) | F01, F06 | Trần Thanh Bình |
| **F08** | Phục vụ nhiều máy khách (`thread-per-connection`): vòng `accept` ở luồng chính, mỗi kết nối giao cho `ClientHandler` chạy trên bể luồng | Phần lõi | Vượt cỡ bể luồng (client thứ 21 phải chờ trong hàng đợi); `ClientHandler` ném ngoại lệ; tắt server khi còn tác vụ đang chạy | `ExecutorService`, `Executors.newFixedThreadPool`, `submit`, `shutdown` (Ch4, Ch5), `Runnable` (Ch4, Ch5) | F06 | Trần Thanh Bình |
| **F09** | Sổ người chơi trực tuyến: ánh xạ tên người chơi → kênh ghi của client | Phải làm | Hai người `JOIN` cùng tên cùng lúc (kiểm tra rồi thêm không nguyên tử, cần đồng bộ); tên rỗng/không hợp lệ; người rời phải được gỡ; duyệt sổ khi có người vào/ra | `ConcurrentHashMap` (Ch5), `Map` (Ch3), `synchronized` (Ch4), `Optional` (Ch6) | F08 | Trần Thanh Bình |
| **F10** | Phát tin chung (broadcast) tới mọi người chơi đang trực tuyến | Phải làm | Client rớt hoặc chậm khi gửi (bắt ngoại lệ, gỡ khỏi sổ); nhiều luồng cùng ghi vào một socket làm lẫn dòng (cần khoá `synchronized` trên từng kênh ghi của client) | `PrintWriter` (Ch5), `ConcurrentHashMap` (Ch5), `synchronized` (Ch4), `forEach`, `Consumer`, `Lambda` (Ch2, Ch3) | F09 | Trần Thanh Bình |
| **F11** | Máy chủ mở ván (nút bấm hoặc lệnh trên Server): chuyển trạng thái `WAITING` → `PLAYING`, phát thông báo khoảng số cho mọi người chơi | Phần lõi | Mở khi đang có ván chạy; mở khi chưa có người chơi nào; mở ván mới khi ván cũ chưa công bố xong; người mới vào giữa ván | `enum` (Ch6), `BufferedReader` / sự kiện `Lambda` (Ch2, Ch5), `synchronized` (Ch4) | F04, F09, F10 | Trần Thanh Bình |
| **F12** | Nhận lượt đoán qua mạng, trả gợi ý lớn hơn/nhỏ hơn cho đúng người đoán, thông báo lượt đoán cho cả phòng | Phần lõi | `GUESS` khi chưa có ván; đầu vào không phải số nguyên; số âm/ngoài khoảng; người chưa `JOIN`; hai lệnh đoán đến cùng lúc | `Integer::parseInt` (Ch2), ngoại lệ riêng custom `Exception` (Ch6), `synchronized` trên ván (Ch4), `record` (Ch6) | F03, F04, F11 | Huỳnh Tiến Đạt |
| **F13** | Công bố người thắng, đóng ván (`FINISHED`), từ chối lượt đoán đến muộn | Phần lõi | Hai người trúng gần như đồng thời (người đến sau nhận thông báo ván đã có người thắng); người thắng rớt mạng ngay sau đó; tin công bố phải phát sau khi trạng thái đã chốt `FINISHED` | `enum` (Ch6), `synchronized` (Ch4), `Optional` (Ch6) | F10, F12 | Huỳnh Tiến Đạt |
| **F14** | Bảng điểm nhiều ván: `Map` tên → điểm, cộng điểm cho người thắng mỗi ván và sắp xếp bảng xếp hạng | Phải làm | Người thắng đã rời mạng vẫn giữ điểm; nhiều ván cùng cập nhật một khoá; hai người đồng điểm khi xếp hạng cần tiêu chí phụ (tên tăng dần); cùng tên vào lại | `Map`/`HashMap`, `getOrDefault`, `merge(k, 1, Integer::sum)` (Ch3), `ConcurrentHashMap` (Ch5), `synchronized` (Ch4), tham chiếu phương thức `::` (Ch2), `Comparator.comparingDouble`/`comparing`, `reversed`, `thenComparing` (Ch3) | F13 | Huỳnh Tiến Đạt |
| **F15** | Lịch sử đoán của mỗi người: `Map<String, List<LuotDoan>>` | Phải làm | `ArrayList` không an toàn luồng nên phải ghi trong `synchronized`; khi đọc trả bản sao phòng thủ `new ArrayList<>(...)` để tránh `ConcurrentModificationException` và giữ tính bất biến; người chưa đoán lần nào (danh sách rỗng) | `Map`, `List`, `ArrayList` (Ch3), sao chép phòng thủ `immutability` (Ch6), `record` (Ch6), `synchronized` (Ch4) | F12 | Huỳnh Tiến Đạt |
| **F16** | Thống kê số lần đoán trung bình (toàn hệ thống và từng người) bằng Streams API | Phải làm | Chưa có ván hoặc lượt đoán nào hoàn tất (tránh chia cho 0, trả giá trị mặc định); ván không ai thắng có tính vào thống kê không (cần chốt quy ước); người vào chỉ xem, không đoán | `Stream`, `IntStream`, `filter`, `map`, `count`, `average` (Ch2), `collect`, `Collectors.groupingBy`, `Collectors.counting`, `Collectors.averagingDouble` (Ch2), `Optional` / `orElse` (Ch6) | F15 | Huỳnh Tiến Đạt |
| **F17** | Hiển thị `SCORE`, `HISTORY`, `STATS` lên màn hình người chơi: định dạng dữ liệu từ server và cập nhật bảng điểm / lịch sử / thống kê trên cửa sổ client | Phần lõi | Bảng điểm rỗng; xem `HISTORY` của tên không tồn tại; kết quả dài nhiều dòng cần thanh cuộn | `Stream`, `sorted`, `Collectors.joining` (Ch2), `Comparator` (Ch3), `forEach` (Ch2, Ch3), `text block` (Ch6) | F14, F15, F16 | Trần Thanh Bình |
| **F18** | Xử lý ngắt kết nối và dọn dẹp tài nguyên | Phần lõi | Rớt mạng giữa ván (lượt đã đoán vẫn giữ trong lịch sử); người thắng rớt mạng; `QUIT` chủ động khác với mất mạng đột ngột; đóng socket tự động | `readLine` trả `null`, `IOException` (Ch5, Ch6), `try-with-resources` (Ch5, Ch6), `InterruptedException` (Ch4) | F09, F10, F13 | Trần Thanh Bình |
| **F19** | Chế độ đoán lần lượt theo vòng (đề cho phép "đoán lần lượt hoặc đồng thời") | Phần lõi | Đoán khi chưa tới lượt; người đang giữ lượt rớt mạng hoặc không trả lời (chuyển lượt tiếp theo); có người mới vào giữa vòng | `Queue`/`Deque`, `ArrayDeque`, `offer`/`poll` (Ch3), `synchronized` (Ch4), `enum` (Ch6) | F12, F18 | Huỳnh Tiến Đạt |
| **F20** | Giới hạn thời gian mỗi ván (tự động đóng ván khi hết giờ) | Mở rộng | Hết giờ đúng lúc có lượt đoán trúng (ai vào khoá `synchronized` trên ván trước sẽ quyết định); phải huỷ tác vụ đếm giờ khi đã có người thắng sớm; hết giờ không ai trúng | `ExecutorService`, `Callable`, `Future` (Ch4), `Thread.sleep` (Ch4), `volatile` cho cờ dừng (Ch4), `synchronized` (Ch4) | F13 | Huỳnh Tiến Đạt |
| **F21** | Nhiều phòng chơi song song: `Map` mã phòng → phòng chơi, mỗi phòng có ván, khoá và bảng điểm riêng | Mở rộng | Tạo phòng trùng tên; một người ở hai phòng cùng lúc; rời phòng khi ván đang chạy; phòng rỗng cần thu hồi; thao tác chạm hai phòng phải giành khoá theo thứ tự cố định để tránh `deadlock` | `ConcurrentHashMap` (Ch5), `Map`, `Set`/`HashSet` (Ch3), `synchronized` + thứ tự giành khoá cố định chống `deadlock` (Ch4), `interface` (Ch1), `Optional` (Ch6) | F13, F14, F20 | Trần Thanh Bình |

## Các mốc phối hợp 2 người (Milestones)

| Mốc | Huỳnh Tiến Đạt (Logic & Dữ liệu) | Trần Thanh Bình (Mạng, Giao diện & Phòng chơi) | Kết quả ghép nhánh kiểm được |
| :--- | :--- | :--- | :--- |
| **M1 – Khởi động song song** | `F02`–`F05` (Model `enum`, `record`, lớp `Game` + test 100 luồng) | `F06`–`F10` (Khung Server/Client TCP, bể luồng, sổ online, broadcast) | Hai bên làm độc lập sau khi chốt `F01`, không ai phải chờ ai |
| **M2 – Ghép mạng & Ván chơi** | `F12`, `F13` (Gắn xử lý đoán và chốt thắng vào Server) | `F11` (Lệnh mở ván từ Server và phát thông báo bắt đầu) | Nhiều client nối vào đoán số đồng thời, nhận gợi ý và công bố người thắng |
| **M3 – Dữ liệu & Độ bền mạng** | `F14`, `F15`, `F16` (Bảng điểm `Map`, lịch sử đoán, thống kê `Streams`) | `F17`, `F18` (Lệnh xem `SCORE`/`HISTORY`/`STATS` và xử lý rớt mạng) | Tra cứu điểm/lịch sử/thống kê đúng sau nhiều ván; client thoát đột ngột không làm sập ván |
| **M4 – Lần lượt & Mở rộng** | `F19`, `F20` (Chế độ đoán theo lượt + giới hạn thời gian mỗi ván) | `F21` (Quản lý nhiều phòng chơi độc lập) | Hết giờ tự đóng ván; nhiều phòng hoạt động song song |

## Ghi chú đối chiếu với slide Chương 1–6

1. **Đã phủ đầy đủ bằng Chương 6:** Các kỹ thuật bắt buộc của đề gồm `enum` trạng thái (F02) và `record` lượt đoán (F03) đã được áp dụng đầy đủ cú pháp của **Chương 6** (hàm dựng gọn `compact constructor` để kiểm tra hợp lệ, `enum` có trường và phương thức, biểu thức `switch`, `Optional`, `var`, `try-with-resources`, sao chép phòng thủ bảo đảm tính bất biến `immutability`).
2. **Lưu ý duy nhất ngoài slide (`ScheduledExecutorService` ở F20):** Phần *Mở rộng* của đề có nhắc tới `ScheduledExecutorService`, nhưng lớp này không xuất hiện trong slide Chương 1–6 (Chương 4 chỉ dạy `ExecutorService`, `Callable`, `Future`, `Thread.sleep`, `volatile`). Do quy tắc *"Cột Kỹ thuật Java chỉ dùng kỹ thuật có trong các slide này"*, bảng trên dùng tổ hợp `ExecutorService` + `Future` + `Thread.sleep` + `volatile` (Ch4); khi code thực tế, nhóm có thể dùng trực tiếp `ScheduledExecutorService` (là giao tiếp con của `ExecutorService` trong gói `java.util.concurrent` của Chương 4).

## Quyết định thiết kế đã áp dụng trong bảng

- **Giao thức văn bản theo dòng** (Ch5: `BufferedReader`/`PrintWriter`) thay vì truyền đối tượng bằng `Serializable`, vì đơn giản hơn và dễ kiểm thử bằng tay. Nếu cần truyền đối tượng (`ObjectOutputStream`/`ObjectInputStream` – Ch5), chỉ F01, F07 và F12 thay đổi.
- **Chế độ đồng thời là mặc định**, vì mục "Đồng thời" của đề đòi đồng bộ trạng thái khi nhiều người đoán cùng lúc. Chế độ lần lượt (F19) tách thành bước riêng dùng `ArrayDeque` (Ch3).
- **Một khoá cho mỗi ván** (`synchronized` trên đối tượng `Game`) để hai việc "kiểm tra ván còn mở" và "ghi nhận lượt đoán / chốt người thắng" không bị xen kẽ.
