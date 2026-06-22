// ColorGenerator.java - Генератор случайных цветов на Java (CLI + Swing GUI)
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.google.gson.*;

public class ColorGenerator {
    // ========== ОСНОВНЫЕ ФУНКЦИИ ==========
    static class RGB {
        int r, g, b;
        RGB(int r, int g, int b) { this.r = r; this.g = g; this.b = b; }
    }
    static class HSL {
        double h, s, l;
        HSL(double h, double s, double l) { this.h = h; this.s = s; this.l = l; }
    }

    static RGB randomRGB() {
        Random rand = new Random();
        return new RGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    static String rgbToHex(int r, int g, int b) {
        return String.format("#%02x%02x%02x", r, g, b);
    }

    static HSL rgbToHSL(int r, int g, int b) {
        double rf = r / 255.0, gf = g / 255.0, bf = b / 255.0;
        double max = Math.max(rf, Math.max(gf, bf));
        double min = Math.min(rf, Math.min(gf, bf));
        double l = (max + min) / 2;
        double h = 0, s = 0;
        if (max != min) {
            double d = max - min;
            s = d / (1 - Math.abs(2*l - 1));
            if (max == rf) {
                h = (gf - bf) / d + (gf < bf ? 6 : 0);
            } else if (max == gf) {
                h = (bf - rf) / d + 2;
            } else {
                h = (rf - gf) / d + 4;
            }
            h *= 60;
            if (h < 0) h += 360;
        }
        return new HSL(h, s*100, l*100);
    }

    static RGB hslToRGB(double h, double s, double l) {
        h = h % 360;
        s /= 100;
        l /= 100;
        double c = (1 - Math.abs(2*l - 1)) * s;
        double x = c * (1 - Math.abs((h/60) % 2 - 1));
        double m = l - c/2;
        double r=0, g=0, b=0;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return new RGB((int)Math.round((r+m)*255), (int)Math.round((g+m)*255), (int)Math.round((b+m)*255));
    }

    // ========== ПАЛИТРЫ ==========
    static List<RGB> generatePalette(RGB base, String scheme, int count) {
        HSL hsl = rgbToHSL(base.r, base.g, base.b);
        List<RGB> colors = new ArrayList<>();
        switch (scheme) {
            case "mono":
                for (int i = 0; i < count; i++) {
                    double factor = count > 1 ? (double)i / (count-1) : 0.5;
                    double newL = Math.max(10, Math.min(90, hsl.l * (0.5 + factor)));
                    colors.add(hslToRGB(hsl.h, hsl.s, newL));
                }
                break;
            case "analog":
                for (int i = 0; i < count; i++) {
                    double newH = (hsl.h - 30 + i * (60.0 / (count-1))) % 360;
                    colors.add(hslToRGB(newH, hsl.s, hsl.l));
                }
                break;
            case "comp":
                colors.add(base);
                colors.add(hslToRGB((hsl.h + 180) % 360, hsl.s, hsl.l));
                break;
            case "triad":
                for (int i = 0; i < 3; i++) {
                    colors.add(hslToRGB((hsl.h + i * 120) % 360, hsl.s, hsl.l));
                }
                break;
        }
        return colors;
    }

    // ========== ИСТОРИЯ ==========
    static class HistoryEntry {
        String date;
        String color;
        String rgb;
        String hsl;
        List<String> colors;
    }

    static final String HISTORY_FILE = "color_history.json";
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static List<HistoryEntry> loadHistory() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(HISTORY_FILE)));
            Type listType = new com.google.gson.reflect.TypeToken<List<HistoryEntry>>(){}.getType();
            return gson.fromJson(content, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    static void saveHistoryEntry(HistoryEntry entry) {
        List<HistoryEntry> history = loadHistory();
        history.add(entry);
        if (history.size() > 100) history = history.subList(history.size()-100, history.size());
        try (FileWriter fw = new FileWriter(HISTORY_FILE)) {
            gson.toJson(history, fw);
        } catch (IOException e) {}
    }

    // ========== ЭКСПОРТ ==========
    static void exportCSS(List<RGB> colors, String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println(":root {");
            for (int i = 0; i < colors.size(); i++) {
                RGB c = colors.get(i);
                pw.printf("  --color-%d: %s;\n", i+1, rgbToHex(c.r, c.g, c.b));
            }
            pw.println("}");
        }
    }

    static void exportJSON(List<RGB> colors, String filename) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        for (RGB c : colors) {
            Map<String, Object> map = new HashMap<>();
            map.put("r", c.r);
            map.put("g", c.g);
            map.put("b", c.b);
            map.put("hex", rgbToHex(c.r, c.g, c.b));
            data.add(map);
        }
        try (FileWriter fw = new FileWriter(filename)) {
            gson.toJson(data, fw);
        }
    }

    static void exportText(List<RGB> colors, String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(filename)) {
            for (RGB c : colors) {
                pw.printf("RGB(%d,%d,%d)  %s\n", c.r, c.g, c.b, rgbToHex(c.r, c.g, c.b));
            }
        }
    }

    // ========== CLI ==========
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--gui")) {
            SwingUtilities.invokeLater(() -> new ColorGeneratorGUI().setVisible(true));
            return;
        }
        // CLI parsing simplified
        boolean showHex = false, showRgb = false, showHsl = false, showHistory = false;
        String palette = null;
        int count = 5;
        String exportFmt = null;
        String output = null;
        Long seed = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--hex": showHex = true; break;
                case "--rgb": showRgb = true; break;
                case "--hsl": showHsl = true; break;
                case "--history": showHistory = true; break;
                case "--palette": palette = args[++i]; break;
                case "--count": count = Integer.parseInt(args[++i]); break;
                case "--export": exportFmt = args[++i]; break;
                case "--output": output = args[++i]; break;
                case "--seed": seed = Long.parseLong(args[++i]); break;
            }
        }
        if (seed != null) {
            new Random(seed); // не используется, но для воспроизводимости можно установить seed в Random
        }
        if (showHistory) {
            List<HistoryEntry> history = loadHistory();
            if (history.isEmpty()) {
                System.out.println("История пуста.");
            } else {
                System.out.println("\n📋 ИСТОРИЯ ЦВЕТОВ");
                for (HistoryEntry e : history) {
                    System.out.printf("%s  %s\n", e.date.substring(0,19), e.color);
                }
            }
            return;
        }
        Random rand = new Random();
        if (seed != null) rand.setSeed(seed);
        RGB color = randomRGB();
        HSL hsl = rgbToHSL(color.r, color.g, color.b);

        if (palette != null) {
            List<RGB> colors = generatePalette(color, palette, count);
            System.out.printf("\n🎨 Палитра '%s':\n", palette);
            for (int i = 0; i < colors.size(); i++) {
                RGB c = colors.get(i);
                System.out.printf("%d. %s  RGB(%d,%d,%d)\n", i+1, rgbToHex(c.r, c.g, c.b), c.r, c.g, c.b);
            }
            if (exportFmt != null) {
                String filename = output != null ? output : "palette." + exportFmt;
                if (exportFmt.equals("css")) exportCSS(colors, filename);
                else if (exportFmt.equals("json")) exportJSON(colors, filename);
                else exportText(colors, filename);
                System.out.println("Экспортировано в " + filename);
            }
            HistoryEntry entry = new HistoryEntry();
            entry.date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            entry.color = "Палитра " + palette;
            entry.colors = new ArrayList<>();
            for (RGB c : colors) entry.colors.add(rgbToHex(c.r, c.g, c.b));
            saveHistoryEntry(entry);
        } else {
            String hexColor = rgbToHex(color.r, color.g, color.b);
            if (showHex || (!showRgb && !showHsl)) System.out.println("HEX: " + hexColor);
            if (showRgb || (!showHex && !showHsl)) System.out.printf("RGB: (%d,%d,%d)\n", color.r, color.g, color.b);
            if (showHsl || (!showHex && !showRgb)) System.out.printf("HSL: (%.1f°, %.1f%%, %.1f%%)\n", hsl.h, hsl.s, hsl.l);
            HistoryEntry entry = new HistoryEntry();
            entry.date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            entry.color = hexColor;
            entry.rgb = String.format("(%d,%d,%d)", color.r, color.g, color.b);
            entry.hsl = String.format("(%.1f°, %.1f%%, %.1f%%)", hsl.h, hsl.s, hsl.l);
            saveHistoryEntry(entry);
        }
    }

    // ========== GUI ==========
    static class ColorGeneratorGUI extends JFrame {
        private JPanel colorPanel;
        private JLabel hexLabel, rgbLabel, hslLabel;
        private JTextArea historyArea;
        private RGB currentColor;

        public ColorGeneratorGUI() {
            setTitle("🎨 Генератор цветов");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(600, 500);
            setLayout(new BorderLayout());

            JPanel top = new JPanel();
            colorPanel = new JPanel();
            colorPanel.setPreferredSize(new Dimension(200, 100));
            colorPanel.setBackground(Color.WHITE);
            top.add(colorPanel);
            JPanel info = new JPanel(new GridLayout(3,1));
            hexLabel = new JLabel("HEX: ");
            rgbLabel = new JLabel("RGB: ");
            hslLabel = new JLabel("HSL: ");
            info.add(hexLabel);
            info.add(rgbLabel);
            info.add(hslLabel);
            top.add(info);
            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel();
            JButton randomBtn = new JButton("🎲 Случайный");
            randomBtn.addActionListener(e -> generateRandom());
            center.add(randomBtn);
            JButton saveBtn = new JButton("💾 Сохранить");
            saveBtn.addActionListener(e -> saveColor());
            center.add(saveBtn);
            JButton historyBtn = new JButton("📋 История");
            historyBtn.addActionListener(e -> showHistory());
            center.add(historyBtn);
            add(center, BorderLayout.CENTER);

            JPanel palettes = new JPanel(new GridLayout(1,4));
            String[] schemes = {"mono", "analog", "comp", "triad"};
            for (String s : schemes) {
                JButton btn = new JButton(s);
                btn.addActionListener(e -> generatePalette(s));
                palettes.add(btn);
            }
            add(palettes, BorderLayout.SOUTH);

            historyArea = new JTextArea(5, 50);
            historyArea.setEditable(false);
            add(new JScrollPane(historyArea), BorderLayout.AFTER_LAST_LINE);

            generateRandom();
            setVisible(true);
        }

        private void generateRandom() {
            Random rand = new Random();
            currentColor = new RGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            updateDisplay(currentColor);
        }

        private void updateDisplay(RGB color) {
            String hex = rgbToHex(color.r, color.g, color.b);
            HSL hsl = rgbToHSL(color.r, color.g, color.b);
            colorPanel.setBackground(Color.decode(hex));
            hexLabel.setText("HEX: " + hex);
            rgbLabel.setText("RGB: (" + color.r + "," + color.g + "," + color.b + ")");
            hslLabel.setText(String.format("HSL: (%.1f°, %.1f%%, %.1f%%)", hsl.h, hsl.s, hsl.l));
        }

        private void saveColor() {
            if (currentColor == null) return;
            HistoryEntry entry = new HistoryEntry();
            entry.date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            entry.color = rgbToHex(currentColor.r, currentColor.g, currentColor.b);
            entry.rgb = String.format("(%d,%d,%d)", currentColor.r, currentColor.g, currentColor.b);
            entry.hsl = String.format("(%.1f°, %.1f%%, %.1f%%)", 
                    rgbToHSL(currentColor.r, currentColor.g, currentColor.b).h,
                    rgbToHSL(currentColor.r, currentColor.g, currentColor.b).s,
                    rgbToHSL(currentColor.r, currentColor.g, currentColor.b).l);
            saveHistoryEntry(entry);
            JOptionPane.showMessageDialog(this, "Сохранено!");
        }

        private void showHistory() {
            List<HistoryEntry> history = loadHistory();
            if (history.isEmpty()) {
                JOptionPane.showMessageDialog(this, "История пуста.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (HistoryEntry e : history) {
                sb.append(e.date.substring(0,19)).append("  ").append(e.color).append("\n");
            }
            historyArea.setText(sb.toString());
        }

        private void generatePalette(String scheme) {
            if (currentColor == null) return;
            List<RGB> colors = generatePalette(currentColor, scheme, 5);
            JDialog dialog = new JDialog(this, "Палитра " + scheme, true);
            dialog.setLayout(new GridLayout(5,1));
            for (RGB c : colors) {
                String hex = rgbToHex(c.r, c.g, c.b);
                JPanel panel = new JPanel();
                panel.setBackground(Color.decode(hex));
                panel.add(new JLabel(hex));
                dialog.add(panel);
            }
            dialog.setSize(300, 400);
            dialog.setVisible(true);
        }
    }
}
