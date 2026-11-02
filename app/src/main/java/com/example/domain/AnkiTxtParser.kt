package com.example.domain

/**
 * Supported field delimiters for Anki TXT/CSV/TSV exports.
 */
enum class AnkiDelimiter(val char: Char?, val russianLabel: String) {
    AUTO(null, "Автоопределение"),
    TAB('\t', "Табуляция (\\t)"),
    SEMICOLON(';', "Точка с запятой (;)"),
    COMMA(',', "Запятая (,)")
}

/**
 * Result of parsing an Anki TXT export file.
 */
data class AnkiParseResult(
    val cards: List<Pair<String, String>>,
    val detectedDelimiter: Char,
    val skippedCommentLines: Int,
    val skippedMalformedRecords: Int
)

/**
 * Robust state-machine parser for Anki TXT exports.
 *
 * Features:
 * - Supports Tab (`\t`), Semicolon (`;`), and Comma (`,`) delimiters, plus `#separator:...` directives.
 * - Skips service/header lines starting with `#` outside quoted fields.
 * - Supports multiline text fields enclosed in double quotes (`"..."`), including escaped `""`.
 * - Maps column 0 -> Front (Question) and column 1 -> Back (Answer), ignoring optional extra tag columns.
 */
object AnkiTxtParser {

