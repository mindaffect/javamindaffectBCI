package nl.ma.utopia.matrix_speller.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.io.IOException;
import java.util.List;

// stuff for the trigger port
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import nl.ma.utopiaserver.messages.Log;
import nl.ma.utopiaserver.messages.PredictedTargetProb;
import nl.ma.utopiaserver.messages.StimulusEvent;
import nl.ma.utopiaserver.messages.Selection;
import nl.ma.utopiaserver.UtopiaClient;
import nl.ma.utopiaserver.messages.UtopiaMessage;

public class LetterMatrixScreen extends StimulusSequenceScreen {
    private static final int LOGLEVEL=1;
    private static final int TRIGGERPORT=8300; // port of sending trigger messages
    private static final BitmapFont font = new BitmapFont();
            // new BitmapFont(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"),
            //         Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"),
            //         false,true);
    SpriteBatch batch;
    private GlyphLayout layout[][]; // used for the rendered strings
    private Texture background[][][];
    Texture optoON;
    String[][] symbols;
    public int[] _objIDs=null;
    private int showOpto;
    private boolean showFeedback;
    private boolean sendEventsp;
    private UtopiaClient client;
    private float width;
    private float height;
    public int Yest;
    public float Perr;
    private float selectionThreshold;
    private StimulusEvent se=null;
    private int mindrawduration=12;

    public LetterMatrixScreen(String[][] symbols, UtopiaClient client){//}, BufferBciInput input) { // Initialize variables
        this.client  = client;
        batch = new SpriteBatch();
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        setSymbols(symbols);
        showOpto=1;
        showFeedback=true;
        selectionThreshold=-1;
        sendEventsp=true;
    }

    //@Override
    //synchronized public boolean isDone(){
    //    boolean isdone=super.isDone();
    //    if ( isdone && framei==-1){
    //        System.out.println(this.getClass().getSimpleName()+"FlipDelta:"+flipStats);
    //        System.out.println("SentTimes:"+sentStats);
    //        flipStats.reset();
    //        sentStats.reset();
    //    }
    //    return isdone;
    //}

    public void setSymbols(String[][] objidssymbols){
        // extract the objIDs from the symbols strings
        String [][]symbols = new String[objidssymbols.length][];
        int[] objIDs=new int[objidssymbols.length*objidssymbols[0].length];
        for (int i=0, oi=0; i<objidssymbols.length; i++) {
            symbols[i]=new String[objidssymbols[i].length];
            for ( int j=0; j<objidssymbols[i].length; j++, oi++){
                String symb=objidssymbols[i][j];
                // this is in objID:symbol format
                if( symb.contains(":") ){
                    String[] bits = symb.split(":",2);
                    objIDs[oi]= Integer.parseInt(bits[0]);
                    symbols[i][j]=bits[1];
                } else {
                    symbols[i][j]=symb;
                    objIDs[oi]=oi+1;
                }
            }
        }
        this.symbols=symbols;
        this._objIDs=objIDs;
        // check if any of the symbols override the objID for this symbol
        initGrid();
    }
    public int getnRows(){ return symbols.length; }
    public int getnCols(){ return symbols[0].length; }
    public int getnSymbols(){ return getnRows()*getnCols(); }
    public int getnVisibleSymbols(){
        int nVisible=0;
        for ( int si=0; si<Math.min(_visible.length,getnSymbols()); si++){
            if( _visible[si] ) nVisible=nVisible+1;
        }
        return nVisible;
    }
    public boolean getshowFeedback(){ return this.showFeedback; }
    public boolean setshowFeedback(boolean val){  return this.showFeedback=val; }

    public int getshowOpto(){ return this.showOpto; }
    public int setshowOpto(int val){  return this.showOpto=val; }

    public float getselectionThreshold(){ return this.selectionThreshold; }
    public float setselectionThreshold(float val){  return this.selectionThreshold=val; }

    public boolean getsendEvents(){ return this.sendEventsp; }
    public boolean setsendEvents(boolean val){  return this.sendEventsp=val; }

