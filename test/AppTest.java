import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    void testMauKhoiTaoApp() {
        App app = new App();
        assertNotNull(app, "Đối tượng App không được null");
        assertEquals(2, 1 + 1, "Phép cộng mẫu phải đúng");
    }
}
