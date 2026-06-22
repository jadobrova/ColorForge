// color_generator.go - Генератор случайных цветов на Go (CLI)
package main

import (
	"bufio"
	"encoding/json"
	"flag"
	"fmt"
	"math"
	"math/rand"
	"os"
	"strconv"
	"strings"
	"time"
)

// ========== ОСНОВНЫЕ ФУНКЦИИ ==========
type RGB struct {
	R int
	G int
	B int
}

type HSL struct {
	H float64
	S float64
	L float64
}

func randomRGB() RGB {
	return RGB{rand.Intn(256), rand.Intn(256), rand.Intn(256)}
}

func rgbToHex(r, g, b int) string {
	return fmt.Sprintf("#%02x%02x%02x", r, g, b)
}

func hexToRGB(hex string) (RGB, error) {
	var r, g, b int
	_, err := fmt.Sscanf(hex, "#%02x%02x%02x", &r, &g, &b)
	if err != nil {
		return RGB{}, err
	}
	return RGB{r, g, b}, nil
}

func rgbToHSL(r, g, b int) HSL {
	rf := float64(r) / 255.0
	gf := float64(g) / 255.0
	bf := float64(b) / 255.0
	max := math.Max(rf, math.Max(gf, bf))
	min := math.Min(rf, math.Min(gf, bf))
	l := (max + min) / 2
	var h, s float64
	if max == min {
		h, s = 0, 0
	} else {
		d := max - min
		s = d / (1 - math.Abs(2*l-1))
		switch max {
		case rf:
			h = (gf - bf) / d
			if gf < bf {
				h += 6
			}
		case gf:
			h = (bf-rf)/d + 2
		case bf:
			h = (rf-gf)/d + 4
		}
		h *= 60
		if h < 0 {
			h += 360
		}
	}
	return HSL{h, s * 100, l * 100}
}

func hslToRGB(h, s, l float64) RGB {
	h = math.Mod(h, 360)
	s /= 100
	l /= 100
	c := (1 - math.Abs(2*l-1)) * s
	x := c * (1 - math.Abs(math.Mod(h/60, 2)-1))
	m := l - c/2
	var r, g, b float64
	if h < 60 {
		r, g, b = c, x, 0
	} else if h < 120 {
		r, g, b = x, c, 0
	} else if h < 180 {
		r, g, b = 0, c, x
	} else if h < 240 {
		r, g, b = 0, x, c
	} else if h < 300 {
		r, g, b = x, 0, c
	} else {
		r, g, b = c, 0, x
	}
	return RGB{int((r + m) * 255), int((g + m) * 255), int((b + m) * 255)}
}

// ========== ПАЛИТРЫ ==========
func generatePalette(base RGB, scheme string, count int) []RGB {
	hsl := rgbToHSL(base.R, base.G, base.B)
	var colors []RGB
	switch scheme {
	case "mono":
		for i := 0; i < count; i++ {
			factor := float64(i) / float64(count-1)
			if count == 1 {
				factor = 0.5
			}
			newL := math.Max(10, math.Min(90, hsl.L*(0.5+factor)))
			colors = append(colors, hslToRGB(hsl.H, hsl.S, newL))
		}
	case "analog":
		for i := 0; i < count; i++ {
			newH := math.Mod(hsl.H-30+float64(i)*(60/float64(count-1)), 360)
			colors = append(colors, hslToRGB(newH, hsl.S, hsl.L))
		}
	case "comp":
		colors = append(colors, base)
		colors = append(colors, hslToRGB(math.Mod(hsl.H+180, 360), hsl.S, hsl.L))
	case "triad":
		for i := 0; i < 3; i++ {
			newH := math.Mod(hsl.H+float64(i)*120, 360)
			colors = append(colors, hslToRGB(newH, hsl.S, hsl.L))
		}
	}
	return colors
}

// ========== ИСТОРИЯ ==========
type HistoryEntry struct {
	Date   string `json:"date"`
	Color  string `json:"color"`
	RGB    string `json:"rgb,omitempty"`
	HSL    string `json:"hsl,omitempty"`
	Colors []string `json:"colors,omitempty"`
}

const historyFile = "color_history.json"

func loadHistory() []HistoryEntry {
	file, err := os.ReadFile(historyFile)
	if err != nil {
		return []HistoryEntry{}
	}
	var history []HistoryEntry
	json.Unmarshal(file, &history)
	return history
}

func saveHistoryEntry(entry HistoryEntry) {
	history := loadHistory()
	history = append(history, entry)
	if len(history) > 100 {
		history = history[len(history)-100:]
	}
	data, _ := json.MarshalIndent(history, "", "  ")
	os.WriteFile(historyFile, data, 0644)
}

// ========== ЭКСПОРТ ==========
func exportCSS(colors []RGB, filename string) error {
	f, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer f.Close()
	f.WriteString(":root {\n")
	for i, c := range colors {
		hex := rgbToHex(c.R, c.G, c.B)
		f.WriteString(fmt.Sprintf("  --color-%d: %s;\n", i+1, hex))
	}
	f.WriteString("}\n")
	return nil
}

