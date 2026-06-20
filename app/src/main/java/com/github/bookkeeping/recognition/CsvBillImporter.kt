package com.github.bookkeeping.recognition

object CsvBillImporter {
    fun parseRows(text: String): List<RecognitionCandidate> {
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("#") }
            .dropWhile { looksLikeHeader(it) }
            .mapNotNull { line -> PaymentTextParser.parseStructuredRow(splitCsv(line)) ?: PaymentTextParser.parse(null, null, line) }
            .toList()
    }

    /**
     * 仅当某行包含表头特征词、且整行不含可解析为金额的数字时，才视为表头。
     * 这样可避免把首条数据（如"购物,36.00"）误判为表头而被丢弃。
     */
    private fun looksLikeHeader(line: String): Boolean {
        val normalized = line.lowercase()
        val hasHeaderWord = normalized.contains("amount") || normalized.contains("金额") ||
            normalized.contains("merchant") || normalized.contains("商户") ||
            normalized.contains("时间") || normalized.contains("time") ||
            normalized.contains("方向") || normalized.contains("direction")
        if (!hasHeaderWord) return false
        // 表头行不应包含具体金额
        return !line.contains(Regex("""\d+\.\d{1,2}""")) && !line.contains(Regex("""\d{4,}"""))
    }

    private fun splitCsv(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    cells += current.toString().trim()
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        cells += current.toString().trim()
        return cells
    }
}