    /**
     * Parses raw Anki TXT content into a list of (Front, Back) card pairs.
     */
    fun parse(
        rawContent: String,
        preferredDelimiter: AnkiDelimiter = AnkiDelimiter.AUTO
    ): AnkiParseResult {
        if (rawContent.isBlank()) {
            return AnkiParseResult(
                cards = emptyList(),
                detectedDelimiter = '\t',
                skippedCommentLines = 0,
                skippedMalformedRecords = 0
            )
        }

        // Normalize BOM and CRLF line endings to LF
        val normalized = rawContent
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val delimiter = preferredDelimiter.char ?: detectDelimiter(normalized)

        val parsedCards = mutableListOf<Pair<String, String>>()
        var skippedComments = 0
        var skippedMalformed = 0

        val currentField = StringBuilder()
        val currentRow = mutableListOf<String>()

        var inQuotes = false
        var atLineStart = true
        var i = 0
        val length = normalized.length

        while (i < length) {
            val ch = normalized[i]

            // Handle service/header lines starting with '#' when at the start of a logical record
            if (atLineStart && !inQuotes && currentRow.isEmpty() && currentField.isEmpty()) {
                if (ch == '#') {
                    skippedComments++
                    // Advance to end of line
                    while (i < length && normalized[i] != '\n') {
                        i++
                    }
                    i++ // consume '\n'
                    atLineStart = true
                    continue
                } else if (ch == '\n') {
                    // Blank line between records
                    i++
                    atLineStart = true
                    continue
                }
            }

            atLineStart = false

            when {
                ch == '"' -> {
                    if (inQuotes) {
                        // Check if this is an escaped quote ("") inside a quoted field
                        if (i + 1 < length && normalized[i + 1] == '"') {
                            currentField.append('"')
                            i += 2
                            continue
                        } else {
                            inQuotes = false
                        }
                    } else {
                        // Opening quote (only treat as quote start if field is empty/whitespace)
                        if (currentField.all { it.isWhitespace() }) {
                            currentField.clear()
                            inQuotes = true
                        } else {
                            currentField.append(ch)
                        }
                    }
                }

                ch == delimiter && !inQuotes -> {
                    currentRow.add(cleanField(currentField.toString()))
                    currentField.clear()
                }

                ch == '\n' && !inQuotes -> {
                    currentRow.add(cleanField(currentField.toString()))
                    currentField.clear()

                    if (currentRow.any { it.isNotBlank() }) {
                        if (currentRow.size >= 2) {
                            val front = currentRow[0]
                            val back = currentRow[1]
                            if (front.isNotBlank() && back.isNotBlank()) {
                                parsedCards.add(front to back)
                            } else {
                                skippedMalformed++
                            }
                        } else {
                            skippedMalformed++
                        }
                    }
                    currentRow.clear()
                    atLineStart = true
                }

                else -> {
                    currentField.append(ch)
                }
            }
            i++
        }

        // Flush trailing record if file did not end with a newline
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cleanField(currentField.toString()))
            if (currentRow.any { it.isNotBlank() }) {
                if (currentRow.size >= 2) {
                    val front = currentRow[0]
                    val back = currentRow[1]
                    if (front.isNotBlank() && back.isNotBlank()) {
                        parsedCards.add(front to back)
                    } else {
                        skippedMalformed++
                    }
                } else {
                    skippedMalformed++
                }
            }
        }

        return AnkiParseResult(
            cards = parsedCards,
            detectedDelimiter = delimiter,
            skippedCommentLines = skippedComments,
            skippedMalformedRecords = skippedMalformed
        )
    }

    /**
     * Inspects `#separator:...` headers or the first non-comment line to determine
     * the field delimiter (`\t`, `;`, or `,`).
     */
    fun detectDelimiter(normalizedText: String): Char {
        val lines = normalizedText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("#")) {
                val lower = trimmed.lowercase()
                if (lower.startsWith("#separator:")) {
                    val value = lower.substringAfter("#separator:").trim()
                    return when (value) {
                        "tab", "\\t" -> '\t'
                        "semicolon", ";" -> ';'
                        "comma", "," -> ','
                        else -> '\t'
                    }
                }
                continue
            }
            // Analyze first data line outside quotes
            var inQuotes = false
            var tabs = 0
            var semicolons = 0
            var commas = 0
            for (ch in line) {
                when (ch) {
                    '"' -> inQuotes = !inQuotes
                    '\t' -> if (!inQuotes) tabs++
                    ';' -> if (!inQuotes) semicolons++
                    ',' -> if (!inQuotes) commas++
                }
            }
            return when {
                tabs > 0 -> '\t'
                semicolons > 0 -> ';'
                commas > 0 -> ','
                else -> '\t'
            }
        }
        return '\t'
    }

    private fun cleanField(raw: String): String {
        return raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .trim()
    }

    /**
     * Built-in sample Anki TXT export featuring `#` headers, tab/semicolon options,
     * and multiline double-quoted fields so users can test the parser with one tap.
     */
    const val SAMPLE_ANKI_TXT = """#separator:tab
#html:true
#deck:Компьютерные сети и Протоколы
#columns:Front	Back	Tags
Что такое трехстороннее рукопожатие (Three-Way Handshake) в TCP?	"Процесс установки соединения между клиентом и сервером из 3 шагов:
1. SYN — клиент отправляет начальный порядковый номер (ISN).
2. SYN-ACK — сервер подтверждает SYN и присылает свой ISN.
3. ACK — клиент подтверждает получение SYN-ACK."	network tcp
Чем отличается TCP от UDP?	"• TCP: ориентирован на соединение, гарантирует доставку и порядок пакетов, контролирует перегрузку (Congestion Control).
• UDP: без установления соединения, минимальные накладные расходы, идеален для стриминга, VoIP и онлайн-игр."	network transport
Что происходит при вводе URL в адресную строку браузера?	"1. Поиск IP-адреса через DNS (кэш ОС -> резолвер).
2. Установка TCP-соединения (SYN, SYN-ACK, ACK).
3. TLS Handshake (для HTTPS).
4. Отправка HTTP GET-запроса и рендеринг ответа."	web http
Какой статус-код HTTP означает ""Too Many Requests"" (превышение лимита запросов)?	HTTP 429 Too Many Requests — используется механизмами Rate Limiting.	http api
Что такое Anycast-маршрутизация в CDN и DNS?	Метод адресации, при котором один и тот же IP-адрес анонсируется несколькими узлами по BGP, и трафик направляется к топологически ближайшему серверу.	cdn dns
"""
}
