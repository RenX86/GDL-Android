package com.renx86.gdlapp

import androidx.compose.ui.graphics.Color
import com.renx86.gdlapp.ui.*
import org.junit.Assert.*
import org.junit.Test

class NeoMarkdownTest {

    @Test
    fun testParseHeadings() {
        val markdown = """
            # Title 1
            ## Subtitle 2
            ### Section 3
            #### Detail 4
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading && (blocks[0] as MarkdownBlock.Heading).level == 1)
        assertEquals("Title 1", (blocks[0] as MarkdownBlock.Heading).text)

        assertTrue(blocks[1] is MarkdownBlock.Heading && (blocks[1] as MarkdownBlock.Heading).level == 2)
        assertEquals("Subtitle 2", (blocks[1] as MarkdownBlock.Heading).text)

        assertTrue(blocks[2] is MarkdownBlock.Heading && (blocks[2] as MarkdownBlock.Heading).level == 3)
        assertEquals("Section 3", (blocks[2] as MarkdownBlock.Heading).text)

        assertTrue(blocks[3] is MarkdownBlock.Heading && (blocks[3] as MarkdownBlock.Heading).level == 4)
        assertEquals("Detail 4", (blocks[3] as MarkdownBlock.Heading).text)
    }

    @Test
    fun testParseLists() {
        val markdown = """
            - Item A
            * Item B
            + Item C
            1. Numbered 1
            2. Numbered 2
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)
        assertEquals(5, blocks.size)

        assertTrue(blocks[0] is MarkdownBlock.UnorderedList)
        assertEquals("Item A", (blocks[0] as MarkdownBlock.UnorderedList).text)

        assertTrue(blocks[1] is MarkdownBlock.UnorderedList)
        assertEquals("Item B", (blocks[1] as MarkdownBlock.UnorderedList).text)

        assertTrue(blocks[2] is MarkdownBlock.UnorderedList)
        assertEquals("Item C", (blocks[2] as MarkdownBlock.UnorderedList).text)

        assertTrue(blocks[3] is MarkdownBlock.OrderedList)
        assertEquals("1", (blocks[3] as MarkdownBlock.OrderedList).number)
        assertEquals("Numbered 1", (blocks[3] as MarkdownBlock.OrderedList).text)

        assertTrue(blocks[4] is MarkdownBlock.OrderedList)
        assertEquals("2", (blocks[4] as MarkdownBlock.OrderedList).number)
        assertEquals("Numbered 2", (blocks[4] as MarkdownBlock.OrderedList).text)
    }

    @Test
    fun testParseCodeBlocksAndHorizontalRules() {
        val markdown = """
            ```kotlin
            val x = 10
            println(x)
            ```
            ---
            > This is a quote
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)
        assertEquals(3, blocks.size)

        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
        val codeBlock = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val x = 10\nprintln(x)", codeBlock.code)

        assertTrue(blocks[1] is MarkdownBlock.HorizontalRule)

        assertTrue(blocks[2] is MarkdownBlock.Blockquote)
        assertEquals("This is a quote", (blocks[2] as MarkdownBlock.Blockquote).text)
    }

    @Test
    fun testParseInlineMarkdown() {
        val input = "Here is **bold**, *italic*, `inline code`, [link](https://github.com), and @RenX86 with ~~strike~~."
        val annotated = parseInlineMarkdown(
            rawText = input,
            linkColor = Color.Blue,
            codeBgColor = Color.LightGray
        )

        val plainText = annotated.text
        assertTrue("Plain text should contain bold word", plainText.contains("bold"))
        assertTrue("Plain text should contain italic word", plainText.contains("italic"))
        assertTrue("Plain text should contain inline code", plainText.contains("inline code"))
        assertTrue("Plain text should contain link text", plainText.contains("link"))
        assertTrue("Plain text should contain @RenX86 mention", plainText.contains("@RenX86"))
        assertTrue("Plain text should contain strike", plainText.contains("strike"))

        // Verify markup characters were stripped from visible text
        assertFalse("Should not contain raw markdown **", plainText.contains("**"))
        assertFalse("Should not contain raw markdown `", plainText.contains("`"))
        assertFalse("Should not contain raw link syntax [link]", plainText.contains("[link]"))
        assertFalse("Should not contain raw strikethrough ~~", plainText.contains("~~"))

        // Verify link annotations
        val linkAnnotations = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals("Should have 2 link annotations (markdown link + mention)", 2, linkAnnotations.size)
    }

    @Test
    fun testRealWorldGitHubChangelog() {
        val changelog = """
            ## What's Changed
            * Improved UI Stutters in #1 by @RenX86 in https://github.com/RenX86/GDL-Android/pull/1
            * Fixed `DownloadService` crash by @RenX86

            **Full Changelog**: https://github.com/RenX86/GDL-Android/compare/v0.2.3...v0.2.4
        """.trimIndent()

        val blocks = parseMarkdownBlocks(changelog)
        assertEquals(4, blocks.size)

        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals("What's Changed", (blocks[0] as MarkdownBlock.Heading).text)

        assertTrue(blocks[1] is MarkdownBlock.UnorderedList)
        assertTrue(blocks[2] is MarkdownBlock.UnorderedList)

        assertTrue(blocks[3] is MarkdownBlock.Paragraph)
        val para = blocks[3] as MarkdownBlock.Paragraph
        assertTrue(para.text.startsWith("**Full Changelog**:"))

        val annotated = parseInlineMarkdown(para.text, Color.Blue, Color.Gray)
        val linkAnnotations = annotated.getLinkAnnotations(0, annotated.length)
        assertTrue(linkAnnotations.isNotEmpty())
    }
}
