package nl.ma.utopia.matrix_speller.screens;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class InstructWaitKeyScreen extends InstructScreen {
    public InstructWaitKeyScreen() {
        this("");
    }

    public InstructWaitKeyScreen(String instruction) {
        super(instruction);
    }

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
            public boolean touchUp(int screenx, int screeny, int pointer, int button){
                setDuration(0);
                return true;
            }
        });
    }
}