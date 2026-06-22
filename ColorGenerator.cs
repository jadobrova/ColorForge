// ColorGenerator.cs - Генератор случайных цветов на C# (CLI + WinForms)
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Windows.Forms;

namespace ColorGenerator
{
    public class RGB
    {
        public int R { get; set; }
        public int G { get; set; }
        public int B { get; set; }
        public RGB(int r, int g, int b) { R = r; G = g; B = b; }
    }

    public class HSL
    {
        public double H { get; set; }
        public double S { get; set; }
        public double L { get; set; }
        public HSL(double h, double s, double l) { H = h; S = s; L = l; }
    }

    public static class ColorUtils
    {
        private static Random rand = new Random();

        public static RGB RandomRGB() => new RGB(rand.Next(256), rand.Next(256), rand.Next(256));

        public static string RgbToHex(int r, int g, int b) => $"#{r:X2}{g:X2}{b:X2}";

        public static HSL RgbToHSL(int r, int g, int b)
        {
            double rf = r / 255.0, gf = g / 255.0, bf = b / 255.0;
            double max = Math.Max(rf, Math.Max(gf, bf));
            double min = Math.Min(rf, Math.Min(gf, bf));
            double l = (max + min) / 2;
            double h = 0, s = 0;
            if (max != min)
            {
                double d = max - min;
                s = d / (1 - Math.Abs(2 * l - 1));
                if (max == rf) h = (gf - bf) / d + (gf < bf ? 6 : 0);
                else if (max == gf) h = (bf - rf) / d + 2;
                else h = (rf - gf) / d + 4;
                h *= 60;
                if (h < 0) h += 360;
            }
            return new HSL(h, s * 100, l * 100);
        }

        public static RGB HslToRGB(double h, double s, double l)
        {
            h = h % 360; s /= 100; l /= 100;
            double c = (1 - Math.Abs(2 * l - 1)) * s;
            double x = c * (1 - Math.Abs((h / 60) % 2 - 1));
            double m = l - c / 2;
            double r = 0, g = 0, b = 0;
            if (h < 60) { r = c; g = x; b = 0; }
            else if (h < 120) { r = x; g = c; b = 0; }
            else if (h < 180) { r = 0; g = c; b = x; }
            else if (h < 240) { r = 0; g = x; b = c; }
            else if (h < 300) { r = x; g = 0; b = c; }
            else { r = c; g = 0; b = x; }
            return new RGB((int)Math.Round((r + m) * 255), (int)Math.Round((g + m) * 255), (int)Math.Round((b + m) * 255));
        }

        public static List<RGB> GeneratePalette(RGB baseColor, string scheme, int count)
        {
            HSL hsl = RgbToHSL(baseColor.R, baseColor.G, baseColor.B);
            List<RGB> colors = new List<RGB>();
            switch (scheme)
            {
                case "mono":
                    for (int i = 0; i < count; i++)
                    {
                        double factor = count > 1 ? (double)i / (count - 1) : 0.5;
                        double newL = Math.Max(10, Math.Min(90, hsl.L * (0.5 + factor)));
                        colors.Add(HslToRGB(hsl.H, hsl.S, newL));
                    }
                    break;
                case "analog":
                    for (int i = 0; i < count; i++)
                    {
                        double newH = (hsl.H - 30 + i * (60.0 / (count - 1))) % 360;
                        colors.Add(HslToRGB(newH, hsl.S, hsl.L));
                    }
                    break;
                case "comp":
                    colors.Add(baseColor);
                    colors.Add(HslToRGB((hsl.H + 180) % 360, hsl.S, hsl.L));
                    break;
                case "triad":
                    for (int i = 0; i < 3; i++)
                        colors.Add(HslToRGB((hsl.H + i * 120) % 360, hsl.S, hsl.L));
                    break;
            }
            return colors;
        }
    }

    // ========== ИСТОРИЯ ==========
    public class HistoryEntry
    {
        public string Date { get; set; }
        public string Color { get; set; }
        public string Rgb { get; set; }
        public string Hsl { get; set; }
        public List<string> Colors { get; set; }
    }

    public static class HistoryManager
    {
        private static string file = "color_history.json";

        public static List<HistoryEntry> Load()
        {
            if (File.Exists(file))
            {
                try
                {
                    string json = File.ReadAllText(file);
                    return JsonSerializer.Deserialize<List<HistoryEntry>>(json) ?? new List<HistoryEntry>();
                }
                catch { return new List<HistoryEntry>(); }
            }
            return new List<HistoryEntry>();
        }

