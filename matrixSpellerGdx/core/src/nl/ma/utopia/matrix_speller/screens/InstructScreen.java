package nl.ma.utopia.matrix_speller.screens;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class InstructScreen extends StimulusScreen {
    private static final BitmapFont font = new BitmapFont();
            // new BitmapFont(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"),
            //         Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"),
            //         false,true);
    private String instruction;
    private SpriteBatch batch;
    //private GlyphLayout layout;
    // BODGE: TODO []: the batch auto-scales to fill the window on resize, thus
    //        store the initial size to draw stimuli in the right place
    private float width;
    private float height;

    public InstructScreen() {
        this("");
    }


    public InstructScreen(String instruction) {
        batch = new SpriteBatch();
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        //layout = new GlyphLayout();
        setInstruction(instruction);
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
        font.getData().setScale(1,1); // reset scale so compute correctly
        float lineHeight = font.getLineHeight();
        float screenlines = Math.min(height / lineHeight, 20f); // max num lines at scale=1 on screen
        float scale = height / screenlines / lineHeight; // scale*lineHeight = height/screenLines = desired height.
        // rescale the font to the screen size
        font.getData().setScale(scale,scale);
    }

    @Override
    public void draw() {

        Gdx.gl.glClearColor(WINDOWBACKGROUNDCOLOR.r,WINDOWBACKGROUNDCOLOR.g,WINDOWBACKGROUNDCOLOR.b,WINDOWBACKGROUNDCOLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float x = (width* .1f);
        float y = (height - (height * .3f)); // In GL, Y is up!

        batch.begin();
        font.draw(batch, instruction, x, y);
        //font.draw(batch, layout, x, y);
        batch.end();
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
    }
}
