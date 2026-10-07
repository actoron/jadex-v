package jadex.micro.llmcall2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Adds token-colored syntax highlighting to the fenced code
 * blocks of Swing-Markdown-Preview HTML output, so the chat
 * view shows colored code without a Swing view rewrite.
 *
 *  The post-processor works on the already-rendered HTML: it
 *  finds <pre><code class="language-X"> blocks, unescapes the
 *  code, tokenizes it headlessly via RSyntaxDocument (no Swing
 *  component, no background thread, state-correct per line),
 *  and wraps colored token types in <span style="color:#...">
 *  elements. Inline styles (not CSS classes) are required
 *  because Swing's HTMLEditorKit does not apply class-selector
 *  colors at render time.
 *
 *  Blocks without a recognized language tag are passed through
 *  unchanged.
 */
public final class CodeHighlighter
{
	//----- token colors (light theme; pre background #f6f8fa, text #1a1a2e) -----

	private static final String KW		= "#cf222e"; // keywords, functions
	private static final String NUM		= "#0550ae"; // numeric literals
	private static final String STR		= "#0a3069"; // string/char literals
	private static final String COM		= "#6e7781"; // comments
	private static final String TYP		= "#953800"; // data types
	private static final String ANN		= "#8250df"; // annotations
	private static final String TAG		= "#22863a"; // markup tags
	private static final String ERR		= "#cf222e"; // malformed tokens

	// matches <pre><code [class="language-X"]>...</code></pre> as emitted
	// by SwingMarkdownRenderer; group 1 = language (null if untagged)
	private static final Pattern PRE_CODE = Pattern.compile(
		"<pre><code(?:\\s+class=\"language-([a-zA-Z0-9+#]+)\")?>\\s*\\n?(.*?)\\n?\\s*</code></pre>",
		Pattern.DOTALL);

	private CodeHighlighter() {}

	/**
	 * Replace every recognized fenced code block in the HTML by a
	 * token-colored version. Blocks with an unknown language tag
	 * (or no tag) are left unchanged.
	 * @param html The Swing-compatible HTML from the Markdown renderer.
	 * @return The HTML with highlighted code blocks.
	 */
	public static String highlightBlocks(String html)
	{
		Matcher	matcher	= PRE_CODE.matcher(html);
		StringBuilder out	= new StringBuilder();
		int			last	= 0;
		while (matcher.find())
		{
			out.append(html, last, matcher.start());
			String lang	= matcher.group(1);
			String style	= styleFor(lang);
			if (style == null)
			{
				// untagged or unknown language: pass through unchanged
				out.append(matcher.group(0));
			}
			else
			{
				String highlighted = highlight(style, unescape(matcher.group(2)));
				if (highlighted == null)
				{
					// tokenization failed: pass through unchanged
					out.append(matcher.group(0));
				}
				else
				{
					out.append("<pre><code class=\"language-").append(lang).append("\">");
					out.append(highlighted);
					out.append("</code></pre>");
				}
			}
			last = matcher.end();
		}
		out.append(html.substring(last));
		return out.toString();
	}

	/**
	 * Token-color the (unescaped) source code of one language.
	 * @param style The RSyntaxTextArea syntax style key, e.g. "text/java".
	 * @param code The raw source code.
	 * @return The highlighted HTML, or null if tokenization failed.
	 */
	private static String highlight(String style, String code)
	{
		code = code.replace("\r\n", "\n").replace('\r', '\n');
		try
		{
			// headless, thread-free document: plain data object, safe on the EDT
			// and outside the EDT alike; one instance per block, no shared state
			RSyntaxDocument	doc	= new RSyntaxDocument(style);
			doc.insertString(0, code, null);
			StringBuilder	out	= new StringBuilder();
			int			lines	= code.split("\n", -1).length;
			for (int li = 0; li < lines; li++)
			{
				if (li > 0)
					out.append('\n');
				// per-line token list: stateful tokenizers (multi-line
				// comments, heredocs) keep correct state without leaking
				for (Token t = doc.getTokenListForLine(li); t != null; t = t.getNextToken())
				{
					String lexeme = t.getLexeme();
					if (lexeme == null)
						continue; // terminal token
					String color = colorFor(t.getType());
					if (color != null)
						out.append("<span style=\"color:").append(color).append("\">");
					out.append(escape(lexeme));
					if (color != null)
						out.append("</span>");
				}
			}
			return out.toString();
		}
		catch (Exception e)
		{
			// cosmetic feature: degrade to uncolored code, never break the chat view
			return null;
		}
	}

