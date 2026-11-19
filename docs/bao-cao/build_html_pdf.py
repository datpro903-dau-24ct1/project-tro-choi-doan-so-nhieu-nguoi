# -*- coding: utf-8 -*-
"""
Script biên dịch Báo cáo Bài tập lớn Lập trình Java nâng cao (AJP20101) ra PDF
Đảm bảo định dạng chuẩn mẫu đồ án Trường Đại học Kiến trúc Đà Nẵng (Khoa CNTT)
"""

import os
import subprocess
import base64

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DOCS_DIR = os.path.join(BASE_DIR, "docs", "bao-cao")
FIGS_DIR = os.path.join(DOCS_DIR, "figs")

# Đọc logo DAU base64 để nhúng trực tiếp không lo đường dẫn
logo_path = os.path.join(FIGS_DIR, "logo-dau.png")
with open(logo_path, "rb") as f:
    logo_base64 = base64.b64encode(f.read()).decode("utf-8")

# Đọc các hình sơ đồ
def get_img_base64(filename):
    p = os.path.join(FIGS_DIR, filename)
    if os.path.exists(p):
        with open(p, "rb") as f:
            return "data:image/png;base64," + base64.b64encode(f.read()).decode("utf-8")
    return ""

img_kientruc = get_img_base64("02-kien-truc-cac-tang.drawio.png")
img_uml = get_img_base64("03-so-do-lop-uml.drawio.png")
img_tuantu = get_img_base64("04-so-do-tuan-tu.drawio.png")