        public static void Save(HistoryEntry entry)
        {
            var history = Load();
            history.Add(entry);
            if (history.Count > 100) history = history.Skip(history.Count - 100).ToList();
            string json = JsonSerializer.Serialize(history, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(file, json);
        }
    }

    // ========== ЭКСПОРТ ==========
    public static class Exporter
    {
        public static void ExportCSS(List<RGB> colors, string filename)
        {
            using (var sw = new StreamWriter(filename))
            {
                sw.WriteLine(":root {");
                for (int i = 0; i < colors.Count; i++)
                {
                    var c = colors[i];
                    sw.WriteLine($"  --color-{i+1}: {ColorUtils.RgbToHex(c.R, c.G, c.B)};");
                }
                sw.WriteLine("}");
            }
        }

        public static void ExportJSON(List<RGB> colors, string filename)
        {
            var data = colors.Select(c => new { r = c.R, g = c.G, b = c.B, hex = ColorUtils.RgbToHex(c.R, c.G, c.B) });
            string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(filename, json);
        }

        public static void ExportText(List<RGB> colors, string filename)
        {
            using (var sw = new StreamWriter(filename))
            {
                foreach (var c in colors)
                    sw.WriteLine($"RGB({c.R},{c.G},{c.B})  {ColorUtils.RgbToHex(c.R, c.G, c.B)}");
            }
        }
    }

