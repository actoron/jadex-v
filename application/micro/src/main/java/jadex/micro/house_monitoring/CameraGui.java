package jadex.micro.house_monitoring;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Simple UI for a camera service.
 *  Shows the current camera image and allows changing the scene via a prompt.
 */
@SuppressWarnings("serial")
public class CameraGui extends JPanel
{
	//-------- attributes --------

	/** The camera service. */
	protected ICameraService	camera;

	/** Panel that renders the current image. */
	protected ImagePanel	imagePanel;

	/** Combo box for the prompt. */
	protected JComboBox<String>	promptField;

	/** Status label. */
	protected JLabel	statusLabel;

	//-------- constructors --------

	/**
	 *  Create a new camera GUI.
	 */
	public CameraGui(ICameraService camera)
	{
		this.camera = camera;

		setLayout(new BorderLayout(4, 4));

		// Image area
		imagePanel = new ImagePanel();
		imagePanel.setPreferredSize(new Dimension(512, 512));
		imagePanel.setBorder(BorderFactory.createLoweredBevelBorder());
		add(imagePanel, BorderLayout.CENTER);

		// Bottom controls
		JPanel controls = new JPanel(new BorderLayout(4, 4));
		controls.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		promptField = new JComboBox<>(Camera.getCachedPrompts());
		promptField.setEditable(true);
		if(promptField.getItemCount() == 0)
			promptField.setSelectedItem("a house at night");
		JButton setBtn = new JButton("Load / Generate");
		statusLabel = new JLabel(" ");

		JPanel btnPanel = new JPanel();
		btnPanel.add(setBtn);

		controls.add(new JLabel("Camera shows :"), BorderLayout.WEST);
		controls.add(promptField, BorderLayout.CENTER);
		controls.add(btnPanel, BorderLayout.EAST);
		controls.add(statusLabel, BorderLayout.SOUTH);
		add(controls, BorderLayout.SOUTH);

		// Button actions
		setBtn.addActionListener(e ->
		{
			String prompt = (String) promptField.getEditor().getItem();
			prompt = prompt == null ? "" : prompt.trim();
			if(!prompt.isEmpty())
			{
				setStatus("Setting prompt...");
				camera.setCurrentImage(prompt).then(v ->
				{
					setStatus("Prompt set. Refreshing image...");
					refreshImage();
				}).catchEx(ex ->
				{
					setStatus("Error setting prompt: " + ex);
				});
			}
		});

		refreshImage();
	}

	//-------- helper methods --------

	/**
	 *  Fetch the current image from the camera service and display it.
	 */
	protected void refreshImage()
	{
		setStatus("Loading image...");
		camera.getCurrentImage().then(img ->
		{
			if(img!=null)
			{
				imagePanel.setImage(img);
				setStatus("Image loaded.");
			}
			else
			{
				imagePanel.setImage(null);
				setStatus("No image available.");
			}
		}).catchEx(ex ->
		{
			setStatus("Error loading image: " + ex);
		});
	}

	/**
	 *  Update the status label on the EDT.
	 */
	protected void setStatus(String text)
	{
		statusLabel.setText(text);
	}
	
	//-------- inner classes --------

	/**
	 *  Panel that paints a RenderedImage.
	 */
	protected static class ImagePanel extends JPanel
	{
		protected RenderedImage	image;

		public void setImage(RenderedImage image)
		{
			this.image = image;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if(image instanceof java.awt.image.BufferedImage)
			{
				java.awt.image.BufferedImage bi = (java.awt.image.BufferedImage) image;
				// Scale to fit while keeping aspect ratio
				double scaleX = (double) getWidth() / bi.getWidth();
				double scaleY = (double) getHeight() / bi.getHeight();
				double scale = Math.min(scaleX, scaleY);
				int w = (int) (bi.getWidth() * scale);
				int h = (int) (bi.getHeight() * scale);
				int x = (getWidth() - w) / 2;
				int y = (getHeight() - h) / 2;
				g.drawImage(bi, x, y, w, h, null);
			}
			else if(image != null)
			{
				// Fallback: convert via BufferedImage
				java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
					image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
				bi.createGraphics().drawRenderedImage(image, new java.awt.geom.AffineTransform());
				g.drawImage(bi, 0, 0, getWidth(), getHeight(), null);
			}
		}
	}

	
	
	public static void main(String[] args) throws Exception
	{
		IComponentHandle	cam	= IComponentManager.get().create(new Camera()).get();
		ICameraService	camserv	= cam.scheduleStep((INoCopyStep<ICameraService>) comp ->
			comp.getFeature(IRequiredServiceFeature.class).getLocalService(ICameraService.class)).get();
		
		SwingUtilities.invokeLater(() ->
		{
			CameraGui gui = new CameraGui(camserv);
			JFrame frame = new JFrame("Security Camera");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setContentPane(gui);
			frame.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					cam.terminate().catchEx(ex ->
					{
						// GUI is already closed; keep this non-fatal.
						System.err.println("Failed to terminate camera component: " + ex);
					});
				}
			});
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}