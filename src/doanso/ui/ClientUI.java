package doanso.ui;

import doanso.domain.KetQua;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// Phục vụ F07: Màn hình giao diện máy khách (Swing JFrame), tách luồng đọc mạng riêng giữ tính đáp ứng
public class ClientUI extends JFrame {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread luongDoc;
    private volatile boolean daKetNoi;
    private String tenNguoiChoi;
    private final boolean cheDoDoHoa;

    private final StringBuilder boDemNhatKy = new StringBuilder();

    // Các thành phần giao diện Swing
    private JTextField txtHost;
    private JTextField txtPort;
    private JTextField txtTen;
    private JTextField txtSoDoan;
    private JButton btnKetNoi;
    private JButton btnDoan;
    private JButton btnScore;
    private JButton btnHistory;
    private JButton btnStats;
    private JButton btnThoat;
    private JTextArea txtNhatKy;
    private JLabel lblTrangThai;

    public ClientUI() {
        this(true);
    }

    public ClientUI(boolean cheDoDoHoa) {
        super("Trò chơi đoán số nhiều người - Máy khách");
        this.cheDoDoHoa = cheDoDoHoa;
        this.daKetNoi = false;
        this.tenNguoiChoi = null;

        khoiTaoThanhPhanGiaoDien();

        if (cheDoDoHoa) {
            setSize(700, 500);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }
    }

