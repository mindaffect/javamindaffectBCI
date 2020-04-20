
package nl.ma.utopia.fakepresentation;
import nl.ma.utopiaserver.UtopiaClient;
import nl.ma.utopiaserver.messages.*;
import java.util.Random;
import java.util.List;
// stuff for the trigger port
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

public class FakePresentation implements Runnable  {
    public static String TAG="FakePres::";
    public static String DEFAULTHOST="127.0.0.1";
    public static int DEFAULTPORT=8400;
    public static String DEFAULTTRIGGERHOST=DEFAULTHOST;
    public static int DEFAULTTRIGGERPORT=8300;
    public static int VERBOSITY=2;
    public static int TIMEOUT_MS=250;
    public static int DEFAULTISI=20;
    public static int DEFAULTNSYMBS=8;
    public static int DEFAULTNTRIAL=10;
    public static int DEFAULTNEPOCH=100;
    public static String DEFAULTMODE="calibration.supervised";
    
    volatile UtopiaClient utopiaClient=null;
    volatile DatagramSocket trigSocket=null;
    volatile InetSocketAddress trigAddress=null;
    int nSymbs  =DEFAULTNSYMBS;    // number of symbols
    int isi_ms  =DEFAULTISI;  // inter-stimulus-interval
    int nTrials =DEFAULTNTRIAL;   // number of trails
    int nEpochs =DEFAULTNEPOCH;  // number of epochs per trial
    int iti_ms  =2000; // inter-trial-interval
    String calibrationMode=DEFAULTMODE;
    
 public static void main(String[] args) throws java.io.IOException,InterruptedException {
        System.out.println("FakePresentation utopiahost:utopiaport triggerhost:triggerport isi_ms nSymbs nTrials nEpochs calmode noiesStr");
		  
        // TODO []: get the defaults from the utopiaclient...
        String host = null; // UtopiaClient.DEFAULTHOST;
        int port = DEFAULTPORT; // UtopiaClient.DEFAULTPORT;
        int spawnutopiaserver=0; 
	  if (args.length>=1 && !args[0].equals("-")) {
			host = args[0];
			int sep = host.indexOf(':');
			if ( sep>0 ) {
				 port=Integer.parseInt(host.substring(sep+1,host.length()));
				 host=host.substring(0,sep);
			}
	  }
        System.out.println(TAG+"Utopia Host: " + host + ":" + port);
        String trighost = DEFAULTTRIGGERHOST; 
        int trigport = DEFAULTTRIGGERPORT;
        if( args.length>=2 &&  !args[1].equals("-") ) {
				trighost = args[1];
				int sep = trighost.indexOf(':');
				if ( sep>0 ) {
					 trigport=Integer.parseInt(trighost.substring(sep+1,trighost.length()));
					 trighost=trighost.substring(0,sep);
				}
        }
        System.out.println(TAG+"Trigger Host: " + trighost + ":" + trigport);
        int isi_ms=DEFAULTISI;
        if( args.length>=3 ){
            try{
                isi_ms=Integer.parseInt(args[2]);
            } catch ( NumberFormatException ex ) {
            }
        }
        System.out.println(TAG+"Inter Stimuls Interval (ms): " + isi_ms);
        int nSymb=DEFAULTNSYMBS;
        if( args.length>=4 ){
            try{
                nSymb=Integer.parseInt(args[3]);
            } catch ( NumberFormatException ex ) {
            }
        }
        System.out.println(TAG+"Number Symbols: " + nSymb);
        int nTrial=DEFAULTNTRIAL;
        if( args.length>=5 ){
            try{
                nTrial=Integer.parseInt(args[4]);
            } catch ( NumberFormatException ex ) {
            }
        }
        System.out.println(TAG+"Number Trials: " + nTrial);
        int nEpoch=DEFAULTNEPOCH;
        if( args.length>=6 ){
            try{
                nEpoch=Integer.parseInt(args[5]);
            } catch ( NumberFormatException ex ) {
            }
        }
        System.out.println(TAG+"Number Epochs: " + nEpoch);
        String calmode=DEFAULTMODE;
        if( args.length>=7 ){
            calmode=args[6];
        }
        System.out.println(TAG+"Caibration Mode: " + calmode);
        float noiseStr=.5f;
        if( args.length>=8 ){
            try{
                noiseStr=(float)Double.parseDouble(args[7]);
            } catch ( NumberFormatException ex ) {
            }
        }
        System.out.println(TAG+"Noise Strength: " + noiseStr);
        // override the default connection locations..        
	FakePresentation fp =new FakePresentation();
        fp.connect(host,port);
        fp.connectTrigger(trighost,trigport);
        fp.isi_ms =isi_ms;
        fp.nSymbs =nSymb;
        fp.nTrials=nTrial;
        fp.nEpochs=nEpoch;
        fp.calibrationMode=calmode;
        fp.run();
    }

