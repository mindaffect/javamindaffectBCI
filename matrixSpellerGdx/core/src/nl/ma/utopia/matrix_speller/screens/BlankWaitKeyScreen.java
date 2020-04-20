package nl.ma.utopia.matrix_speller.screens;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;

/**
 * Created by Lars on 1-12-2015.
 */
public class BlankWaitKeyScreen extends BlankScreen {
    @Override
    public void start() {
        super.start();
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
}