	/**
	 * Map a fenced-code language tag to an RSyntaxTextArea syntax
	 * style key.
	 * @param lang The language tag from the Markdown fence, or null.
	 * @return The syntax style key, or null if not supported.
	 */
	private static String styleFor(String lang)
	{
		if (lang == null)
			return null;
		switch (lang.toLowerCase())
		{
			case "java":		return "text/java";
			case "python":
			case "py":		return "text/python";
			case "c":			return "text/c";
			case "cpp":
			case "c++":
			case "cxx":
			case "cc":
			case "h":
			case "hpp":
			case "h++":		return "text/cpp";
			case "javascript":
			case "js":
			case "mjs":		return "text/javascript";
			case "json":		return "text/json";
			case "xml":		return "text/xml";
			case "html":
			case "htm":		return "text/html";
			case "css":		return "text/css";
			case "sql":		return "text/sql";
			case "bash":
			case "sh":
			case "shell":
			case "shellscript":
			case "zsh":		return "text/unix";
			case "markdown":
			case "md":		return "text/markdown";
			case "ruby":
			case "rb":		return "text/ruby";
			case "go":
			case "golang":	return "text/golang";
			case "kotlin":
			case "kt":		return "text/kotlin";
			case "typescript":
			case "ts":		return "text/typescript";
			case "rust":
			case "rs":		return "text/rust";
			case "csharp":
			case "cs":
			case "c#":		return "text/cs";
			case "yaml":
			case "yml":		return "text/yaml";
			case "php":		return "text/php";
			case "lua":		return "text/lua";
			case "scala":		return "text/scala";
			case "swift":		return "text/plain";
			case "dockerfile":
			case "docker":	return "text/dockerfile";
			case "makefile":
			case "make":		return "text/makefile";
			case "powershell":
			case "ps1":		return "text/powershell";
			case "fortran":	return "text/fortran";
			case "perl":
			case "pl":		return "text/perl";
			case "dart":		return "text/dart";
			default:		return null;
		}
	}

	/**
	 * Map a token type to a color. Null leaves the token in the
	 * default text color.
	 * @param type The RSyntaxTextArea token type.
	 * @return The hex color, or null.
	 */
	private static String colorFor(int type)
	{
		switch (type)
		{
			case TokenTypes.RESERVED_WORD:
			case TokenTypes.RESERVED_WORD_2:
			case TokenTypes.LITERAL_BOOLEAN:
			case TokenTypes.PREPROCESSOR:
			case TokenTypes.FUNCTION:
				return KW;
			case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
			case TokenTypes.LITERAL_NUMBER_FLOAT:
			case TokenTypes.LITERAL_NUMBER_HEXADECIMAL:
				return NUM;
			case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
			case TokenTypes.LITERAL_CHAR:
			case TokenTypes.LITERAL_BACKQUOTE:
				return STR;
			case TokenTypes.COMMENT_EOL:
			case TokenTypes.COMMENT_MULTILINE:
			case TokenTypes.COMMENT_DOCUMENTATION:
			case TokenTypes.COMMENT_KEYWORD:
			case TokenTypes.COMMENT_MARKUP:
				return COM;
			case TokenTypes.DATA_TYPE:
				return TYP;
			case TokenTypes.ANNOTATION:
				return ANN;
			case TokenTypes.MARKUP_CDATA:
			case TokenTypes.MARKUP_CDATA_DELIMITER:
			case TokenTypes.MARKUP_DTD:
			case TokenTypes.MARKUP_ENTITY_REFERENCE:
			case TokenTypes.MARKUP_PROCESSING_INSTRUCTION:
			case TokenTypes.MARKUP_TAG_DELIMITER:
			case TokenTypes.MARKUP_TAG_NAME:
			case TokenTypes.MARKUP_TAG_ATTRIBUTE:
			case TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE:
			case TokenTypes.MARKUP_COMMENT:
				return TAG;
			case TokenTypes.ERROR_IDENTIFIER:
			case TokenTypes.ERROR_NUMBER_FORMAT:
			case TokenTypes.ERROR_STRING_DOUBLE:
			case TokenTypes.ERROR_CHAR:
				return ERR;
			default:
				return null; // whitespace, newlines, identifiers, operators
		}
	}

	/**
	 * Undo the renderer's HTML escaping (&amp; &lt; &gt; &quot;).
	 */
	private static String unescape(String s)
	{
		return s.replace("&quot;", "\"")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&nbsp;", " ")
			.replace("&amp;", "&");
	}

	/**
	 * Escape text for HTML (the renderer's escaping, plus &quot;).
	 */
	private static String escape(String s)
	{
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}
}
