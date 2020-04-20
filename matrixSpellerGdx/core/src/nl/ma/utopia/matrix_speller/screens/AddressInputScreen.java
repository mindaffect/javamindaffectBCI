package nl.ma.utopia.matrix_speller.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class AddressInputScreen extends InstructScreen {
    public String host;
    public int port;
    
    public AddressInputScreen(String host, int port){
        super("Please enter host:port in diaglog box");
        this.host=host;
        this.port=port;
    }

    @Override
    public void start(){
        super.start();
        AddressListener listener=new AddressListener();
        Gdx.input.getTextInput(listener, "Enter Utopia Server Address", this.host, "hostname:port");
    }

    public class AddressListener implements Input.TextInputListener {
        @Override
        public void canceled() {
            host = "127.0.0.1";
            port = 1972;            
            setDuration(0); // set to finish the screen
        }

        @Override
        public void input(String text) {
            try {
                String split[] = text.split(":");
                if( split.length>1 ) {
                    host = split[0];
                    port = Integer.parseInt(split[1]);
                } else {
                    host = text;
                }
            } catch (NumberFormatException e) {
                AddressListener listener = new AddressListener();
                Gdx.input.getTextInput(listener, "Bad buffer address", text, "");
            } catch (ArrayIndexOutOfBoundsException e) {
                AddressListener listener = new AddressListener();
                Gdx.input.getTextInput(listener, "Bad buffer address", text, "");
            }
            // when valid address then finish this screen.
            setDuration(0);
        }
    }

}
