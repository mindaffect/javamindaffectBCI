package nl.ma.utopia.matrix_speller.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.Graphics.DisplayMode;
import nl.ma.utopia.matrix_speller.MatrixSpellerGame;

public class DesktopLauncher {
    final static String usage="DesktopLauncher host:port stimulusFile nSymbs triallength_s";

    public static void main (String[] args) {
        DisplayMode displayMode=LwjglApplicationConfiguration.getDesktopDisplayMode();
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.setFromDisplayMode(displayMode);
        config.fullscreen=false;
        config.vSyncEnabled=true; // try to force vsync
        config.width=640; config.height=480;


        System.out.println("Usage: " + usage);
	  String host = "localhost";
	  int port    = 8400;
	  if (args.length>=1) {
				host = args[0];
				int sep = host.indexOf(':');
				if ( sep>0 ) {
					 port=Integer.parseInt(host.substring(sep+1,host.length()));
					 host=host.substring(0,sep);
				}
	  }			
	  System.out.println("Utopia Host: " + host + ":" + port);
		  // TODO [] : if not set then spawn (for an interval) a dialog
		  // to ask for the destination host+port
		// Open the file from which to read the classifier parameters
      String stimulusFile=null;
		if (args.length>=2) {
			 stimulusFile = args[1];
		}
      System.out.println("stimulusFile = " + stimulusFile);

      int nSymbs=-1;
		if (args.length>=3) {
			try {
				 nSymbs = Integer.parseInt(args[2]);
			}
			catch (NumberFormatException e) {
				 System.err.println("Couldnt understand your number symbols");
			}			 
		}
		System.out.println("nSymbs: " + nSymbs);

      float seqDuration=-1;
		if (args.length>=4) {
			try {
				 seqDuration = Float.parseFloat(args[3]);
			}
			catch (NumberFormatException e) {
				 System.err.println("Couldnt understand your sequenceDuration");
			}			 
		}
		System.out.println("trialLen_s: " + seqDuration);

      MatrixSpellerGame msg=new MatrixSpellerGame();
      if( nSymbs>0 ) msg.nSymbs=nSymbs;
      if( stimulusFile!=null ) msg.stimulusFile=stimulusFile;
      if( seqDuration>0 )  msg.seqDurationCalibrate =seqDuration;

      // now launch the app
      new LwjglApplication(msg, config);
	}
}
