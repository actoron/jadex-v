package jadex.bt.cleanerworld.gui.libgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
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

import jadex.bt.cleanerworld.environment.ILocation;
import jadex.bt.cleanerworld.environment.impl.Chargingstation;
import jadex.bt.cleanerworld.environment.impl.Cleaner;
import jadex.bt.cleanerworld.environment.impl.Environment;
import jadex.bt.cleanerworld.environment.impl.Location;
import jadex.bt.cleanerworld.environment.impl.Waste;
import jadex.bt.cleanerworld.environment.impl.Wastebin;


public class EnvGui extends ApplicationAdapter 
{
    final Environment env = Environment.getInstance();

    protected SpriteBatch batch;
    protected OrthographicCamera camera;
    protected Viewport viewport;
    
    protected ShapeRenderer shaperen;
    protected BitmapFont font;
 
    protected Texture background;
    protected Texture wastetex;
    protected Texture wastebintex; 
    protected Texture wastebinfulltex; 
    protected Texture stationtex;
    protected Texture cleanertex;
    protected Texture backgroundtex;
    protected Texture uptex;
    protected Texture downtex;
    protected Texture daytex;
    protected Texture nighttex;
    
    @Override
    public void create() 
    {
    	font = new BitmapFont();
        batch = new SpriteBatch();
        shaperen = new ShapeRenderer();
        background = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/background.png"));
        wastetex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/waste.png"));
        wastebintex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/wastebin.png"));
        wastebinfulltex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/wastebin_full.png"));
        stationtex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/chargingstation.png"));
        cleanertex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/cleaner.png"));
        backgroundtex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/background.png"));
        uptex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/up.png"));
        downtex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/down.png"));
        daytex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/day.png"));
        nighttex = new Texture(Gdx.files.internal("jadex/bt/cleanerworld/gui/images/night.png"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        
        viewport = new StretchViewport(800, 600, camera);
        viewport.apply();
    }

    protected Vector2 convertToVector2(ILocation loc, int w, int h) 
    {
        if (w <= 0) w = Gdx.graphics.getWidth();
        if (h <= 0) h = Gdx.graphics.getHeight();
        return new Vector2((float)(loc.getX() * w), (float)(loc.getY() * h));
    }
    
    @Override
    public void resize(int width, int height) 
    {
        viewport.update(width, height);
    }
    
    public void render() 
    {
    	boolean handledclick = false;
    	
        // Clear the screen with a neutral color
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1); 
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        batch.begin();

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        
        Vector2 mpos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mpos);
        float mousex = mpos.x;
        float mousey = mpos.y;
        
        //System.out.println("mouse: "+mousex+" "+mousey);
        
        // Draw the background using the viewport dimensions
        background.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        //batch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.draw(background, 0, 0, w, h, 0, 0, w / background.getWidth(), h / background.getHeight());
        
        // Render waste bins
        for(Wastebin bin : env.getWastebins()) 
        {
            Texture bintex = bin.isFull() ? wastebinfulltex : wastebintex;
            Vector2 pos = convertToVector2(bin.getLocation(), w, h);  
            batch.draw(bintex, pos.x, pos.y);

            // Optionally draw bin info (e.g. ID and capacity)
            String info = bin.getId() + " (" + bin.getWastes().length + "/" + bin.getCapacity() + ")";
            //BitmapFont font = new BitmapFont();  
            //font.draw(batch, info, pos.x, pos.y - 20);  
            drawText(batch, font, info, pos.x, pos.y, bintex.getWidth(), 10);
            
            float distance = Vector2.dst(mousex, mousey, pos.x + bintex.getWidth() / 2, pos.y + bintex.getHeight() / 2);
            
            if(distance < 100) 
            {
            	float xsu = pos.x + bintex.getWidth() / 2 - 16;
            	float ysu = pos.y + bintex.getHeight() + 2;
            	float xsd = pos.x + bintex.getWidth() / 2;
            	float ysd = pos.y + bintex.getHeight() + 2;
                batch.draw(uptex, xsu, ysu);
                batch.draw(downtex, xsd, ysd);
                
                if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) 
                {
                    if(isMouseOverIcon(mousex, mousey, xsu, ysu, uptex.getWidth(), uptex.getHeight())) 
                    {
                    	//System.out.println("fill bin: "+bin);
                    	Wastebin ebin = env.getWastebin(bin.getId());
                        if(ebin!=null)
                        	ebin.fill();
                        handledclick = true;
                    } 
                    else if(isMouseOverIcon(mousex, mousey, xsd, ysd, downtex.getWidth(), downtex.getHeight()))
                    {
                    	Wastebin ebin = env.getWastebin(bin.getId());
                        if(ebin!=null)
                        	ebin.empty();
                        handledclick = true;
                    }
                }
            }
        }

        // Render waste
        for(Waste waste : env.getWastes()) 
        {
            Vector2 pos = convertToVector2(waste.getLocation(), w, h);
            batch.draw(wastetex, pos.x, pos.y);
        }

        // Render charging stations
        for(Chargingstation station : env.getChargingstations()) 
        {
            Vector2 pos = convertToVector2(station.getLocation(), w, h);
            batch.draw(stationtex, pos.x, pos.y);
            
            // Optionally draw station ID
            //BitmapFont font = new BitmapFont();
            //font.draw(batch, station.getId(), pos.x, pos.y - 20);
            //drawText(batch, font, station.getId(), (float)(pos.x+Math.random()*10), pos.y, stationtex.getWidth(), 10);
            drawText(batch, font, station.getId(), pos.x, pos.y, stationtex.getWidth(), 10);
            
            //System.out.println("station: "+pos.x+" "+pos.y);
        }

