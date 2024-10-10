package jadex.bt.envexample;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


public class EnvGui extends ApplicationAdapter 
{
    protected SpriteBatch batch;
    protected OrthographicCamera camera;
    protected Viewport viewport;
    protected Texture backgroundtex;
    protected Texture robottex;
    protected ShapeRenderer shaperen;
    protected BitmapFont font;

    // The environment dimensions.
    protected Vector2 edim = new Vector2(1, 1);
    
    // The world (paint) dimensions.
    protected Vector2 wdim = new Vector2(800, 600);
    
    // The (virtual) pixel dimensions.
    protected Vector2 pdim = new Vector2(800, 600);
    
    @Override
    public void create() 
    {
    	font = new BitmapFont();
 
        batch = new SpriteBatch();
        shaperen = new ShapeRenderer();
        backgroundtex = new Texture(Gdx.files.internal("jadex/bt/envexample/images/background.png"));
        robottex = new Texture(Gdx.files.internal("jadex/bt/envexample/images/robot.png"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, wdim.x, wdim.y);
        
        viewport = new StretchViewport(wdim.x, wdim.y, camera);
        viewport.apply();
        
        Vector2 scale = mapPixelToWorld(new Vector2(1, 1));
        font.getData().setScale(scale.x, scale.y);

    }

    @Override
    public void resize(int width, int height) 
    {
        viewport.update(width, height);
    }
    
    public void render() 
    {
        // Clear the screen with a neutral color
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1); 
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        batch.begin();
        
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        
        // Draw the background using the viewport dimensions
        batch.draw(backgroundtex, 0, 0, w, h);
        
        // Render robots
        for(BTRandomAgent agent : Environment.getAgents()) 
        {
        	Vector2 pos = agent.getCenterPosition();

        	Vector2 wpos = mapEnvToWorld(pos);
        	//Vector2 wdim = mapPixelToWorld(new Vector2(robottex.getWidth(), robottex.getHeight()));
        	Vector2 wdim = mapEnvToWorld(new Vector2(agent.getSize(), agent.getSize()));
        	
            batch.draw(robottex, wpos.x - wdim.x / 2, wpos.y - wdim.y / 2, wdim.x, wdim.y); 
        	
		    //batch.draw(robottex, wpos.x, wpos.y, wdim.x, wdim.y);
		    font.setColor(Color.BLACK);
		    drawText(batch, font, agent.toString(), mapEnvToWorld(agent.getPosition()), wdim.x, 0);
		    //font.draw(batch, "halloooo", wpos.x, wpos.y);
        }
        batch.end();
        
        Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for(BTRandomAgent agent : Environment.getAgents()) 
        {
        	int size = 3;
        	Vector2 pos = agent.getWaypoint();

        	Vector2 wpos = mapEnvToWorld(pos);
        	Vector2 wdim = mapPixelToWorld(new Vector2(size, size));

        	Vector2 wcpos = mapEnvToWorld(agent.getCenterPosition());
        	
        	batch.begin();
 	        shaperen.begin(ShapeRenderer.ShapeType.Filled);
 	        shaperen.setColor(getColorFromName(agent.toString())); 
 	        shaperen.ellipse(wpos.x-wdim.x, wpos.y-wdim.x, wdim.x*2, wdim.x*2); 
 	        
	        //shaperen.setColor(0, 1, 0, 0.5f); 
	        shaperen.ellipse(wcpos.x-wdim.x, wcpos.y-wdim.x, wdim.x*2, wdim.x*2); 
 	        
 	        shaperen.end();
 	        batch.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    // model -> world
    protected Vector2 mapEnvToWorld(Vector2 pos) 
    {
        float worldx = pos.x * (viewport.getWorldWidth() / edim.x); 
        float worldy = pos.y * (viewport.getWorldHeight() / edim.y); 
        return new Vector2(worldx, worldy);
    }

    // pix -> world
    protected Vector2 mapPixelToWorld(Vector2 pos) 
    {
        float worldx = pos.x * (viewport.getWorldWidth() / pdim.x);
        float worldy = pos.y * (viewport.getWorldHeight() / pdim.y);
        return new Vector2(worldx, worldy);
    }
    
    public void drawText(SpriteBatch batch, BitmapFont font, String text, 
    	Vector2 wpos, float textureWidth, float offsetY) 
	{
	    //float textureCenterX = wpos.x + mapPixelToWorld(new Vector2(textureWidth, 0)).x / 2;
	    float textureCenterX = wpos.x + textureWidth / 2;

	    GlyphLayout layout = new GlyphLayout();
	    layout.setText(font, text);
	    float textWidthPixels = layout.width; 

	    float textWidthWorld = mapPixelToWorld(new Vector2(textWidthPixels, 0)).x;

	    float textX = textureCenterX - textWidthWorld / 2;  
	    float textY = wpos.y - mapPixelToWorld(new Vector2(0, offsetY)).y; 
	    
	    //System.out.println("text: "+wpos+" "+textX+" "+textY);

	    font.draw(batch, text, textX, textY);
	}
    
    public static Color getColorFromName(String name) 
    {
        int hash = name.hashCode();
        
        float r = ((hash & 0xFF0000) >> 16) / 255f; 
        float g = ((hash & 0x00FF00) >> 8) / 255f; 
        float b = (hash & 0x0000FF) / 255f;         

        return new Color(r, g, b, 0.5f); 
    }
    
    @Override
    public void dispose() 
    {
        batch.dispose();
        backgroundtex.dispose();
        robottex.dispose();
    }

    public static void createEnv() 
    {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Example Robot World");
        config.setWindowedMode(800, 600);
        //config.setForegroundFPS(10);
        new Lwjgl3Application(new EnvGui(), config);
    }

    public static void main(String[] args) 
    {
        EnvGui.createEnv();
    }
}


