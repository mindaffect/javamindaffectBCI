package nl.ma.utopia.matrix_speller.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/**
 * Created by Lars on 1-12-2015.
 */
public class BlankScreen extends StimulusScreen {

    @Override
    public void draw() {
        Gdx.gl.glClearColor(WINDOWBACKGROUNDCOLOR.r,WINDOWBACKGROUNDCOLOR.g,WINDOWBACKGROUNDCOLOR.b,WINDOWBACKGROUNDCOLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void update(float delta) {
    }
}
