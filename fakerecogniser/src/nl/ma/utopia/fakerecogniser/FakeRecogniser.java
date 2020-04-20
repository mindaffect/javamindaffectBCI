package nl.ma.utopia.fakerecogniser;
import nl.ma.utopiaserver.UtopiaClient;
import nl.ma.utopiaserver.UtopiaServer;
import nl.ma.utopiaserver.messages.*;
import java.util.Random;
import java.util.List;

public class FakeRecogniser implements Runnable  {
    public static String DEFAULTHOST="127.0.0.1";
    public static int DEFAULTPORT=8400;
    public static int VERBOSITY=2;
    public static int TIMEOUT_MS=250;
    
    volatile UtopiaClient utopiaClient=null;    
	 public static void main(String[] args) throws java.io.IOException,InterruptedException {
        if ( args.length==0 ) {
				System.out.println("FakeRecogniser utopiahost:utopiaport spawnserverp");
		  }
        // TODO []: get the defaults from the utopiaclient...
        String host = DEFAULTHOST; // UtopiaClient.DEFAULTHOST;
        int port = DEFAULTPORT; // UtopiaClient.DEFAULTPORT;
		  if (args.length>=1) {
				host = args[0];
				int sep = host.indexOf(':');
				if ( sep>0 ) {
					 port=Integer.parseInt(host.substring(sep+1,host.length()));
					 host=host.substring(0,sep);
				}
				System.out.println("Utopia Host: " + host + ":" + port);
		  }
        int spawnutopiaserver=0; 
        if( args.length>=2 ){
            try {
                spawnutopiaserver = Integer.parseInt(args[1]);
            } catch ( NumberFormatException ex ) {
            }
        }
        // spawn a server if wanted.
        if( spawnutopiaserver>0 ) {
            System.out.println("Spawning UtopiaServer on : " + port);
            UtopiaServer us = new UtopiaServer(port);
	    us.VERBOSITY=0;
            Thread usthread = new Thread(us);
            usthread.start();
        }
        // override the default connection locations..        
		  FakeRecogniser fr =new FakeRecogniser();
        fr.connect(host,port);
        fr.run();
    }

    /** Constructor to setup the GUI components */
    public FakeRecogniser() {
        utopiaClient = new UtopiaClient();
        rng = new Random();
    }
    public boolean connect(String host, int port) { 
        // make the initial connection
        for(int j =0; j<20; j++){ // timeout and continue in debug-mode...
            try {
                utopiaClient.connect(host,port);
            } catch ( java.io.IOException ex ) {
                System.out.println("Couldnt connect to " + host + ":" + port + " waiting...");
                System.out.println(ex);
            }
            if( !utopiaClient.isConnected() ) {
                try { Thread.sleep(1000); } catch( InterruptedException ex ){}
            } else { // initial-heartbeats..
                System.out.println("Sending initial heartbeats....");
                for ( int i=0; i<10; i++ ) {
                    try { utopiaClient.sendMessage(new Heartbeat(utopiaClient.gettimeStamp())); } catch ( java.io.IOException ex ) {}
                    try{ Thread.sleep(100); } catch( InterruptedException ex ){}
                }
                break;
            }
        }
        if( utopiaClient.isConnected() ) {
            try { 
            utopiaClient.sendMessage(new Subscribe(utopiaClient.gettimeStamp(),"NMPES"));
            } catch ( java.io.IOException ex ) {
                System.out.println("Error subscribing to messages");
                ex.printStackTrace();
            }
        }
        return utopiaClient.isConnected();
    }

