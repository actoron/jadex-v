package jadex.bdi.marsworld.ui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import jadex.bdi.marsworld.environment.Carry;
import jadex.bdi.marsworld.environment.Environment;
import jadex.bdi.marsworld.environment.Homebase;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Producer;
import jadex.bdi.marsworld.environment.Sentry;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.environment.Target.Status;
import jadex.bdi.marsworld.math.IVector2;


public class EnvGui extends ApplicationAdapter 
{
    protected SpriteBatch batch;
    protected OrthographicCamera camera;
    protected Viewport viewport;
    protected Texture backgroundtex;
    protected Texture carrytex;
    protected Texture producertex;
    protected Texture sentrytex;
    protected Texture targettex;
    protected Texture homebasetex;
    protected ShapeRenderer shaperen;
    protected BitmapFont font;

    // The environment dimensions.
    protected Vector2 edim = new Vector2(1, 1);
    
    // The world (paint) dimensions.
    protected Vector2 wdim = new Vector2(1200, 800);
    
    // The (virtual) pixel dimensions.
    protected Vector2 pdim = new Vector2(1200, 800);
    
    protected MarsworldEnvironment env;
    
    public EnvGui(String envid)
    {
    	env = (MarsworldEnvironment)Environment.get(envid);
    }
    
    @Override
    public void create() 
    {
    	font = new BitmapFont();
 
        batch = new SpriteBatch();
        shaperen = new ShapeRenderer();
        backgroundtex = new Texture(Gdx.files.internal("jadex/bdi/marsworld/images/mars.png"));
        backgroundtex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        carrytex = new Texture(Gdx.files.internal("jadex/bdi/marsworld/images/carry.png"));
        carrytex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        producertex = new Texture(Gdx.files.internal("jadex/bdi/marsworld/images/producer.png"));
        producertex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        sentrytex = new Texture(Gdx.files.internal("jadex/bdi/marsworld/images/sentry.png"));
        sentrytex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        targettex = new Texture(Gdx.files.internal("jadex/bdi/marsworld/images/flag.png"));
        targettex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        homebasetex = new Texture(Gdx.files.internal("jadex/bdi/marsworld/images/target.png"));
        homebasetex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

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
    
    @Override
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
               
        // Render carries
        for(Carry carry: env.getSpaceObjectsByType(Carry.class).get())
        {
        	Vector2 pos = convert(carry.getPosition());

        	Vector2 wpos = mapEnvToWorld(pos);
        	//Vector2 wdim = mapPixelToWorld(new Vector2(robottex.getWidth(), robottex.getHeight()));
        	Vector2 wdim = mapEnvToWorld(new Vector2((float)carry.getWidth(), (float)carry.getHeight()));
        	
        	//System.out.println("carry: "+carry.getName()+" "+carry.getPosition()+" "+wpos.x+" "+wpos.y);
        	
            batch.draw(carrytex, wpos.x - wdim.x / 2, wpos.y - wdim.y / 2, wdim.x, wdim.y); 
        	
		    //batch.draw(robottex, wpos.x, wpos.y, wdim.x, wdim.y);
		    font.setColor(Color.BLACK);
		    drawText(batch, font, carry.getName(), mapEnvToWorld(pos), wdim.x, 0);
		    //font.draw(batch, "halloooo", wpos.x, wpos.y);
        }
        
        // Render producers
        for(Producer pro: env.getSpaceObjectsByType(Producer.class).get())
        {
        	Vector2 pos = convert(pro.getPosition());

        	Vector2 wpos = mapEnvToWorld(pos);
        	//Vector2 wdim = mapPixelToWorld(new Vector2(robottex.getWidth(), robottex.getHeight()));
        	Vector2 wdim = mapEnvToWorld(new Vector2((float)pro.getWidth(), (float)pro.getHeight()));
        	
        	//System.out.println("carry: "+pro.getName()+" "+pro.getPosition()+" "+wpos.x+" "+wpos.y);
        	
            batch.draw(producertex, wpos.x - wdim.x / 2, wpos.y - wdim.y / 2, wdim.x, wdim.y); 
        	
		    //batch.draw(robottex, wpos.x, wpos.y, wdim.x, wdim.y);
		    font.setColor(Color.BLACK);
		    drawText(batch, font, pro.getName(), mapEnvToWorld(pos), wdim.x, 0);
		    //font.draw(batch, "halloooo", wpos.x, wpos.y);
        }
        
        // Render sentries
        for(Sentry sentry: env.getSpaceObjectsByType(Sentry.class).get())
        {
        	Vector2 pos = convert(sentry.getPosition());

        	Vector2 wpos = mapEnvToWorld(pos);
        	//Vector2 wdim = mapPixelToWorld(new Vector2(robottex.getWidth(), robottex.getHeight()));
        	Vector2 wdim = mapEnvToWorld(new Vector2((float)sentry.getWidth(), (float)sentry.getHeight()));
        	
        	//System.out.println("carry: "+pro.getName()+" "+pro.getPosition()+" "+wpos.x+" "+wpos.y);
        	
            batch.draw(sentrytex, wpos.x - wdim.x / 2, wpos.y - wdim.y / 2, wdim.x, wdim.y); 
        	
		    //batch.draw(robottex, wpos.x, wpos.y, wdim.x, wdim.y);
		    font.setColor(Color.BLACK);
		    drawText(batch, font, sentry.getName(), mapEnvToWorld(pos), wdim.x, 0);
		    //font.draw(batch, "halloooo", wpos.x, wpos.y);
        }
        
        // Render targets
        for(Target target: env.getSpaceObjectsByType(Target.class).get())
        {
        	Vector2 pos = convert(target.getPosition());

        	Vector2 wpos = mapEnvToWorld(pos);
        	//Vector2 wdim = mapPixelToWorld(new Vector2(robottex.getWidth(), robottex.getHeight()));
        	Vector2 wdim = mapEnvToWorld(new Vector2((float)target.getWidth(), (float)target.getHeight()));
        	
            batch.draw(targettex, wpos.x - wdim.x / 2, wpos.y - wdim.y / 2, wdim.x, wdim.y); 
        	
		    //batch.draw(robottex, wpos.x, wpos.y, wdim.x, wdim.y);
		    font.setColor(Color.BLACK);
		    String txt = ""+target.getStatus()+" "+target.getPosition();
		    if(target.getStatus()==Status.Analyzed)
		    	txt += " ore: "+target.getOre()+" capa: "+target.getCapacity();   
		    drawText(batch, font, txt, mapEnvToWorld(pos), wdim.x, wdim.y/2);
		    //font.draw(batch, "halloooo", wpos.x, wpos.y);
        }
        
        Homebase base = env.getSpaceObjectsByType(Homebase.class).get().iterator().next();
        Vector2 pos = convert(base.getPosition());
    	Vector2 wpos = mapEnvToWorld(pos);
    	Vector2 wdim = mapEnvToWorld(new Vector2((float)base.getWidth(), (float)base.getHeight()));
        batch.draw(homebasetex, wpos.x - wdim.x / 2, wpos.y - wdim.y / 2, wdim.x, wdim.y); 
        font.setColor(Color.BLACK);
	    drawText(batch, font, "Ore: "+base.getOre(), mapEnvToWorld(pos), wdim.x, wdim.y/2);
        
        batch.end();
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
        carrytex.dispose();
        producertex.dispose();
        sentrytex.dispose();
        targettex.dispose();
    }

    public static void createEnvGui(String convid) 
    {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Marsworld");
        config.setWindowedMode(1200, 800);
        config.setForegroundFPS(10);
        new Lwjgl3Application(new EnvGui(convid), config);
    }

    public Vector2 convert(IVector2 val)
    {
    	return new Vector2(val.getXAsFloat(), val.getYAsFloat());
    }
}


