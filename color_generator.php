<?php
// color_generator.php - Генератор случайных цветов на PHP (CLI + веб)
// CLI: php color_generator.php --hex
// Веб: откройте как HTML

// ========== ОСНОВНЫЕ ФУНКЦИИ ==========
function randomRGB() {
    return [rand(0,255), rand(0,255), rand(0,255)];
}

function rgbToHex($r, $g, $b) {
    return sprintf("#%02x%02x%02x", $r, $g, $b);
}

function hexToRgb($hex) {
    $hex = ltrim($hex, '#');
    if (strlen($hex) == 3) {
        $hex = $hex[0].$hex[0].$hex[1].$hex[1].$hex[2].$hex[2];
    }
    return [hexdec(substr($hex,0,2)), hexdec(substr($hex,2,2)), hexdec(substr($hex,4,2))];
}

function rgbToHsl($r, $g, $b) {
    $rf = $r/255; $gf = $g/255; $bf = $b/255;
    $max = max($rf, $gf, $bf);
    $min = min($rf, $gf, $bf);
    $l = ($max + $min) / 2;
    if ($max == $min) {
        $h = $s = 0;
    } else {
        $d = $max - $min;
        $s = $d / (1 - abs(2*$l - 1));
        if ($max == $rf) $h = ($gf - $bf) / $d + ($gf < $bf ? 6 : 0);
        elseif ($max == $gf) $h = ($bf - $rf) / $d + 2;
        else $h = ($rf - $gf) / $d + 4;
        $h *= 60;
        if ($h < 0) $h += 360;
    }
    return [$h, $s*100, $l*100];
}

function hslToRgb($h, $s, $l) {
    $h = fmod($h, 360); $s /= 100; $l /= 100;
    $c = (1 - abs(2*$l - 1)) * $s;
    $x = $c * (1 - abs(fmod($h/60, 2) - 1));
    $m = $l - $c/2;
    if ($h < 60) { $r=$c; $g=$x; $b=0; }
    elseif ($h < 120) { $r=$x; $g=$c; $b=0; }
    elseif ($h < 180) { $r=0; $g=$c; $b=$x; }
    elseif ($h < 240) { $r=0; $g=$x; $b=$c; }
    elseif ($h < 300) { $r=$x; $g=0; $b=$c; }
    else { $r=$c; $g=0; $b=$x; }
    return [(int)round(($r+$m)*255), (int)round(($g+$m)*255), (int)round(($b+$m)*255)];
}

// ========== ПАЛИТРЫ ==========
function generatePalette($base, $scheme, $count = 5) {
    list($r, $g, $b) = $base;
    list($h, $s, $l) = rgbToHsl($r, $g, $b);
    $colors = [];
    switch ($scheme) {
        case 'mono':
            for ($i=0; $i<$count; $i++) {
                $factor = $count > 1 ? $i / ($count-1) : 0.5;
                $newL = max(10, min(90, $l * (0.5 + $factor)));
                $colors[] = hslToRgb($h, $s, $newL);
            }
            break;
        case 'analog':
            for ($i=0; $i<$count; $i++) {
                $newH = fmod($h - 30 + $i * (60 / ($count-1)), 360);
                $colors[] = hslToRgb($newH, $s, $l);
            }
            break;
        case 'comp':
            $colors[] = $base;
            $colors[] = hslToRgb(fmod($h+180, 360), $s, $l);
            break;
        case 'triad':
            for ($i=0; $i<3; $i++) {
                $colors[] = hslToRgb(fmod($h + $i*120, 360), $s, $l);
            }
            break;
    }
    return $colors;
}

// ========== ИСТОРИЯ ==========
$historyFile = 'color_history.json';

function loadHistory() {
    global $historyFile;
    if (file_exists($historyFile)) {
        $data = file_get_contents($historyFile);
        return json_decode($data, true) ?: [];
    }
    return [];
}

