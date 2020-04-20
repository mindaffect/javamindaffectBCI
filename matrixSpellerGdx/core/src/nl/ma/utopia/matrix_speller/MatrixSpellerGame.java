package nl.ma.utopia.matrix_speller;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import nl.ma.utopia.matrix_speller.screens.*;
import nl.ma.utopiaserver.UtopiaClient;
import nl.ma.utopiaserver.messages.*;
import sun.security.ssl.Debug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;

public class MatrixSpellerGame extends ApplicationAdapter {
    private static final int FLIPLOGINTERVAL = 2000;
    private static final int LOGLEVEL=1;
    StimulusScreen screen; // the currently displayed screen
    UtopiaClient client;

    public int nSymbs;
    public int nSeqCalibration=10;
    public int nSeqPrediction =20;
    public float seqDurationCalibrate =4.2f;
    public float seqDurationPrediction=15.0f;
    public boolean cuedPrediction=true;
    public String stimulusFile="stimulus/mgold_61_6521_psk_60hz.txt";
    public float frameRate    =60; // Graphics re-draw rate
    public int   showOpto=1; // show the optosensor?
    public boolean doElectrodeQuality=false; // do we do the electrode quality stuff...
    public float selectionThreshold =.1f; // error threshold for selections in feedback phase

    float tgtDuration          =1.5f;
    float feedbackDuration     =1.5f;
    float interStimulusDuration=3.0f;

    public static final String DEFAULTHOST="10.0.0.5";
    public static final int DEFAULTPORT=8400;
    public String host;
    public int    port;

    // the default layout for the symbols set
    private static final String[][] DEFAULTSYMBOLS = {{"0","1","2"},{"3","4","5"},{"6","7","8"}};
    String[][] symbols;
    int MAXNSEQ=50;

    AddressInputScreen address;
    ConnectingScreen connecting;
    LetterMatrixScreen letter;
    ElectrodeQualityScreen electrodeQuality;
    InstructWaitKeyScreen instruct;
    BlankScreen   blank;
    BlankScreenExit exitScreen;

    FlipStats flipStats;
    FlipStats blankStats;
    FlipStats deltaStats;
    FlipStats sentStats;
    boolean waitBlankingp=false;
    private long fliplogtime=0;
    //private nl.ma.utopiaserver.BoundOffsetTracker bot;
    public static boolean REWRITEFLIPTIMES=true;
    private FrameTracker frameTracker;
    private int frameOffset=-1; // offset between GDX frame number and bot time...
    private double framedelta;
    private int mindrawduration=0;
    ShapeRenderer sr;

    @Override
	public void create () {
        flipStats = new FlipStats(400);
        blankStats= new FlipStats(400);
        sentStats  =new FlipStats(400);
        deltaStats =new FlipStats(400);
        // N.B. negative half-life means replace the lowest weight point...
        //bot = new nl.ma.utopiaserver.BoundOffsetTracker(30,true,-60*100, 100);
        frameTracker=new FrameTracker();
        // TODO[] : get the nomional frame rate from GDX
        frameTracker.setFreqs((60-.2)/1000,.05/1000,(60+.2)/1000);
        //bot.setM(1000.0d/60d);
        sr=new ShapeRenderer();

        // intialize connection to the RECOGNISER
        host=DEFAULTHOST;
        port=DEFAULTPORT;
        client = new UtopiaClient();

        // read the set of symbols to use from the config file
        FileHandle fh = Gdx.files.internal("symbols.txt");
        try {
            BufferedReader is = new BufferedReader(new InputStreamReader(fh.read()));
            symbols = readSymbols(is);
            is.close();
        } catch ( IOException ex ) {
            Gdx.app.log("IOEXCEPTION","Couldn't read the symbols file!  using default");
            Gdx.app.log("IOEXCEPTION",ex.toString());
            symbols = DEFAULTSYMBOLS;
        }

        nSymbs = symbols.length * symbols[0].length;

        // Setup the screen objects
        address = new AddressInputScreen(host,port);
        connecting = new ConnectingScreen(client,host,port);
        letter=new LetterMatrixScreen(symbols,client);
        letter.setshowOpto(showOpto); // set the opto status
        electrodeQuality = new ElectrodeQualityScreen(8,client);
        instruct=new InstructWaitKeyScreen();
        blank=new BlankScreen();
        exitScreen = new BlankScreenExit();

        Gdx.graphics.setVSync(true); // try to force display locked updates

        // set initial blank screen
        setScreen(blank);

        // start the thread for the experiment controller
        // TODO []: convert to not use controller thread so can run on html..
        startControllerThread();

        // // TODO [] : switch to non-continuous render mode so we have more control over
        // //  exactly when a frame gets re-drawn and the time it happens?
        //Gdx.graphics.setContinuousRendering(false);
        if( Gdx.graphics.isContinuousRendering() ) {
            Gdx.graphics.requestRendering();
        }
    }
    UtopiaClient getUtopiaClient() { return client; }    