    // methods for mapping from visual object array indices to BCI obj-IDs and back again
    public int[] setObjIDs(int[] objIDs){
        this._objIDs=new int[objIDs.length];
        System.arraycopy(objIDs,0,this._objIDs,0,objIDs.length);
        return this._objIDs;
    }
    public int[] getObjIDs(){
        return this._objIDs;
    }
    public int objID2index(int objID){
        if( _objIDs==null ){
            return objID-1;
        } else {
            for ( int i=0; i<_objIDs.length; i++){
                if ( objID == _objIDs[i]) return i;
            }
            return -1;
        }
    }
    public int index2objID(int idx){
        if ( _objIDs==null ){
            return idx+1;
        } else {
            if ( idx<_objIDs.length ) return _objIDs[idx];
        }
        return -1;
    }

    /**
     * Override set a don't send events flag
     * @param stimSeq
     * @param stimTime_ms
     * @param sendEventp
     */
    @Override
    public void setStimSeq(float [][]stimSeq, int[] stimTime_ms, boolean sendEventp){
        super.setStimSeq(stimSeq,stimTime_ms,new boolean[]{sendEventp});
        setsendEvents(sendEventp);
    }
    public float getlastPerr(){ return Perr; }
    public int getlastYest(){ return Yest; }

    public int getMinDrawDuration(){ return this.mindrawduration; }
    public int setMinDrawDuration(int val){  return this.mindrawduration=val; }


    
    private void initGrid(){

        float scale = height/ (font.getLineHeight() * getnRows()*5);
        font.getData().scaleX=scale;
        font.getData().scaleY=scale;

        // make pixmap for the background
        float curvature = .2f;
        int bgwidth=64;
        int bgheight=bgwidth;
        int cornerRadius = (int)(bgwidth*curvature);
        int cornerOffset = (int)(bgwidth*.3f); // leave 10% margin between symbols
        Texture bg;
        if ( Gdx.files.internal("background/0.png").exists() ) {
            bg = new Texture(Gdx.files.internal("background/0.png"));
        } else {
            Pixmap pm = new Pixmap(bgwidth, bgheight, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fillCircle(cornerOffset,       cornerOffset,        cornerRadius); // TOP-LEFT
            pm.fillCircle(bgwidth-cornerOffset, cornerOffset,        cornerRadius); // TOP-RIGHT
            pm.fillCircle(cornerOffset,       bgheight-cornerOffset, cornerRadius); // BOTTOM-LEFT
            pm.fillCircle(bgwidth-cornerOffset, bgheight-cornerOffset, cornerRadius); // BOTTOM-RIGHT
            pm.fillRectangle(cornerOffset,          cornerOffset-cornerRadius,     bgwidth-2*cornerOffset+1,                    bgheight-2*(cornerOffset-cornerRadius)+1); // vert-rect
            pm.fillRectangle(cornerOffset-cornerRadius ,cornerOffset,              bgwidth-2*(cornerOffset-cornerRadius)+1,     bgheight-2*cornerOffset+1); // horz-rect
            //make the background Textures
            //pm.fillCircle((int)(radius*.5f), (int)(radius*.3f), (int)(radius*.3f));
            bg = new Texture(pm);
        }

        // make the foreground letters + background
        background = new Texture[getnRows()][getnCols()][1];
        layout = new GlyphLayout[getnRows()][getnCols()];
        Texture bgijc=bg;
        for ( int yi=0; yi<getnRows(); yi++){
            for ( int xj=0; xj<symbols[yi].length; xj++){
                layout[yi][xj]=new GlyphLayout();
                //setText(BitmapFont font, java.lang.CharSequence str, Color color, float targetWidth, int halign, boolean wrap)
                layout[yi][xj].setText(font,symbols[yi][xj]);

                bgijc=bg;
                // override with file-name if given
                if ( Gdx.files.internal("background/"+yi+","+xj+".png").exists() ) {
                    bgijc = new Texture(Gdx.files.internal("background/"+yi+","+xj+".png"));
                }
                for ( int ci=0; ci<1; ci++) { // per-state texture
                    // override with file name if given
                    if ( Gdx.files.internal("background/"+yi+","+xj+"_"+ci+".png").exists() ) {
                        bgijc = new Texture(Gdx.files.internal("background/"+yi+","+xj+"_"+ci+".png"));
                    }
                    background[yi][xj][ci] = bgijc;
                }
            }
        }

        // make the opto sensor texture
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        optoON = new Texture(pm);
    }

    long frametime;
    long framestart;
    long frameend;

    @Override
    synchronized public void update(float delta, long frametime) {
        framestart = getTime_ms();
        if ( framei < 0) { // first call since start, record timing
            _t0 = getTime_ms(); // absolute time we started this stimulus
            _frameStartTime = _t0; // absolute time we started this frame
            framei = 0;
            // predicted info
            Yest = -1;
            Perr = 1f;
        }

        // send info on previous stimulus event
        if( se!=null && client!=null && client.isConnected() && sendEventsp ) {
            try {
                if ( false ) {
                    if (frameend < frametime && frametime < framestart) {
                        System.out.print("G");
                    } else {
                        System.out.print("B[" + (frametime - frameend) + "," + (framestart - frametime) + "]");
                    }
                }
                se.settimeStamp((int)frametime);
                client.sendMessage(se);

                // get state of objID==0
                int optoState=-1;
                for ( int i=0; i < se.objIDs.length; i++){
                    if ( se.objIDs[i]==0 ) {
                        optoState=se.objState[i];
                        break;
                    }
                }
                // send debugging trigger if the target is known
                if ( optoState>=0 ) {
                    sendTrigger((byte) optoState);
                    // send the 2nd timing message with lb/up of flip time
                    if( LOGLEVEL > 0 )
                        client.sendMessage(new Log((int) frametime, "FrameIdx:" + Gdx.graphics.getFrameId() + "Fliptime:" + frametime + " FlipLB:" + frameend + " FlipUB:" + framestart + " Opto:" + optoState));
                }

            } catch (IOException ex) {
            }
        }

        // get the next stimulus frame to display
        int nframes = updateFramei();

        // do we have a new frame, if so do new-frame specific updating
        if ( isNewframe() ) { // should-send events && new frame to draw
            if (VERB > 0) {
                System.out.print((getTime_ms() - _t0) + " ( " + framei + " ) " +
                        (_stimDuration_ms[framei] + _frameStartTime - _t0) + " ss=[");
                for (int i = 0; i < Math.min(_ss.length, getnSymbols()); i++)
                    System.out.print(_ss[i] + " ");
                System.out.println("]");
            }
        }

        // client message stuff...
        // stuff to do if we changed what's on screen
        if ( (client != null && client.isConnected())) {
            try {
                // check for new prediction messages from the server
                List<UtopiaMessage> msgs = client.getNewMessages(0);
                // get the last PredictedTargetProb message
                for (UtopiaMessage msg : msgs) {
                    if (msg.msgID() == PredictedTargetProb.MSGID) {
                        PredictedTargetProb ptm = (PredictedTargetProb) msg;
                        this.Yest = ptm.Yest;
                        this.Perr = ptm.Perr;
                    }
                }
                if( !msgs.isEmpty() ) {
                    if (this.VERB > 5 ) System.out.println("Got " + msgs.size() + " messages");
                    if (this.Yest >= 0) {
                        System.out.println("Predicted Target " + this.Yest + " (" + this.Perr + ")");
                    }
                }
            } catch (IOException e) {
                System.out.println("LETTERMATRIX: error in get messages");
                e.printStackTrace();
            }
        }

        // check if this is a valid selection!
        if( getValidSelection()>0 ) {
            doSelection(this.Yest);
        }
    }

    @Override
    synchronized int updateFramei() {
        // advance a **single** frame after frame duration
        long curTime    = getTime_ms();
        int oframei=framei;
        framei=framei<0?0:framei;// ensure is valid frame

        // skip to the next stimulus frame we should display
        if ( _stimDuration_ms != null ) {
            // Skip to the next frame to draw
            //System.out.println(framei+" s:"+(_frameStartTime-_t0)+" d:"+_stimDuration_ms[framei]+" c:"+(curTime-_t0));
            if( _frameStartTime + _stimDuration_ms[framei] < curTime){
                _frameStartTime = _frameStartTime+_stimDuration_ms[framei];
                framei++;
                if ( framei>=_stimSeq.length){
                    framei=0;
                }
                // reset frame-start if jumped multiple frames..
                if ( _frameStartTime + _stimDuration_ms[framei]< curTime  ) {
                    System.out.println("[" + oframei + "]@" + (curTime-_t0) +
                            "ms: Dropped frames" +
                            " deltaTime = " + (curTime- _frameStartTime - _stimDuration_ms[oframei<0?0:oframei]));
                    _frameStartTime = curTime;
                }
            }
            //System.out.println("new:"+framei+" s:"+(_frameStartTime-_t0)+" d:"+_stimDuration_ms[framei]);
            _nextFrameTime = Math.min(_frameStartTime+_stimDuration_ms[framei],duration_ms+_t0);
            _ss = _stimSeq[framei];
            _es = _eventSeq[Math.min(framei,_eventSeq.length-1)];
        }
        newframe=framei>=0 && framei!=oframei; // record if this is a new frame on the screen
        return framei-oframei;
    }


        /*
     * return selected objID if should be selected (i.e. passed Perr threshold) or -1 if no valid selections
     */
    public int getValidSelection(){
        int selObjID=-1;
        if ( selectionThreshold>0 && Yest>=0 && Perr < selectionThreshold ){
            selObjID=Yest;
        }
        return selObjID;
    }

    /*
     * Do any additional processing associated with a target becomming selectable,
     * that is stop the stimulus and send the selection message.
     */
    public void doSelection(int Yest){
        // mark to stop stimulus and terminate the trial early
        if (client != null && client.isConnected() && sendEventsp) {
            try {
                client.sendMessage(new Selection((int)getTime_ms(),Yest));
            } catch ( IOException ex ) {
                ex.printStackTrace();
                System.out.println("LETTERMATRIXSCREEN"+"selection send exception");
            }
        }
        setDuration(0); // finish stimulus now!
    }

    @Override
    public void draw() {
        int nRows = getnRows();
        int nCols = getnCols();
        float xstep = width / (nCols+1);
        float ystep = height / (nRows+1);
        float bsize = Math.min(xstep, ystep);

        Gdx.gl.glClearColor(WINDOWBACKGROUNDCOLOR.r,WINDOWBACKGROUNDCOLOR.g,WINDOWBACKGROUNDCOLOR.b,WINDOWBACKGROUNDCOLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        int si = 0;
        for (int xi = 0; xi < nCols; xi++) {
            for (int yi = 0; yi < nRows; yi++, si++) { // N.B. increment symbol counter each time..
                if( !_visible[si] ) { continue; } // don't draw non-visible stimuli
                float x = (xi + .5f) * xstep;
                float y = height - (yi + 1.5f) * ystep;
                int stimState = (int) _ss[si];
                int colIdx=0, bgIdx=0;
                if (si < _ss.length) {
                    colIdx = Math.min(Math.max(stimState,0), _colors.length-1);
                    bgIdx  = Math.min(Math.max(stimState,0), background[yi][xi].length-1);
                }
                Color curColor = _colors[colIdx];
                batch.setColor(curColor);
                // draw the block background
                batch.draw(background[yi][xi][bgIdx], x, y, xstep, ystep);//bsize, bsize);

                // overdraw with the feedback with alpha-set to blend in the feedback color
                if( showFeedback ) { // show selection feedback by modifying object color
                    // if this is the current Yest (including objID==0 as special case)
                    // then draw with that info
                    if( si == objID2index(Yest) || (Yest==0 && si==_tgt) ){
                        Color fbCol = new Color(_colors[3]);
                        fbCol.a=Perr*.2f; // set blend strength inverse to prob-err
                        batch.setColor(fbCol);
                        // over draw with the feedback
                        batch.draw(background[yi][xi][bgIdx], x, y, xstep, ystep);
                    }
                }

                // draw the forground text
                font.draw(batch, layout[yi][xi], x+xstep/2, y+ystep*.6f);
            }
        }
        // reset the current pen color
        batch.setColor(Color.WHITE);

        // include the opto-sensor if wanted.
        if( showOpto>0 ){
            if( _tgt >= 0 && _tgt < _ss.length ) {
                if( _ss[_tgt]==1 && _visible[_tgt]) {
                    batch.setColor(_colors[1].r,_colors[1].g,_colors[1].b,1f);
                    // setting of showOpto says if draw on left, right, or both..
                    if( (showOpto&1)>0 ) batch.draw(optoON, 0, height-ystep/2, xstep/2, ystep/2);
                    if( (showOpto&2)>0 ) batch.draw(optoON, width-xstep/2, height-ystep/2, xstep/2, ystep/2);
                } else {
                    batch.setColor(_colors[0].r,_colors[0].g,_colors[0].b,1f);
                    if( (showOpto&1)>0 ) batch.draw(optoON, 0, height-ystep/2, xstep/2, ystep/2);
                    if( (showOpto&2)>0 ) batch.draw(optoON, width-xstep/2, height-ystep/2, xstep/2, ystep/2);
                }
            }
        }
        batch.end();

        // Last possible thing, so is as close to re-draw time as possible, send info on the frame contents
        if( _es && isNewframe() ) {
            se = mkStimulusEvent();
        } else {
            se = null;
        }

        // lengthen draw to limit the flip times
        long drawduration = getTime_ms()-framestart;
        if ( drawduration<mindrawduration ) {
            try {
                Thread.sleep(mindrawduration-drawduration);
            } catch ( InterruptedException e ){
            }
        }
        frameend=getTime_ms();
    }

    protected StimulusEvent mkStimulusEvent(){
        int [] objIDs;
        int [] objState;
        int ts = -1;
        int nSymbols = getnSymbols();
        int nActive  = Math.min(_ss.length,nSymbols);
        int nVisible = getnVisibleSymbols();
        if( _tgt>=0 && _tgt<nActive && _visible[_tgt] ) {
            // known target, put in objID=0
            objIDs = new int[nVisible + 1];
            objState = new int[nVisible + 1];
            objIDs[nVisible] = 0;
            objState[nVisible] = (int) _ss[_tgt];
        }else { // unknown target
            objIDs = new int[nVisible];
            objState = new int[nVisible];
        }
        int oi=0; // index into the stimulus state array
        for ( int si=0; si<nActive; si++) { // index into the stimulus state
            if( _visible[si] ) {
                if ( _objIDs==null ) {
                    objIDs[oi] = si + 1; // WARNING: objIDs start from 1
                } else {
                    objIDs[oi] = _objIDs[si];
                }
                objState[oi] = (int) _ss[si];
                oi = oi + 1;
                if(oi>=nVisible) break;
            }
        }
        return new StimulusEvent(ts,objIDs,objState);
    }

    protected boolean sendTrigger(byte value) {
        boolean sent=false;
            try {
                DatagramPacket trigmsg = new DatagramPacket(new byte[]{value},1,java.net.InetAddress.getLocalHost(),TRIGGERPORT);
                // Open the socket, send message and close..
                // TODO: more efficient/lower-latency to keep socket open?
                DatagramSocket dsocket = new DatagramSocket();
                dsocket.send(trigmsg);
                dsocket.close();
                sent=true;
            } catch ( IOException ex ) {
            }
        return sent;
        }
};