function saveHistoryEntry($entry) {
    global $historyFile;
    $history = loadHistory();
    $history[] = $entry;
    if (count($history) > 100) $history = array_slice($history, -100);
    file_put_contents($historyFile, json_encode($history, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

// ========== ЭКСПОРТ ==========
function exportCSS($colors, $filename) {
    $content = ":root {\n";
    foreach ($colors as $i => $c) {
        list($r,$g,$b) = $c;
        $content .= "  --color-".($i+1).": ".rgbToHex($r,$g,$b).";\n";
    }
    $content .= "}\n";
    file_put_contents($filename, $content);
}

function exportJSON($colors, $filename) {
    $data = [];
    foreach ($colors as $c) {
        list($r,$g,$b) = $c;
        $data[] = ['r'=>$r, 'g'=>$g, 'b'=>$b, 'hex'=>rgbToHex($r,$g,$b)];
    }
    file_put_contents($filename, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
}

function exportText($colors, $filename) {
    $content = '';
    foreach ($colors as $c) {
        list($r,$g,$b) = $c;
        $content .= "RGB($r,$g,$b)  ".rgbToHex($r,$g,$b)."\n";
    }
    file_put_contents($filename, $content);
}

// ========== CLI ==========
if (php_sapi_name() === 'cli') {
    $options = getopt("", ["hex", "rgb", "hsl", "history", "palette:", "count:", "export:", "output:", "seed:"]);
    if (isset($options['seed'])) {
        srand((int)$options['seed']);
    }
    if (isset($options['history'])) {
        $history = loadHistory();
        if (empty($history)) {
            echo "История пуста.\n";
        } else {
            echo "\n📋 ИСТОРИЯ ЦВЕТОВ\n";
            foreach (array_slice($history, -10) as $entry) {
                echo $entry['date'] . "  " . $entry['color'] . "\n";
            }
        }
        exit;
    }
    $color = randomRGB();
    list($r, $g, $b) = $color;
    list($h, $s, $l) = rgbToHsl($r, $g, $b);
    $palette = $options['palette'] ?? null;
    $count = isset($options['count']) ? (int)$options['count'] : 5;
    if ($palette) {
        $colors = generatePalette($color, $palette, $count);
        echo "\n🎨 Палитра '$palette':\n";
        foreach ($colors as $i => $c) {
            list($cr,$cg,$cb) = $c;
            echo ($i+1).". ".rgbToHex($cr,$cg,$cb)."  RGB($cr,$cg,$cb)\n";
        }
        if (isset($options['export'])) {
            $filename = $options['output'] ?? "palette.".$options['export'];
            if ($options['export'] == 'css') exportCSS($colors, $filename);
            elseif ($options['export'] == 'json') exportJSON($colors, $filename);
            else exportText($colors, $filename);
            echo "Экспортировано в $filename\n";
        }
        $hexColors = array_map(function($c) { list($r,$g,$b)=$c; return rgbToHex($r,$g,$b); }, $colors);
        saveHistoryEntry([
            'date' => date('c'),
            'color' => "Палитра $palette",
            'colors' => $hexColors
        ]);
    } else {
        $showHex = isset($options['hex']) || (!isset($options['rgb']) && !isset($options['hsl']));
        $showRgb = isset($options['rgb']) || (!isset($options['hex']) && !isset($options['hsl']));
        $showHsl = isset($options['hsl']) || (!isset($options['hex']) && !isset($options['rgb']));
        if ($showHex) echo "HEX: ".rgbToHex($r,$g,$b)."\n";
        if ($showRgb) echo "RGB: ($r,$g,$b)\n";
        if ($showHsl) echo "HSL: (".round($h,1)."°, ".round($s,1)."%, ".round($l,1)."%)\n";
        saveHistoryEntry([
            'date' => date('c'),
            'color' => rgbToHex($r,$g,$b),
            'rgb' => "($r,$g,$b)",
            'hsl' => "(".round($h,1)."°, ".round($s,1)."%, ".round($l,1)."%)"
        ]);
    }
    exit;
}

// ========== ВЕБ-ИНТЕРФЕЙС ==========
?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>🎨 Генератор цветов (PHP)</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f4f4f4; }
        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
        .color-box { width: 100%; height: 150px; border: 1px solid #ccc; margin-bottom: 10px; }
        .info { font-size: 1.2em; }
        button { padding: 8px 16px; margin: 5px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; }
        button:hover { background: #2980b9; }
        .palette { display: flex; flex-wrap: wrap; }
        .swatch { width: 50px; height: 50px; margin: 2px; border: 1px solid #ccc; }
    </style>
</head>
<body>
<div class="container">
    <h1>🎨 Генератор цветов (PHP)</h1>
    <div class="color-box" id="colorBox" style="background-color: #ffffff;"></div>
    <div class="info" id="colorInfo">HEX: #ffffff</div>
    <div>
        <button onclick="generateColor()">🎲 Случайный</button>
        <button onclick="saveColor()">💾 Сохранить</button>
        <button onclick="showHistory()">📋 История</button>
    </div>
    <div>
        <button onclick="generatePalette('mono')">Монохромная</button>
        <button onclick="generatePalette('analog')">Аналоговая</button>
        <button onclick="generatePalette('comp')">Комплементарная</button>
        <button onclick="generatePalette('triad')">Триадная</button>
    </div>
    <div id="paletteDisplay" class="palette"></div>
    <pre id="historyDisplay" style="max-height:200px; overflow:auto; border:1px solid #ccc; padding:10px; margin-top:10px;"></pre>
</div>
<script>
    function updateColor(r,g,b) {
        const hex = '#' + [r,g,b].map(c => c.toString(16).padStart(2,'0')).join('');
        document.getElementById('colorBox').style.backgroundColor = hex;
        document.getElementById('colorInfo').textContent = `HEX: ${hex}  RGB: (${r},${g},${b})`;
        return hex;
    }

    function generateColor() {
        fetch('?action=random')
            .then(res => res.json())
            .then(data => {
                updateColor(data.r, data.g, data.b);
            });
    }

    function saveColor() {
        const hex = document.getElementById('colorInfo').textContent.split(' ')[1];
        fetch('?action=save&color=' + encodeURIComponent(hex))
            .then(() => alert('Сохранено!'));
    }

    function showHistory() {
        fetch('?action=history')
            .then(res => res.json())
            .then(data => {
                document.getElementById('historyDisplay').textContent = data.map(e => e.date + '  ' + e.color).join('\n');
            });
    }

    function generatePalette(scheme) {
        fetch('?action=palette&scheme=' + scheme)
            .then(res => res.json())
            .then(data => {
                const container = document.getElementById('paletteDisplay');
                container.innerHTML = '';
                data.colors.forEach(c => {
                    const div = document.createElement('div');
                    div.className = 'swatch';
                    div.style.backgroundColor = c;
                    container.appendChild(div);
                });
            });
    }

    // Загружаем случайный цвет при загрузке
    generateColor();
</script>
</body>
</html>
