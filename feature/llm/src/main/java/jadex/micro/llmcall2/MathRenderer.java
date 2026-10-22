package jadex.micro.llmcall2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;

/**
 *  Renders TeX math in LLM chat output as PNG images that the Swing
 *  HTML view displays inline.
 *  <p>
 *  {@link #preprocess(String)} rewrites the TeX markup of a Markdown
 *  source before the Markdown renderer turns it into HTML:
 *  <ul>
 *  <li>{@code $$...$$} display formulas are rasterized with
 *      JLaTeXMath and replaced by a Markdown image reference in its
 *      own paragraph</li>
 *  <li>{@code $...$} inline formulas are replaced by inline image
 *      references in the running text</li>
 *  </ul>
 *  Fenced code blocks and inline code spans are left verbatim:
 *  the rewrite never applies inside code, so code that shows TeX
 *  commands or shell variables is displayed as code, not as
 *  formula images.
 *  The image references are plain Markdown image syntax with
 *  {@code file:} URLs into a per-JVM cache directory, so the Markdown
 *  renderer turns them into {@code img} elements that the Swing HTML
 *  view loads natively: no custom URL protocol and no
 *  {@code URLStreamHandler} factory are involved. Formulas that
 *  JLaTeXMath cannot parse are left in the source unchanged, so the
 *  user still sees the TeX source instead of nothing, and no failure
 *  can ever break the chat view.
 */
public final class MathRenderer
{
	//-------- constants --------

	/** Point size for display ($$...$$) formulas. */
	static final float	DISPLAY_SIZE	= 20f;

	/** Point size for inline ($...$) formulas. */
	static final float	TEXT_SIZE		= 15f;

	//-------- attributes --------

	/** The cache directory that holds the rendered formula PNGs,
	 *  created by {@link #ensureDir()} on first use. */
	private static volatile Path	cache_dir	= null;

	//-------- initialization --------

	/**
	 *  No-op, prevents instantiation of this utility class.
	 */
	private MathRenderer()
	{
	}

	//-------- public API --------

	/**
	 *  Rewrite the TeX markup of a Markdown source so that the chat
	 *  view displays the formulas as images instead of the raw TeX
	 *  text. The result is still valid Markdown: each formula
	 *  becomes a Markdown image reference, which the Markdown
	 *  renderer turns into an {@code img} element that the Swing
	 *  HTML view loads from the local formula cache. Formulas that
	 *  JLaTeXMath cannot parse are left in the source unchanged, so
	 *  the user still sees the TeX source instead of nothing, and
	 *  no failure can ever break the chat view.
	 *  <p>
	 *  Code blocks and code spans are protected: math replacement
	 *  never applies inside fenced ({@code ```}) or backtick code,
	 *  so code that shows TeX, shell variables, or dollar amounts
	 *  renders verbatim as code.
	 *  @param markdown The Markdown source of a chat fragment.
	 *  @return The Markdown with the TeX markup replaced by image
	 *         references, or the original string if it contains no
	 *         TeX.
	 */
	public static String preprocess(String markdown)
	{
		if(markdown==null || markdown.isEmpty())
			return markdown;
		if(!markdown.contains("$") && !markdown.contains("\\"))
			return markdown;
		// Protect code from the math rewrite: fenced blocks first, then
		// inline code spans. Placeholders contain no $ or backslash, so
		// the math patterns cannot touch them.
		List<String>	code	= new ArrayList<>();
		String	result	= protectFencedCode(markdown, code);
		result	= protectInlineCode(result, code);
		// Display formulas first, so the inline patterns never see the $$ delimiters.
		result	= apply(DISPLAY_MATH, result, TeXConstants.STYLE_DISPLAY, DISPLAY_SIZE, true);
		result	= apply(INLINE_MATH, result, TeXConstants.STYLE_TEXT, TEXT_SIZE, false);
		result	= restoreCode(result, code);
		return result;
	}

	//-------- replacement --------

	/**
	 *  Replace every match of the given pattern with a Markdown
	 *  image reference to the rendered formula, or with the match
	 *  verbatim if it cannot be rendered.
	 *  @param p The pattern to apply.
	 *  @param input The text to rewrite.
	 *  @param style The TeX style constant to render with.
	 *  @param size The point size to render at.
	 *  @param display Whether a match is a display ($$...$$) formula.
	 *  @return The rewritten text.
	 */
	private static String apply(Pattern p, String input, int style, float size, boolean display)
	{
		Matcher		m		= p.matcher(input);
		StringBuilder	sb		= new StringBuilder(input.length());
		int		last	= 0;
		while(m.find())
		{
			sb.append(input, last, m.start());
			sb.append(replaceSpan(m.toMatchResult(), style, size, display));
			last	= m.end();
		}
		sb.append(input, last, input.length());
		return sb.toString();
	}

