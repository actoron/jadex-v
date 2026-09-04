package jadex.common;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;

public class SEmoji 
{
    private static final String TWEMOJI_URL = "https://raw.githubusercontent.com/jdecked/twemoji/main/assets/72x72/";

    private static final Map<String, Image> emojiCache = new ConcurrentHashMap<>();

    public static ImageIcon getEmojiIcon(String emoji)
    {
        return getEmojiIcon(emoji, 20);
    }

    public static ImageIcon getEmojiIcon(String emoji, int size)
    {
        try
        {
            String filename = emoji.codePoints().mapToObj(cp -> Integer.toHexString(cp)).collect(Collectors.joining("-"));

            Image image = emojiCache.get(filename);

            if (image == null)
            {
                image = loadTwemoji(filename);

                if (image != null)
                    emojiCache.put(filename, image);
            }

            if (image != null)
            {
                Image scaled = image.getScaledInstance(
                    size, size, Image.SCALE_SMOOTH);

                return new ImageIcon(scaled);
            }

            // Normal Unicode symbol fallback, e.g. ✓
            return getTextIcon(emoji, size);
        }
        catch (Exception e)
        {
            System.err.println(
                "Could not load emoji '" + emoji + "': " + e);

            return getTextIcon(emoji, size);
        }
    }

    private static Image loadTwemoji(String filename)
    {
        Image image = loadTwemojiFile(filename + ".png");

        if (image != null)
            return image;

        image = loadTwemojiFile(filename + "-fe0f.png");

        if (image != null)
            return image;

        return null;
    }

    private static Image loadTwemojiFile(String filename)
    {
        try
        {
            URL url = URI.create(TWEMOJI_URL + filename).toURL();

            ImageIcon icon = new ImageIcon(url);

            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0)
                return null;

            return icon.getImage();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static ImageIcon getTextIcon(String text, int size)
    {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        var g = image.createGraphics();

        g.setFont(new java.awt.Font("Noto Sans Symbols 2",java.awt.Font.PLAIN,size));

        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        var fm = g.getFontMetrics();

        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();

        g.drawString(text, x, y);

        g.dispose();

        return new ImageIcon(image);
    }
}