        // Render cleaners
        for(Cleaner cleaner : env.getCleaners()) 
        {
            Vector2 pos = convertToVector2(cleaner.getLocation(), w, h);
            
            //int colorcode	= Math.abs(cleaner.getId().toString().hashCode()%8);
			float vw = (float)cleaner.getVisionRange()*w;
			float vh = (float)cleaner.getVisionRange()*h;
			//System.out.println("vw: "+vw+" "+vh);
            
			float mx = pos.x+cleanertex.getWidth()/2;
			float my = pos.y+cleanertex.getHeight()/2;
			
            batch.end();
            batch.begin();
			Gdx.gl.glEnable(GL20.GL_BLEND);
		    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	        shaperen.begin(ShapeRenderer.ShapeType.Filled);
	        // (colorcode&1)!=0?255:100, (colorcode&2)!=0?255:100, (colorcode&4)!=0?255:100
	        shaperen.setColor(1, 0, 0, 0.5f); 
	        shaperen.ellipse(mx-vw, my-vh, vw*2, vh*2); 
	        shaperen.end();
		    Gdx.gl.glDisable(GL20.GL_BLEND);
		    batch.end();

		    batch.begin();
		    batch.draw(cleanertex, pos.x, pos.y);
            String info = cleaner.getId() + "\nBattery: " + (int)(cleaner.getChargestate() * 100) + "%" +
            	"\nWaste: " + (cleaner.getCarriedWaste() != null ? "Yes" : "No");
            //BitmapFont font = new BitmapFont();
            drawText(batch, font, info, pos.x, pos.y, cleanertex.getWidth(), 10);
            //font.draw(batch, info, pos.x + 45, pos.y);
        }
        
        batch.end();
        batch.begin();
        float iconx = 10;  
        float icony = 560;  
        float iconw = 32;  
        float iconh = 32; 
        if(env.getDaytime()) 
        {
            batch.draw(nighttex, iconx, icony, iconw, iconh);  
        } 
        else 
        {
            batch.draw(daytex, iconx, icony, iconw, iconh);  
        }
        batch.end();
        
        batch.begin();
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) 
        {
            if(!handledclick && isMouseOverIcon(mousex, mousey, iconx, icony, iconw, iconh)) 
            {
                env.toggleDaytime();
                handledclick = true;
            }
        }
 
        //wastecntlab.setText("Waste count: "+env.getWastes().length);
        
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shaperen.begin(ShapeRenderer.ShapeType.Filled);
        shaperen.setColor(0, 0, 0, env.getDaytime() ? 0f : 0.3f); 
        shaperen.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); 
        shaperen.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        //stage.act(Gdx.graphics.getDeltaTime());
        /*stage.getBatch().begin();
        stage.getBatch().draw(background, 0, 0, stage.getWidth(), stage.getHeight());
        stage.getBatch().end();*/
        //stage.draw();

        if(!handledclick && Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) 
        {
    		final Location	mouseloc = new Location((double)mousex/(double)w, (double)mousey/(double)h);
    		final double tol = 5/(double)h;

    		final Environment env = Environment.getInstance();
    		
    		Waste[] wastes = env.getWastes();
    		Waste nearest = null;
    		double dist = 0;
    		for(int i=0; i<wastes.length; i++)
    		{
    			if(nearest==null || wastes[i].getLocation().getDistance(mouseloc)<dist)
    			{
    				nearest = wastes[i];
    				dist = wastes[i].getLocation().getDistance(mouseloc);
    			}
    		}
    		Waste waste = null;
    		if(dist<tol)
    			waste = nearest;

    		// If waste is near clicked position remove the waste
    		if(waste!=null)
    		{
    			env.removeWaste(waste);
    		}

    		// If position is clean add a new waste
    		else
    		{
    			env.addWaste(new Waste(mouseloc));
    		}
        }
        
        batch.end();
    }
    
    private boolean isMouseOverIcon(float mousex, float mousey, float iconx, float icony, float iconw, float iconh) 
    {
        boolean ret = mousex >= iconx && mousex <= iconx + iconw && mousey >= icony && mousey <= icony + iconh;
        //System.out.println("isOver: "+ret+" "+mousex+" "+mousey+" "+iconx+" "+icony+" "+iconw+" "+iconh);
        return ret;
    }
    
    public void drawText(SpriteBatch batch, BitmapFont font, String text, 
    	float textureX, float textureY, float textureWidth, float offsetY) 
    {
		GlyphLayout layout = new GlyphLayout();
		layout.setText(font, text);
		float textureCenterX = textureX + textureWidth / 2;
		float textWidth = layout.width;
		float textX = textureCenterX - textWidth / 2;  
		float textY = textureY - offsetY;
		font.draw(batch, text, textX, textY);
	}
    
    protected Vector2 convertToVector2(ILocation loc, float w, float h)
    {
    	return new Vector2((float)(loc.getX()*w), (float)(loc.getY()*h));
    }
    
    @Override
    public void dispose() 
    {
        batch.dispose();
        background.dispose();
    }

    public static void createEnv() 
    {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Cleaner World");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(10);
        new Lwjgl3Application(new EnvGui(), config);
    }

    public static void main(String[] args) 
    {
        EnvGui.createEnv();
    }
}


