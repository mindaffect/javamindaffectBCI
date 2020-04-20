package nl.ma.utopia.matrix_speller.screens;
import com.badlogic.gdx.graphics.Color;

public abstract class StimulusSequenceScreen extends StimulusScreen {
    Color[] defaultColors={new Color(.05f,.05f,.05f, 1.0f), // bgColor  dark GREY
                           new Color(1f,1f,1f, 1.0f),    // fgColor  WHITE
                           new Color(0f,1f,0f, 1.0f),    // tgtColor GREEN
                           new Color(0f,0f,1f, 1f)};     // fbColor BLUE
    public static int VERB=0;

    // model of the whole sequence
    int   _tgt=-1;
    float[][] _stimSeq=null;
    boolean[] _visible=null;
    int[] _stimDuration_ms =null;
    boolean[] _eventSeq=null;
    Color[] _colors=null;

    // Model for tracking were we currently are in the stimulus display
    int   framei=-1;
    float[] _ss=null;   // the current stimulus state
    boolean _es=false;  // the current event state, i.e. send event or not?
    float[] _sprob=null;// current probabilities of each target
    volatile long _nextFrameTime=-1; // *relative* time until the next stimulus change
    volatile long _t0=-1; // absolute time we started running
    volatile long _frameStartTime =-1; // absolute time we started current loop
    int nskip=0;
    boolean newframe=false;

    synchronized public void start(){
        super.start();
        System.out.println("Cursor Screen : " + duration_ms);
        // set to -1 to indicate that no-valid frames have been drawn yet
        framei=-1;
        // just record the current time so we know when to quit
        _t0=-1;
        _frameStartTime =-1; // absolute time we started this stimulus loop
    }

    synchronized public boolean isDone(){
        // mark as finished
        long ts = getTime_ms();
        long elapsed = ts - startTime ;
        boolean done = elapsed > duration_ms;
        //boolean _isDone = _t0>0 && getTime_ms() > duration_ms + _t0;
        if ( done ){
            if ( VERB>=0 )
                System.out.println("Run-time: " + elapsed + " / " + duration_ms);
            framei=-1; // mark as finished
        }
        return done;
    }

    synchronized public void setStimSeq(float [][]stimSeq, int[] stimTime_ms, boolean [] eventSeq, Color[] colors){
        if( eventSeq==null ) { // if not given event info, then assume it's always true
            _eventSeq = new boolean[]{true};
        } else {
            _eventSeq = new boolean[eventSeq.length];
            java.lang.System.arraycopy(eventSeq,0,_eventSeq,0,eventSeq.length);
        }
        // copy the stimulus sequence info to our local copy (thread safer...)
        _stimDuration_ms = new int[stimTime_ms.length]; // copy locally
        //java.lang.System.arraycopy(stimTime_ms,0, _stimDuration_ms,0,stimTime_ms.length);
        // Convert to stimulus durations
        for (int ei = 0; ei< _stimDuration_ms.length-1; ei++){
            _stimDuration_ms[ei]=stimTime_ms[ei+1]-stimTime_ms[ei];
        }
        _stimDuration_ms[_stimDuration_ms.length-1]=0;
        // Validate that this stimTime sequence is correct...
        if ( _stimDuration_ms.length==1 && _stimDuration_ms[_stimDuration_ms.length-1]==0 &&
                duration_ms>0 )
            _stimDuration_ms[0]=duration_ms;
        // Copy the stimulus sequence
        _stimSeq     = new float[stimSeq.length][];
        for ( int ti=0; ti<stimSeq.length; ti++){ // time points
            _stimSeq[ti]=new float[stimSeq[ti].length];
            java.lang.System.arraycopy(stimSeq[ti],0,_stimSeq[ti],0,stimSeq[ti].length);
        }
        // Init the state tracking variables
        _ss   = _stimSeq[0];
        _es   = _eventSeq[0];
        // initialize the color-table for this stimulus sequence
        _colors=colors;
        // initialize the set of visible stimuli to be all stimuli
        if( _visible==null || _visible.length != _ss.length ) {
            _visible = new boolean[_ss.length];
        }
        for ( int si=0; si<_visible.length; si++ ) _visible[si]=true;
    }

