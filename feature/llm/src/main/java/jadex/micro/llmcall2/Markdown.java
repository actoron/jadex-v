package jadex.micro.llmcall2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  A small, self-contained Markdown renderer that produces the
 *  HTML subset understood by Swing's {@link javax.swing.JEditorPane}
 *  (inline tags only, no nested elements). It supports the common
 *  Markdown constructs that LLMs actually emit:
 *  headings, bold / italic / strikethrough, inline code, fenced
 *  code blocks, blockquotes, unordered/ordered lists, links,
 *  horizontal rules, and line breaks.
 *  <p>
 *  This avoids pulling in a heavyweight Markdown-to-JHTML library;
 *  Swing's editor pane only renders a constrained HTML subset
 *  anyway, so a dedicated renderer is the pragmatic choice.
 *  <p>
 *  Use {@link #toHtml(String)} to convert a Markdown document to the
 *  HTML string that should be set on a {@code JEditorPane}.
 */
public final class Markdown
{
	private Markdown()
	{
	}

	/** Pattern for a fenced code block (``` or ~~~). */
	private static final Pattern	FENCE	= Pattern.compile("^(```|~~~)(\\S*)\\s*$");

	/** Pattern for a blockquote line (a literal &gt;). */
	private static final Pattern	QUOTE	= Pattern.compile("^\\s*>\\s?");

	/** Pattern for an unordered list item. */
	private static final Pattern	ULIST	= Pattern.compile("^\\s*[-*+]\\s+(.*)$");

	/** Pattern for an ordered list item. */
	private static final Pattern	OLIST	= Pattern.compile("^\\s*(\\d+)[.)]\\s+(.*)$");

	/** Pattern for a heading line. */
	private static final Pattern	HEADING	= Pattern.compile("^(#{1,6})\\s+(.*)$");

	/** Pattern for a horizontal rule. */
	private static final Pattern	HRULE	= Pattern.compile("^\\s*([-*_])\\s*(?:\\1\\s*){2,}$");

	/** Inline code spans. */
	private static final Pattern	INLINE_CODE	= Pattern.compile("`([^`]*)`");

	/** Inline links: [text](url). */
	private static final Pattern	LINK	= Pattern.compile("\\[([^\\]]*)]\\(([^)]*)\\)");

	/** Bold: **text** or __text__. */
	private static final Pattern	BOLD	= Pattern.compile("\\*\\*([^*]+?)\\*\\*|__([^_]+?)__");

	/** Italic: *text* or _text_. */
	private static final Pattern	ITALIC	= Pattern.compile("(?<!\\*)\\*(?!\\*)([^*]+?)(?<!\\*)\\*(?!\\*)|(?<![A-Za-z0-9])_([^_]+?)_(?![A-Za-z0-9])");

	/** Strikethrough: ~~text~~. */
	private static final Pattern	STRUCK	= Pattern.compile("~~([^~]+?)~~");

	/**
	 *  Convert a Markdown document to Swing-compatible HTML.
	 *  The result is a fragment intended to be embedded inside a
	 *  {@code <body>} element.
	 *  @param markdown The Markdown source.
	 *  @return The Swing-HTML representation.
	 */
	public static String toHtml(String markdown)
	{
		if(markdown==null || markdown.isEmpty())
			return "";

		String[]	lines	= markdown.split("\n", -1);
		StringBuilder	out	= new StringBuilder();
		boolean	in_code	= false;
		StringBuilder	code	= new StringBuilder();

		boolean	in_ul	= false;
		boolean	in_ol	= false;
		boolean	in_quote	= false;

		for(int i=0; i<lines.length; i++)
		{
			String	line	= lines[i];

			// Fenced code blocks: toggle on/off.
			Matcher	fm	= FENCE.matcher(line);
			if(fm.matches())
			{
				if(!in_code)
				{
					if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
					if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }
					in_code	= true;
					code	= new StringBuilder();
				}
				else
				{
					out.append("<pre>")
						.append(escape(code.toString()))
						.append("</pre>");
					in_code	= false;
				}
				continue;
			}
			if(in_code)
			{
				code.append(line).append("\n");
				continue;
			}

			// Horizontal rule.
			if(HRULE.matcher(line).matches())
			{
				if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
				if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }
				in_quote	= false;
				out.append("<hr>\n");
				continue;
			}

			// Heading.
			Matcher	hm	= HEADING.matcher(line);
			if(hm.matches())
			{
				if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
				if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }
				in_quote	= false;
				int		lvl	= Math.min(hm.group(1).length(), 6);
				out.append("<h").append(lvl).append(">")
					.append(inline(escape(hm.group(2))))
					.append("</h").append(lvl).append(">\n");
				continue;
			}

			// Blockquote.
			Matcher	qm	= QUOTE.matcher(line);
			if(qm.find())
			{
				if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
				if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }
				in_quote	= true;
				String	rest	= line.substring(qm.end());
				out.append("<blockquote>").append(inline(escape(rest))).append("</blockquote>\n");
				continue;
			}
			else if(in_quote && line.trim().isEmpty())
			{
				in_quote	= false;
			}

			// Unordered list.
			Matcher	um	= ULIST.matcher(line);
			if(um.matches())
			{
				if(in_ol)
					out.append("</ol>\n");
				in_ol	= false;
				if(!in_ul)
				{
					out.append("<ul>\n");
					in_ul	= true;
				}
				out.append("<li>").append(inline(escape(um.group(1)))).append("</li>\n");
				continue;
			}

			// Ordered list.
			Matcher	om	= OLIST.matcher(line);
			if(om.matches())
			{
				if(in_ul)
					out.append("</ul>\n");
				in_ul	= false;
				if(!in_ol)
				{
					out.append("<ol>\n");
					in_ol	= true;
				}
				out.append("<li>").append(inline(escape(om.group(2)))).append("</li>\n");
				continue;
			}

			// Blank line: close open lists and end the block.
			if(line.trim().isEmpty())
			{
				if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
				if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }
				in_quote	= false;
				continue;
			}

			// Plain paragraph text.
			if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
			if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }
			in_quote	= false;
			out.append("<p>").append(inline(escape(line))).append("</p>\n");
		}

		// Close any list left open at end of input.
		if(in_ul)	{ out.append("</ul>\n"); in_ul=false; }
		if(in_ol)	{ out.append("</ol>\n"); in_ol=false; }

		// If a code block was never closed, flush it.
		if(in_code)
			out.append("<pre>").append(escape(code.toString())).append("</pre>");

		return out.toString();
	}

	/**
	 *  Apply inline Markdown (bold, italic, strikethrough, links,
	 *  inline code) to an already HTML-escaped string.
	 *  @param escaped The HTML-escaped text.
	 *  @return The text with inline tags applied.
	 */
	private static String inline(String escaped)
	{
		// Inline code first so its contents are not re-styled.
		Matcher	cm	= INLINE_CODE.matcher(escaped);
		StringBuilder	buf	= new StringBuilder();
		int	last	= 0;
		while(cm.find())
		{
			buf.append(escaped, last, cm.start());
			buf.append("<code>").append(cm.group(1)).append("</code>");
			last	= cm.end();
		}
		buf.append(escaped, last, escaped.length());
		escaped	= buf.toString();

		// Bold.
		Matcher	bm	= BOLD.matcher(escaped);
		buf	= new StringBuilder();
		last	= 0;
		while(bm.find())
		{
			buf.append(escaped, last, bm.start());
			String	inner	= bm.group(1)!=null? bm.group(1): bm.group(2);
			buf.append("<b>").append(inner).append("</b>");
			last	= bm.end();
		}
		buf.append(escaped, last, escaped.length());
		escaped	= buf.toString();

		// Italic.
		Matcher	im	= ITALIC.matcher(escaped);
		buf	= new StringBuilder();
		last	= 0;
		while(im.find())
		{
			buf.append(escaped, last, im.start());
			String	inner	= im.group(1)!=null? im.group(1): im.group(2);
			buf.append("<i>").append(inner).append("</i>");
			last	= im.end();
		}
		buf.append(escaped, last, escaped.length());
		escaped	= buf.toString();

		// Strikethrough.
		Matcher	sm	= STRUCK.matcher(escaped);
		buf	= new StringBuilder();
		last	= 0;
		while(sm.find())
		{
			buf.append(escaped, last, sm.start());
			buf.append("<strike>").append(sm.group(1)).append("</strike>");
			last	= sm.end();
		}
		buf.append(escaped, last, escaped.length());
		escaped	= buf.toString();

		// Links.
		Matcher	lm	= LINK.matcher(escaped);
		buf	= new StringBuilder();
		last	= 0;
		while(lm.find())
		{
			buf.append(escaped, last, lm.start());
			buf.append("<a href=\"").append(lm.group(2)).append("\">").append(lm.group(1)).append("</a>");
			last	= lm.end();
		}
		buf.append(escaped, last, escaped.length());
		return buf.toString();
	}

	/**
	 *  Escape a string for safe inclusion in Swing HTML.
	 */
	private static String escape(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\n", "<br>");
	}
}
