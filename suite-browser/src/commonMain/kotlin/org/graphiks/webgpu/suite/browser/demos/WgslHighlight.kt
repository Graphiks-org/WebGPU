package org.graphiks.webgpu.suite.browser.demos

internal data class WgslToken(val text: String, val kind: String?)

private val WgslWords = Regex("[A-Za-z_][A-Za-z_0-9]*")
private val WgslAttributes = Regex("@[A-Za-z_][A-Za-z_0-9]*")
private val WgslNumbers = Regex(
    "(?:0[xX][0-9a-fA-F]+(?:\\.[0-9a-fA-F]*)?(?:[pP][+-]?[0-9]+)?|" +
        "(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)[iufh]?",
)
private val WgslTypes = Regex("(?:[fiu](?:16|32)|bool|array|atomic|ptr|" +
    "vec[234][fhiu]?|mat[234]x[234][fh]?|sampler(?:_comparison)?|texture_[A-Za-z_0-9]+)")
private val WgslKeywords = setOf(
    "struct", "fn", "var", "let", "const", "override", "return", "if", "else",
    "for", "while", "loop", "break", "continue", "continuing", "switch", "case",
    "default", "discard", "enable", "requires", "alias", "true", "false",
    "uniform", "storage", "workgroup", "private", "function", "read", "write", "read_write",
)

/** Lexical presentation only: preserve every character, including nested comments and whitespace. */
internal fun highlightWgsl(source: String): List<WgslToken> {
    val tokens = mutableListOf<WgslToken>()
    fun append(text: String, kind: String?) {
        val previous = tokens.lastOrNull()
        if (kind == null && previous?.kind == null && previous != null) {
            tokens[tokens.lastIndex] = WgslToken(previous.text + text, null)
        } else {
            tokens.add(WgslToken(text, kind))
        }
    }
    var offset = 0
    while (offset < source.length) {
        val start = offset
        when {
            source.startsWith("//", offset) -> {
                while (offset < source.length && source[offset] != '\n' && source[offset] != '\r') offset++
                append(source.substring(start, offset), "comment")
            }
            source.startsWith("/*", offset) -> {
                var depth = 1
                offset += 2
                while (offset < source.length && depth > 0) {
                    when {
                        source.startsWith("/*", offset) -> { depth++; offset += 2 }
                        source.startsWith("*/", offset) -> { depth--; offset += 2 }
                        else -> offset++
                    }
                }
                append(source.substring(start, offset), "comment")
            }
            else -> {
                val attribute = WgslAttributes.matchAt(source, offset)
                val number = WgslNumbers.matchAt(source, offset)
                val word = WgslWords.matchAt(source, offset)
                val match = attribute ?: number ?: word
                if (match == null) {
                    append(source[offset++].toString(), null)
                } else {
                    val kind = when {
                        attribute != null -> "attribute"
                        number != null -> "number"
                        match.value in WgslKeywords -> "keyword"
                        WgslTypes.matches(match.value) -> "type"
                        else -> null
                    }
                    append(match.value, kind)
                    offset += match.value.length
                }
            }
        }
    }
    return tokens
}

/** Never parse shader text as HTML; even comment markup is inserted as literal text. */
internal fun cityShaderCode(source: String): DomElement = createElement("code").also { code ->
    for (token in highlightWgsl(source)) {
        code.appendChild(createElement("span").also { span ->
            token.kind?.let { span.setClass("wgsl-$it") }
            span.setText(token.text)
        })
    }
}