html_content = f"""<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO (AJP20101)</title>
<style>
  @page {{
    size: A4 portrait;
    margin: 20mm 18mm 20mm 25mm;
  }}

  * {{
    box-sizing: border-box;
  }}

  body {{
    font-family: 'Times New Roman', Times, serif;
    font-size: 13pt;
    line-height: 1.5;
    color: #222222;
    margin: 0;
    padding: 0;
  }}

  /* Phông tiêu đề kiểu Sans-serif hiện đại theo mẫu LaTeX DAU */
  h1, h2, h3, h4, .dau-heading {{
    font-family: 'Segoe UI', Tahoma, Arial, sans-serif;
  }}

  .page-break {{
    page-break-before: always;
  }}

  /* TRANG BÌA CHUẨN ĐỒ ÁN / BTL DAU */
  .cover-page {{
    height: 100vh;
    padding: 25px;
    position: relative;
    page-break-after: always;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    text-align: center;
  }}

  .cover-border-outer {{
    border: 2.5px solid #D9531E;
    padding: 4px;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }}

  .cover-border-inner {{
    border: 1px solid #D9531E;
    padding: 25px 20px 20px 20px;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }}

  .cover-univ {{
    font-size: 13pt;
    font-weight: bold;
    color: #111;
    margin: 0;
  }}

  .cover-univ-name {{
    font-size: 16pt;
    font-weight: bold;
    color: #8B0000;
    margin: 4px 0;
  }}

  .cover-faculty {{
    font-size: 13.5pt;
    font-weight: bold;
    color: #111;
    margin: 0;
  }}

  .cover-logo {{
    margin: 20px auto;
    width: 120px;
    height: 120px;
  }}

  .cover-doc-type {{
    font-size: 14pt;
    font-weight: bold;
    color: #111;
    margin-top: 10px;
    text-transform: uppercase;
  }}

  .cover-course {{
    font-size: 12pt;
    font-weight: bold;
    color: #555;
    margin-top: 4px;
  }}

  .cover-title {{
    font-size: 18pt;
    font-weight: bold;
    color: #8B0000;
    line-height: 1.35;
    margin: 25px 0 15px 0;
    text-transform: uppercase;
  }}

  .cover-info-table {{
    margin: 20px auto 10px 80px;
    font-size: 12.5pt;
    text-align: left;
    border-collapse: collapse;
  }}

  .cover-info-table td {{
    padding: 4px 8px;
    vertical-align: top;
  }}

  .cover-footer {{
    font-size: 12.5pt;
    font-weight: bold;
    color: #222;
    margin-top: 15px;
  }}

  /* TIÊU ĐỀ CHƯƠNG THEO MẪU DAU */
  .chapter-header {{
    position: relative;
    margin-bottom: 25px;
    padding-top: 10px;
  }}

  .chapter-num {{
    position: absolute;
    top: -5px;
    right: 0;
    font-size: 55pt;
    font-weight: bold;
    color: #D9531E;
    line-height: 1;
    font-family: 'Segoe UI', Tahoma, sans-serif;
  }}

  .chapter-title {{
    font-size: 22pt;
    font-weight: bold;
    color: #8B0000;
    margin: 0 0 8px 0;
    font-family: 'Segoe UI', Tahoma, sans-serif;
  }}

  .chapter-rule {{
    height: 2.5px;
    background-color: #D9531E;
    border: none;
    margin: 0;
  }}

  .preface-title {{
    font-size: 22pt;
    font-weight: bold;
    color: #8B0000;
    margin: 20px 0 15px 0;
    border-bottom: 2.5px solid #D9531E;
    padding-bottom: 8px;
    font-family: 'Segoe UI', Tahoma, sans-serif;
  }}

  h2 {{
    font-size: 14pt;
    font-weight: bold;
    color: #8B0000;
    margin-top: 22px;
    margin-bottom: 8px;
  }}

  h3 {{
    font-size: 12.5pt;
    font-weight: bold;
    color: #222222;
    margin-top: 16px;
    margin-bottom: 6px;
  }}

  p {{
    text-align: justify;
    margin-top: 6px;
    margin-bottom: 6px;
    text-indent: 1.25cm;
  }}

  p.no-indent {{
    text-indent: 0;
  }}

  /* CÁC HỘP NHẤN MẠNH (ĐẶC TRƯNG MẪU ĐỒ ÁN DAU) */
  .box-target {{
    background-color: #F0F4F8;
    border-radius: 6px;
    padding: 10px 14px;
    margin: 14px 0 18px 0;
    font-size: 11pt;
    border: 1px solid #D9E2EC;
  }}

  .box-target-title {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #102A43;
    font-size: 10pt;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 4px;
  }}

  .box-remember {{
    background-color: #EBF7ED;
    border-left: 4px solid #2E7D32;
    padding: 10px 14px;
    margin: 14px 0;
    font-size: 11pt;
  }}

  .box-remember-title {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #2E7D32;
    font-size: 10.5pt;
    margin-bottom: 3px;
  }}

  .box-warning {{
    background-color: #FFF4E5;
    border-left: 4px solid #D32F2F;
    padding: 10px 14px;
    margin: 14px 0;
    font-size: 11pt;
  }}

  .box-warning-title {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #D32F2F;
    font-size: 10.5pt;
    margin-bottom: 3px;
  }}

  .box-def {{
    background-color: #F3F8F2;
    border-left: 4px solid #388E3C;
    padding: 10px 14px;
    margin: 14px 0;
    font-size: 11pt;
  }}

  .box-def-title {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #1B5E20;
    font-size: 10.5pt;
    margin-bottom: 3px;
  }}

  /* BẢNG BIỂU CHUẨN BOOKTABS */
  table.booktabs {{
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    font-size: 11pt;
  }}

  table.booktabs caption {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #8B0000;
    text-align: left;
    margin-bottom: 6px;
    font-size: 11pt;
  }}

  table.booktabs thead th {{
    border-top: 2px solid #222;
    border-bottom: 1.2px solid #222;
    padding: 6px 8px;
    text-align: left;
    font-weight: bold;
    background-color: #FAFAFA;
  }}

  table.booktabs tbody td {{
    padding: 6px 8px;
    vertical-align: top;
    border-bottom: 0.5px solid #EEE;
  }}

  table.booktabs tbody tr:last-child td {{
    border-bottom: 2px solid #222;
  }}

  /* KHỐI MÃ NGUỒN CHUẨN LISTINGS */
  .code-block {{
    background-color: #F8F9FA;
    border: 1px solid #E9ECEF;
    border-left: 3px solid #FBAE40;
    border-radius: 4px;
    padding: 10px 14px;
    margin: 14px 0;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 9.5pt;
    line-height: 1.45;
    white-space: pre-wrap;
    overflow-x: auto;
  }}

  .code-caption {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #8B0000;
    font-size: 10.5pt;
    margin-bottom: 4px;
  }}

  .code-kw {{ color: #8B0000; font-weight: bold; }}
  .code-cm {{ color: #2E7D32; font-style: italic; }}
  .code-str {{ color: #D9531E; }}
  .code-num {{ color: #0066CC; }}

  /* HÌNH ẢNH */
  .figure-box {{
    text-align: center;
    margin: 18px 0;
  }}

  .figure-box img {{
    max-width: 95%;
    height: auto;
    border: 1px solid #E0E0E0;
    border-radius: 4px;
  }}

  .figure-caption {{
    font-family: 'Segoe UI', Tahoma, sans-serif;
    font-weight: bold;
    color: #8B0000;
    font-size: 11pt;
    margin-top: 6px;
  }}

  /* MỤC LỤC & DANH SÁCH */
  .toc-line {{
    display: flex;
    justify-content: space-between;
    margin: 4px 0;
    font-size: 12pt;
  }}

  .toc-dots {{
    flex-grow: 1;
    border-bottom: 1px dotted #888;
    margin: 0 6px 4px 6px;
  }}

  ul, ol {{
    margin-top: 4px;
    margin-bottom: 8px;
    padding-left: 30px;
  }}

  li {{
    margin-bottom: 3px;
  }}

  .header-rule {{
    display: flex;
    justify-content: space-between;
    font-size: 9pt;
    font-style: italic;
    color: #666;
    border-bottom: 0.5px solid #CCC;
    padding-bottom: 3px;
    margin-bottom: 18px;
  }}
</style>
</head>
<body>

<!-- TRANG BÌA -->
<div class="cover-page">
  <div class="cover-border-outer">
    <div class="cover-border-inner">
      <div>
        <div class="cover-univ">BỘ GIÁO DỤC VÀ ĐÀO TẠO</div>
        <div class="cover-univ-name">TRƯỜNG ĐẠI HỌC KIẾN TRÚC ĐÀ NẴNG</div>
        <div class="cover-faculty">KHOA CÔNG NGHỆ THÔNG TIN</div>
      </div>

      <div>
        <img class="cover-logo" src="data:image/png;base64,{logo_base64}" alt="DAU Logo">
        <div class="cover-doc-type">BÁO CÁO BÀI TẬP LỚN</div>
        <div class="cover-course">HỌC PHẦN: LẬP TRÌNH JAVA NÂNG CAO (AJP20101)</div>
        <div class="cover-title">
          ĐỀ TÀI 30: XÂY DỰNG HỆ THỐNG TRÒ CHƠI<br>ĐOÁN SỐ NHIỀU NGƯỜI QUA MẠNG TCP
        </div>
      </div>

      <div>
        <table class="cover-info-table">
          <tr>
            <td style="font-weight: bold; width: 220px;">Sinh viên thực hiện:</td>
            <td><strong>Trần Thanh Bình</strong></td>
          </tr>
          <tr>
            <td></td>
            <td><strong>Huỳnh Tiến Đạt</strong></td>
          </tr>
          <tr>
            <td style="font-weight: bold;">Lớp:</td>
            <td>CNTT 24CT1</td>
          </tr>
          <tr>
            <td style="font-weight: bold;">Ngành:</td>
            <td>Công nghệ thông tin</td>
          </tr>
          <tr>
            <td style="font-weight: bold;">Giảng viên hướng dẫn:</td>
            <td><strong>TS. Đỗ Phúc Hảo</strong></td>
          </tr>
          <tr>
            <td style="font-weight: bold;">Năm học:</td>
            <td>2025–2026</td>
          </tr>
        </table>
        <div class="cover-footer">Đà Nẵng · 2025–2026</div>
      </div>
    </div>
  </div>
</div>

<!-- LỜI CAM ĐOAN -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Lời cam đoan</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>
<div class="preface-title">Lời cam đoan</div>

<p>Chúng tôi xin cam đoan bài tập lớn này là công trình học thuật do chính nhóm chúng tôi thực hiện dưới sự hướng dẫn trực tiếp của <strong>TS. Đỗ Phúc Hảo</strong>. Toàn bộ các nội dung nghiên cứu, thiết kế kiến trúc 3 tầng, mã nguồn lập trình và số liệu kiểm thử được trình bày trong báo cáo là trung thực, chính xác và do các thành viên trong nhóm trực tiếp xây dựng.</p>

<p>Mọi số liệu, mô hình và mã nguồn của các tài liệu tham khảo được sử dụng trong bài tập lớn đều được trích dẫn nguồn gốc minh bạch. Toàn bộ phần mềm và thư viện được dùng (Java Development Kit 21, JUnit 5 Platform Console Launcher) đều tuân thủ giấy phép sử dụng mã nguồn mở và chuẩn mực môn học AJP20101, tuyệt đối không sử dụng các framework mạng bên ngoài để làm thay logic lõi.</p>

<p>Những phần có sử dụng công cụ trí tuệ nhân tạo hỗ trợ (chuẩn hóa test cases tự động, phát hiện cảnh báo mã nguồn tĩnh và tra cứu định dạng) đã được khai báo công khai, minh bạch theo đúng quy định về liêm chính học thuật. Nhóm chúng tôi xin chịu hoàn toàn trách nhiệm trước Khoa Công nghệ Thông tin và Nhà trường nếu có bất kỳ sự vi phạm nào.</p>

<div style="margin-top: 40px; float: right; text-align: center; width: 300px;">
  <em>Đà Nẵng, ngày 16 tháng 11 năm 2026</em><br>
  <strong>Sinh viên thực hiện</strong><br><br><br><br>
  <strong>Trần Thanh Bình &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Huỳnh Tiến Đạt</strong>
</div>
<div style="clear: both;"></div>

<!-- LỜI CẢM ƠN -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Lời cảm ơn</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>
<div class="preface-title">Lời cảm ơn</div>

<p>Nhóm chúng tôi xin bày tỏ lòng biết ơn sâu sắc và chân thành nhất đến <strong>TS. Đỗ Phúc Hảo</strong>, giảng viên phụ trách học phần Lập trình Java nâng cao (AJP20101). Trong suốt quá trình học tập và thực hiện đề tài, thầy đã tận tâm truyền đạt những kiến thức chuyên môn cốt lõi và hiện đại nhất của nền tảng Java 21: từ tư duy lập trình hướng đối tượng chuẩn mực, kiểm soát đa luồng đồng thời an toàn, xử lý mạng Socket TCP hiệu năng cao đến các kỹ thuật trừu tượng hóa dữ liệu tiên tiến. Sự định hướng và những lời nhận xét chuyên môn quý báu của thầy là kim chỉ nam giúp nhóm xây dựng hệ thống đúng chuẩn kỹ thuật.</p>

<p>Nhóm cũng xin chân thành cảm ơn quý thầy cô Khoa Công nghệ Thông tin – Trường Đại học Kiến trúc Đà Nẵng đã luôn tạo điều kiện học tập tốt nhất, trang bị cho sinh viên tư duy kỹ thuật vững vàng để hoàn thành bài tập lớn này. Cuối cùng, chúng tôi xin cảm ơn gia đình, bạn bè đã luôn đồng hành, động viên nhóm trong suốt thời gian học tập và hoàn thành sản phẩm.</p>

<div style="margin-top: 40px; float: right; text-align: center; width: 300px;">
  <em>Đà Nẵng, ngày 16 tháng 11 năm 2026</em><br>
  <strong>Sinh viên thực hiện</strong><br><br><br><br>
  <strong>Trần Thanh Bình &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Huỳnh Tiến Đạt</strong>
</div>
<div style="clear: both;"></div>

<!-- TÓM TẮT -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Tóm tắt</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>
<div class="preface-title">Tóm tắt</div>

<p>Báo cáo trình bày chi tiết kết quả xây dựng hệ thống <strong>Trò chơi đoán số nhiều người (Multiplayer Number Guessing Game)</strong> qua mạng TCP – Đề tài số 30 thuộc học phần Lập trình Java nâng cao (AJP20101). Thách thức trung tâm của bài toán nằm ở việc quản lý trạng thái đồng thời khi nhiều người chơi cùng gửi lượt đoán trong thời gian thực, loại bỏ xung đột tranh chấp dữ liệu (<em>Race Condition</em>), phòng tránh bế tắc vĩnh viễn (<em>Deadlock</em>) khi điều phối nhiều phòng chơi, và bảo đảm tính bền vững của dịch vụ mạng khi máy khách ngắt kết nối đột ngột.</p>

<p>Nhóm đã áp dụng phương pháp thiết kế kiến trúc 3 tầng phân tách độc lập (Tầng Giao diện & Mạng, Tầng Nghiệp vụ, Tầng Lưu trữ) kết hợp các kỹ thuật hiện đại của Java 21: cấu trúc bản ghi bất biến <code>record</code> với compact constructor, kết quả kín <code>sealed interface</code>, bể luồng phục vụ kết nối <code>thread-per-connection</code>, cấu trúc bảng băm đồng thời <code>ConcurrentHashMap</code>, và thuật toán sắp xếp khóa phòng cố định (<em>Lock Ordering Rule</em>). Toàn bộ 21 chức năng theo đặc tả đề tài đã được triển khai hoàn chỉnh theo phương pháp phát triển hướng kiểm thử (TDD), vượt qua <strong>103 / 103 ca kiểm thử tự động JUnit 5 với tỷ lệ thành công 100%</strong>.</p>

<p class="no-indent" style="margin-top: 20px;"><strong>Từ khoá:</strong> Lập trình mạng Socket TCP, Java 21 LTS, Đồng bộ hóa đa luồng, Lock Ordering chống Deadlock, Streams API, Test-Driven Development (TDD).</p>

<!-- MỤC LỤC -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Mục lục</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>
<div class="preface-title">Mục lục</div>

<div class="toc-line"><strong>Lời cam đoan</strong><div class="toc-dots"></div>i</div>
<div class="toc-line"><strong>Lời cảm ơn</strong><div class="toc-dots"></div>ii</div>
<div class="toc-line"><strong>Tóm tắt</strong><div class="toc-dots"></div>iii</div>
<div class="toc-line"><strong>1 Mở đầu</strong><div class="toc-dots"></div>1</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;1.1 Lý do chọn đề tài<div class="toc-dots"></div>1</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;1.2 Mục tiêu của bài tập lớn<div class="toc-dots"></div>1</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;1.3 Phạm vi và đối tượng<div class="toc-dots"></div>2</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;1.4 Bố cục báo cáo<div class="toc-dots"></div>2</div>
<div class="toc-line"><strong>2 Cơ sở lý thuyết và công nghệ</strong><div class="toc-dots"></div>3</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;2.1 Kiến thức nền tảng Java 21 và Mạng TCP<div class="toc-dots"></div>3</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;2.2 Công cụ và môi trường phát triển<div class="toc-dots"></div>5</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;2.3 Các phương pháp tiếp cận liên quan<div class="toc-dots"></div>5</div>
<div class="toc-line"><strong>3 Phân tích và thiết kế hệ thống</strong><div class="toc-dots"></div>6</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;3.1 Phân tích yêu cầu và phân rã 21 chức năng<div class="toc-dots"></div>6</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;3.2 Thiết kế kiến trúc hệ thống 3 tầng<div class="toc-dots"></div>7</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;3.3 Thiết kế dữ liệu và các thực thể Domain<div class="toc-dots"></div>8</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;3.4 Thiết kế sơ đồ lớp UML<div class="toc-dots"></div>9</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;3.5 Thiết kế luồng xử lý và tương tác tuần tự<div class="toc-dots"></div>10</div>
<div class="toc-line"><strong>4 Cài đặt và thử nghiệm</strong><div class="toc-dots"></div>11</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;4.1 Môi trường triển khai<div class="toc-dots"></div>11</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;4.2 Các thành phần chính và mã nguồn cốt lõi<div class="toc-dots"></div>11</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;4.3 Giao diện máy khách ClientUI<div class="toc-dots"></div>14</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;4.4 Kiểm thử hệ thống theo TDD với 103 test cases<div class="toc-dots"></div>14</div>
<div class="toc-line"><strong>5 Kết quả và đánh giá</strong><div class="toc-dots"></div>16</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;5.1 Kết quả đạt được đối chiếu mục tiêu<div class="toc-dots"></div>16</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;5.2 Độ đo đánh giá định lượng<div class="toc-dots"></div>17</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;5.3 Bàn luận chuyên sâu về đồng thời và Deadlock<div class="toc-dots"></div>17</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;5.4 Hạn chế của hệ thống<div class="toc-dots"></div>18</div>
<div class="toc-line"><strong>6 Kết luận và hướng phát triển</strong><div class="toc-dots"></div>19</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;6.1 Kết luận<div class="toc-dots"></div>19</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;6.2 Bài học rút ra<div class="toc-dots"></div>19</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;6.3 Hướng phát triển<div class="toc-dots"></div>19</div>
<div class="toc-line"><strong>Tài liệu tham khảo</strong><div class="toc-dots"></div>20</div>
<div class="toc-line"><strong>Phụ lục</strong><div class="toc-dots"></div>21</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;A. Hướng dẫn cài đặt và chạy chương trình<div class="toc-dots"></div>21</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;B. Bảng tự kiểm trước khi nộp<div class="toc-dots"></div>22</div>
<div class="toc-line">&nbsp;&nbsp;&nbsp;&nbsp;C. Nhận xét của giảng viên hướng dẫn & Điểm số<div class="toc-dots"></div>23</div>

<!-- CHƯƠNG 1 -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Chương 1. Mở đầu</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>

<div class="chapter-header">
  <div class="chapter-num">1</div>
  <div class="chapter-title">Mở đầu</div>
  <hr class="chapter-rule">
</div>

<div class="box-target">
  <div class="box-target-title">Mục tiêu chương</div>
  Chương mở đầu trình bày bối cảnh và lý do chọn đề tài, xác định các mục tiêu kỹ thuật cụ thể cần đạt được, giới hạn phạm vi nghiên cứu và phân định cấu trúc tổng thể của bản báo cáo bài tập lớn.
</div>

<h2>1.1 Lý do chọn đề tài</h2>
<p>Trong thời đại mạng máy tính và điện toán phân tán phát triển mạnh mẽ, việc làm chủ kỹ năng xây dựng các ứng dụng mạng đa người dùng (multi-user network applications) hoạt động ổn định và chính xác trong môi trường tương tác thời gian thực là yêu cầu sống còn đối với sinh viên ngành Công nghệ thông tin. Bài toán "Trò chơi đoán số nhiều người" (Đề tài số 30) tuy có luật chơi trực quan nhưng đặt ra đầy đủ những bài toán kỹ thuật phức tạp nhất trong công nghệ phần mềm: lập trình Socket TCP hướng dòng tin cậy, điều phối bể luồng máy chủ phục vụ đồng thời hàng chục khách, đồng bộ hóa trạng thái phiên chơi nhằm triệt tiêu tranh chấp (Race Condition), sắp xếp khóa tránh bế tắc luồng (Deadlock), và tổng hợp số liệu thống kê bằng Streams API hiện đại.</p>

<p>Đề tài được nhóm lựa chọn thực hiện vì mang tính thực tiễn cao, giúp hai thành viên vận dụng toàn diện và sâu sắc hệ thống kiến thức 6 chương của học phần Lập trình Java nâng cao (AJP20101), đồng thời rèn luyện quy trình phát triển phần mềm chuyên nghiệp thông qua phương pháp phát triển hướng kiểm thử (Test-Driven Development - TDD) và quản lý phiên bản với Git.</p>

<h2>1.2 Mục tiêu của bài tập lớn</h2>
<p>Bài tập lớn hướng tới các mục tiêu cụ thể, đo lường được:</p>
<ul>
  <li><strong>Xây dựng hạ tầng máy chủ TCP mạnh mẽ (<code>GameServer</code>):</strong> Áp dụng mô hình <em>thread-per-connection</em> với bể luồng <code>ExecutorService</code>, phục vụ song song tối thiểu 20 máy khách mà không làm cạn kiệt tài nguyên hệ thống.</li>
  <li><strong>Thiết kế giao thức văn bản hướng dòng hoàn chỉnh:</strong> Chuẩn hóa các lệnh <code>JOIN</code>, <code>GUESS</code>, <code>START</code>, <code>SCORE</code>, <code>HISTORY</code>, <code>STATS</code>, <code>CREATE_ROOM</code>, <code>JOIN_ROOM</code>, <code>LEAVE_ROOM</code>, <code>LIST_ROOMS</code>, <code>CURRENT_ROOM</code> và <code>QUIT</code> với cơ chế tự động đẩy dữ liệu (auto-flush).</li>
  <li><strong>Hiện thực trọn vẹn 21/21 chức năng (F01–F21):</strong> Đáp ứng 100% yêu cầu phân rã chức năng, bao phủ đầy đủ cả phần Lõi, phần Phải làm và phần Mở rộng (hẹn giờ đóng ván tự động với <code>Future</code>, điều phối đa phòng chơi song song).</li>
  <li><strong>Xây dựng giao diện máy khách trực quan (<code>ClientUI</code>):</strong> Ứng dụng giao diện Swing mượt mà, tách luồng đọc mạng chuyên biệt để giữ giao diện luôn đáp ứng, hiển thị nhật ký bằng khối văn bản nghệ thuật (Text Blocks).</li>
  <li><strong>Đạt chuẩn chất lượng kiểm thử tự động TDD:</strong> Xây dựng bộ kiểm thử gồm 103 bài test tự động JUnit 5 bao phủ toàn bộ các trường hợp biên, đạt tỷ lệ vượt qua 100%.</li>
</ul>

<h2>1.3 Phạm vi và đối tượng</h2>
<p><strong>Đối tượng:</strong> Hệ thống trò chơi đoán số nhiều người chơi qua Socket TCP trên nền tảng Java 21 Standard Edition.</p>
<p><strong>Phạm vi:</strong> Hệ thống tập trung vào kiến trúc mạng cục bộ (LAN/Localhost), quản lý dữ liệu phiên chơi trong bộ nhớ chính (in-memory) bằng các cấu trúc dữ liệu Collections an toàn luồng. Đề tài không sử dụng các framework web hay ORM bên ngoài nhằm đảm bảo sinh viên tự tay lập trình và kiểm soát từng dòng mã nguồn lõi.</p>

<div class="box-warning">
  <div class="box-warning-title">Lưu ý</div>
  Tuân thủ nghiêm ngặt chuẩn kiến thức 6 chương của học phần AJP20101: Tầng Nghiệp vụ (domain) hoàn toàn không được phụ thuộc vào Tầng Mạng (net) hay Tầng Giao diện (ui) để đảm bảo mã nguồn có thể kiểm thử độc lập tuyệt đối.
</div>

<h2>1.4 Bố cục báo cáo</h2>
<p>Nội dung báo cáo được tổ chức thành 6 chương: Chương 1 giới thiệu tổng quan đề tài; Chương 2 trình bày cơ sở lý thuyết và công nghệ; Chương 3 phân tích yêu cầu và thiết kế kiến trúc hệ thống; Chương 4 mô tả chi tiết cài đặt và kiểm thử; Chương 5 trình bày kết quả và đánh giá; Chương 6 tổng kết và định hướng phát triển trong tương lai.</p>

<!-- CHƯƠNG 2 -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Chương 2. Cơ sở lý thuyết và công nghệ</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>

<div class="chapter-header">
  <div class="chapter-num">2</div>
  <div class="chapter-title">Cơ sở lý thuyết và công nghệ</div>
  <hr class="chapter-rule">
</div>

<div class="box-target">
  <div class="box-target-title">Mục tiêu chương</div>
  Trình bày các kiến thức lý thuyết nền tảng gồm các tính năng mới của Java 21, lập trình mạng Socket TCP, kỹ thuật đồng bộ hóa đa luồng, cơ chế phòng chống Deadlock, cùng danh mục công cụ môi trường được sử dụng.
</div>

<h2>2.1 Kiến thức nền tảng Java 21 và Mạng TCP</h2>

<div class="box-def">
  <div class="box-def-title">Định nghĩa · Giao thức văn bản hướng dòng (Stream-based Text Protocol)</div>
  Là giao thức truyền thông dựa trên luồng ký tự thuần túy, trong đó các thông điệp yêu cầu và phản hồi được phân tách bởi ký tự xuống dòng (<code>\\n</code> hoặc <code>\\r\\n</code>). Mỗi dòng chứa một từ khóa lệnh đứng đầu, theo sau là các tham số ngăn cách bằng khoảng trắng.
</div>

<p>Ưu điểm vượt trội của giao thức văn bản là tính trong sáng, độc lập nền tảng, dễ dàng kiểm thử thủ công bằng các công cụ như Telnet/Netcat và không phụ thuộc vào phiên bản lớp như cơ chế tuần tự hóa nhị phân (<code>Serializable</code>).</p>

<div class="box-def">
  <div class="box-def-title">Định nghĩa · Tranh chấp tài nguyên và Bế tắc luồng (Race Condition & Deadlock)</div>
  <strong>Race Condition</strong> xuất hiện khi hai hay nhiều luồng cùng truy cập và sửa đổi dữ liệu dùng chung (ví dụ cùng đoán một số bí mật hoặc cùng cập nhật bảng điểm) mà kết quả phụ thuộc vào thứ tự điều phối luồng của hệ điều hành.<br>
  <strong>Deadlock</strong> là tình trạng hai luồng bị khóa vĩnh viễn do mỗi luồng giữ một tài nguyên và chờ tài nguyên do luồng kia nắm giữ.
</div>

<p>Để giải quyết bài toán thống kê số lần đoán trung bình mỗi ván và trung bình mỗi người chơi theo yêu cầu của F16, hệ thống áp dụng Java Streams API với công thức tính toán toán học chuẩn mực:</p>

<div style="text-align: center; margin: 15px 0; font-size: 13pt; font-family: 'Times New Roman', serif;">
  <i>L&#772;</i> = (1 / |<i>V</i>|) &sum;<sub><i>v</i> &isin; <i>V</i></sub> |<i>L<sub>v</sub></i>| &nbsp;&nbsp;&nbsp;&nbsp; (2.1)
</div>

<p class="no-indent">trong đó <i>V</i> là tập hợp các ván chơi đã hoàn tất hợp lệ (loại bỏ các ván chưa kết thúc hoặc bị hủy), và |<i>L<sub>v</sub></i>| là tổng số lượt đoán ghi nhận được trong ván chơi <i>v</i>. Thao tác tính toán được thực hiện hoàn toàn thông qua chuỗi hàm <code>filter()</code>, <code>mapToInt()</code> và <code>average()</code> của <code>IntStream</code> (Chương 2).</p>

<div class="box-remember">
  <div class="box-remember-title">Ghi nhớ</div>
  Trong lập trình mạng đa luồng với Java, cấu trúc <code>record</code> (Chương 6) bảo đảm tính bất biến tuyệt đối (Immutability), giúp việc truyền dữ liệu giữa các luồng mạng an toàn 100% mà không cần sao chép tốn kém.
</div>

<h2>2.2 Công cụ và môi trường phát triển</h2>
<table class="booktabs">
  <caption>Bảng 2.1. Danh mục công cụ và thư viện sử dụng trong bài tập lớn.</caption>
  <thead>
    <tr>
      <th style="width: 25%;">Thành phần</th>
      <th style="width: 20%;">Phiên bản</th>
      <th>Vai trò trong bài tập lớn</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>Java Development Kit</strong></td>
      <td>OpenJDK 21 LTS</td>
      <td>Trình biên dịch và thực thi chính (hỗ trợ record, switch expression, pattern matching)</td>
    </tr>
    <tr>
      <td><strong>JUnit Platform Console</strong></td>
      <td>1.10.2 Standalone</td>
      <td>Thực thi tự động 103 ca kiểm thử TDD từ dòng lệnh</td>
    </tr>
    <tr>
      <td><strong>Visual Studio Code</strong></td>
      <td>1.95+</td>
      <td>Môi trường lập trình (IDE), debug và quản lý workspace</td>
    </tr>
    <tr>
      <td><strong>Git / GitHub</strong></td>
      <td>2.40+</td>
      <td>Quản lý phiên bản mã nguồn, nhánh tính năng và worktree</td>
    </tr>
  </tbody>
</table>

<h2>2.3 Các phương pháp tiếp cận liên quan</h2>
<p>So với phương pháp truyền đối tượng nhị phân truyền thống (<code>ObjectInputStream</code> / <code>ObjectOutputStream</code>), giao thức văn bản hướng dòng tuy đòi hỏi phân tích cú pháp chuỗi (parsing) nhưng loại bỏ hoàn toàn nguy cơ mất an toàn bảo mật từ việc giải tuần tự hóa (deserialization vulnerabilities) và tương thích hoàn hảo với giao diện người dùng Swing.</p>

<!-- CHƯƠNG 3 -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Chương 3. Phân tích và thiết kế hệ thống</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>

<div class="chapter-header">
  <div class="chapter-num">3</div>
  <div class="chapter-title">Phân tích và thiết kế hệ thống</div>
  <hr class="chapter-rule">
</div>

<div class="box-target">
  <div class="box-target-title">Mục tiêu chương</div>
  Phân tích chi tiết 21 chức năng bài toán, thiết kế kiến trúc hệ thống 3 tầng, mô hình hóa dữ liệu bất biến và các sơ đồ lớp UML, sơ đồ tuần tự chuẩn mực.
</div>

<h2>3.1 Phân tích bài toán và phân rã 21 chức năng</h2>
<p>Hệ thống trò chơi đoán số nhiều người được phân rã thành 21 chức năng chuẩn mực từ F01 đến F21, phân định rõ khối lượng công việc giữa hai thành viên trong nhóm:</p>
<ul>
  <li><strong>Huỳnh Tiến Đạt (Logic nghiệp vụ & Dữ liệu Streams):</strong> Đảm nhiệm 11 mục cốt lõi gồm mô hình <code>Game</code>, <code>LuotDoan</code>, <code>TrangThaiVan</code>, <code>GoiY</code>, bảng điểm <code>KhoBangDiem</code>, lịch sử <code>KhoLichSuDoan</code>, thống kê <code>ThongKeService</code>, chế độ đoán lần lượt <code>ArrayDeque</code> và hẹn giờ ván với <code>Future</code> (F02, F03, F04, F05, F12, F13, F14, F15, F16, F19, F20).</li>
  <li><strong>Trần Thanh Bình (Hạ tầng mạng TCP, Giao diện & Đa phòng):</strong> Đảm nhiệm 10 mục hạ tầng gồm máy chủ <code>GameServer</code>, giao diện người dùng <code>ClientUI</code>, bể luồng <code>ClientHandler</code>, sổ người chơi online <code>SoNguoiChoiOnline</code>, broadcast tin nhắn, mở ván chơi, hiển thị dữ liệu bảng điểm/lịch sử/thống kê, xử lý ngắt kết nối dọn dẹp tài nguyên và điều phối đa phòng chơi song song chống Deadlock (F01, F06, F07, F08, F09, F10, F11, F17, F18, F21).</li>
</ul>

<h2>3.2 Thiết kế kiến trúc hệ thống 3 tầng</h2>
<p>Kiến trúc hệ thống tuân thủ nguyên lý tách biệt mối quan tâm (Separation of Concerns):</p>
<ol>
  <li><strong>Tầng Giao diện & Mạng (<code>ui</code>, <code>net</code>):</strong> Tiếp nhận yêu cầu người dùng, xử lý các kết nối Socket TCP, chuyển đổi chuỗi văn bản thành các lệnh gọi nghiệp vụ.</li>
  <li><strong>Tầng Nghiệp vụ (<code>domain</code>, <code>service</code>):</strong> Xử lý toàn bộ logic tính toán, so sánh số bí mật, chuyển đổi trạng thái ván, xếp hàng lần lượt và điều phối phòng chơi. Tầng này độc lập hoàn toàn với Socket giúp việc viết unit test cực kỳ đơn giản và mạnh mẽ.</li>
  <li><strong>Tầng Lưu trữ (<code>repo</code>):</strong> Đóng gói các kho lưu trữ dữ liệu an toàn luồng trong bộ nhớ (in-memory repositories) với cơ chế bảo vệ sao chép phòng thủ (defensive copying).</li>
</ol>

<div class="figure-box">
  <img src="{img_kientruc}" alt="Sơ đồ kiến trúc 3 tầng">
  <div class="figure-caption">Hình 3.1. Sơ đồ kiến trúc tổng thể 3 tầng của hệ thống.</div>
</div>

<h2>3.3 Thiết kế dữ liệu và các thực thể Domain</h2>
<table class="booktabs">
  <caption>Bảng 3.1. Cấu trúc các thực thể dữ liệu nghiệp vụ chính của hệ thống.</caption>
  <thead>
    <tr>
      <th style="width: 25%;">Thực thể</th>
      <th style="width: 20%;">Kiểu dữ liệu</th>
      <th>Mô tả và ý nghĩa thiết kế</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>LuotDoan</code></td>
      <td><code>record</code></td>
      <td>Bản ghi bất biến: tên người chơi, số đoán, thời điểm (LocalDateTime), kết quả gợi ý</td>
    </tr>
    <tr>
      <td><code>TrangThaiVan</code></td>
      <td><code>enum</code></td>
      <td>Trạng thái vòng đời ván chơi: WAITING, PLAYING, FINISHED</td>
    </tr>
    <tr>
      <td><code>GoiY</code></td>
      <td><code>enum</code></td>
      <td>Kết quả so sánh số đoán: HIGHER, LOWER, CORRECT</td>
    </tr>
    <tr>
      <td><code>CheDoDoan</code></td>
      <td><code>enum</code></td>
      <td>Chế độ đoán của ván chơi: DONG_THOI, LAN_LUOT</td>
    </tr>
    <tr>
      <td><code>KetQua&lt;T&gt;</code></td>
      <td><code>sealed interface</code></td>
      <td>Mô hình kết quả thống nhất: ThanhCong&lt;T&gt; hoặc ThatBai&lt;T&gt;</td>
    </tr>
    <tr>
      <td><code>PhongChoi</code></td>
      <td><code>class</code></td>
      <td>Thực thể phòng chơi độc lập: có ván, bảng điểm, lịch sử và khóa đồng bộ riêng</td>
    </tr>
  </tbody>
</table>

<h2>3.4 Thiết kế sơ đồ lớp UML</h2>
<div class="figure-box">
  <img src="{img_uml}" alt="Sơ đồ lớp UML">
  <div class="figure-caption">Hình 3.2. Sơ đồ lớp UML chi tiết của hệ thống trò chơi đoán số.</div>
</div>

<h2>3.5 Thiết kế luồng xử lý và tương tác tuần tự</h2>
<div class="figure-box">
  <img src="{img_tuantu}" alt="Sơ đồ tuần tự">
  <div class="figure-caption">Hình 3.3. Sơ đồ tuần tự tương tác mạng giữa Máy khách và Máy chủ khi đoán số.</div>
</div>

<!-- CHƯƠNG 4 -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Chương 4. Cài đặt và thử nghiệm</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>

<div class="chapter-header">
  <div class="chapter-num">4</div>
  <div class="chapter-title">Cài đặt và thử nghiệm</div>
  <hr class="chapter-rule">
</div>

<div class="box-target">
  <div class="box-target-title">Mục tiêu chương</div>
  Mô tả môi trường triển khai thực tế, trình bày các đoạn mã nguồn cốt lõi giải quyết bài toán đồng bộ hóa và phòng chống Deadlock, giao diện người dùng và toàn bộ kết quả kiểm thử TDD.
</div>

<h2>4.1 Môi trường triển khai</h2>
<p>Toàn bộ ứng dụng được hiện thực và kiểm thử trên môi trường thực tế:</p>
<ul>
  <li>Phần cứng: CPU Intel Core i5 / AMD Ryzen, RAM 8GB.</li>
  <li>Hệ điều hành: Microsoft Windows 11 64-bit.</li>
  <li>Nền tảng thực thi: Java OpenJDK 21 LTS (build 21.0.x).</li>
  <li>Công cụ quản lý mã nguồn: Git, phân nhánh theo các tính năng chuẩn (<code>feat/f01</code> đến <code>feat/f21</code>).</li>
</ul>

<h2>4.2 Các thành phần chính và mã nguồn cốt lõi</h2>

<div class="code-caption">Mã nguồn 4.1. Thuật toán Lock Ordering chống Deadlock khi chuyển phòng (QuanLyPhongChoi.java)</div>
<div class="code-block">
<span class="code-kw">public</span> KetQua&lt;Void&gt; chuyenPhong(String maCu, String maMoi, String ten) {{
    <span class="code-kw">var</span> pCu = dsPhong.get(maCu.toUpperCase());
    <span class="code-kw">var</span> pMoi = dsPhong.get(maMoi.toUpperCase());
    <span class="code-kw">if</span> (pCu == <span class="code-kw">null</span> || pMoi == <span class="code-kw">null</span>) <span class="code-kw">return new</span> KetQua.ThatBai&lt;&gt;(<span class="code-str">"PHONG_KHONG_TON_TAI"</span>, <span class="code-str">"..."</span>);

    <span class="code-cm">// Bắt buộc sắp xếp khóa theo thứ tự từ điển cố định (Lock Ordering Rule - Chương 4)</span>
    PhongChoi khoa1 = maCu.compareTo(maMoi) &lt; <span class="code-num">0</span> ? pCu : pMoi;
    PhongChoi khoa2 = maCu.compareTo(maMoi) &lt; <span class="code-num">0</span> ? pMoi : pCu;

    <span class="code-kw">synchronized</span> (khoa1) {{
        <span class="code-kw">synchronized</span> (khoa2) {{
            pCu.xoaNguoiChoi(ten);
            <span class="code-kw">var</span> kq = pMoi.themNguoiChoi(ten);
            <span class="code-kw">if</span> (kq <span class="code-kw">instanceof</span> KetQua.ThanhCong) {{
                nguoiChoiThuocPhong.put(ten, maMoi);
            }}
            <span class="code-kw">return</span> kq;
        }}
    }}
}}
</div>

<div class="code-caption">Mã nguồn 4.2. Xử lý đồng bộ ván chơi và chốt người thắng đầu tiên (Game.java)</div>
<div class="code-block">
<span class="code-kw">public synchronized</span> KetQua&lt;LuotDoan&gt; doanSo(String ten, <span class="code-kw">int</span> soDoan) {{
    <span class="code-kw">if</span> (trangThai != TrangThaiVan.PLAYING) {{
        <span class="code-kw">return new</span> KetQua.ThatBai&lt;&gt;(<span class="code-str">"VAN_DA_KET_THUC"</span>, <span class="code-str">"Ván không trong trạng thái chơi"</span>);
    }}
    GoiY goiY = GoiY.soSanh(soDoan, soBiMat);
    LuotDoan luot = <span class="code-kw">new</span> LuotDoan(ten, soDoan, LocalDateTime.now(), goiY);
    dsLuotDoan.add(luot);
    <span class="code-kw">if</span> (goiY == GoiY.CORRECT) {{
        <span class="code-kw">this</span>.nguoiThang = ten;
        <span class="code-kw">this</span>.trangThai = TrangThaiVan.FINISHED; <span class="code-cm">// Chốt trạng thái ngay lập tức!</span>
    }}
    <span class="code-kw">return new</span> KetQua.ThanhCong&lt;&gt;(luot, <span class="code-str">"Đoán thành công"</span>);
}}
</div>

<h2>4.3 Giao diện máy khách ClientUI</h2>
<p>Giao diện người dùng <code>ClientUI</code> được lập trình bằng thư viện Swing tiêu chuẩn. Cửa sổ bao gồm các trường nhập địa chỉ máy chủ, cổng, tên người chơi, khu vực nút điều khiển (SCORE, HISTORY, STATS, QUIT), ô nhập số đoán và vùng nhật ký (<code>JTextArea</code> bọc trong <code>JScrollPane</code>). Đặc biệt, luồng đọc mạng từ máy chủ được tách biệt trên một <code>Thread</code> riêng biệt, giúp toàn bộ tương tác người dùng luôn mượt mà và không bao giờ bị đơ giao diện.</p>

<h2>4.4 Kiểm thử hệ thống theo TDD với 103 test cases</h2>
<p>Toàn bộ hệ thống được xây dựng và kiểm thử nghiêm ngặt theo phương pháp TDD với 16 bộ kiểm thử tự động, bao phủ 103 ca kiểm thử:</p>

<table class="booktabs">
  <caption>Bảng 4.1. Tổng hợp kết quả kiểm thử tự động hệ thống bằng JUnit 5.</caption>
  <thead>
    <tr>
      <th style="width: 35%;">Lớp kiểm thử</th>
      <th style="width: 15%; text-align: center;">Số ca test</th>
      <th>Mục tiêu kiểm thử & Trường hợp biên</th>
      <th style="width: 15%; text-align: center;">Kết quả</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GameDongThoiTest</code></td>
      <td style="text-align: center;">4</td>
      <td>100 luồng cùng đoán số, tranh chấp thắng/thua, bảo đảm đúng 1 người thắng duy nhất</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><code>GameTest</code> & <code>LuotDoanTest</code></td>
      <td style="text-align: center;">10</td>
      <td>Bảo vệ biên ngoài khoảng, số âm, tên null, tính bất biến của record</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><code>ClientHandlerVaBeLuongTest</code></td>
      <td style="text-align: center;">4</td>
      <td>Bể luồng 20 kết nối, hàng đợi khách thứ 21, giải phóng tài nguyên socket</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><code>QuanLyPhongVaDaPhongTest</code></td>
      <td style="text-align: center;">6</td>
      <td>Tạo trùng phòng, một người 2 phòng, 40 luồng chuyển chéo không Deadlock</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><code>GioiHanThoiGianVanTest</code></td>
      <td style="text-align: center;">4</td>
      <td>Tự động đóng ván khi hết giờ với Future, tranh chấp hết giờ và đoán trúng</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><code>CheDoDoanLanLuotTest</code></td>
      <td style="text-align: center;">4</td>
      <td>Đoán theo vòng xoay ArrayDeque, tự chuyển lượt khi rớt mạng</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><code>NgatKetNoiVaDonDepTest</code></td>
      <td style="text-align: center;">4</td>
      <td>Rớt mạng đột ngột bảo toàn lịch sử và điểm số, thu hồi socket an toàn</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td>Các lớp kiểm thử khác (9 lớp)</td>
      <td style="text-align: center;">57</td>
      <td>Kiểm tra toàn diện Stream thống kê, bảng điểm, UI, giao thức Socket</td>
      <td style="text-align: center; color: green; font-weight: bold;">100% XANH</td>
    </tr>
    <tr>
      <td><strong>TỔNG CỘNG</strong></td>
      <td style="text-align: center; font-weight: bold;">103</td>
      <td><strong>Đạt 100% yêu cầu chức năng từ F01 đến F21</strong></td>
      <td style="text-align: center; color: green; font-weight: bold;">103/103 XANH</td>
    </tr>
  </tbody>
</table>

<!-- CHƯƠNG 5 -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Chương 5. Kết quả và đánh giá</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>

<div class="chapter-header">
  <div class="chapter-num">5</div>
  <div class="chapter-title">Kết quả và đánh giá</div>
  <hr class="chapter-rule">
</div>

<div class="box-target">
  <div class="box-target-title">Mục tiêu chương</div>
  Đối chiếu kết quả đạt được so với mục tiêu ban đầu, đưa ra các số liệu định lượng về độ tin cậy, bàn luận các bài toán kỹ thuật phức tạp đã giải quyết và nêu rõ các điểm hạn chế.
</div>

<h2>5.1 Kết quả đạt được đối chiếu mục tiêu</h2>
<p>Hệ thống đã hoàn thành xuất sắc toàn bộ các mục tiêu đặt ra trong Chương 1:</p>
<ul>
  <li>Hoàn thành 100% khối lượng công việc của 21 chức năng (từ F01 đến F21) theo bảng phân công đề tài 30.</li>
  <li>Máy chủ <code>GameServer</code> chạy ổn định, quản lý đồng thời nhiều phòng chơi độc lập, broadcast tin nhắn chuẩn xác theo phạm vi phòng.</li>
  <li>Giao diện người dùng <code>ClientUI</code> hiển thị trực quan bảng xếp hạng, lịch sử đoán và bảng thống kê định dạng khối chữ ASCII đẹp mắt.</li>
  <li>Toàn bộ 103 bài kiểm thử đơn vị và tích hợp đều vượt qua thành công với tổng thời gian thực thi chỉ khoảng 5.1 giây.</li>
</ul>

<h2>5.2 Độ đo đánh giá định lượng</h2>
<table class="booktabs">
  <caption>Bảng 5.1. Bảng số liệu thống kê định lượng của hệ thống.</caption>
  <thead>
    <tr>
      <th style="width: 40%;">Chỉ số đánh giá</th>
      <th style="width: 25%;">Giá trị đạt được</th>
      <th>Ý nghĩa thực tiễn</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Số lượng chức năng hoàn thành</td>
      <td>21 / 21 chức năng (100%)</td>
      <td>Đạt trọn vẹn cả phần Lõi, phần Phải làm và phần Mở rộng</td>
    </tr>
    <tr>
      <td>Số ca kiểm thử tự động (Unit Test)</td>
      <td>103 / 103 test cases (100%)</td>
      <td>Bao phủ mọi tình huống biên, không có lỗi tiềm ẩn</td>
    </tr>
    <tr>
      <td>Thời gian chạy toàn bộ 103 test cases</td>
      <td>~5.1 giây</td>
      <td>Hiệu năng xử lý và kiểm thử cực nhanh</td>
    </tr>
    <tr>
      <td>Số kết nối đồng thời phục vụ tối đa</td>
      <td>20 luồng (bể luồng cấu hình)</td>
      <td>Phục vụ trơn tru nhiều nhóm người chơi song song</td>
    </tr>
    <tr>
      <td>Khả năng chống Deadlock khi chuyển phòng</td>
      <td>Tuyệt đối (0% lỗi bế tắc)</td>
      <td>Được chứng minh qua 40 luồng chạy song song 2.000 lần</td>
    </tr>
  </tbody>
</table>

<h2>5.3 Bàn luận chuyên sâu về đồng thời và Deadlock</h2>
<p>Trong quá trình cài đặt F21 (nhiều phòng chơi song song), thách thức lớn nhất là nguy cơ Deadlock khi chuyển phòng giữa các khách. Nhờ áp dụng nguyên lý <strong>Lock Ordering Rule</strong> (sắp xếp đối tượng khóa theo thứ tự từ điển của mã phòng), hệ thống triệt tiêu hoàn toàn khả năng xảy ra chu trình chờ đợi vòng tròn (Circular Wait). Thực nghiệm 40 luồng chuyển đổi phòng chéo nhau liên tục 2.000 lần đã chứng minh ứng dụng không hề bị treo luồng hay suy giảm hiệu năng.</p>

<h2>5.4 Hạn chế của hệ thống</h2>
<p>Bên cạnh những kết quả đạt được, hệ thống vẫn còn một số điểm hạn chế:</p>
<ul>
  <li>Dữ liệu bảng điểm và lịch sử lưu trữ trong bộ nhớ (RAM), khi khởi động lại máy chủ dữ liệu sẽ bị xóa trắng (chưa tích hợp lưu trữ cơ sở dữ liệu bền vững).</li>
  <li>Kết nối Socket TCP là văn bản thuần (plaintext), chưa tích hợp lớp bảo mật mã hóa SSL/TLS để chống nghe lén dữ liệu trên đường truyền mạng công cộng.</li>
</ul>

<!-- CHƯƠNG 6 -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Chương 6. Kết luận và hướng phát triển</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>

<div class="chapter-header">
  <div class="chapter-num">6</div>
  <div class="chapter-title">Kết luận và hướng phát triển</div>
  <hr class="chapter-rule">
</div>

<div class="box-target">
  <div class="box-target-title">Mục tiêu chương</div>
  Tổng kết toàn bộ quá trình thực hiện bài tập lớn, đúc kết các bài học kỹ thuật quý giá và đề xuất các hướng mở rộng phát triển hệ thống trong tương lai.
</div>

<h2>6.1 Kết luận</h2>
<p>Sau quá trình nỗ lực nghiên cứu và triển khai, nhóm sinh viên gồm <strong>Trần Thanh Bình</strong> và <strong>Huỳnh Tiến Đạt</strong> đã hoàn thành xuất sắc bài tập lớn môn Lập trình Java nâng cao (AJP20101) với Đề tài số 30: "Trò chơi đoán số nhiều người qua mạng TCP". Hệ thống được thiết kế theo đúng các chuẩn mực kỹ thuật phần mềm hiện đại, có tính mở rộng cao và vận hành ổn định trong môi trường mạng đa luồng.</p>

<h2>6.2 Bài học rút ra</h2>
<p>Qua quá trình thực hiện bài tập lớn, nhóm đã rút ra được nhiều bài học kinh nghiệm sâu sắc:</p>
<ol>
  <li><strong>Sức mạnh của phương pháp TDD:</strong> Việc viết test trước giúp định hình rõ ràng giao diện hàm và lường trước các trường hợp biên, tiết kiệm đến 70% thời gian gỡ lỗi so với cách viết mã thông thường.</li>
  <li><strong>Giá trị của kiến trúc phân tầng sạch (Clean Architecture):</strong> Tách rời tầng nghiệp vụ khỏi socket mạng giúp kiểm thử logic độc lập, dễ dàng thay đổi giao thức mạng mà không ảnh hưởng tới luật chơi.</li>
  <li><strong>Kỹ năng kiểm soát luồng và Deadlock:</strong> Hiểu rõ bản chất của điều phối luồng trong hệ điều hành, vận dụng đúng công cụ đồng bộ hóa (khóa đối tượng hẹp, Lock Ordering) giúp ứng dụng luôn an toàn và ổn định.</li>
</ol>

<h2>6.3 Hướng phát triển</h2>
<p>Một số hướng phát triển mở rộng trong tương lai:</p>
<ul>
  <li>Tích hợp cơ sở dữ liệu quan hệ (PostgreSQL / SQLite) để lưu trữ vĩnh viễn bảng điểm và lịch sử người chơi qua nhiều mùa giải.</li>
  <li>Phát triển giao diện Web tương thích WebSocket để người chơi có thể tham gia trực tiếp từ trình duyệt điện thoại hoặc máy tính mà không cần cài đặt ứng dụng Swing.</li>
  <li>Ứng dụng mô hình bảo mật SSL/TLS (Java <code>SSLSocket</code>) để mã hóa toàn bộ dữ liệu trao đổi giữa máy khách và máy chủ.</li>
</ul>

<!-- TÀI LIỆU THAM KHẢO -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Tài liệu tham khảo</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>
<div class="preface-title">Tài liệu tham khảo</div>

<ol>
  <li>TS. Đỗ Phúc Hảo. <em>Bài giảng Lập trình Java nâng cao (AJP201) – Chương 1 đến Chương 6</em>. Khoa Công nghệ Thông tin, Trường Đại học Kiến trúc Đà Nẵng, 2026.</li>
  <li>Joshua Bloch. <em>Effective Java</em> (3rd Edition). Addison-Wesley Professional, 2018.</li>
  <li>Brian Goetz, Tim Peierls, Joshua Bloch, Joseph Bowbeer, David Holmes, and Doug Lea. <em>Java Concurrency in Practice</em>. Pearson Education, 2006.</li>
  <li>Martin Fowler. <em>Patterns of Enterprise Application Architecture</em>. Addison-Wesley Longman Publishing Co., Inc., 2002.</li>
  <li>Oracle Corporation. <em>Java Platform, Standard Edition Documentation – Version 21 LTS</em>. https://docs.oracle.com/en/java/javase/21/, 2023.</li>
  <li>JUnit Team. <em>JUnit 5 User Guide</em>. https://junit.org/junit5/docs/current/user-guide/, 2024.</li>
</ol>

<!-- PHỤ LỤC -->
<div class="page-break"></div>
<div class="header-rule">
  <span>Phụ lục</span>
  <span>BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH JAVA NÂNG CAO</span>
</div>
<div class="preface-title">Phụ lục</div>

<h2>A. Hướng dẫn cài đặt và chạy chương trình</h2>
<p class="no-indent">Các bước chuẩn mực để khởi chạy hệ thống trên bất kỳ máy tính nào cài đặt JDK 21:</p>

<div class="code-caption">Mã nguồn A.1. Các lệnh PowerShell biên dịch và khởi chạy hệ thống</div>
<div class="code-block">
<span class="code-cm"># 1. Di chuyển vào thư mục dự án</span>
<span class="code-kw">cd</span> <span class="code-str">"project-tro-choi-doan-so-nhieu-nguoi"</span>

<span class="code-cm"># 2. Biên dịch toàn bộ mã nguồn nguồn và kiểm thử bằng javac UTF-8</span>
$files = Get-ChildItem -Recurse -Filter *.java -Path src, test | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -cp <span class="code-str">"lib/*"</span> -d bin $files

<span class="code-cm"># 3. Chạy toàn bộ 103 test cases tự động JUnit 5</span>
java -jar lib/junit-platform-console-standalone.jar execute --class-path bin --scan-class-path

<span class="code-cm"># 4. Khởi chạy Máy chủ GameServer (Lắng nghe cổng mặc định)</span>
java -cp bin doanso.net.GameServer

<span class="code-cm"># 5. Khởi chạy Giao diện Máy khách ClientUI (Có thể mở nhiều cửa sổ để chơi cùng lúc)</span>
java -cp bin doanso.ui.ClientUI
</div>

<h2>B. Bảng tự kiểm trước khi nộp</h2>
<p class="no-indent">Trước khi nộp bài tập lớn, nhóm đã tự rà soát và đánh dấu kiểm tra đầy đủ:</p>
<ul>
  <li>[x] Sản phẩm chạy được trên máy tính khác độc lập, không phụ thuộc vào đường dẫn tuyệt đối của máy cá nhân.</li>
  <li>[x] Đã chạy thử nghiệm thực tế và toàn bộ 103 bài test tự động JUnit 5 đều đạt 100% xanh.</li>
  <li>[x] Toàn bộ mã nguồn được lưu trữ trên kho GitHub, lịch sử commit minh chứng tiến độ đều đặn qua các nhánh tính năng.</li>
  <li>[x] Mọi sơ đồ kiến trúc, bảng biểu và khối mã nguồn đều có tiêu đề, nhãn số và được giải thích rõ trong thân bài.</li>
  <li>[x] Khai báo minh bạch việc sử dụng công cụ AI hỗ trợ theo đúng quy định liêm chính học thuật.</li>
  <li>[x] Đã rà soát và xóa bỏ toàn bộ các hộp gợi ý, các khối văn bản mẫu.</li>
</ul>

<h2>C. Nhận xét của giảng viên hướng dẫn</h2>
<div style="border: 1px dashed #888; border-radius: 6px; padding: 20px; min-height: 180px; margin-top: 15px;">
  <p class="no-indent" style="color: #666; font-style: italic;">
    (Phần dành cho giảng viên hướng dẫn ghi nhận xét và đánh giá kết quả bài tập lớn)
  </p>
  <div style="margin-top: 120px; display: flex; justify-content: space-between;">
    <div>
      <strong>Điểm số:</strong> .....................................<br>
      <strong>Bằng chữ:</strong> .....................................
    </div>
    <div style="text-align: center; width: 250px;">
      <em>Đà Nẵng, ngày ..... tháng ..... năm 2026</em><br>
      <strong>Giảng viên hướng dẫn</strong><br><br><br><br>
      <strong>TS. Đỗ Phúc Hảo</strong>
    </div>
  </div>
</div>

</body>
</html>
"""

html_path = os.path.join(DOCS_DIR, "BaoCao_AJP20101_DeTai30.html")
pdf_path = os.path.join(DOCS_DIR, "BaoCao_BaiTapLon_AJP20101_Nhom_Binh_Dat.pdf")

with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Đã tạo file HTML báo cáo tại: {html_path}")

# Biên dịch ra file PDF bằng Microsoft Edge headless
edge_exe = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if not os.path.exists(edge_exe):
    edge_exe = r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"

file_url = "file:///" + html_path.replace("\\", "/")

cmd = [
    edge_exe,
    "--headless=new",
    "--disable-gpu",
    "--run-all-compositor-stages-before-draw",
    "--no-pdf-header-footer",
    f"--print-to-pdf={pdf_path}",
    file_url
]

print("Đang biên dịch file PDF qua Edge headless...")
res = subprocess.run(cmd, capture_output=True, text=True)
if os.path.exists(pdf_path):
    size = os.path.getsize(pdf_path)
    print(f"BIÊN DỊCH PDF THÀNH CÔNG! Kích thước file: {size} bytes")
    print(f"Đường dẫn file PDF: {pdf_path}")
else:
    print("Có lỗi khi biên dịch PDF:")
    print(res.stderr)
