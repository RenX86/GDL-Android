package com.renx86.gdlapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renx86.gdlapp.ui.theme.*

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class UnorderedList(val indent: Int, val text: String) : MarkdownBlock
    data class OrderedList(val indent: Int, val number: String, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Code Block (```)
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith("```")) {
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // 2. Blank Line
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 3. Horizontal Rule (---, ***, ___)
        if (trimmed.matches(Regex("""^(\-{3,}|\*{3,}|_{3,})$"""))) {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // 4. Heading (# to ######)
        val headingMatch = Regex("""^(#{1,6})\s+(.*)$""").find(trimmed)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2].trim()
            blocks.add(MarkdownBlock.Heading(level, text))
            i++
            continue
        }

        // 5. Blockquote (> ...)
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        // 6. Unordered List (*, -, +)
        val ulMatch = Regex("""^(\s*)([\*\-\+])\s+(.*)$""").find(line)
        if (ulMatch != null) {
            val indent = ulMatch.groupValues[1].length / 2
            val text = ulMatch.groupValues[3].trim()
            blocks.add(MarkdownBlock.UnorderedList(indent, text))
            i++
            continue
        }

        // 7. Ordered List (1. or 1))
        val olMatch = Regex("""^(\s*)(\d+)[\.\)]\s+(.*)$""").find(line)
        if (olMatch != null) {
            val indent = olMatch.groupValues[1].length / 2
            val number = olMatch.groupValues[2]
            val text = olMatch.groupValues[3].trim()
            blocks.add(MarkdownBlock.OrderedList(indent, number, text))
            i++
            continue
        }

        // 8. Paragraph
        blocks.add(MarkdownBlock.Paragraph(trimmed))
        i++
    }

    return blocks
}

private val inlineRegex = Regex(
    """(?<codePart>`(?<code>[^`]+)`)""" +
    """|(?<linkPart>\[(?<linkText>[^\]]+)\]\((?<linkUrl>[^)]+)\))""" +
    """|(?<urlPart>https?://[^\s<>"{}|\\^`\[\]()]+)""" +
    """|(?<mentionPart>@(?<user>[a-zA-Z0-9_-]+))""" +
    """|(?<boldItalicPart>\*\*\*(?<boldItalicText>[^*]+)\*\*\*|___(?<boldItalicText2>[^_]+)___)""" +
    """|(?<boldPart>\*\*(?<boldText>[^*]+)\*\*|__(?<boldText2>[^_]+)__)""" +
    """|(?<italicPart>\*(?<italicText>[^*]+)\*|_(?<italicText2>[^_]+)_)""" +
    """|(?<strikePart>~~(?<strikeText>[^~]+)~~)"""
)

fun parseInlineMarkdown(
    rawText: String,
    linkColor: Color,
    codeBgColor: Color,
    onLinkClick: ((String) -> Unit)? = null
): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0

        inlineRegex.findAll(rawText).forEach { match ->
            if (match.range.first > lastIndex) {
                append(rawText.substring(lastIndex, match.range.first))
            }

            val code = match.groups["code"]?.value
            val linkText = match.groups["linkText"]?.value
            val linkUrl = match.groups["linkUrl"]?.value
            val url = match.groups["urlPart"]?.value
            val user = match.groups["user"]?.value
            val boldItalic = match.groups["boldItalicText"]?.value ?: match.groups["boldItalicText2"]?.value
            val bold = match.groups["boldText"]?.value ?: match.groups["boldText2"]?.value
            val italic = match.groups["italicText"]?.value ?: match.groups["italicText2"]?.value
            val strike = match.groups["strikeText"]?.value

            when {
                code != null -> {
                    val start = length
                    append(code)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            background = codeBgColor,
                            fontWeight = FontWeight.Medium
                        ),
                        start,
                        length
                    )
                }

                linkText != null && linkUrl != null -> {
                    val cleanText = linkText.removeSurrounding("**").removeSurrounding("*")
                    val isBold = linkText.startsWith("**") && linkText.endsWith("**")
                    val start = length
                    append(cleanText)
                    addLink(
                        LinkAnnotation.Url(
                            url = linkUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = onLinkClick?.let { onClick ->
                                { onClick(linkUrl) }
                            }
                        ),
                        start,
                        length
                    )
                }

                url != null -> {
                    val start = length
                    append(url)
                    addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = onLinkClick?.let { onClick ->
                                { onClick(url) }
                            }
                        ),
                        start,
                        length
                    )
                }

                user != null -> {
                    val profileUrl = "https://github.com/$user"
                    val start = length
                    append("@$user")
                    addLink(
                        LinkAnnotation.Url(
                            url = profileUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = onLinkClick?.let { onClick ->
                                { onClick(profileUrl) }
                            }
                        ),
                        start,
                        length
                    )
                }

                boldItalic != null -> {
                    val start = length
                    append(boldItalic)
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        start,
                        length
                    )
                }

                bold != null -> {
                    val start = length
                    append(bold)
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start,
                        length
                    )
                }

                italic != null -> {
                    val start = length
                    append(italic)
                    addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        start,
                        length
                    )
                }

                strike != null -> {
                    val start = length
                    append(strike)
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        start,
                        length
                    )
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < rawText.length) {
            append(rawText.substring(lastIndex))
        }
    }
}

