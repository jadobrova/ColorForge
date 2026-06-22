// color_generator.js - Генератор случайных цветов на JavaScript (Node.js CLI + веб)
// CLI: node color_generator.js --hex
// Веб: откройте как HTML

// ========== ОСНОВНЫЕ ФУНКЦИИ ==========
function randomRGB() {
    return {
        r: Math.floor(Math.random() * 256),
        g: Math.floor(Math.random() * 256),
        b: Math.floor(Math.random() * 256)
    };
}

function rgbToHex(r, g, b) {
    return '#' + [r, g, b].map(c => c.toString(16).padStart(2, '0')).join('');
}

function hexToRgb(hex) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

function rgbToHsl(r, g, b) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h, s, l = (max + min) / 2;
    if (max === min) {
        h = s = 0;
    } else {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break;
            case g: h = ((b - r) / d + 2) / 6; break;
            case b: h = ((r - g) / d + 4) / 6; break;
        }
    }
    return { h: h * 360, s: s * 100, l: l * 100 };
}

function hslToRgb(h, s, l) {
    h = h % 360; s /= 100; l /= 100;
    const c = (1 - Math.abs(2 * l - 1)) * s;
    const x = c * (1 - Math.abs((h / 60) % 2 - 1));
    const m = l - c / 2;
    let r, g, b;
    if (h < 60) { r = c; g = x; b = 0; }
    else if (h < 120) { r = x; g = c; b = 0; }
    else if (h < 180) { r = 0; g = c; b = x; }
    else if (h < 240) { r = 0; g = x; b = c; }
    else if (h < 300) { r = x; g = 0; b = c; }
    else { r = c; g = 0; b = x; }
    return { r: Math.round((r + m) * 255), g: Math.round((g + m) * 255), b: Math.round((b + m) * 255) };
}

// ========== ПАЛИТРЫ ==========
function generatePalette(baseRGB, scheme, count = 5) {
    const hsl = rgbToHsl(baseRGB.r, baseRGB.g, baseRGB.b);
    let colors = [];
    switch (scheme) {
        case 'mono':
            for (let i = 0; i < count; i++) {
                const factor = count > 1 ? i / (count - 1) : 0.5;
                const newL = Math.max(10, Math.min(90, hsl.l * (0.5 + factor)));
                colors.push(hslToRgb(hsl.h, hsl.s, newL));
            }
            break;
        case 'analog':
            for (let i = 0; i < count; i++) {
                const newH = (hsl.h - 30 + i * (60 / (count - 1 || 1))) % 360;
                colors.push(hslToRgb(newH, hsl.s, hsl.l));
            }
            break;
        case 'comp':
            colors.push(baseRGB);
            colors.push(hslToRgb((hsl.h + 180) % 360, hsl.s, hsl.l));
            break;
        case 'triad':
            for (let i = 0; i < 3; i++) {
                const newH = (hsl.h + i * 120) % 360;
                colors.push(hslToRgb(newH, hsl.s, hsl.l));
            }
            break;
    }
    return colors;
}

// ========== ИСТОРИЯ (localStorage для веб) ==========
function loadHistory() {
    if (typeof localStorage !== 'undefined') {
        try {
            return JSON.parse(localStorage.getItem('color_history')) || [];
        } catch { return []; }
    }
    return [];
}

function saveHistory(entry) {
    if (typeof localStorage !== 'undefined') {
        let history = loadHistory();
        history.push(entry);
        if (history.length > 100) history = history.slice(-100);
        localStorage.setItem('color_history', JSON.stringify(history));
    }
}

function getHistory() { return loadHistory(); }