	/**
	 *  Build the Markdown image reference for one formula match, or
	 *  the original text unchanged if the formula cannot be
	 *  rasterized.
	 *  @param m The matched formula span.
	 *  @param style The TeX style constant to render with.
	 *  @param size The point size to render at.
	 *  @param display Whether the formula is a display ($$...$$) one.
	 *  @return The image reference, or the original text.
	 */
	private static String replaceSpan(MatchResult m, int style, float size, boolean display)
	{
		String	url	= ensurePng(m.group(1), style, size);
		if(url==null)
			return m.group();	// keep the source text verbatim
		String	image	= "![formula](" + url + ")";
		return display? "\n\n" + image + "\n\n": image;
	}

	/**
	 *  Make sure the PNG of the given formula is in the cache and
	 *  return the {@code file:} URL that the Swing HTML view loads
	 *  it from, or null if the formula cannot be rasterized.
	 *  @param tex The TeX source.
	 *  @param style The TeX style constant to render with.
	 *  @param size The point size to render at.
	 *  @return The file URL of the cached PNG, or null.
	 */
	private static String ensurePng(String tex, int style, float size)
	{
		if(tex==null || tex.trim().isEmpty())
			return null;
		String	key	= keyFor(tex, style, size);
		try
		{
			Path	png	= ensureDir().resolve(key + ".png");
			if(!Files.exists(png))
			{
				// Concurrent renders of the same formula may write at the
				// same time; the bytes are identical, so the race is harmless.
				try
				{
					TeXFormula	formula	= new TeXFormula(tex);	// throws ParseException
					Image		image	= formula.createBufferedImage(style, size, Color.BLACK, Color.WHITE);
					Files.write(png, pngBytes(image));
				}
				catch(ParseException ex)	// not a valid formula -> keep the source text
				{
					return null;
				}
			}
			return png.toUri().toURL().toExternalForm();
		}
		catch(Exception ex)	// any failure keeps the source text instead of breaking the chat view
		{
			return null;
		}
	}