@Composable
fun NeoMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = NeoBorder,
    linkColor: Color = NeoTheme.colors.primary,
    codeBgColor: Color = NeoTheme.colors.background,
    codeTextColor: Color = NeoBorder,
    bulletColor: Color = NeoYellow
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val onLinkClick: (String) -> Unit = remember(context, uriHandler) {
        { url ->
            try {
                uriHandler.openUri(url)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    val blocks = remember(content) { parseMarkdownBlocks(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val (fontSize, fontWeight, topPadding) = when (block.level) {
                        1 -> Triple(18.sp, FontWeight.Black, 10.dp)
                        2 -> Triple(16.sp, FontWeight.Black, 8.dp)
                        3 -> Triple(14.5.sp, FontWeight.Bold, 6.dp)
                        else -> Triple(13.5.sp, FontWeight.Bold, 4.dp)
                    }
                    val annotated = remember(block.text, linkColor, codeBgColor) {
                        parseInlineMarkdown(block.text, linkColor, codeBgColor, onLinkClick)
                    }
                    Text(
                        text = annotated,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = textColor,
                        modifier = Modifier.padding(top = topPadding, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.UnorderedList -> {
                    val annotated = remember(block.text, linkColor, codeBgColor) {
                        parseInlineMarkdown(block.text, linkColor, codeBgColor, onLinkClick)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indent * 12).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = bulletColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = annotated,
                            fontSize = 13.5.sp,
                            color = textColor,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                is MarkdownBlock.OrderedList -> {
                    val annotated = remember(block.text, linkColor, codeBgColor) {
                        parseInlineMarkdown(block.text, linkColor, codeBgColor, onLinkClick)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indent * 12).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NeoTextSecondary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = annotated,
                            fontSize = 13.5.sp,
                            color = textColor,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .neoBrutalist(
                                backgroundColor = codeBgColor,
                                borderWidth = 1.5.dp,
                                shadowOffset = 2.dp
                            )
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = codeTextColor,
                            lineHeight = 16.sp
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    val annotated = remember(block.text, linkColor, codeBgColor) {
                        parseInlineMarkdown(block.text, linkColor, codeBgColor, onLinkClick)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .drawBehind {
                                drawLine(
                                    color = bulletColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 10f
                                )
                            }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = annotated,
                            fontSize = 13.sp,
                            color = NeoTextSecondary,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        thickness = 1.5.dp,
                        color = NeoBorder.copy(alpha = 0.25f)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text, linkColor, codeBgColor) {
                        parseInlineMarkdown(block.text, linkColor, codeBgColor, onLinkClick)
                    }
                    Text(
                        text = annotated,
                        fontSize = 13.5.sp,
                        color = textColor,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
