#!/bin/bash

# Универсальный инструмент для работы с ContentProvider Heis2025
# Использование: ./content_provider_tool.sh [команда] [параметры]

APP_PACKAGE="com.example.heis2025"
DB_PATH="databases/text_records.db"
CONTENT_URI="content://com.example.heis2025.provider/text_records"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функция для проверки подключения
check_connection() {
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}❌ Устройство не подключено!${NC}"
        exit 1
    fi
    
    if ! adb shell pm list packages | grep -q "$APP_PACKAGE"; then
        echo -e "${RED}❌ Приложение $APP_PACKAGE не установлено!${NC}"
        exit 1
    fi
}

# Показать все записи
show_all() {
    echo -e "${BLUE}📊 Все записи:${NC}"
    echo "---------------"
    adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'SELECT _id, text, is_deleted, datetime(created_at/1000, \"unixepoch\") as created_at FROM text_records ORDER BY created_at DESC;'"
}

# Показать только активные записи
show_active() {
    echo -e "${GREEN}✅ Активные записи:${NC}"
    echo "------------------"
    adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'SELECT _id, text, datetime(created_at/1000, \"unixepoch\") as created_at FROM text_records WHERE is_deleted = 0 ORDER BY created_at DESC;'"
}

# Показать только удаленные записи
show_deleted() {
    echo -e "${RED}🗑️ Удаленные записи:${NC}"
    echo "--------------------"
    adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'SELECT _id, text, datetime(created_at/1000, \"unixepoch\") as created_at FROM text_records WHERE is_deleted = 1 ORDER BY created_at DESC;'"
}

# Показать статистику
show_stats() {
    echo -e "${YELLOW}📈 Статистика:${NC}"
    echo "-------------"
    TOTAL=$(adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM text_records;'")
    ACTIVE=$(adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM text_records WHERE is_deleted = 0;'")
    DELETED=$(adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM text_records WHERE is_deleted = 1;'")
    
    echo "Всего записей: $TOTAL"
    echo "Активных: $ACTIVE"
    echo "Удаленных: $DELETED"
}

# Показать структуру таблицы
show_schema() {
    echo -e "${BLUE}📋 Структура таблицы:${NC}"
    echo "---------------------"
    adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH '.schema text_records'"
}

# Поиск по тексту
search_text() {
    local search_term="$1"
    if [ -z "$search_term" ]; then
        echo -e "${RED}❌ Укажите поисковый запрос!${NC}"
        echo "Использование: ./content_provider_tool.sh search \"текст для поиска\""
        exit 1
    fi
    
    echo -e "${BLUE}🔍 Поиск по тексту: '$search_term'${NC}"
    echo "----------------------------------"
    adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH \"SELECT _id, text, is_deleted, datetime(created_at/1000, 'unixepoch') as created_at FROM text_records WHERE text LIKE '%$search_term%' ORDER BY created_at DESC;\""
}

# Показать записи за последние N дней
show_recent() {
    local days="${1:-7}"
    echo -e "${BLUE}📅 Записи за последние $days дней:${NC}"
    echo "--------------------------------"
    local timestamp=$(($(date +%s) - days * 24 * 60 * 60))
    adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH \"SELECT _id, text, is_deleted, datetime(created_at/1000, 'unixepoch') as created_at FROM text_records WHERE created_at > $timestamp ORDER BY created_at DESC;\""
}

# Тестировать ContentProvider через content resolver
test_content_provider() {
    echo -e "${YELLOW}🧪 Тестирование ContentProvider:${NC}"
    echo "--------------------------------"
    
    echo "📖 Чтение данных через content resolver..."
    local query_result=$(adb shell "content query --uri $CONTENT_URI --projection _id,text,is_deleted,created_at" 2>&1)
    if echo "$query_result" | grep -q "Permission Denial"; then
        echo -e "${GREEN}✅ ContentProvider работает! Требуются разрешения (это правильно)${NC}"
    elif echo "$query_result" | grep -q "No result found"; then
        echo -e "${YELLOW}⚠️ ContentProvider доступен, но данных нет${NC}"
    else
        echo "$query_result"
    fi
    
    echo ""
    echo "✏️ Попытка записи данных..."
    local insert_result=$(adb shell "content insert --uri $CONTENT_URI --bind text:s:'Test from script' --bind is_deleted:i:0 --bind created_at:l:$(date +%s)000" 2>&1)
    if echo "$insert_result" | grep -q "Permission Denial"; then
        echo -e "${GREEN}✅ ContentProvider защищен разрешениями (это правильно)${NC}"
    elif echo "$insert_result" | grep -q "Row: 1"; then
        echo -e "${GREEN}✅ Запись успешно добавлена!${NC}"
    else
        echo "Результат: $insert_result"
    fi
}

# Очистить все записи (ОСТОРОЖНО!)
clear_all() {
    echo -e "${RED}⚠️ ВНИМАНИЕ: Это удалит ВСЕ записи!${NC}"
    read -p "Вы уверены? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        adb shell "run-as $APP_PACKAGE sqlite3 $DB_PATH 'DELETE FROM text_records;'"
        echo -e "${GREEN}✅ Все записи удалены${NC}"
    else
        echo "Операция отменена"
    fi
}

# Показать помощь
show_help() {
    echo -e "${BLUE}🔧 Инструмент для работы с ContentProvider Heis2025${NC}"
    echo "=================================================="
    echo ""
    echo "Использование: ./content_provider_tool.sh [команда] [параметры]"
    echo ""
    echo -e "${GREEN}Основные команды:${NC}"
    echo "  all                    - Показать все записи"
    echo "  active                 - Показать только активные записи"
    echo "  deleted                - Показать только удаленные записи"
    echo "  stats                  - Показать статистику"
    echo "  schema                 - Показать структуру таблицы"
    echo "  search \"текст\"         - Поиск по тексту"
    echo "  recent [дни]           - Показать записи за последние N дней"
    echo "  test                   - Тестировать ContentProvider"
    echo "  clear                  - Очистить все записи (ОСТОРОЖНО!)"
    echo "  help                   - Показать эту справку"
    echo ""
    echo -e "${YELLOW}Примеры:${NC}"
    echo "  ./content_provider_tool.sh all"
    echo "  ./content_provider_tool.sh search \"ContentProvider\""
    echo "  ./content_provider_tool.sh recent 3"
    echo "  ./content_provider_tool.sh test"
}

# Основная логика
main() {
    check_connection
    
    case "${1:-help}" in
        "all")
            show_all
            ;;
        "active")
            show_active
            ;;
        "deleted")
            show_deleted
            ;;
        "stats")
            show_stats
            ;;
        "schema")
            show_schema
            ;;
        "search")
            search_text "$2"
            ;;
        "recent")
            show_recent "$2"
            ;;
        "test")
            test_content_provider
            ;;
        "clear")
            clear_all
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

main "$@"