    public FakePresentation() {
        utopiaClient = new UtopiaClient();
        rng = new Random();
    }

    public boolean connect(String host, int port) { 
        // make the initial connection
        for(int j =0; j<20; j++){ // timeout and continue in debug-mode..
            try {
                utopiaClient.connect(host,port);
            } catch ( java.io.IOException ex ) {
                System.out.println(TAG+"Couldnt connect to " + host + ":" + port);
                System.out.println(ex);
            }
            if( !utopiaClient.isConnected() ) {
                System.out.println(TAG+"Couldnt connect to " + host + ":" + port + " waiting...");
                try { Thread.sleep(1000); } catch( InterruptedException ex ){}
            } else { // initial-heartbeats..
                System.out.println(TAG+"Connected to " + host + ":" + port);
                System.out.println(TAG+"Sending initial heartbeats....");
                for ( int i=0; i<10; i++ ) {
                    try { utopiaClient.sendMessage(new Heartbeat(utopiaClient.gettimeStamp())); } catch ( java.io.IOException ex ) {}
                    try{ Thread.sleep(100); } catch( InterruptedException ex ){}
                }
                break;
            }
        }
        return utopiaClient.isConnected();
    }
    public boolean connectTrigger(String host, int port) {
        try { 
            trigSocket = new DatagramSocket();
        } catch ( java.net.SocketException ex ) {
            System.err.println(TAG+"UDP Socket already bound?");
            System.err.println(ex.getMessage());
        }
        trigAddress = new InetSocketAddress(host,port);
        return true;
    }    

    protected StimulusEvent mkStimulusEvent(boolean[] flashObj) {
        int ts   = utopiaClient.gettimeStamp();
        // TODO [] : pre-make and store these?
        int[] objIDs   = new int[nSymbs]; 
        int[] objState = new int[nSymbs];
        for ( int i=0; i<objIDs.length; i++){
            objIDs[i]=i+1;
            objState[i]=flashObj[i]?1:0;
        }
        return new StimulusEvent(ts,objIDs,objState);
    }
    protected StimulusEvent mkStimulusEvent(int flashObj) {
        int ts   = utopiaClient.gettimeStamp();
        // TODO [] : pre-make and store these?
        int[] objIDs   = new int[nSymbs]; 
        int[] objState = new int[nSymbs];
        for ( int i=0; i<objIDs.length; i++){
            objIDs[i]=i+1;
            objState[i]=0;
            if( objIDs[i]==flashObj ) objState[i]=1;
        }
        return new StimulusEvent(ts,objIDs,objState);
    }
    protected StimulusEvent mkStimulusEvent(int flashObj, int tgtObj) {
        int ts   = utopiaClient.gettimeStamp();
        // TODO [] : pre-make and store these?
        int[] objIDs   = new int[nSymbs+1]; // N.B. +1 for objID==0
        int[] objState = new int[nSymbs+1];
        // set the target object info
        objIDs[0]=0;
        if( flashObj==tgtObj ) objState[0]=1;
        // set the rest of the objects info
        for ( int i=1; i<objIDs.length; i++){
            objIDs[i]=i;
            objState[i]=0;
            if( objIDs[i]==flashObj ) objState[i]=1;
        }
        return new StimulusEvent(ts,objIDs,objState);
    }
    protected StimulusEvent mkStimulusEvent(boolean[] flashObj, int tgtObj) {
        int ts   = utopiaClient.gettimeStamp();
        // TODO [] : pre-make and store these?
        int[] objIDs   = new int[nSymbs+1]; // N.B. +1 for objID==0
        int[] objState = new int[nSymbs+1];
        // set the target object info
        objIDs[0]=0;
        objState[0]=flashObj[tgtObj-1]?1:0; // same as tgtObj state
        // set the rest of the objects info
        for ( int i=1; i<objIDs.length; i++){
            objIDs[i]=i;
            objState[i]=flashObj[i-1]?1:0; // N.B. index=id-1
        }
        return new StimulusEvent(ts,objIDs,objState);
    }