    @Override
    synchronized public void update(float delta) {
        //if( _t0<0 && framei<0 ){ // first call since start, record time
        if(  framei<0 ){ // first call since start, record timing
            _t0= getTime_ms(); // absolute time we started this stimulus
            _frameStartTime =_t0; // absolute time we started this stimulus loop
            framei=0;
        }
        // get the next stimulus frame to display
        int nframes = updateFramei();
        // do we have a new frame, if so do new-frame specific updating
        if ( nframes > 1 ) { // new frame to draw
            if ( VERB>0 ){
                System.out.print((getTime_ms()-startTime) + " ( " + framei + " ) " +
                        (_stimDuration_ms[framei]+ _frameStartTime -_t0) + " ss=[");
                for(int i=0;i<_ss.length;i++)System.out.print(_ss[i]+" ");
                System.out.println("]");
            }
            System.out.println("in parent");
        }
    }

    // update the current model for the new frame.
    // Return number of frames forward
    synchronized int updateFramei(){
        // get the current time since start of this loop
        long curTime    = getTime_ms();
        int oframei=framei;
        framei=framei<0?0:framei;// ensure is valid frame

        // skip to the next stimulus frame we should display
        if ( _stimDuration_ms != null ) {
            // Skip to the next frame to draw
            while ( _frameStartTime+_stimDuration_ms[framei] <= curTime ){
                _frameStartTime += _stimDuration_ms[framei];
                framei++;
                if ( framei>=_stimSeq.length ) { // loop and update loop start time
                    framei = 0;
                    if (_stimDuration_ms[_stimDuration_ms.length - 1] <= 0)
                        break; // guard against zero-stimTime sequences
                }
            }
            if( oframei>0 && framei-oframei>1 ) {
                System.out.println("[" + oframei + "]@" + (curTime-_t0) +
                        "ms: Dropped " + (framei-oframei-1) + " frames" +
                        " deltaTime = " + (curTime- _frameStartTime - _stimDuration_ms[oframei]));
            }
            _nextFrameTime = Math.min(_frameStartTime+_stimDuration_ms[framei],duration_ms+_t0);
            _ss = _stimSeq[framei];
            _es = _eventSeq[Math.min(framei,_eventSeq.length-1)];
            //java.lang.System.copyarray(_stimSeq[framei],0,_ss,0,_ss.length);// paranoid?
        }
        newframe=framei>=0 && framei!=oframei; // record if this is a new frame on the screen
        return framei-oframei;
    }

    public boolean isNewframe() { return newframe; }

    // helper functions for default cases
    public void setStimSeq(float [][]stimSeq, int[] stimTime_ms){
        setStimSeq(stimSeq,stimTime_ms,false); // don't send stimstate events
    }
    public void setStimSeq(float [][]stimSeq, int[] stimTime_ms, boolean sendEventp){
        boolean[] eventSeq=new boolean[]{sendEventp};
        setStimSeq(stimSeq,stimTime_ms,eventSeq,defaultColors);
    }
    public void setStimSeq(float [][]stimSeq, int[] stimTime_ms, boolean [] eventSeq){
        setStimSeq(stimSeq,stimTime_ms,eventSeq,defaultColors);
    }
    public void setStimSeq(float [][]stimSeq, int[] stimTime_ms, boolean [] eventSeq, boolean contColor){
        if( contColor )
            setStimSeq(stimSeq,stimTime_ms,eventSeq,null);
        else
            setStimSeq(stimSeq,stimTime_ms,eventSeq,defaultColors);
    }

    // set the target symbol information
    public void setTarget(int tgt){ _tgt=tgt;}
    public void setTarget(int[] tgt){
        for( int i=0; i<tgt.length; i++) if (tgt[i]>0) {_tgt=i; break;}
    }

    // set the subset of visible targets
    public void setVisible(){ setVisible(-1); }
    public void setVisible(int tgt){
        for ( int si=0 ; si<_visible.length; si++ ) _visible[si]=false; // reset all invisible
        if ( tgt>=0 ) _visible[tgt]=true;
    }
    public void setVisible(boolean[] visible){
        for ( int si=0; si<Math.min(_visible.length,visible.length); si++){
            _visible[si]=visible[si];
        }
    }
};