    // ========== CLI ==========
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length > 0 && args[0] == "--gui")
            {
                Application.EnableVisualStyles();
                Application.Run(new ColorGeneratorGUI());
                return;
            }
            // CLI parsing
            bool showHex = false, showRgb = false, showHsl = false, showHistory = false;
            string palette = null;
            int count = 5;
            string exportFmt = null;
            string output = null;
            int? seed = null;
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--hex": showHex = true; break;
                    case "--rgb": showRgb = true; break;
                    case "--hsl": showHsl = true; break;
                    case "--history": showHistory = true; break;
                    case "--palette": palette = args[++i]; break;
                    case "--count": count = int.Parse(args[++i]); break;
                    case "--export": exportFmt = args[++i]; break;
                    case "--output": output = args[++i]; break;
                    case "--seed": seed = int.Parse(args[++i]); break;
                }
            }
            if (seed.HasValue) new Random(seed.Value);
            if (showHistory)
            {
                var history = HistoryManager.Load();
                if (history.Count == 0) Console.WriteLine("История пуста.");
                else
                {
                    Console.WriteLine("\n📋 ИСТОРИЯ ЦВЕТОВ");
                    foreach (var e in history)
                        Console.WriteLine($"{e.Date.Substring(0,19)}  {e.Color}");
                }
                return;
            }

            var color = ColorUtils.RandomRGB();
            var hsl = ColorUtils.RgbToHSL(color.R, color.G, color.B);

            if (palette != null)
            {
                var colors = ColorUtils.GeneratePalette(color, palette, count);
                Console.WriteLine($"\n🎨 Палитра '{palette}':");
                for (int i = 0; i < colors.Count; i++)
                {
                    var c = colors[i];
                    Console.WriteLine($"{i+1}. {ColorUtils.RgbToHex(c.R, c.G, c.B)}  RGB({c.R},{c.G},{c.B})");
                }
                if (exportFmt != null)
                {
                    string filename = output ?? $"palette.{exportFmt}";
                    if (exportFmt == "css") Exporter.ExportCSS(colors, filename);
                    else if (exportFmt == "json") Exporter.ExportJSON(colors, filename);
                    else Exporter.ExportText(colors, filename);
                    Console.WriteLine($"Экспортировано в {filename}");
                }
                HistoryManager.Save(new HistoryEntry
                {
                    Date = DateTime.Now.ToString("o"),
                    Color = $"Палитра {palette}",
                    Colors = colors.Select(c => ColorUtils.RgbToHex(c.R, c.G, c.B)).ToList()
                });
            }
            else
            {
                string hex = ColorUtils.RgbToHex(color.R, color.G, color.B);
                if (showHex || (!showRgb && !showHsl)) Console.WriteLine($"HEX: {hex}");
                if (showRgb || (!showHex && !showHsl)) Console.WriteLine($"RGB: ({color.R},{color.G},{color.B})");
                if (showHsl || (!showHex && !showRgb)) Console.WriteLine($"HSL: ({hsl.H:F1}°, {hsl.S:F1}%, {hsl.L:F1}%)");
                HistoryManager.Save(new HistoryEntry
                {
                    Date = DateTime.Now.ToString("o"),
                    Color = hex,
                    Rgb = $"({color.R},{color.G},{color.B})",
                    Hsl = $"({hsl.H:F1}°, {hsl.S:F1}%, {hsl.L:F1}%)"
                });
            }
        }
    }

    // ========== GUI ==========
    public class ColorGeneratorGUI : Form
    {
        private Panel colorPanel;
        private Label hexLabel, rgbLabel, hslLabel;
        private TextBox historyBox;
        private RGB currentColor;

        public ColorGeneratorGUI()
        {
            Text = "🎨 Генератор цветов";
            Size = new Size(600, 500);
            StartPosition = FormStartPosition.CenterScreen;

            var top = new FlowLayoutPanel();
            colorPanel = new Panel { Width = 200, Height = 100, BackColor = Color.White };
            top.Controls.Add(colorPanel);
            var info = new TableLayoutPanel { ColumnCount = 1, RowCount = 3 };
            hexLabel = new Label { Text = "HEX: " };
            rgbLabel = new Label { Text = "RGB: " };
            hslLabel = new Label { Text = "HSL: " };
            info.Controls.Add(hexLabel);
            info.Controls.Add(rgbLabel);
            info.Controls.Add(hslLabel);
            top.Controls.Add(info);
            Controls.Add(top);

            var buttons = new FlowLayoutPanel();
            var randomBtn = new Button { Text = "🎲 Случайный" };
            randomBtn.Click += (s, e) => GenerateRandom();
            buttons.Controls.Add(randomBtn);
            var saveBtn = new Button { Text = "💾 Сохранить" };
            saveBtn.Click += (s, e) => SaveColor();
            buttons.Controls.Add(saveBtn);
            var historyBtn = new Button { Text = "📋 История" };
            historyBtn.Click += (s, e) => ShowHistory();
            buttons.Controls.Add(historyBtn);
            Controls.Add(buttons);

            var palettePanel = new FlowLayoutPanel();
            foreach (var scheme in new[] { "mono", "analog", "comp", "triad" })
            {
                var btn = new Button { Text = scheme };
                btn.Click += (s, e) => GeneratePalette(scheme);
                palettePanel.Controls.Add(btn);
            }
            Controls.Add(palettePanel);

            historyBox = new TextBox { Multiline = true, Height = 100, ReadOnly = true, ScrollBars = ScrollBars.Vertical };
            Controls.Add(historyBox);

            GenerateRandom();
        }

        private void GenerateRandom()
        {
            currentColor = ColorUtils.RandomRGB();
            UpdateDisplay(currentColor);
        }

        private void UpdateDisplay(RGB color)
        {
            string hex = ColorUtils.RgbToHex(color.R, color.G, color.B);
            HSL hsl = ColorUtils.RgbToHSL(color.R, color.G, color.B);
            colorPanel.BackColor = Color.FromArgb(color.R, color.G, color.B);
            hexLabel.Text = $"HEX: {hex}";
            rgbLabel.Text = $"RGB: ({color.R},{color.G},{color.B})";
            hslLabel.Text = $"HSL: ({hsl.H:F1}°, {hsl.S:F1}%, {hsl.L:F1}%)";
        }

        private void SaveColor()
        {
            if (currentColor == null) return;
            var entry = new HistoryEntry
            {
                Date = DateTime.Now.ToString("o"),
                Color = ColorUtils.RgbToHex(currentColor.R, currentColor.G, currentColor.B),
                Rgb = $"({currentColor.R},{currentColor.G},{currentColor.B})",
                Hsl = $"({ColorUtils.RgbToHSL(currentColor.R, currentColor.G, currentColor.B).H:F1}°, ...)"
            };
            HistoryManager.Save(entry);
            MessageBox.Show("Сохранено!");
        }

        private void ShowHistory()
        {
            var history = HistoryManager.Load();
            if (history.Count == 0) { MessageBox.Show("История пуста."); return; }
            historyBox.Text = string.Join("\n", history.Select(e => $"{e.Date.Substring(0,19)}  {e.Color}"));
        }

        private void GeneratePalette(string scheme)
        {
            if (currentColor == null) return;
            var colors = ColorUtils.GeneratePalette(currentColor, scheme, 5);
            var form = new Form { Text = $"Палитра {scheme}", Size = new Size(300, 400) };
            var flow = new FlowLayoutPanel { Dock = DockStyle.Fill };
            foreach (var c in colors)
            {
                var panel = new Panel { Width = 250, Height = 50 };
                panel.BackColor = Color.FromArgb(c.R, c.G, c.B);
                panel.Controls.Add(new Label { Text = ColorUtils.RgbToHex(c.R, c.G, c.B), ForeColor = Color.White });
                flow.Controls.Add(panel);
            }
            form.Controls.Add(flow);
            form.ShowDialog();
        }
    }
}
