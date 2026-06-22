// color_generator.rs - Генератор случайных цветов на Rust (CLI)
use clap::{Arg, App};
use rand::Rng;
use rand::SeedableRng;
use rand::rngs::StdRng;
use serde::{Serialize, Deserialize};
use std::fs;
use std::io::{self, Write};
use std::time::{SystemTime, UNIX_EPOCH};

// ========== ОСНОВНЫЕ ФУНКЦИИ ==========
#[derive(Debug, Clone, Copy)]
struct RGB {
    r: u8,
    g: u8,
    b: u8,
}

#[derive(Debug)]
struct HSL {
    h: f64,
    s: f64,
    l: f64,
}

fn random_rgb(rng: &mut impl Rng) -> RGB {
    RGB {
        r: rng.gen(),
        g: rng.gen(),
        b: rng.gen(),
    }
}

fn rgb_to_hex(r: u8, g: u8, b: u8) -> String {
    format!("#{:02x}{:02x}{:02x}", r, g, b)
}

fn rgb_to_hsl(r: u8, g: u8, b: u8) -> HSL {
    let rf = r as f64 / 255.0;
    let gf = g as f64 / 255.0;
    let bf = b as f64 / 255.0;
    let max = rf.max(gf).max(bf);
    let min = rf.min(gf).min(bf);
    let l = (max + min) / 2.0;
    let (h, s) = if max == min {
        (0.0, 0.0)
    } else {
        let d = max - min;
        let s = d / (1.0 - (2.0 * l - 1.0).abs());
        let h = if max == rf {
            (gf - bf) / d + if gf < bf { 6.0 } else { 0.0 }
        } else if max == gf {
            (bf - rf) / d + 2.0
        } else {
            (rf - gf) / d + 4.0
        } * 60.0;
        (h % 360.0, s * 100.0)
    };
    HSL { h, s, l: l * 100.0 }
}

fn hsl_to_rgb(h: f64, s: f64, l: f64) -> RGB {
    let h = h % 360.0;
    let s = s / 100.0;
    let l = l / 100.0;
    let c = (1.0 - (2.0 * l - 1.0).abs()) * s;
    let x = c * (1.0 - ((h / 60.0) % 2.0 - 1.0).abs());
    let m = l - c / 2.0;
    let (r, g, b) = if h < 60.0 {
        (c, x, 0.0)
    } else if h < 120.0 {
        (x, c, 0.0)
    } else if h < 180.0 {
        (0.0, c, x)
    } else if h < 240.0 {
        (0.0, x, c)
    } else if h < 300.0 {
        (x, 0.0, c)
    } else {
        (c, 0.0, x)
    };
    RGB {
        r: ((r + m) * 255.0).round() as u8,
        g: ((g + m) * 255.0).round() as u8,
        b: ((b + m) * 255.0).round() as u8,
    }
}

// ========== ПАЛИТРЫ ==========
fn generate_palette(base: RGB, scheme: &str, count: usize) -> Vec<RGB> {
    let hsl = rgb_to_hsl(base.r, base.g, base.b);
    let mut colors = Vec::new();
    match scheme {
        "mono" => {
            for i in 0..count {
                let factor = if count > 1 { i as f64 / (count - 1) as f64 } else { 0.5 };
                let new_l = (10.0f64).max((90.0f64).min(hsl.l * (0.5 + factor)));
                colors.push(hsl_to_rgb(hsl.h, hsl.s, new_l));
            }
        }
        "analog" => {
            for i in 0..count {
                let new_h = (hsl.h - 30.0 + i as f64 * (60.0 / (count - 1) as f64)) % 360.0;
                colors.push(hsl_to_rgb(new_h, hsl.s, hsl.l));
            }
        }
        "comp" => {
            colors.push(base);
            colors.push(hsl_to_rgb((hsl.h + 180.0) % 360.0, hsl.s, hsl.l));
        }
        "triad" => {
            for i in 0..3 {
                let new_h = (hsl.h + i as f64 * 120.0) % 360.0;
                colors.push(hsl_to_rgb(new_h, hsl.s, hsl.l));
            }
        }
        _ => {}
    }
    colors
}

// ========== ИСТОРИЯ ==========
#[derive(Serialize, Deserialize, Clone)]
struct HistoryEntry {
    date: String,
    color: String,
    rgb: Option<String>,
    hsl: Option<String>,
    colors: Option<Vec<String>>,
}

const HISTORY_FILE: &str = "color_history.json";

fn load_history() -> Vec<HistoryEntry> {
    if let Ok(data) = fs::read_to_string(HISTORY_FILE) {
        if let Ok(entries) = serde_json::from_str(&data) {
            return entries;
        }
    }
    Vec::new()
}

fn save_history_entry(entry: HistoryEntry) {
    let mut history = load_history();
    history.push(entry);
    if history.len() > 100 {
        history = history[history.len()-100..].to_vec();
    }
    let json = serde_json::to_string_pretty(&history).unwrap();
    fs::write(HISTORY_FILE, json).unwrap();
}

// ========== ЭКСПОРТ ==========
fn export_css(colors: &[RGB], filename: &str) -> Result<(), Box<dyn std::error::Error>> {
    let mut content = String::from(":root {\n");
    for (i, c) in colors.iter().enumerate() {
        let hex = rgb_to_hex(c.r, c.g, c.b);
        content.push_str(&format!("  --color-{}: {};\n", i+1, hex));
    }
    content.push_str("}\n");
    fs::write(filename, content)?;
    Ok(())
}

