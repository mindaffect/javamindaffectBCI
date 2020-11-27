package nl.ma.mindaffectBCI.minimalpresentation;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import nl.ma.noisetag.Noisetag;
import nl.ma.noisetag.StimulusState;
import nl.ma.utopiaserver.TimeStampClockInterface;
import nl.ma.utopiaserver.messages.PredictedTargetDist;
import nl.ma.utopiaserver.messages.PredictedTargetProb;
import nl.ma.utopiaserver.messages.Selection;

public class MainActivity extends AppCompatActivity {
    private static final String TAG=MainActivity.class.getSimpleName();

    ArrayList<Button> buttons=null;

    Button opto=null;
    Button exitbutton=null;
    TextView instruct=null;
    EditText spelledText=null;
    TableLayout selectionMatrix=null;
    Noisetag nt=null;
    private PHASES currentPhase;
    private PHASES nextPhase;
    private float selectionThreshold = .1f;
    private boolean cuedPrediction = true;
    private boolean showTargetOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FullScreencall();

        spelledText = (EditText) findViewById(R.id.editTextTextPersonName);

        // Get all the selectable buttons into a button list
        buttons = null;

        opto = findViewById(R.id.opto);
        opto.setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);

        exitbutton = (Button) findViewById(R.id.exit);
        exitbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finishAffinity();
            }
        });

        instruct = findViewById(R.id.instruct);

        // setup the noisetagging framework
        nt = new Noisetag(); // default stimFile
        nt.setTimeStampClock(tsclock);
        nt.connectedHandlers.add(new Noisetag.Observer<String>() {
                                     public void update(String hostport) {
                                         Log.v(TAG, "Got connected callback:" + hostport);
                                         doNextPhase();
                                     }
                                 }
        );
        // do on-click when selection happens
        nt.selectionHandlers.add(new Noisetag.Observer<Selection>() {
            @Override
            public void update(Selection selection) {
                int selidx = selection.objID - 1;
                if ( buttons != null && selidx > 0 && selidx < buttons.size()) {
                    buttons.get(selection.objID - 1).callOnClick();
                } else {
                    Log.v(TAG, String.format("Warning: got selection outside button set! %d", selidx));
                }
            }
        });

        instruct.setText(R.string.welcomeText);
        instruct.setVisibility(View.VISIBLE);
        instruct.bringToFront();
        instruct.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "got click");
                doNextPhase();
            }
        });
        // phase transition when flicker finishes
        nt.endSequenceHandlers.add(new Noisetag.Observer<Object>() {
            @Override
            public void update(Object o) {
                Log.v(TAG, "got end-sequence callback");
                doNextPhase();
            }
        });

        initFrameCallback();

        Display display = ((WindowManager) getSystemService(this.WINDOW_SERVICE)).getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        System.out.println(String.format("Display refresh-rate: %fHz", refreshRate));

        this.currentPhase = PHASES.CONNECTING;
        doPhase();

        nt.startNetworkThread(); // do network stuff in separate thread
        nt.connect("-", -1, 1); // search for decoder
        //nt.setFramesPerBit(10);
        // listen for on-connected.
        // test discovery!

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    SSDP.VERB = 2;
//                    SSDP.discover("utopia/1.1", 5000);
//                } catch (IOException ex) {
//                    ;
//                }
//            }
//        }).start();
    }

    private ArrayList<Button> getButtons(TableLayout tbl){
        ArrayList<Button> buttons=new ArrayList<Button>();
        for (int i = 0; i < tbl.getChildCount(); i++) {
            if (tbl.getChildAt(i) instanceof TableRow) {
                TableRow parentRow = (TableRow) tbl.getChildAt(i);
                for (int j = 0; j < parentRow.getChildCount(); j++) {
                    if (parentRow.getChildAt(j) instanceof Button) {
                        Button button = (Button) parentRow.getChildAt(j);
                        buttons.add(button);
                        button.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                spelledText.append(((Button) v).getText());
                            }
                        });
                    }
                }
            }
        }
        return buttons;
    }


    public enum PHASES { CONNECTING, CALIBRATION_INSTRUCT, CALIBRATION, PERFORMANCE, PREDICTION_INSTRUCT, PREDICTION, FINISH, QUIT };

    private void doNextPhase() {
        this.currentPhase = this.nextPhase;
        doPhase();
    }
    private void doPhase() {
        // ensure we run on the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doPhaseonUI();
            }
        });
    }

    private void doPhaseonUI(){
        if ( this.currentPhase == null ) this.currentPhase=PHASES.CONNECTING;
        switch ( this.currentPhase ){
            case CONNECTING:
                instruct.setText(R.string.welcomeText);
                instruct.setVisibility(View.VISIBLE);
                this.nextPhase=PHASES.CALIBRATION_INSTRUCT;
                break;

            case CALIBRATION_INSTRUCT:
                instruct.setText(R.string.calibrate_instruct);
                instruct.setVisibility(View.VISIBLE);
                this.nextPhase=PHASES.CALIBRATION;
                spelledText.setText("Calibrating....");
                break;

            case CALIBRATION:
                instruct.setVisibility(View.GONE);
                if ( selectionMatrix != null ) {
                    selectionMatrix.setVisibility(View.INVISIBLE);
                }
                selectionMatrix = findViewById(R.id.calLayout);
                selectionMatrix.setVisibility(View.VISIBLE);
                buttons = getButtons(selectionMatrix);
                nt.clearLastPrediction();
                nt.setnumActiveObjIDs(buttons.size());
                nt.startCalibration(10, null, 60*4); // 240 frames calib => 4s
                showTargetOnly = true;
                startFlicker();
                this.nextPhase = PHASES.PERFORMANCE;
                break;

            case PERFORMANCE:
                showTargetOnly = false;
                instruct.setText(R.string.wait_performance);
                instruct.setVisibility(View.VISIBLE);
                PredictedTargetProb ptp = nt.getLastPrediction();
                if ( ptp != null ){
                    instruct.setText(String.format(getString(R.string.calibrate_performance_template),100.0f*(1-ptp.Perr)));
                } else {
                    // stop this phase when we get a prediction callback
                    nt.predictionHandlers.add(new Noisetag.Observer<PredictedTargetProb>() {
                        @Override
                        public void update(PredictedTargetProb predictedTargetProb) {
                            System.out.println("Got prediction: " + predictedTargetProb.toString());
                            instruct.setText(String.format(getString(R.string.calibrate_performance_template), 100.0f * (1 - predictedTargetProb.Perr)));
                        }
                    });
                }
                this.nextPhase = PHASES.PREDICTION_INSTRUCT;
                break;

            case PREDICTION_INSTRUCT:
                // TODO [] : remove only the performance result waiting?
                nt.predictionHandlers.clear();
                instruct.setText(R.string.prediction_instruct);
                instruct.setVisibility(View.VISIBLE);
                this.nextPhase=PHASES.PREDICTION;
                spelledText.setText("");
                break;

            case PREDICTION:
                instruct.setVisibility(View.GONE);

                if ( selectionMatrix != null ) {
                    selectionMatrix.setVisibility(View.INVISIBLE);
                }
                selectionMatrix = findViewById(R.id.tableLayout);
                selectionMatrix.setVisibility(View.VISIBLE);
                buttons = getButtons(selectionMatrix);

                showTargetOnly = false;
                nt.predictionHandlers.add(new Noisetag.Observer<PredictedTargetProb>() {
                    @Override
                    public void update(PredictedTargetProb predictedTargetProb) {
                        System.out.println("Got prediction: " + predictedTargetProb.toString());
                    }
                });
                nt.setnumActiveObjIDs(buttons.size());
                nt.startPrediction(100, null, selectionThreshold, cuedPrediction, 10*60); // 10s prediction trials
                //startFlicker();
                this.nextPhase = PHASES.FINISH;
                break;

            case FINISH:
                //stopFlicker();
                instruct.setVisibility(View.VISIBLE);
                instruct.setText(R.string.finish_text);
                this.nextPhase = PHASES.QUIT;
                break;

            case QUIT:
                this.finishAffinity();
                break;
        }
    }

    public void FullScreencall() {
        View decorView = getWindow().getDecorView();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(Build.VERSION.SDK_INT < 19){
            decorView.setSystemUiVisibility(View.GONE);
        } else {
            //for higher api versions.
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public void onConnected(String hostport){
    }

    @Override
    protected void onResume() {
        super.onResume();
        // restart the frame callback
        doPhase();//startFlicker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop the frame-callback
        stopFlicker();
    }

    private ColorStateList state2color(int state){
        return state2color(state,-1);
    }
    private ColorStateList state2color(int state, float ptgt){
        ColorStateList col;
        if ( state == 1 ){ // Flash
             col = ColorStateList.valueOf(Color.WHITE);
        } else if ( state == 2 ){ // Cue
            col =  ColorStateList.valueOf(Color.GREEN);
        } else if ( state == 3 ){ // Feedback
            col =  ColorStateList.valueOf(Color.BLUE);
        } else {
            col =  ColorStateList.valueOf(Color.BLACK);
        }
        return col;
    }

    int nframe=0;
    int t0=-1;
    int lastlastframetime=0;
    private void onFrameCallback() {
        int curframetime = (int)(System.nanoTime()/1000000);
        //System.out.println(String.format("Clocks: ft=%d nt=%d ctm=%d",lastframetime,curframetime,System.currentTimeMillis()));
        nframe = nframe+1;
        if ( t0==-1 ){
            t0=lastframetime;
        }
        // TODO[X]: last frame time info milliseconds...
        // send info on the *previous* stimulus state, with recorded vsync time
        nt.sendStimulusState(lastframetime);

        // update and get the new stimulus state to display
        nt.updateStimulusState(lastframetime);
        StimulusState ss = nt.getStimulusState();
        if ( ss== null ){
            // do end of sequence stuff?
            System.out.print( "e" );
            if ( nframe %60 ==0 ) System.out.println();
            return;
        }
        int targetState=-1;
        if (ss.targetIdx >= 0 && ss.targetIdx < ss.stimulusState.length ){
            targetState = ss.stimulusState[ss.targetIdx];
        }

        // re-draw the buttons with the updated stimulus state
        int bi=0;
        for ( Button b : buttons ){
            if ( bi < ss.stimulusState.length ){
                int state = ss.stimulusState[bi];
                ColorStateList col = state2color(state);
                // everything but the target is state 0 if in showTargetOnly mode
                if ( showTargetOnly && bi != ss.targetIdx ){
                    col = state2color(0);
                }
                b.setBackgroundTintList(col);
            }
            bi++;
        }

        // show the opto sensor
        if ( opto != null ) {
            if ( targetState > 0 ) {
                opto.setBackgroundTintList(state2color(1));
            } else {
                opto.setBackgroundTintList(state2color(0));
            }
        }

        // some logging...
        if ( targetState>=0 ) {
            System.out.print( targetState==0?".":"*");
        }
        if ( nframe %60 ==0 ) {
            float elapsed_ms = (lastframetime-t0);
            System.out.println(String.format("\nFPS= %df / %fs = %f fps",nframe, elapsed_ms/1e3f, nframe / (elapsed_ms/1e3f)));
        }
        //System.out.println(String.format("%f) frametimes :  %d - %d =%d",(lastframetime-t0)/1e9f, lastframetime, lastlastframetime, lastframetime-lastlastframetime));
        //System.out.println(String.format("%f) lag :  %d - %d =%d",(lastframetime-t0)/1e9f, curframetime, lastframetime, curframetime - lastframetime));
        lastlastframetime=lastframetime;
    }

    // time-stamp server which provides time-stamps consistent with those used by the choreographer for
    // scheduling android frame drawing...
    public TimeStampClockInterface tsclock = new TimeStampClockInterface() {
        public long getTimeStamp(){ return System.nanoTime()/1000000; }
    };

    // Get a call-back every vsync..
    // Android only test wrapper code to get the last frametime...
    android.view.Choreographer.FrameCallback frameCallback=null;
    public static int lastframetime;
    public void initFrameCallback() {
        frameCallback = new android.view.Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                lastframetime = (int) (frameTimeNanos/1000000); // record frame time in milliseconds
                onFrameCallback();
                android.view.Choreographer.getInstance().postFrameCallback(frameCallback);
            }
        };
    }
    public void startFlicker(){
        Choreographer c = android.view.Choreographer.getInstance();
        android.view.Choreographer.getInstance().postFrameCallback(frameCallback);
    }
    public void stopFlicker(){
        android.view.Choreographer.getInstance().removeFrameCallback(frameCallback);
    }
    public long getLastFrameTime() {
        return lastframetime;
    }




}