    // internal object used to communicate between the
    // screen rendering thread and the experiment controller
    // thread, using wait() and notify() on this object
    private final Object _controller = new Object();
    private long framestart;
    private long frameend;
	@Override
	public void render () {
        // N.B. to avoid extra delay between call to screen.render and the actual frame flip
        // make the screen render the last thing in this function.
        // Force a waitBlanking at the start of this function, so we know that the next line
        // is as close as possible after the screen actually changed..
        //long bstart = getTime_ms();
        // manually track the frame-timing information
        framestart = getTime_ms();
        long frametime=framestart;
       // blankStats.addDeltaTime(tflip-bstart);
        long oft=frametime;

        long frameIdx = Gdx.graphics.getFrameId() + frameOffset;
        if ( REWRITEFLIPTIMES ) { // shift to estimated frame time
            double ob=frameTracker.b;
            frametime = (long)frameTracker.addPoint(frametime);
            if ( Math.abs(frameTracker.b-ob) > .1 ) {
                System.out.println("b:"+ob+"->"+frameTracker.b+" db="+(frameTracker.b-ob));
            }
        }
        flipStats.addFlipTime(oft);
        deltaStats.addDeltaTime(oft-frametime);
        sentStats.addFlipTime(frametime);
        if( LOGLEVEL>0 && client!=null && client.isConnected()) {
            try {
                client.sendMessage(new Log((int)oft, "Flip:" + Gdx.graphics.getFrameId() + " frameIdx:" + frameIdx));
            } catch ( IOException ex ){
                System.out.println(ex.getStackTrace());
            }
        }

        // draw the current screen
        if ( screen != null ) {
            //waitBlanking();
            screen.render(Gdx.graphics.getRawDeltaTime(),frametime);
            //Gdx.gl.glFlush();
            //Gdx.gl.glFinish();
            //waitBlanking();
            if (screen.isDone()) {
                if ( frametime > fliplogtime)
                {
                    //System.out.println("BlankStats:"+blankStats);
                    System.out.println(this.getClass().getSimpleName()+" FlipStats:"+flipStats); flipStats.reset();
                    System.out.println(this.getClass().getSimpleName()+" DeltaStats:"+deltaStats); deltaStats.reset();
                    //System.out.println(this.getClass().getSimpleName()+" sentStats:"+sentStats); sendStats.reset();
                    fliplogtime = frametime + FLIPLOGINTERVAL;
                }
                // if this screen is finished, then notify the main controller
                // thread to move onto to the next experiment phase
                synchronized(_controller) {
                    _controller.notify();
                }
            }
        }
        // manually block until last flip completed?
        //waitBlanking();
        if( !Gdx.graphics.isContinuousRendering() ) {
            Gdx.graphics.requestRendering();
        }
        // manually block until last flip completed?
       //waitBlanking();
        //
        // Note: This function is the last thing called **before** the **blocking**
        // call to glSwapBuffers --> Thus, there can be upto 1 frames delay between the end of this
        // call and the actual update on the screen!!
        // Getting a better estimate of the **actual** flip time would need a more sophiscated approach
        // E.G. adding a #frame->time-stamp tracker? e.g. Gdx.graphics.getFrameId) -> getTime_ms()

        // manually block until last flip completed?
        //waitBlanking();

        long drawduration = getTime_ms()-framestart;
        if ( drawduration<mindrawduration ) {
            try {
                Thread.sleep(mindrawduration-drawduration);
            } catch ( InterruptedException e ){
            }
        }
        frameend=getTime_ms();
    }

