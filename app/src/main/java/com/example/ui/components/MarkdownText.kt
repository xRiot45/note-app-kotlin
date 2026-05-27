package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 14.sp
) {
    Text(
        text = parseMarkdown(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.5).sp
    )
}

fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = text.split("\n")
    lines.forEachIndexed { idx, line ->
        when {
            line.startsWith("# ") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp))
                builder.append(line.substring(2))
                builder.pop()
            }
            line.startsWith("## ") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                builder.append(line.substring(3))
                builder.pop()
            }
            line.startsWith("### ") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                builder.append(line.substring(4))
                builder.pop()
            }
            line.startsWith("- ") -> {
                builder.append("  •  ")
                builder.append(parseInlineStyles(line.substring(2)))
            }
            line.startsWith("* ") -> {
                builder.append("  •  ")
                builder.append(parseInlineStyles(line.substring(2)))
            }
            else -> {
                builder.append(parseInlineStyles(line))
            }
        }
        if (idx < lines.size - 1) {
            builder.append("\n")
        }
    }
    return builder.toAnnotatedString()
}

private fun parseInlineStyles(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(text.substring(i + 2, end))
                    builder.pop()
                    i = end + 2
                } else {
                    builder.append("**")
                    i += 2
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else {
                    builder.append("*")
                    i += 1
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else {
                    builder.append("`")
                    i += 1
                }
            }
            else -> {
                builder.append(text[i].toString())
                i++
            }
        }
    }
    return builder.toAnnotatedString()
}