    enum ExptPhases { ELECTRODEQUALITY, CALIBRATE, PREDICTION, IDLE };
    Random rng=null;
    int curTargetIdx =-1; // index in objIDs for currently selected target
    float curPerr=0; // current estimated target error
    float signalStr=1f/60f; // approx 60 stimuli to hit perfect decoding
    float noiseStr =signalStr*2; // noise...
    String curMode=null;
    ExptPhases curPhase;
    // main loop for the fake recogniser
    public void run(){
        System.out.println("Waiting for messages");
        boolean modechanged=false;
        int trialEpochs=0; // time so far in this trial
        int otrialEpochs=trialEpochs;
        int[] objIDs = null;

        while ( true ){
            // Blocking call!!!
            List<UtopiaMessage> inmsgs=null;
            try {
                // get new messages and update the internal state
                inmsgs=utopiaClient.getNewMessages(TIMEOUT_MS);
            } catch ( java.io.IOException ex ) {
                System.out.println("Problem reading from stream"); 
                System.exit(-1);
            }
            otrialEpochs=trialEpochs; // to check if we got new epochs
            modechanged=false; // to check if we got new mode
            for( UtopiaMessage msg : inmsgs ) {
                if( VERBOSITY>=1 )
                    System.out.println("Got Message: " + msg.toString() + " <- server");
                if( msg.msgID() == ModeChange.MSGID ){
                    ModeChange mc = (ModeChange)msg;
                    String newmode = mc.newmode;
                        
                    if( curMode==null || !curMode.equalsIgnoreCase(newmode) ){
                        trialEpochs=0;                            
                        if( newmode.equalsIgnoreCase("calibrate.supervised") ) {
                            modechanged=true;
                            trialEpochs=0;
                            curPhase=ExptPhases.CALIBRATE;
                            System.out.println("Mode: " + newmode);
                        } else if ( newmode.equalsIgnoreCase("Prediction.static") ) {
                            modechanged=true;
                            trialEpochs=0;
                            curPhase=ExptPhases.PREDICTION;
                            System.out.println("Mode: " + newmode);
                        } else if ( newmode.equalsIgnoreCase("ElectrodeQuality") ) {
                            modechanged=true;
                            trialEpochs=0;
                            curPhase=ExptPhases.ELECTRODEQUALITY;
                            System.out.println("Mode: " + newmode);
                        } else {
                            modechanged=true;
                            trialEpochs=0;
                            curPhase=ExptPhases.IDLE;
                            System.out.println("Mode: " + "IDLE");
                        }
                                
                    }
                } else if ( msg.msgID() == NewTarget.MSGID ) {
                    trialEpochs=0;
                    objIDs=null;
                    curTargetIdx=-1;
                    System.out.println("NewTarget");
                }  else if ( msg.msgID() == StimulusEvent.MSGID ) {
                    // track the current set of valid objIDs
                    StimulusEvent se = (StimulusEvent)msg;
                    // TODO [] : make this merge objIDs
                    int[] newobjIDs = se.objIDs;
                    objIDs=se.objIDs;
                    trialEpochs = trialEpochs+1; // rec number epochs in trial
                }
            }

            // send events based on the current mode + trialTime
            if ( curPhase==ExptPhases.PREDICTION )  {
                if ( trialEpochs==0 || modechanged ) {
                    System.out.println("Restart Prediction");
                    // clear the prediction state
                    objIDs=null;
                    curTargetIdx =-1;
                    curPerr=-1;
                    trialEpochs=1; // bodge only resetart once
                }
                // identify a new targetIdx
                if( objIDs!=null
                        && (curTargetIdx <0 || curTargetIdx>=objIDs.length) ) {
                    // pick a new target
                    curTargetIdx = rng.nextInt(objIDs.length);
                    System.out.println("Set target: #" + curTargetIdx + "="+objIDs[curTargetIdx]);
                    curPerr=1.0f;
                }
                if ( objIDs!=null
                        && curTargetIdx >=0 && curTargetIdx<objIDs.length
                        && trialEpochs>otrialEpochs ) {
                    // update Perr
                    float delta = rng.nextFloat()*noiseStr+signalStr;
                    if( VERBOSITY>2 ) System.out.println("Delta:"+delta);
                    curPerr = curPerr - delta;
                    // bound check
                    if ( curPerr > 1.0f ) curPerr=1.0f;
                    if ( curPerr < 0.0f ) curPerr=0.0f;
                    try {
                        PredictedTargetProb ptp = new PredictedTargetProb(utopiaClient.gettimeStamp(),objIDs[curTargetIdx],curPerr);
                        if (VERBOSITY>1 ) System.out.println("Prediction:" + ptp + "-> server");
                        utopiaClient.sendMessage(ptp);
                    } catch ( java.io.IOException ex ){
                    }
                }
            } else { // TODO [] something in other modes?
            }                
            System.out.print('*');System.out.flush();
        }        
    }
}