    private void waitBlanking(){
	    // use non-continuous rendering? and do this after request rendering?
        sr.begin(ShapeRenderer.ShapeType.Point);
        sr.setColor(0,0,0,0); // Transparent
        sr.point(10,10,10);
        sr.end();
        Gdx.gl.glFinish(); // this should block to the window is writable again.
    }
    private void waitScreen(){
        // pause the current thread until the current screen is complete
        synchronized ( _controller ) {
            try { _controller.wait(); } catch (InterruptedException ignored) {};
        }

    }

    private void setScreen(StimulusScreen screen) {
        synchronized (screen ) {
            if (this.screen != null) {
                this.screen.hide();
            }
            this.screen = screen;
            this.screen.show();
        }
    }
    public void runScreen(StimulusScreen screen) {
            setScreen(screen);
            this.screen.start();
            waitScreen();
    }

    // general game methods
    @Override
    public void pause() { if(this.screen != null) this.screen.pause(); }
    public void resume(){ if(this.screen != null) this.screen.resume(); }
    public void resize(int width, int height){
        if(this.screen != null) this.screen.resize(width,height);
    }

    //--------------------------------------------------------------------------------
    // Main function to control the experiment at the top level
    public void runExpt() throws java.io.IOException  {
        StimSeq ss=null;
        int   nSymbs     =this.nSymbs;
        float isi=1f/10;
        String blockName="Untitled";

        blockName = stimulusFile;
        try {
            System.out.println("Loading stimulus file: " + stimulusFile);
            FileHandle fh = Gdx.files.internal(stimulusFile);
            BufferedReader is = new BufferedReader(new InputStreamReader(fh.read()));
            ss = StimSeq.fromString(is);
            is.close();
        } catch ( IOException ex) {
            System.out.println("Couldnt load stimulus file: " + stimulusFile);
            System.out.println(ex.toString());
            runScreen(exitScreen);
        }
        //System.out.println("Loaded:"+ss);
        StimSeq oss=ss;
        //ss.phaseShiftKey(true); // map to psk version, if needed!
        if( frameRate>0 ) ss.setFrameRate(frameRate); // reset to 30hz stimuluation-- because of frame-time jitter...
        //System.out.println("PSK:"+ss);

        System.out.println("Starting Controller");

        if( client!=null && !client.isConnected() ){
            // set the startup screen and wait for it to finish
            instruct.setInstruction("Searching for the utopia-hub...\nPlease wait");
            instruct.setDuration(0);
            runScreen(instruct);
            //  Try to auto-discover the utopia-hub
            client.connect();
            if( !client.isConnected() ){ // try connect to the default
                client.connect(host,port);
            }
        }

        if( client!=null && client.isConnected() ) {
            instruct.setInstruction("Connected to: " + client.host + ":" + client.port );
            instruct.setDuration(1);
            runScreen(instruct);
        } else {
            // Ask the user for the address to try

            // Get server address:---------------------------
            address.setDuration(3000);
            runScreen(address);
            host = address.host;
            port = address.port;

            // Connecting: to given host/port------------------------------
            connecting.setDuration(30);
            connecting.host = host;
            connecting.port = port;
            runScreen(connecting);
            // if we connected correctly then start the message listener thread -- to not block the render thread
            if (client != null && client.isConnected()) {
                client.startListenerThread();
            }
        }

        // Subscribe to the sub-set of messages we care about.
        if ( client!=null && client.isConnected()){
            // only get : Quality, ModeChange, Selection, NewTarget messages
            client.sendMessage(new Subscribe((int)getTime_ms(), "QPSN"));
        }

        // Electrode Quality ----------------------------------
        if (client != null && client.isConnected()) {
            client.sendMessage(new ModeChange((int)getTime_ms(), "ElectrodeQuality"));
        }
        // set the startup screen and wait for it to finish
        instruct.setInstruction("Electrode Quality Check\nUse the Electrode Quality Screen\nto check when the electrodes\nhave sufficient quality\nPress when quality is good.\n\nTouch to continue.");
        instruct.setDuration(10);
        instruct.setisDoneOnKeyPress();
        runScreen(instruct);

        electrodeQuality.setDuration(30);
        electrodeQuality.setisDoneOnKeyPress();
        runScreen(electrodeQuality);
        if (client != null && client.isConnected()) {
            client.sendMessage(new ModeChange((int)getTime_ms(), "idle"));
        }

        // display test screen
        instruct.setInstruction("Display timing test... please wait");
        instruct.setDuration(.5f);
        instruct.setisDoneOnKeyPress();
        runScreen(instruct);
        letter.setStimSeq(ss.stimSeq, ss.stimTime_ms, false);
        letter.setDuration(3f);
        //bot.reset();
        //frameTracker.reset();
        runScreen(letter);

        String str = "\n Stimulus Timing:\n"+flipStats+"\n"+frameTracker+"\n press key to continue";
        System.out.println(str);
        instruct.setInstruction(str);
        instruct.setisDoneOnKeyPress();
        instruct.setDuration(10);
        runScreen(instruct);

            // Signal Quality ----------------------------------------------
        if ( doElectrodeQuality ) {
            if (client != null && client.isConnected()) {
                client.sendMessage(new ModeChange((int)getTime_ms(), "SignalQuality"));
            }
            // set the startup screen and wait for it to finish
            instruct.setInstruction("Signal Quality Check\nLook at the target location\nand adjust electrodes\nuntil all have good quality.\n\nTouch to continue.");
            instruct.setDuration(3000);
            runScreen(instruct);
            // Play the stimulus
            // N.B. Always set the stimSeq first....
            letter.setStimSeq(ss.stimSeq, ss.stimTime_ms, true); // send stim-events
            int sqid = letter.getnRows() / 2 + letter.getnRows() * letter.getnCols() / 2; // middle of the grid
            letter.setVisible(sqid); // only a single target on the screen
            letter.setTarget(sqid);  // and it's the target
            letter.setDuration(60 * 1000);
            letter.setisDoneOnKeyPress(); // set to finish the stimulus when a key is pressed
            runScreen(letter);
        }

        // Calibration: -----------------------------------------------------------
        // set the startup screen and wait for it to finish
        instruct.setInstruction("Calibration:\nLook at the target location.\nGood LUCK.\n\nTouch to continue.");
        instruct.setDuration(30);
        runScreen(instruct);

        // Play this sequence
        // for each block each target happens twice
        {
            if( client!=null && client.isConnected() ) {
                client.sendMessage(new ModeChange((int)getTime_ms(),"Calibrate.supervised"));
            }

            int[] tgtSeq = new int[nSeqCalibration];
            for (int i = 0; i < tgtSeq.length; i++) {
                tgtSeq[i] = (int) (nSymbs * Math.random());
            }
            //StimSeq.shuffle(tgtSeq);
            letter.setselectionThreshold(-1); // don't end trials early
            letter.setshowFeedback(false);    // don't show feedback during the trials
            runBlock(letter, blockName, seqDurationCalibrate, tgtSeq, ss.stimSeq, ss.stimTime_ms);
            if (client != null && client.isConnected()) {
                client.sendMessage(new ModeChange((int)getTime_ms(), "idle"));
            }

            instruct.setInstruction("Waiting for final results.\n");
            instruct.setDuration(5);
            runScreen(instruct);


            if( client!=null && client.isConnected() ) {
                List<UtopiaMessage> msgs = client.getNewMessages(0);
                float Perr = letter.Perr;
                // get the last PredictedTargetProb message
                for (UtopiaMessage msg : msgs) {
                    if (msg.msgID() == PredictedTargetProb.MSGID) {
                        PredictedTargetProb ptm = (PredictedTargetProb) msg;
                        Perr = ptm.Perr;
                    }
                }

                // show the calibration performance
                instruct.setInstruction("Predicted Error Rate:\n" + Perr * 100f + "%\n\nTouch to continue");
                instruct.setDuration(5);
                runScreen(instruct);
            }
        }

        // Feedback: ---------------------------------------------------------
        // set the startup screen and wait for it to finish
        instruct.setInstruction("Feedback:\nHopefully the training worked well?\nIn any case now we try to use the trained model\n\nTouch to continue.");
        instruct.setDuration(30);
        runScreen(instruct);

        // Play the prediction stimulus
        if( client!=null && client.isConnected() ) {
            client.sendMessage(new ModeChange((int)getTime_ms(),"Prediction.static"));
        }
        for ( int nrep = 0 ; nrep<10; nrep++ ){
            int [] tgtSeq=new int[nSeqPrediction];
            for (int i = 0; i < tgtSeq.length; i++) {
                if ( cuedPrediction ) {
                    tgtSeq[i] = (int) (nSymbs * Math.random());
                } else {
                    tgtSeq[i] = -1;
                }
            }
            letter.setshowFeedback(true); // mark to show feedback during trials
            letter.setselectionThreshold(selectionThreshold); // set to terminate trials early
            runBlock(letter, blockName, seqDurationPrediction, tgtSeq, ss.stimSeq, ss.stimTime_ms);
        }
        if( client!=null && client.isConnected() ) {
            client.sendMessage(new ModeChange((int)getTime_ms(),"idle"));
        }

        // Finally display thanks
        instruct.setInstruction("That ends the experiment.\nThanks for participating");
        instruct.setDuration(5);
        runScreen(instruct);

        // and then quit
        if( client!=null && client.isConnected() ) {
            client.sendMessage(new ModeChange((int)getTime_ms(),"shutdown"));
        }
        runScreen(exitScreen);
    }