    private void khoiTaoThanhPhanGiaoDien() {
        txtHost = new JTextField("127.0.0.1", 10);
        txtPort = new JTextField("8888", 5);
        txtTen = new JTextField("NguoiChoi", 10);
        txtSoDoan = new JTextField(8);
        txtSoDoan.setEnabled(false);

        btnKetNoi = new JButton("Kết nối");
        btnDoan = new JButton("Đoán");
        btnDoan.setEnabled(false);
        btnScore = new JButton("Bảng điểm");
        btnScore.setEnabled(false);
        btnHistory = new JButton("Lịch sử");
        btnHistory.setEnabled(false);
        btnStats = new JButton("Thống kê");
        btnStats.setEnabled(false);
        btnThoat = new JButton("Thoát");
        btnThoat.setEnabled(false);

        lblTrangThai = new JLabel("Chưa kết nối");

        txtNhatKy = new JTextArea();
        txtNhatKy.setEditable(false);
        txtNhatKy.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Sự kiện nút Kết nối
        btnKetNoi.addActionListener(e -> {
            String host = txtHost.getText().trim();
            int port;
            try {
                port = Integer.parseInt(txtPort.getText().trim());
            } catch (NumberFormatException ex) {
                hienThongBao("Cổng phải là số nguyên", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String ten = txtTen.getText().trim();
            KetQua<Void> kq = ketNoi(host, port, ten);
            if (kq instanceof KetQua.ThatBai<Void> tb) {
                hienThongBao("Không thể kết nối: " + tb.lyDo(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Sự kiện nút Đoán
        btnDoan.addActionListener(e -> {
            String so = txtSoDoan.getText();
            KetQua<Void> kq = guiSoDoan(so);
            if (kq instanceof KetQua.ThatBai<Void> tb) {
                hienThongBao(tb.lyDo(), "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            } else {
                txtSoDoan.setText("");
            }
        });

        // Sự kiện các nút chức năng
        btnScore.addActionListener(e -> guiLenh("SCORE"));
        btnHistory.addActionListener(e -> guiLenh("HISTORY"));
        btnStats.addActionListener(e -> guiLenh("STATS"));
        btnThoat.addActionListener(e -> dongKetNoi());

        // Bố cục giao diện
        setLayout(new BorderLayout(5, 5));

        // Panel trên: Cấu hình kết nối
        JPanel pnlTren = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTren.setBorder(BorderFactory.createTitledBorder("Kết nối máy chủ"));
        pnlTren.add(new JLabel("Host:"));
        pnlTren.add(txtHost);
        pnlTren.add(new JLabel("Port:"));
        pnlTren.add(txtPort);
        pnlTren.add(new JLabel("Tên:"));
        pnlTren.add(txtTen);
        pnlTren.add(btnKetNoi);
        add(pnlTren, BorderLayout.NORTH);

        // Panel giữa: Nhật ký tin nhắn
        JPanel pnlGiua = new JPanel(new BorderLayout());
        pnlGiua.setBorder(BorderFactory.createTitledBorder("Nhật ký ván chơi & Tin từ máy chủ"));
        pnlGiua.add(new JScrollPane(txtNhatKy), BorderLayout.CENTER);
        add(pnlGiua, BorderLayout.CENTER);

        // Panel dưới: Thao tác đoán và các lệnh
        JPanel pnlDuoi = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel pnlDoan = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlDoan.add(new JLabel("Số đoán:"));
        pnlDoan.add(txtSoDoan);
        pnlDoan.add(btnDoan);
        pnlDoan.add(btnScore);
        pnlDoan.add(btnHistory);
        pnlDoan.add(btnStats);
        pnlDoan.add(btnThoat);

        JPanel pnlStatus = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlStatus.add(new JLabel("Trạng thái: "));
        pnlStatus.add(lblTrangThai);

        pnlDuoi.add(pnlDoan);
        pnlDuoi.add(pnlStatus);
        add(pnlDuoi, BorderLayout.SOUTH);
    }

    public synchronized KetQua<Void> ketNoi(String host, int port, String ten) {
        if (ten == null || ten.isBlank()) {
            return new KetQua.ThatBai<>("TEN_KHONG_HOP_LE", "Tên người chơi không được rỗng");
        }
        if (daKetNoi) {
            return new KetQua.ThatBai<>("DA_KET_NOI", "Đã có kết nối đang mở");
        }
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            this.daKetNoi = true;
            this.tenNguoiChoi = ten.strip();

            // Gửi lệnh JOIN ngay khi kết nối
            out.println("JOIN " + this.tenNguoiChoi);

            // Tách luồng đọc mạng riêng biệt (Chương 4) để giao diện không bị treo
            luongDoc = new Thread(this::luongDocMang, "LuongDocClient-" + this.tenNguoiChoi);
            luongDoc.setDaemon(true);
            luongDoc.start();

            capNhatGiaoDienKhiKetNoi(true);
            themNhatKy("[Hệ thống] Đã kết nối tới " + host + ":" + port + " với tên: " + this.tenNguoiChoi);
            return new KetQua.ThanhCong<>(null, "Kết nối thành công");
        } catch (IOException e) {
            daKetNoi = false;
            themNhatKy("[Lỗi kết nối] Không thể kết nối tới " + host + ":" + port + " (" + e.getMessage() + ")");
            return new KetQua.ThatBai<>("KET_NOI_THAT_BAI", "Không thể kết nối: " + e.getMessage());
        }
    }

    public KetQua<Void> guiSoDoan(String chuoiSo) {
        if (!daKetNoi) {
            return new KetQua.ThatBai<>("CHUA_KET_NOI", "Chưa kết nối tới máy chủ");
        }
        if (chuoiSo == null || chuoiSo.isBlank()) {
            return new KetQua.ThatBai<>("SO_DOAN_RONG", "Số đoán không được để trống");
        }
        try {
            int so = Integer.parseInt(chuoiSo.strip());
            guiLenh("GUESS " + so);
            return new KetQua.ThanhCong<>(null, "Đã gửi số: " + so);
        } catch (NumberFormatException e) {
            return new KetQua.ThatBai<>("SO_DOAN_KHONG_HOP_LE", "Số đoán phải là số nguyên");
        }
    }

    public void guiLenh(String lenh) {
        if (daKetNoi && out != null) {
            synchronized (out) {
                out.println(lenh);
                out.flush();
            }
        }
    }

    private void luongDocMang() {
        try {
            String dong;
            while (daKetNoi && in != null && (dong = in.readLine()) != null) {
                themNhatKy("[Máy chủ] " + dong);
                xuLyTinNhanServer(dong);
            }
        } catch (IOException e) {
            if (daKetNoi) {
                themNhatKy("[MẤT KẾT NỐI] Đứt kết nối tới máy chủ: " + e.getMessage());
            }
        } finally {
            xuLyMatKetNoi();
        }
    }

    private void xuLyTinNhanServer(String dong) {
        if (dong.startsWith("INFO BAT_DAU_VAN")) {
            capNhatTrangThai("Ván mới đã bắt đầu! Hãy đoán số.");
        } else if (dong.startsWith("INFO NGUOI_THANG")) {
            capNhatTrangThai("Ván chơi kết thúc: " + dong);
        } else if (dong.startsWith("HIGHER")) {
            capNhatTrangThai("Số bí mật LỚN HƠN số bạn vừa đoán.");
        } else if (dong.startsWith("LOWER")) {
            capNhatTrangThai("Số bí mật NHỎ HƠN số bạn vừa đoán.");
        } else if (dong.startsWith("WIN")) {
            capNhatTrangThai("CHÚC MỪNG! BẠN ĐÃ ĐOÁN ĐÚNG!");
        }
    }

    private void xuLyMatKetNoi() {
        if (daKetNoi) {
            daKetNoi = false;
            themNhatKy("[Hệ thống] Đã ngắt kết nối với máy chủ.");
            capNhatGiaoDienKhiKetNoi(false);
        }
        dongTaiNguyen();
    }

    public synchronized void dongKetNoi() {
        if (daKetNoi) {
            try {
                guiLenh("QUIT");
            } catch (Exception ignored) {
            }
        }
        daKetNoi = false;
        dongTaiNguyen();
        capNhatGiaoDienKhiKetNoi(false);
        themNhatKy("[Hệ thống] Đã đóng kết nối.");
    }

    private void dongTaiNguyen() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        if (out != null) out.close();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void capNhatGiaoDienKhiKetNoi(boolean ketNoi) {
        Runnable r = () -> {
            btnKetNoi.setEnabled(!ketNoi);
            txtHost.setEnabled(!ketNoi);
            txtPort.setEnabled(!ketNoi);
            txtTen.setEnabled(!ketNoi);

            txtSoDoan.setEnabled(ketNoi);
            btnDoan.setEnabled(ketNoi);
            btnScore.setEnabled(ketNoi);
            btnHistory.setEnabled(ketNoi);
            btnStats.setEnabled(ketNoi);
            btnThoat.setEnabled(ketNoi);

            lblTrangThai.setText(ketNoi ? "Đang kết nối (" + tenNguoiChoi + ")" : "Chưa kết nối");
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void capNhatTrangThai(String trangThai) {
        SwingUtilities.invokeLater(() -> lblTrangThai.setText(trangThai));
    }

    public synchronized void themNhatKy(String dong) {
        boDemNhatKy.append(dong).append("\n");
        SwingUtilities.invokeLater(() -> {
            txtNhatKy.append(dong + "\n");
            txtNhatKy.setCaretPosition(txtNhatKy.getDocument().getLength());
        });
    }

    private void hienThongBao(String noiDung, String tieuDe, int kieuThongBao) {
        if (cheDoDoHoa) {
            JOptionPane.showMessageDialog(this, noiDung, tieuDe, kieuThongBao);
        }
    }

    public boolean daKetNoi() {
        return daKetNoi;
    }

    public String getTenNguoiChoi() {
        return tenNguoiChoi;
    }

    public synchronized String getNoiDungNhatKy() {
        return boDemNhatKy.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientUI::new);
    }
}