func exportJSON(colors []RGB, filename string) error {
	type ColorData struct {
		R   int    `json:"r"`
		G   int    `json:"g"`
		B   int    `json:"b"`
		Hex string `json:"hex"`
	}
	data := make([]ColorData, len(colors))
	for i, c := range colors {
		data[i] = ColorData{c.R, c.G, c.B, rgbToHex(c.R, c.G, c.B)}
	}
	jsonData, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(filename, jsonData, 0644)
}

func exportText(colors []RGB, filename string) error {
	f, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer f.Close()
	for _, c := range colors {
		f.WriteString(fmt.Sprintf("RGB(%d,%d,%d)  %s\n", c.R, c.G, c.B, rgbToHex(c.R, c.G, c.B)))
	}
	return nil
}

// ========== CLI ==========
func main() {
	rand.Seed(time.Now().UnixNano())

	var (
		hexFlag    bool
		rgbFlag    bool
		hslFlag    bool
		palette    string
		count      int
		history    bool
		exportFmt  string
		output     string
		seed       int64
	)
	flag.BoolVar(&hexFlag, "hex", false, "Вывести HEX")
	flag.BoolVar(&rgbFlag, "rgb", false, "Вывести RGB")
	flag.BoolVar(&hslFlag, "hsl", false, "Вывести HSL")
	flag.StringVar(&palette, "palette", "", "Тип палитры (mono, analog, comp, triad)")
	flag.IntVar(&count, "count", 5, "Количество цветов в палитре")
	flag.BoolVar(&history, "history", false, "Показать историю")
	flag.StringVar(&exportFmt, "export", "", "Формат экспорта (css, json, txt)")
	flag.StringVar(&output, "output", "", "Имя файла для экспорта")
	flag.Int64Var(&seed, "seed", 0, "Seed для воспроизводимости")
	flag.Parse()

	if seed != 0 {
		rand.Seed(seed)
	}

	if history {
		hist := loadHistory()
		if len(hist) == 0 {
			fmt.Println("История пуста.")
		} else {
			fmt.Println("\n📋 ИСТОРИЯ ЦВЕТОВ")
			for _, entry := range hist {
				if len(entry.Colors) > 0 {
					fmt.Printf("%s  Палитра %s\n", entry.Date[:19], entry.Color)
				} else {
					fmt.Printf("%s  %s\n", entry.Date[:19], entry.Color)
				}
			}
		}
		return
	}

	color := randomRGB()
	hsl := rgbToHSL(color.R, color.G, color.B)

	if palette != "" {
		colors := generatePalette(color, palette, count)
		fmt.Printf("\n🎨 Палитра '%s':\n", palette)
		for i, c := range colors {
			fmt.Printf("%d. %s  RGB(%d,%d,%d)\n", i+1, rgbToHex(c.R, c.G, c.B), c.R, c.G, c.B)
		}
		if exportFmt != "" {
			filename := output
			if filename == "" {
				filename = fmt.Sprintf("palette.%s", exportFmt)
			}
			var err error
			switch exportFmt {
			case "css":
				err = exportCSS(colors, filename)
			case "json":
				err = exportJSON(colors, filename)
			default:
				err = exportText(colors, filename)
			}
			if err != nil {
				fmt.Printf("Ошибка экспорта: %v\n", err)
			} else {
				fmt.Printf("Экспортировано в %s\n", filename)
			}
		}
		// Сохранить в историю
		hexColors := make([]string, len(colors))
		for i, c := range colors {
			hexColors[i] = rgbToHex(c.R, c.G, c.B)
		}
		saveHistoryEntry(HistoryEntry{
			Date:   time.Now().Format(time.RFC3339),
			Color:  fmt.Sprintf("Палитра %s", palette),
			Colors: hexColors,
		})
	} else {
		hexColor := rgbToHex(color.R, color.G, color.B)
		if hexFlag || (!rgbFlag && !hslFlag) {
			fmt.Printf("HEX: %s\n", hexColor)
		}
		if rgbFlag || (!hexFlag && !hslFlag) {
			fmt.Printf("RGB: (%d,%d,%d)\n", color.R, color.G, color.B)
		}
		if hslFlag || (!hexFlag && !rgbFlag) {
			fmt.Printf("HSL: (%.1f°, %.1f%%, %.1f%%)\n", hsl.H, hsl.S, hsl.L)
		}
		// Сохранить в историю
		saveHistoryEntry(HistoryEntry{
			Date:  time.Now().Format(time.RFC3339),
			Color: hexColor,
			RGB:   fmt.Sprintf("(%d,%d,%d)", color.R, color.G, color.B),
			HSL:   fmt.Sprintf("(%.1f°, %.1f%%, %.1f%%)", hsl.H, hsl.S, hsl.L),
		})
	}
}