    protected boolean sendMessage(UtopiaMessage msg){
        try {
            utopiaClient.sendMessage(msg);
        } catch ( java.io.IOException ex ) {
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }
    protected boolean sendMessageUDP(UtopiaMessage msg){
        try { 
            utopiaClient.sendMessageUDP(msg);
        } catch ( java.io.IOException ex ) {
            System.err.println(ex.getMessage());
            return false;
        }
        return true; // assume it went ok
    }
        
    protected boolean sendTrigger(byte value) {
        boolean sent=false;
        try {
            DatagramPacket trigmsg = new DatagramPacket(new byte[]{value},1,trigAddress);
            trigSocket.send(trigmsg);
            sent=true;
        } catch ( java.io.IOException ex ) {
        }
        return sent;
    }
    
    Random rng=null;
    float noiseStr=.5f; // noise...
    String curMode=null;
    // main loop for the fake presentation
    public void run(){
        runTrials(calibrationMode);
	runTrials("prediction.static");
    }

    protected void runTrials(String mode){
        curMode=mode;
        System.out.println(TAG+"Starting "+curMode);
        sendMessage(new ModeChange(-1,curMode));
        boolean[] curFlash=new boolean[nSymbs];// to hold the flash stat
        int curTarget=0;
        for ( int ti=0 ; ti < nTrials; ti++ ){
            // get current target
            curTarget = rng.nextInt(nSymbs)+1; // N.B. objID start at 1
            System.out.print(ti+") tgt="+curTarget); 
            for ( int ei=0; ei<nEpochs; ei++){
                // get symb flashed this time
                for ( int i=0; i<nSymbs; i++ ){
                    curFlash[i]=rng.nextBoolean();
                }
                //System.out.print(curFlash + " ");
                // Flash this symbol
                doFlash(curFlash,curTarget);
                // wait for the isi
                sleepMilliSec(isi_ms);
            }
            System.out.println();
            // wait for the inter-trial-interval
            sleepMilliSec(iti_ms);
            sendMessage(new NewTarget(-1));
        }
        sendMessage(new ModeChange(-1,"Idle"));        
    }

    /*
     * Do the actual processing required for a flash 
     * @param flashObj - the objectID which is being flashed
     * @param tgtObj   - the objectID which is the current target object (if known)
     */
    protected void doFlash(int flashObj, int tgtObj){
        boolean tgtFlash = (flashObj==tgtObj);
        //System.out.print("(" + flashObj + "==" + tgtObj + ")=" + tgtFlash);
        sendMessage(mkStimulusEvent(flashObj,tgtObj));
        double trigval= (tgtFlash?1.0:0.0) + rng.nextGaussian()*noiseStr;
        sendTrigger((byte)(trigval>1.0?1:0));
        System.out.print(tgtFlash?"*":".");
    }
    /*
     * Do the actual processing required for a flash 
     * @param flashObj - boolean indicator of the set of object IDs being flashed
     * @param tgtObj   - the objectID which is the current target object (if known)
     */
    protected void doFlash(boolean[] flashObj, int tgtObj){
        boolean tgtFlash = (flashObj[tgtObj-1]); // N.B. idx=id-1
        sendMessage(mkStimulusEvent(flashObj,tgtObj));
        sendTrigger((byte)(tgtFlash?1:0));
        System.out.print(tgtFlash?"*":".");
    }

    /*
     * Sleep for indicated milliseconds, return false if interrupted
     */
    protected boolean sleepMilliSec(int ms){
        try {
            Thread.sleep(ms);
        } catch ( InterruptedException ex ) {
            return false;
        }
        return true;
    }
}
