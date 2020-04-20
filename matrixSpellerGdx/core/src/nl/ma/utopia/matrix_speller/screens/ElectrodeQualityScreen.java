package nl.ma.utopia.matrix_speller.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.io.IOException;
import java.util.List;

// stuff for the trigger port
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import nl.ma.utopiaserver.messages.SignalQuality;
import nl.ma.utopiaserver.UtopiaClient;
import nl.ma.utopiaserver.messages.UtopiaMessage;

public class ElectrodeQualityScreen extends StimulusScreen {
    private Color[] _colors={new Color(.05f,.05f,.05f, 1.0f), // bgColor  dark GREY
                             new Color(1f,0f,0f, 1.0f),    // minColor RED
                             new Color(0f,1f,0f, 1.0f)    // maxColor GREEN
    }; 
    private static final BitmapFont font = new BitmapFont();
            // new BitmapFont(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"),
            //         Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"),
            //         false,true);
    SpriteBatch batch;
    private GlyphLayout layout[]; // used for the rendered strings
    private Texture background;
    private UtopiaClient client;
    private int nCh;
    private float[] quality;
    private float width;
    private float height;
    
    public ElectrodeQualityScreen(int nCh, UtopiaClient client){ // Initialize variables
        this.client  = client;
        batch = new SpriteBatch();
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        setSymbols(nCh);
    }

    public void setSymbols(int nCh){
        this.nCh = nCh;

        float scale = height/ (font.getLineHeight() * 5*5);
        font.getData().scaleX=scale;
        font.getData().scaleY=scale;

        // make the foreground letters
        layout = new GlyphLayout[nCh];
        for ( int xi=0; xi<nCh; xi++){
            layout[xi]=new GlyphLayout();
            //setText(BitmapFont font, java.lang.CharSequence str, Color color, float targetWidth, int halign, boolean wrap)
            layout[xi].setText(font,"Ch"+(xi+1));
        }
        //make the background Texture
        int bgwidth=256;
        int bgheight=bgwidth;
        int cornerRadius = (int)(bgwidth*.5f*.9f);
        int cornerOffset = (int)(bgwidth*.5f); // leave 10% margin between symbols
        Pixmap pm = new Pixmap(bgwidth, bgheight, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fillCircle(cornerOffset,       cornerOffset,        cornerRadius); // circle
        background = new Texture(pm);
    }

    @Override
    // N.B. be sure to synchronize to work cross-thread
    synchronized public void update(float delta) {
        // client message stuff...
        // stuff to do if we changed what's on screen
        if (client != null && client.isConnected()) {
            try {
                // check for new prediction messages from the server
                List<UtopiaMessage> msgs = client.getNewMessages();
                // get the last PredictedTargetProb message
                for (UtopiaMessage msg : msgs) {
                    if (msg.msgID() == SignalQuality.MSGID) {
                        SignalQuality eq = (SignalQuality) msg;
                        this.quality = eq.signalQuality;
                    }
                }
                if( !msgs.isEmpty() ) {
                    System.out.println("Got " + msgs.size() + " messages");
                    if (this.quality !=null) {
                        System.out.println("Qualities: " + this.quality);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
        
    @Override
    public void draw() {
        int nCh= this.nCh;
        if ( quality!=null ) nCh=Math.min(nCh,quality.length);
        float xstep = width / (nCh+1);
        float ystep = height / 5;
        float bsize = Math.min(xstep, ystep);

        Gdx.gl.glClearColor(WINDOWBACKGROUNDCOLOR.r,WINDOWBACKGROUNDCOLOR.g,WINDOWBACKGROUNDCOLOR.b,WINDOWBACKGROUNDCOLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        int si = 0, yi=1;
        for (int xi = 0; xi < nCh; xi++) {
            float x = (xi + .5f) * xstep;
            float y = height - (yi + 1.5f) * ystep;
            float noisestr=1f;//(float)Math.random();
            if( quality!=null && xi<quality.length ) noisestr=quality[xi];
            noisestr=noisestr>1f?1f:noisestr; noisestr=noisestr<0f?0f:noisestr; // limit to 0-1

            // color for this output is a linear mix btw red and green
            Color curColor = new Color(0,0,0,1);
            curColor.r= _colors[1].r*noisestr + _colors[2].r*(1-noisestr);
            curColor.g= _colors[1].g*noisestr + _colors[2].g*(1-noisestr);
            curColor.b= _colors[1].b*noisestr + _colors[2].b*(1-noisestr);

            batch.setColor(curColor);//curColor);
            // draw the block background
            batch.draw(background, x, y, xstep, ystep);//bsize, bsize);

            // draw the foreground text
            font.draw(batch, layout[xi], x+xstep*.4f, y+ystep*.6f);
        }
        // reset the current pen color
        batch.setColor(Color.WHITE);
        batch.end();
    }
};