    public void runBlock(LetterMatrixScreen stimSeqScreen, String stimType, float seqDuration, int[] tgtSeq, float[][] stimSeq, int[] stimTime_ms, boolean contColor)  throws IOException  {
        // Run a block of the experiment were we vary the target but not the stimulus sequence

        // set the startup screen and wait for it to finish
        instruct.setDuration(5);
        instruct.setInstruction("The block " + stimType + " starts in " + instruct.getDuration_ms() + "ms\n\n\nPress any key to continue.");
        System.out.println("\n-----------\nThe next block starts in 5s\n"+stimType+"\n-----------\n");
        runScreen(instruct);

        // blank before start
        blank.setDuration_ms((int) (interStimulusDuration * 1000));
        waitBlankingp=true;
        runScreen(blank);
        waitBlankingp=false;

        TrialPerfTracker perfTracker=new TrialPerfTracker();
        float[][] tgtStim=new float[1][stimSeq[0].length];
        float[][] blankStim=new float[1][stimSeq[0].length];
        int[]     tgtTime={0};
        boolean showStimFeedback=letter.getshowFeedback();
        for ( int seqi=0; seqi<tgtSeq.length; seqi++){
            // show target, if set
            int tgtID=tgtSeq[seqi];
            if( tgtID >= 0 ) {
                for (int ti = 0; ti < tgtStim[0].length; ti++) {
                    if (ti == tgtSeq[seqi]) {
                        tgtStim[0][ti] = 2;
                    } else {
                        tgtStim[0][ti] = 0;
                    }
                }
                // N.B. Always set the stimSeq first....
                //stimSeqScreen.setMatrix(symbols); // TODO []: set matrix at run time...
                stimSeqScreen.setStimSeq(tgtStim, tgtTime, false); // no stim events
                stimSeqScreen.setDuration(tgtDuration);
                waitBlankingp = true;
                runScreen(stimSeqScreen);
                waitBlankingp = false;
            }

            // tell the system it's a new target.
            if( client!=null && client.isConnected() ) {
                client.sendMessage(new NewTarget((int)getTime_ms()));
            }

            // Play the stimulus
            // N.B. Always set the stimSeq first....
            if ( showStimFeedback ) {
                stimSeqScreen.setselectionThreshold(selectionThreshold); // early stop
                stimSeqScreen.setshowFeedback(true); // show feedback prior to stoppping
            } else {
                stimSeqScreen.setselectionThreshold(-1); // don't early stop
                stimSeqScreen.setshowFeedback(false); // don't show feedback
            }
            stimSeqScreen.setStimSeq(stimSeq, stimTime_ms, true); // send stim-events
            stimSeqScreen.setDuration(seqDuration);
            stimSeqScreen.setTarget(tgtID);
            long elapsed_ms=getTime_ms();
            runScreen(stimSeqScreen);
            elapsed_ms=getTime_ms()-elapsed_ms; // record trial duration

            // tell the system it's a new target => end of trial
            if( client!=null && client.isConnected() ) {
                client.sendMessage(new NewTarget((int)getTime_ms()));
            }

            // Show the feedback (if there is any)
            // BODGE [] : use class test!... YUCK!!
            int selObjID=stimSeqScreen.getlastYest();
            if( stimSeqScreen.getselectionThreshold()>0 ) {
                // Blank for inter-sequence, just to be sure
                blank.setDuration_ms(100);
                runScreen(blank);

                // then show the feedback-screen
                // setup the stimulus to show only the target and the prediction
                boolean[] visTgts=new boolean[nSymbs];
                for ( int si=0; si<nSymbs; si++) {
                    if ( si==tgtID ) {
                        visTgts[si]=true;
                        tgtStim[0][si]=2; // mark as target
                    }
                    if( si == selObjID-1 ) {
                        visTgts[si]=true;
                        tgtStim[0][si]=3; // mark as prediction
                    }
                }
                stimSeqScreen.setselectionThreshold(-1); // don't early stop
                stimSeqScreen.setshowFeedback(true); // don't show feedback
                stimSeqScreen.setStimSeq(tgtStim, tgtTime, false); // don't show flashes, no-stim events
                stimSeqScreen.setVisible(visTgts); // only show the selected object
                stimSeqScreen.setDuration(feedbackDuration); // for feedbackDuration seconds
                waitBlankingp=true;
                runScreen(stimSeqScreen); // run-it
                waitBlankingp=false;
            }

            // record the performance information
            perfTracker.addTrial(tgtID,selObjID-1,(int)elapsed_ms);
            
            // Blank for inter-sequence
            blank.setDuration((int) (interStimulusDuration));
            runScreen(blank);
        }

        // final performance info
        instruct.setDuration(5);
        instruct.setInstruction("Performance:\n " +
                                "All      " + perfTracker.allTrials + "\n" +
                                "Correct: " + perfTracker.correctTrials + "\n" +
                                "Wrong:   " + perfTracker.wrongTrials);
        runScreen(instruct);
        
    }
    
