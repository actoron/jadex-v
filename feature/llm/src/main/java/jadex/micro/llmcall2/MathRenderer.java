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
 *  <li>backtick-wrapped TeX (an LLM common escape hatch, e.g.
 *      <code>`\frac{a}{b}`</code>) is replaced by an image reference
 *      when it parses as a formula, otherwise it is kept as-is and the
 *      Markdown renderer displays the raw source in a monospaced
 *      <code>code</code> block</li>
 *  </ul>
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
		String	result	= markdown;
		// Display formulas first, so the inline patterns never see the $$ delimiters.
		result	= apply(DISPLAY_MATH, result, TeXConstants.STYLE_DISPLAY, DISPLAY_SIZE, true);
		result	= apply(INLINE_MATH, result, TeXConstants.STYLE_TEXT, TEXT_SIZE, false);
		result	= apply(CODE_MATH, result, TeXConstants.STYLE_TEXT, TEXT_SIZE, false);
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
	 *  Backtick-wrapped TeX: `...` containing a backslash command.
	 */
	private static final Pattern	CODE_MATH	=
		Pattern.compile("`([^`\\n]*\\\\[^`\\n]*)`");
}
