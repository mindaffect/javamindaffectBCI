package nl.ma.utopia.matrix_speller.screens;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.InputAdapter;

import java.io.IOException;

import nl.ma.utopiaserver.messages.Heartbeat;
import nl.ma.utopiaserver.UtopiaClient;

public class ConnectingScreen extends InstructScreen {
    UtopiaClient client;
    public String host;
    public int port;
    float timeSinceAction;
    public static final float RECONNECTINTERVAL=1;
    private static final float HEARTBEATINTERVAL = .05f;

    public ConnectingScreen(UtopiaClient client, String host, int port) {
        super("Connecting to server\n\n\n(Press to continue in disconnected mode)");
        this.client = client;
        this.host = host;
        this.port = port;
        timeSinceAction =0;
        setDuration(30);
    }

    @Override
    public void start() {
        super.start();
        Gdx.input.setInputProcessor(
                new InputAdapter() {
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
                }
        );
    }

    @Override
    public void update(float delta) {
        timeSinceAction += delta;
        if ( !client.isConnected() ) {
            if (timeSinceAction > RECONNECTINTERVAL) {
                timeSinceAction = 0;
                try {
                    client.connect(host, port);
                } catch (IOException ex) {
                    setInstruction("Couldn't connect to server " + host + ":" + port + " ... waiting" +
                            "\n\n" + ((int) getTimeSpent_ms()));
                }
            }
            if (client.isConnected()) {
                setInstruction("Connected " + host + ":" + port + "!");
                setDuration_ms((int)(getTimeSpent_ms() + 1000)); // set to run the heartbeat set
                timeSinceAction=0;
            }
        }
        if( client.isConnected() ){
            if( timeSinceAction > HEARTBEATINTERVAL ){
                timeSinceAction=0;
                try {
                    client.sendMessage(new Heartbeat(client.gettimeStamp()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
