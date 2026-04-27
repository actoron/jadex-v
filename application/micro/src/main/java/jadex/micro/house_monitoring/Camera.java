package jadex.micro.house_monitoring;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.ProvideService;

/**
 *  Camera implementation that generates images based on prompts using an image generation model.
 */
@ProvideService(tags = "%{\"name=\"+$component.getId().getLocalName()}")
public class Camera	implements ICameraService
{
	/** The current prompt. */
	protected String	prompt	= "a house at night";
	
	@Override
	public IFuture<RenderedImage> getCurrentImage()
	{
		if(prompt==null)
		{
			return new Future<>((RenderedImage)null);
		}
		else
		{
			try
			{
				BufferedImage image = getSecurityCameraImage(prompt);
				return new Future<>(image);
			}
			catch(Exception e)
			{
				return new Future<>(e);
			}
		}
	}
	
	@Override
	public IFuture<Void> setCurrentImage(String prompt)
	{
		this.prompt = prompt;
		return IFuture.DONE;
	}
	
	//-------- helper methods --------	

	/**
	 *  Get the image for the given prompt, either from cache or by generating it.
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
			ImageModel	imagemodel

//			// Nano-banana
//				= GoogleAiGeminiImageModel.builder()
//				.apiKey(System.getenv("GOOGLE_API_KEY"))
//				.modelName("gemini-2.5-flash-image")
//				.build();
			
			// Local model using https://github.com/mudler/LocalAI
				= OpenAiImageModel.builder()
				.baseUrl("http://localhost:8080/v1")
//				.modelName("flux.2-klein-4b")
				.modelName("flux.2-klein-9b")
				.build();
			
			dev.langchain4j.data.image.Image	image	= imagemodel.generate("security camera image of "+prompt).content();
			byte[]	bytes;
			if(image.base64Data()!=null)
			{
				bytes = java.util.Base64.getDecoder().decode(image.base64Data());
			}
			else
			{
				bytes = image.url().toURL().openStream().readAllBytes();
			}
			bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
			
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
	
	/**
	 *  Get the cached prompts.
	 */
	public static String[] getCachedPrompts()
	{
		File	basedir	= new File("./generated_images");
		if(basedir.exists() && basedir.isDirectory())
		{
			return java.util.Arrays.stream(basedir.listFiles((dir, name) -> name.endsWith(".jpg")))
					.map(file -> file.getName().substring(0, file.getName().length()-4))
					.toArray(String[]::new);
		}
		else
		{
			return new String[0];
		}
	}
}