// ========== ЭКСПОРТ ==========
function exportCSS(colors, filename) {
    // В Node.js используем fs, в браузере генерируем ссылку для скачивания.
    if (typeof module !== 'undefined' && module.exports) {
        const fs = require('fs');
        let css = ":root {\n";
        colors.forEach((c, i) => {
            const hex = rgbToHex(c.r, c.g, c.b);
            css += `  --color-${i+1}: ${hex};\n`;
        });
        css += "}\n";
        fs.writeFileSync(filename, css);
    } else {
        // Браузер: скачиваем
        let css = ":root {\n";
        colors.forEach((c, i) => {
            const hex = rgbToHex(c.r, c.g, c.b);
            css += `  --color-${i+1}: ${hex};\n`;
        });
        css += "}\n";
        const blob = new Blob([css], {type: 'text/css'});
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename || 'palette.css';
        a.click();
        URL.revokeObjectURL(a.href);
    }
}

function exportJSON(colors, filename) {
    const data = colors.map(c => ({ r: c.r, g: c.g, b: c.b, hex: rgbToHex(c.r, c.g, c.b) }));
    if (typeof module !== 'undefined' && module.exports) {
        const fs = require('fs');
        fs.writeFileSync(filename, JSON.stringify(data, null, 2));
    } else {
        const blob = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename || 'palette.json';
        a.click();
        URL.revokeObjectURL(a.href);
    }
}

function exportText(colors, filename) {
    let txt = '';
    colors.forEach(c => {
        txt += `RGB(${c.r},${c.g},${c.b})  ${rgbToHex(c.r, c.g, c.b)}\n`;
    });
    if (typeof module !== 'undefined' && module.exports) {
        const fs = require('fs');
        fs.writeFileSync(filename, txt);
    } else {
        const blob = new Blob([txt], {type: 'text/plain'});
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename || 'palette.txt';
        a.click();
        URL.revokeObjectURL(a.href);
    }
}

// ========== CLI (Node.js) ==========
if (typeof module !== 'undefined' && require.main === module) {
    const args = process.argv.slice(2);
    let hex = false, rgb = false, hsl = false, palette = null, count = 5, history = false, exportFmt = null, output = null, seed = null;
    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '--hex': hex = true; break;
            case '--rgb': rgb = true; break;
            case '--hsl': hsl = true; break;
            case '--palette': palette = args[++i]; break;
            case '--count': count = parseInt(args[++i]); break;
            case '--history': history = true; break;
            case '--export': exportFmt = args[++i]; break;
            case '--output': output = args[++i]; break;
            case '--seed': seed = parseInt(args[++i]); break;
        }
    }
    if (seed !== null) {
        // Простой seed для Math.random (mulberry32)
        let s = seed;
        const rng = () => {
            s |= 0; s = s + 0x6D2B79F5 | 0;
            let t = Math.imul(s ^ s >>> 15, 1 | s);
            t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
            return ((t ^ t >>> 14) >>> 0) / 4294967296;
        };
        // Заменяем Math.random
        const originalRandom = Math.random;
        Math.random = rng;
    }

    if (history) {
        const hist = loadHistory();
        if (hist.length === 0) console.log('История пуста.');
        else {
            console.log('\n📋 ИСТОРИЯ ЦВЕТОВ');
            hist.slice(-10).forEach(entry => {
                console.log(`${entry.date}  ${entry.color}`);
            });
        }
        process.exit(0);
    }

    const color = randomRGB();
    const hslColor = rgbToHsl(color.r, color.g, color.b);

    if (palette) {
        const colors = generatePalette(color, palette, count);
        console.log(`\n🎨 Палитра '${palette}':`);
        colors.forEach((c, i) => {
            console.log(`${i+1}. ${rgbToHex(c.r, c.g, c.b)}  RGB(${c.r},${c.g},${c.b})`);
        });
        if (exportFmt) {
            const filename = output || `palette.${exportFmt}`;
            if (exportFmt === 'css') exportCSS(colors, filename);
            else if (exportFmt === 'json') exportJSON(colors, filename);
            else exportText(colors, filename);
            console.log(`Экспортировано в ${filename}`);
        }
        // Сохранить в историю
        saveHistory({
            date: new Date().toISOString(),
            color: `Палитра ${palette}`,
            colors: colors.map(c => rgbToHex(c.r, c.g, c.b))
        });
    } else {
        const hexColor = rgbToHex(color.r, color.g, color.b);
        if (hex || (!rgb && !hsl)) console.log(`HEX: ${hexColor}`);
        if (rgb || (!hex && !hsl)) console.log(`RGB: (${color.r},${color.g},${color.b})`);
        if (hsl || (!hex && !rgb)) console.log(`HSL: (${hslColor.h.toFixed(1)}°, ${hslColor.s.toFixed(1)}%, ${hslColor.l.toFixed(1)}%)`);
        saveHistory({
            date: new Date().toISOString(),
            color: hexColor,
            rgb: `(${color.r},${color.g},${color.b})`,
            hsl: `(${hslColor.h.toFixed(1)}°, ${hslColor.s.toFixed(1)}%, ${hslColor.l.toFixed(1)}%)`
        });
    }
    // Восстанавливаем Math.random, если меняли
    if (seed !== null) Math.random = originalRandom;
}

