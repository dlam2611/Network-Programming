package client.ui;

import java.awt.Color;
import java.awt.Font;

/** Màu sắc và font của theme sáng. */
public final class Theme {
    private Theme() { }

    public static final Color MINE_BG = new Color(0x0084FF);    // tin của mình (bên phải)
    public static final Color MINE_TEXT = Color.WHITE;
    public static final Color MINE_TIME = new Color(0xD6E9FF);
    public static final Color OTHER_BG = new Color(0xE9E9EB);   // tin người khác (bên trái)
    public static final Color OTHER_TEXT = new Color(0x222222);
    public static final Color OTHER_TIME = new Color(0x888888);
    public static final Color SIDE_BG = new Color(0xF5F6F8);
    public static final Color BORDER = new Color(0xDDDDDD);

    // Segoe UI hiển thị đủ dấu tiếng Việt
    public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final String[] CANDIDATES = {"Segoe UI", "Segoe UI Emoji", "Arial", Font.DIALOG};

    public static Font bold(float size) { return FONT.deriveFont(Font.BOLD, size); }
    public static Font size(float size) { return FONT.deriveFont(size); }

    /** Chọn font đầu tiên hiển thị được toàn bộ chuỗi (có dấu tiếng Việt và/hoặc emoji). */
    public static Font fontFor(String text, int style, float size) {
        for (String name : CANDIDATES) {
            Font f = new Font(name, style, 14).deriveFont(size);
            if (f.canDisplayUpTo(text) == -1) return f;
        }
        return new Font(Font.DIALOG, style, 14).deriveFont(size);
    }
}