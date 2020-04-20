package nl.ma.utopia.matrix_speller.screens;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import nl.ma.utopiaserver.TimeStampClock;

public abstract class StimulusScreen implements Screen {
    public static final Color WINDOWBACKGROUNDCOLOR=new Color(.2f,.2f,.2f,1f);
    protected int duration_ms;
    protected long startTime = 0;

    public int getWidth(){
        return Gdx.graphics.getBackBufferWidth();
    }
    public int getHeight() { return Gdx.graphics.getBackBufferHeight(); }

    public void setDuration_ms(int ms){duration_ms = ms;}
    public void setDuration(float duration){setDuration_ms((int) (1000 * duration));}

    public int getDuration_ms() {
        return duration_ms;
    }

    public static final long getTime_ms() { return TimeStampClock.getTime();}
    
    public long getTimeLeft_ms() {
        return (startTime + duration_ms) - getTime_ms();
    }
    public long getTimeSpent_ms(){ return getTime_ms() - startTime;}
    
    public void start() {
        startTime = getTime_ms();
        donelogged=false;
        Gdx.app.log(this.getClass().getSimpleName(), "Start at: " + startTime + ", duration: " + duration_ms);
    }

    private boolean donelogged=false;
    public boolean isDone() {
        long ts = getTime_ms();
        long elapsed = ts - startTime ;
        boolean done = elapsed > duration_ms;
        if(done) {
            if (!donelogged) { // guard for logging lots of times..
                Gdx.app.log(this.getClass().getSimpleName(), "Run-time: " + (getTime_ms() - startTime));
                donelogged=true;
            }
        } else {
            donelogged=false;
        }
        return done;
    }

    public abstract void draw();
    public abstract void update(float delta);
    public void update(float delta, long frametime){ update(delta); }

    public void setisDoneOnKeyPress() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                setDuration(0); // Force finish.
                return true;
            }

            @Override
            public boolean touchUp(int screenx, int screeny, int pointer, int button) {
                setDuration(0);
                return true;
            }
        });
    }
    //-------------------- methods from GDXScreen from here -----------

    @Override
    public void render(float delta) {
        update(delta);
        draw();
    }

    /**
     * render with given frame-time
     * @param delta
     * @param frametime - time-stamp for when the frame was rendered (in ms)
     */
    public void render(float delta, long frametime){
        update(delta,frametime);
        draw();
    }

    @Override
    // redirect show->start the screen and it's clock
    public void show(){
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
        // Hmmm, not supported yet!
    }

    @Override
    public void resume() {
        // Hmmm, not supported yet!
    }

    @Override
    public void hide() {
        // Hmm, not supported yet!
    }

    @Override
    public void dispose() {
        // Hmm, not supported yet!
    }
}