    public void runBlock(LetterMatrixScreen stimSeqScreen, String stimType, float seqDuration, int[] tgtSeq, float[][] stimSeq, int[] stimTime_ms)  throws java.io.IOException  {
        runBlock(stimSeqScreen,stimType,seqDuration,tgtSeq,stimSeq,stimTime_ms,false);
    }

    long getTime_ms(){return nl.ma.utopiaserver.TimeStampClock.getTime();}//currentTimeMillis();}

    public void startControllerThread() {
        Thread updateThread = new Thread() {
            @Override
            public void run() {
                try {
                    runExpt();
                } catch (java.io.IOException ex){
                    System.out.println("IO exception" + ex);
                    ex.printStackTrace();
                }
            }
        };
        updateThread.start(); // called back run()
    }


    public static String[][] readSymbols(BufferedReader bufferedReader) throws IOException {
        if ( bufferedReader == null ) {
            System.out.println("could not allocate reader");
            throw new IOException("Couldn't allocate a reader");
        }
        // tempory store for all the values loaded from file
        List<String[]> rows = new java.util.LinkedList<String[]>(); 
        String line;
        int width=-1; // keep track of number cols per line
        while ( (line = bufferedReader.readLine()) != null ) {
            // skip comment lines
            if ( line == null || line.startsWith("#") ){
                continue;
            } 
            //System.out.println("Reading line " + rows.size());

            // split the line into entries on the split character
            String[] values = line.split("[ ,	]"); // split on , or white-space
            if ( width>0 && values.length != width ) {
                throw new IOException("Row widths are not consistent!");
            } else if ( width<0 ) {
                width = values.length;
            }
            // add to the tempory store
            rows.add(values);
        }
        //if ( line==null ) System.out.println("line == null");
        if ( width<0 ) return null; // didn't load anything

        // Now put the data into an array        
        String[][] symbols = new String[rows.size()][width];
        // N.B. this is not very efficient... should have a better solution, e.g. rows.toArray
        for( int i=0; i<rows.size(); i++) symbols[i]=rows.get(i);
        return symbols;
    }


    
}