	/**
	 *  The PNG bytes of the given formula image, drawn onto a white
	 *  opaque canvas that matches the white chat view background.
	 *  @param image The rasterized formula.
	 *  @return The encoded PNG bytes.
	 *  @throws IOException If the image cannot be encoded.
	 */
	private static byte[] pngBytes(Image image)
		throws IOException
	{
		// createBufferedImage returns a java.awt.Image; draw it onto an
		// opaque white BufferedImage (a RenderedImage) so ImageIO can encode it.
		BufferedImage	canvas	= new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D	g		= canvas.createGraphics();
		try
		{
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
			g.drawImage(image, 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		ByteArrayOutputStream	out	= new ByteArrayOutputStream();
		if(!ImageIO.write(canvas, "png", out))
			throw new IOException("no PNG image writer");
		return out.toByteArray();
	}

	//-------- code protection --------

	/**
	 *  Replace every fenced code block in the source by a placeholder
	 *  that survives the math rewrite. A fence is a line of {@code ```}
	 *  (optionally followed by a language tag); the block ends at the
	 *  next {@code ```} line or at the end of the source (an LLM that
	 *  is cut off mid-block still gets its code kept verbatim).
	 *  @param input The Markdown source.
	 *  @param code Out parameter that collects the code blocks in
	 *             order; restoreCode puts them back.
	 *  @return The source with the code blocks removed.
	 */
	private static String protectFencedCode(String input, List<String> code)
	{
		Matcher	m	= FENCED_CODE.matcher(input);
		StringBuilder	sb	= new StringBuilder(input.length());
		int	last	= 0;
		while(m.find())
		{
			sb.append(input, last, m.start());
			code.add(m.group());
			sb.append(placeholder(code.size() - 1, true));
			last	= m.end();
		}
		sb.append(input, last, input.length());
		return sb.toString();
	}

	/**
	 *  Replace every inline code span in the source by a placeholder
	 *  that survives the math rewrite.
	 *  @param input The Markdown source, already fence-stripped.
	 *  @param code Out parameter that collects the code spans in order.
	 *  @return The source with the code spans removed.
	 */
	private static String protectInlineCode(String input, List<String> code)
	{
		Matcher	m	= INLINE_CODE.matcher(input);
		StringBuilder	sb	= new StringBuilder(input.length());
		int	last	= 0;
		while(m.find())
		{
			sb.append(input, last, m.start());
			code.add(m.group());	// include the backticks
			sb.append(placeholder(code.size() - 1, false));
			last	= m.end();
		}
		sb.append(input, last, input.length());
		return sb.toString();
	}

	/**
	 *  Put the protected code back into the rewritten source.
	 *  @param input The math-rewritten source.
	 *  @param code The collected code blocks and spans, in order.
	 *  @return The source with the code reinserted verbatim.
	 */
	private static String restoreCode(String input, List<String> code)
	{
		if(code.isEmpty())
			return input;
		StringBuilder	sb	= new StringBuilder(input.length() + code.toString().length());
		Matcher	m	= PLACEHOLDER.matcher(input);
		int	last	= 0;
		while(m.find())
		{
			int	index	= Integer.parseInt(m.group(1));
			sb.append(input, last, m.start());
			sb.append(index>=0 && index<code.size()? code.get(index): m.group());
			last	= m.end();
		}
		sb.append(input, last, input.length());
		return sb.toString();
	}

	/**
	 *  The placeholder that stands in for one protected code
	 *  block or span: a word character run that no math pattern
	 *  can match.
	 *  @param index The 0-based index into the code list.
	 *  @param block Whether the placeholder stands for a whole
	 *              fenced block (which needs its own line).
	 *  @return The placeholder text.
	 */
	private static String placeholder(int index, boolean block)
	{
		return block? ("\n\n@jadexcode" + index + "@\n\n")
			: "@jadexcode" + index + "@";
	}

	//-------- cache directory --------

	/**
	 *  The cache directory that holds the rendered formula PNGs,
	 *  creating it on first use.
	 *  @return The cache directory.
	 */
	private static synchronized Path ensureDir()
	{
		if(cache_dir==null)
		{
			try
			{
				cache_dir	= Files.createTempDirectory("jadex-math");
			}
			catch(IOException ex)
			{
				throw new IllegalStateException("cannot create the formula cache directory", ex);
			}
		}
		return cache_dir;
	}

	/**
	 *  The cache file name of a formula: the SHA-256 hex digest of
	 *  the TeX source, style and size. The hex name is URL-safe and
	 *  unique, so it never collides or needs escaping in a file URL.
	 *  @param tex The TeX source.
	 *  @param style The TeX style constant.
	 *  @param size The point size.
	 *  @return The hex file name without extension.
	 */
	private static String keyFor(String tex, int style, float size)
	{
		try
		{
			MessageDigest	d	= MessageDigest.getInstance("SHA-256");
			byte[]	digest	= d.digest((tex + "\u0000" + style + "\u0000" + size).getBytes(StandardCharsets.UTF_8));
			StringBuilder	hex	= new StringBuilder(digest.length * 2);
			for(byte	b: digest)
				hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
			return hex.toString();
		}
		catch(NoSuchAlgorithmException ex)	// SHA-256 is available in every JVM
		{
			throw new IllegalStateException(ex);
		}
	}

	//-------- regex patterns --------

	/**
	 *  $$...$$ display formulas, non-greedy, may span lines.
	 */
	private static final Pattern	DISPLAY_MATH	=
		Pattern.compile("\\$\\$\\s*([\\s\\S]+?)\\s*\\$\\$");

	/**
	 *  $...$ inline formulas; no $ or line breaks inside.
	 */
	private static final Pattern	INLINE_MATH	=
		Pattern.compile("\\$([^$\\n]+?)\\$");

	/**
	 *  ```...``` fenced code blocks, language tag allowed, may span lines;
	 *  an unterminated fence (a cut-off LLM answer) runs to the end of the
	 *  source.
	 */
	private static final Pattern	FENCED_CODE	=
		Pattern.compile("(?m)^\\s*```[^\n]*\\n(?:\\n|[\\s\\S])*?```|^\\s*```[^\n]*\\n(?:\\n|[\\s\\S]*)$", Pattern.MULTILINE);

	/**
	 *  `...` inline code spans; no backtick or line break inside.
	 */
	private static final Pattern	INLINE_CODE	=
		Pattern.compile("`([^`\\n]+)`");

	/**
	 *  The placeholders that the code protection inserts, keyed by
	 *  their 0-based index into the code list.
	 */
	private static final Pattern	PLACEHOLDER	=
		Pattern.compile("@jadexcode(\\d+)@");
}