// ========== Браузерная версия ==========
if (typeof window !== 'undefined') {
    window.randomRGB = randomRGB;
    window.rgbToHex = rgbToHex;
    window.hexToRgb = hexToRgb;
    window.rgbToHsl = rgbToHsl;
    window.hslToRgb = hslToRgb;
    window.generatePalette = generatePalette;
    window.loadHistory = loadHistory;
    window.saveHistory = saveHistory;
    window.exportCSS = exportCSS;
    window.exportJSON = exportJSON;
    window.exportText = exportText;
    // Генерация случайного цвета и отображение
    document.addEventListener('DOMContentLoaded', function() {
        const colorBox = document.getElementById('color-box');
        const hexSpan = document.getElementById('hex');
        const rgbSpan = document.getElementById('rgb');
        const hslSpan = document.getElementById('hsl');
        const generateBtn = document.getElementById('generate-btn');
        const paletteBtns = document.querySelectorAll('.palette-btn');

        function updateColor(color) {
            const hex = rgbToHex(color.r, color.g, color.b);
            const hsl = rgbToHsl(color.r, color.g, color.b);
            colorBox.style.backgroundColor = hex;
            hexSpan.textContent = hex;
            rgbSpan.textContent = `(${color.r},${color.g},${color.b})`;
            hslSpan.textContent = `(${hsl.h.toFixed(1)}°, ${hsl.s.toFixed(1)}%, ${hsl.l.toFixed(1)}%)`;
            // Сохранить в историю
            saveHistory({
                date: new Date().toISOString(),
                color: hex,
                rgb: `(${color.r},${color.g},${color.b})`,
                hsl: `(${hsl.h.toFixed(1)}°, ${hsl.s.toFixed(1)}%, ${hsl.l.toFixed(1)}%)`
            });
        }

        generateBtn.addEventListener('click', function() {
            updateColor(randomRGB());
        });

        paletteBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                const scheme = this.dataset.scheme;
                const base = randomRGB();
                const colors = generatePalette(base, scheme, 5);
                // Показываем палитру в отдельном div
                const paletteDiv = document.getElementById('palette-display');
                paletteDiv.innerHTML = '';
                colors.forEach(c => {
                    const hex = rgbToHex(c.r, c.g, c.b);
                    const swatch = document.createElement('div');
                    swatch.style.cssText = `background-color: ${hex}; width: 50px; height: 50px; display: inline-block; margin: 2px; border: 1px solid #ccc;`;
                    paletteDiv.appendChild(swatch);
                });
                // Сохранить палитру в историю
                saveHistory({
                    date: new Date().toISOString(),
                    color: `Палитра ${scheme}`,
                    colors: colors.map(c => rgbToHex(c.r, c.g, c.b))
                });
            });
        });

        // Инициализация случайным цветом
        updateColor(randomRGB());
    });
}
