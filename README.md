ColorForge — Генератор случайных цветов на 7 языках
ColorForge — коллекция из семи независимых реализаций генератора случайных цветов. Каждая версия работает на своём языке программирования и предлагает расширенные возможности для работы с цветовыми моделями (HEX, RGB, HSL), генерации палитр, экспорта и визуализации.

✨ Уникальные возможности (общие для всех версий)
🎲 Генерация случайных цветов в форматах HEX (#RRGGBB) и RGB (r,g,b).

🌈 Поддержка цветовой модели HSL (оттенок, насыщенность, светлота) с возможностью генерации по заданным параметрам.

🎨 Создание цветовых палитр:

Монохромная (разные оттенки одного цвета)

Аналоговая (соседние цвета на круге)

Комплементарная (противоположные цвета)

Триадная (равносторонний треугольник)

📋 История сгенерированных цветов с возможностью возврата к предыдущим.

💾 Экспорт палитры в CSS, JSON и текстовый формат.

🖼️ Визуализация цвета (в веб-версиях — live preview, в CLI — цветной вывод).

🔢 Гибкие параметры генерации: фиксированный оттенок, насыщенность или яркость.

🖥️ Интерфейсы:

Командная строка (CLI) с множеством опций.

Интерактивный режим (ввод параметров).

Веб-интерфейс (JavaScript и PHP) с визуальным отображением.

📋 Сравнение реализаций
Язык	CLI	Веб/GUI	Форматы	Палитры	История	Экспорт
Python	✅	GUI (Tkinter)	HEX, RGB, HSL	✅	✅	CSS, JSON
JavaScript	✅	Веб	HEX, RGB, HSL	✅	✅ (localStorage)	CSS, JSON
Go	✅	❌	HEX, RGB, HSL	✅	✅ (файл)	CSS, JSON
Rust	✅	❌	HEX, RGB, HSL	✅	✅ (файл)	CSS, JSON
Java	✅	GUI (Swing)	HEX, RGB, HSL	✅	✅ (файл)	CSS, JSON
C#	✅	GUI (WinForms)	HEX, RGB, HSL	✅	✅ (файл)	CSS, JSON
PHP	✅	Веб	HEX, RGB, HSL	✅	✅ (сессия)	CSS, JSON
🚀 Быстрый старт
Python
bash
# Не требует внешних библиотек (Tkinter встроен)
python color_generator.py
JavaScript (Node.js)
bash
node color_generator.js
Для веб-версии откройте color_generator.html в браузере.

Go
bash
go run color_generator.go
Rust
bash
cargo run
Java
bash
javac ColorGenerator.java && java ColorGenerator
C#
bash
csc ColorGenerator.cs && ColorGenerator.exe
PHP (веб)
bash
php -S localhost:8000
# Откройте http://localhost:8000/color_generator.php
🧪 Пример использования (CLI)
text
🎨 ГЕНЕРАТОР СЛУЧАЙНЫХ ЦВЕТОВ
Команды: random, palette, history, export, help
> random
HEX: #7F4A2B
RGB: (127, 74, 43)
HSL: (22°, 49%, 33%)

> palette --scheme analog --count 5
Цветовая палитра:
#7F4A2B, #7F6A2B, #7F8A2B, #5F7F2B, #3F7F2B