fn export_json(colors: &[RGB], filename: &str) -> Result<(), Box<dyn std::error::Error>> {
    #[derive(Serialize)]
    struct ColorData {
        r: u8,
        g: u8,
        b: u8,
        hex: String,
    }
    let data: Vec<ColorData> = colors.iter().map(|c| ColorData {
        r: c.r,
        g: c.g,
        b: c.b,
        hex: rgb_to_hex(c.r, c.g, c.b),
    }).collect();
    let json = serde_json::to_string_pretty(&data)?;
    fs::write(filename, json)?;
    Ok(())
}

fn export_text(colors: &[RGB], filename: &str) -> Result<(), Box<dyn std::error::Error>> {
    let mut content = String::new();
    for c in colors {
        content.push_str(&format!("RGB({},{},{})  {}\n", c.r, c.g, c.b, rgb_to_hex(c.r, c.g, c.b)));
    }
    fs::write(filename, content)?;
    Ok(())
}

// ========== CLI ==========
fn main() {
    let matches = App::new("Color Generator")
        .arg(Arg::with_name("hex").long("hex").help("Вывести HEX"))
        .arg(Arg::with_name("rgb").long("rgb").help("Вывести RGB"))
        .arg(Arg::with_name("hsl").long("hsl").help("Вывести HSL"))
        .arg(Arg::with_name("palette").long("palette").takes_value(true).help("Тип палитры (mono, analog, comp, triad)"))
        .arg(Arg::with_name("count").long("count").takes_value(true).default_value("5").help("Количество цветов в палитре"))
        .arg(Arg::with_name("history").long("history").help("Показать историю"))
        .arg(Arg::with_name("export").long("export").takes_value(true).help("Формат экспорта (css, json, txt)"))
        .arg(Arg::with_name("output").long("output").takes_value(true).help("Имя файла для экспорта"))
        .arg(Arg::with_name("seed").long("seed").takes_value(true).help("Seed для воспроизводимости"))
        .get_matches();

    let seed: u64 = matches.value_of("seed").unwrap_or("0").parse().unwrap_or(0);
    let mut rng: Box<dyn Rng> = if seed != 0 {
        Box::new(StdRng::seed_from_u64(seed))
    } else {
        Box::new(rand::thread_rng())
    };

    if matches.is_present("history") {
        let history = load_history();
        if history.is_empty() {
            println!("История пуста.");
        } else {
            println!("\n📋 ИСТОРИЯ ЦВЕТОВ");
            for entry in history.iter().take(10) {
                println!("{}  {}", &entry.date[..19], entry.color);
            }
        }
        return;
    }

    let color = random_rgb(&mut *rng);
    let hsl = rgb_to_hsl(color.r, color.g, color.b);

    if let Some(scheme) = matches.value_of("palette") {
        let count: usize = matches.value_of("count").unwrap().parse().unwrap_or(5);
        let colors = generate_palette(color, scheme, count);
        println!("\n🎨 Палитра '{}':", scheme);
        for (i, c) in colors.iter().enumerate() {
            println!("{}. {}  RGB({},{},{})", i+1, rgb_to_hex(c.r, c.g, c.b), c.r, c.g, c.b);
        }
        if let Some(export_fmt) = matches.value_of("export") {
            let filename = matches.value_of("output").unwrap_or(&format!("palette.{}", export_fmt));
            let result = match export_fmt {
                "css" => export_css(&colors, filename),
                "json" => export_json(&colors, filename),
                _ => export_text(&colors, filename),
            };
            if let Err(e) = result {
                println!("Ошибка экспорта: {}", e);
            } else {
                println!("Экспортировано в {}", filename);
            }
        }
        // Сохранить в историю
        let hex_colors: Vec<String> = colors.iter().map(|c| rgb_to_hex(c.r, c.g, c.b)).collect();
        save_history_entry(HistoryEntry {
            date: chrono::Local::now().to_rfc3339(),
            color: format!("Палитра {}", scheme),
            rgb: None,
            hsl: None,
            colors: Some(hex_colors),
        });
    } else {
        let hex_color = rgb_to_hex(color.r, color.g, color.b);
        let show_hex = matches.is_present("hex") || (!matches.is_present("rgb") && !matches.is_present("hsl"));
        let show_rgb = matches.is_present("rgb") || (!matches.is_present("hex") && !matches.is_present("hsl"));
        let show_hsl = matches.is_present("hsl") || (!matches.is_present("hex") && !matches.is_present("rgb"));
        if show_hex {
            println!("HEX: {}", hex_color);
        }
        if show_rgb {
            println!("RGB: ({},{},{})", color.r, color.g, color.b);
        }
        if show_hsl {
            println!("HSL: ({:.1}°, {:.1}%, {:.1}%)", hsl.h, hsl.s, hsl.l);
        }
        save_history_entry(HistoryEntry {
            date: chrono::Local::now().to_rfc3339(),
            color: hex_color,
            rgb: Some(format!("({},{},{})", color.r, color.g, color.b)),
            hsl: Some(format!("({:.1}°, {:.1}%, {:.1}%)", hsl.h, hsl.s, hsl.l)),
            colors: None,
        });
    }
}
