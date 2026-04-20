package jadex.micro.house_monitoring;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import dev.langchain4j.model.googleai.GoogleAiGeminiImageModel;

public class Camera
{
	public static void main(String[] args) throws Exception
	{
		String	prompt	= "a burglar breaking into a house at night";
		
		BufferedImage bufferedImage = getSecurityCameraImage(prompt);
		
		// Show the image in a window
		Image img	= bufferedImage;
		javax.swing.SwingUtilities.invokeLater(() -> {
			javax.swing.JFrame frame = new javax.swing.JFrame("Generated Image");
			frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			frame.setSize(400, 400);
			javax.swing.JLabel label = new javax.swing.JLabel(new javax.swing.ImageIcon(img));
			frame.getContentPane().add(label);
			frame.setVisible(true);
		});
	}

	/**
	 *  Get the image for the given prompt, either from cache or by generating it using the Gemini Image Model.
	 */
	protected static BufferedImage getSecurityCameraImage(String prompt) throws Exception
	{
		File	basedir	= new File("./generated_images");
		basedir.mkdirs();
		File	imagefile = new File(basedir, prompt.replaceAll("[^a-zA-Z0-9._ -]", "_")+".jpg");
		
		BufferedImage bufferedImage = null;
		if(imagefile.exists())
		{
			bufferedImage = ImageIO.read(imagefile);
		}
		else
		{
			GoogleAiGeminiImageModel	imagemodel	= GoogleAiGeminiImageModel.builder()
				.apiKey(System.getenv("GOOGLE_API_KEY"))
				.modelName("gemini-2.5-flash-image")	// its nano-banana
				.build();
			
			String image	= imagemodel.generate("security camera image of "+prompt).content().base64Data();
			byte[] originalBytes = java.util.Base64.getDecoder().decode(image);
			bufferedImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
			
			// Reduce JPEG quality to 80% to reduce file size
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			writeParam.setCompressionQuality(0.8f);
			
			// Write the image to a file
			try(FileOutputStream fos = new FileOutputStream(imagefile);
				ImageOutputStream ios = ImageIO.createImageOutputStream(fos))
			{
				writer.setOutput(ios);
				writer.write(null, new IIOImage(bufferedImage, null, null), writeParam);
			}
			finally
			{
				writer.dispose();
			}
		}
		return bufferedImage;
	}
}