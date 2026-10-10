package org.graphiks.webgpu.suite.browser.demos

import kotlin.test.Test
import kotlin.test.assertEquals

class WgslHighlightTest {
    @Test fun highlightsWgslWithoutChangingSourceIncludingWhitespace() {
        val source = "@group(0) var<uniform> frame: vec4<f32>;\n\n// keep <img onerror=bad> literal\n"
        val tokens = highlightWgsl(source)
        assertEquals(source, tokens.joinToString("") { it.text })
        assertEquals(listOf("@group"), tokens.filter { it.kind == "attribute" }.map { it.text })
        assertEquals(listOf("var", "uniform"), tokens.filter { it.kind == "keyword" }.map { it.text })
        assertEquals(listOf("vec4", "f32"), tokens.filter { it.kind == "type" }.map { it.text })
        assertEquals(listOf("0"), tokens.filter { it.kind == "number" }.map { it.text })
        assertEquals(listOf("// keep <img onerror=bad> literal"), tokens.filter { it.kind == "comment" }.map { it.text })
    }

    @Test fun nestedCommentsDoNotHighlightTheirContentsAsCode() {
        val source = "/* outer @group /* inner 12u */ fn */\nlet value = 1.5e-2f + 0xffu;"
        val tokens = highlightWgsl(source)
        assertEquals(source, tokens.joinToString("") { it.text })
        assertEquals(listOf("/* outer @group /* inner 12u */ fn */"),
            tokens.filter { it.kind == "comment" }.map { it.text })
        assertEquals(listOf("let"), tokens.filter { it.kind == "keyword" }.map { it.text })
        assertEquals(listOf("1.5e-2f", "0xffu"), tokens.filter { it.kind == "number" }.map { it.text })
        assertEquals(emptyList(), highlightWgsl(""))
        val unfinished = "/* unfinished\n@vertex fn"
        assertEquals(listOf(WgslToken(unfinished, "comment")), highlightWgsl(unfinished))
    }
}
