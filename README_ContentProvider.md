# ContentProvider Tool для Heis2025

## Описание
Универсальный инструмент для работы с ContentProvider приложения Heis2025. Позволяет просматривать, анализировать и управлять данными через командную строку.

## Использование

### Основные команды
```bash
./content_provider_tool.sh [команда] [параметры]
```

### Доступные команды

| Команда | Описание | Пример |
|---------|----------|--------|
| `all` | Показать все записи | `./content_provider_tool.sh all` |
| `active` | Показать только активные записи | `./content_provider_tool.sh active` |
| `deleted` | Показать только удаленные записи | `./content_provider_tool.sh deleted` |
| `stats` | Показать статистику | `./content_provider_tool.sh stats` |
| `schema` | Показать структуру таблицы | `./content_provider_tool.sh schema` |
| `search "текст"` | Поиск по тексту | `./content_provider_tool.sh search "ContentProvider"` |
| `recent [дни]` | Показать записи за последние N дней | `./content_provider_tool.sh recent 7` |
| `test` | Тестировать ContentProvider | `./content_provider_tool.sh test` |
| `clear` | Очистить все записи (ОСТОРОЖНО!) | `./content_provider_tool.sh clear` |
| `help` | Показать справку | `./content_provider_tool.sh help` |

## Требования
- Подключенное Android устройство или эмулятор
- Установленное приложение Heis2025
- ADB (Android Debug Bridge)

## Особенности
- **Безопасность**: ContentProvider защищен разрешениями
- **Логическое удаление**: Записи не удаляются физически, а помечаются флагом `is_deleted`
- **Цветной вывод**: Разные цвета для разных типов информации
- **Проверка подключения**: Автоматическая проверка устройства и приложения

## Примеры использования

### Просмотр статистики
```bash
./content_provider_tool.sh stats
```

### Поиск записей
```bash
./content_provider_tool.sh search "важный текст"
```

### Просмотр записей за последние 3 дня
```bash
./content_provider_tool.sh recent 3
```

### Тестирование ContentProvider
```bash
./content_provider_tool.sh test
```

## Структура данных
Таблица `text_records` содержит следующие поля:
- `_id` - Уникальный идентификатор (INTEGER PRIMARY KEY)
- `text` - Текст записи (TEXT NOT NULL)
- `is_deleted` - Флаг удаления (INTEGER, 0=активная, 1=удаленная)
- `created_at` - Время создания (INTEGER, timestamp в миллисекундах)

## Ручная работа с ContentProvider через ADB

### Просмотр данных через ContentResolver
```bash
# Чтение всех записей (требует разрешения)
adb shell "content query --uri content://com.example.heis2025.provider/text_records"

# Чтение с проекцией полей
adb shell "content query --uri content://com.example.heis2025.provider/text_records --projection _id,text,is_deleted,created_at"

# Чтение с сортировкой
adb shell "content query --uri content://com.example.heis2025.provider/text_records --sort created_at"
```

### Добавление записей
```bash
# Добавление новой записи (требует разрешения)
adb shell "content insert --uri content://com.example.heis2025.provider/text_records --bind text:s:'Тестовая запись' --bind is_deleted:i:0 --bind created_at:l:$(date +%s)000"
```

### Обновление записей
```bash
# Обновление записи по ID (требует разрешения)
adb shell "content update --uri content://com.example.heis2025.provider/text_records/1 --bind text:s:'Обновленный текст'"
```

### Удаление записей
```bash
# Логическое удаление записи по ID (требует разрешения)
adb shell "content delete --uri content://com.example.heis2025.provider/text_records/1"
```

### Прямая работа с базой данных
```bash
# Подключение к базе данных приложения
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db"

# Просмотр всех записей
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db 'SELECT * FROM text_records;'"

# Просмотр только активных записей
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db 'SELECT * FROM text_records WHERE is_deleted = 0;'"

# Просмотр только удаленных записей
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db 'SELECT * FROM text_records WHERE is_deleted = 1;'"

# Статистика
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db 'SELECT COUNT(*) as total, SUM(CASE WHEN is_deleted = 0 THEN 1 ELSE 0 END) as active, SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) as deleted FROM text_records;'"

# Поиск по тексту
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db \"SELECT * FROM text_records WHERE text LIKE '%поиск%';\""

# Просмотр структуры таблицы
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db '.schema text_records'"

# Просмотр записей за последние 7 дней
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db \"SELECT * FROM text_records WHERE created_at > $(($(date +%s) - 7*24*60*60))000;\""
```

### Работа с разрешениями
```bash
# Предоставление разрешений shell пользователю
adb shell "pm grant com.android.shell com.example.heis2025.permission.READ_TEXT_RECORDS"
adb shell "pm grant com.android.shell com.example.heis2025.permission.WRITE_TEXT_RECORDS"

# Проверка разрешений
adb shell "dumpsys package com.android.shell | grep -A 10 'requested permissions'"

# Отзыв разрешений
adb shell "pm revoke com.android.shell com.example.heis2025.permission.READ_TEXT_RECORDS"
adb shell "pm revoke com.android.shell com.example.heis2025.permission.WRITE_TEXT_RECORDS"
```

### Полезные команды для отладки
```bash
# Проверка установленных приложений
adb shell "pm list packages | grep heis2025"

# Проверка ContentProvider
adb shell "dumpsys package com.example.heis2025 | grep -A 5 'ContentProvider'"

# Просмотр логов приложения
adb logcat | grep "com.example.heis2025"

# Проверка процессов приложения
adb shell "ps | grep heis2025"

# Очистка данных приложения
adb shell "pm clear com.example.heis2025"
```

### Форматирование вывода
```bash
# Красивый вывод с заголовками
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db '.headers on' '.mode column' 'SELECT _id, text, is_deleted, datetime(created_at/1000, \"unixepoch\") as created_at FROM text_records ORDER BY created_at DESC;'"

# Экспорт в CSV
adb shell "run-as com.example.heis2025 sqlite3 databases/text_records.db '.mode csv' '.output /data/local/tmp/export.csv' 'SELECT * FROM text_records;'"
adb pull /data/local/tmp/export.csv ./text_records_export.csv
```

## Безопасность
- ContentProvider требует специальные разрешения для доступа
- Команда `clear` требует подтверждения
- Все операции логируются
- Ручные команды ADB также требуют соответствующих разрешений
