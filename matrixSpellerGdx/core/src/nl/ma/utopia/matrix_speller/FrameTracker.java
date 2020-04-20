package nl.ma.utopia.matrix_speller;
public class FrameTracker {
    /**
     * De-jittering frame time tracker.
     */
    public double[] freqs;
    public double a;
    public double b;
    public double alpha;
    private double[] scos;
    private double[] ssin;
    private double[] scos2;
    private double[] ssin2;
    private double N;
    private double[] L;
    private int maxLi=0;
    public FrameTracker(){ reset(); }
    public void reset(){
        a=0;
        b=0;
        N=0;
        if( freqs!=null ) {
            for( int i=0; i<freqs.length; i++) {
                this.L[i]=0;
                this.scos[i]=0;
                this.ssin[i]=0;
                this.scos2[i]=0;
                this.ssin2[i]=0;
            }
        }
        alpha=.999;
        maxLi=0;
    }
    public void setFreqs(double start, double step, double end){
        int nfreq = (int)((end-start)/step)+1;
        double[] freqs=new double[nfreq];
        freqs[0]=start;
        for ( int i=1; i<nfreq; i++ ){
            freqs[i]=freqs[i-1]+step;
        }
        setFreqs(freqs);
    }
    public void setFreqs(double[] freqs){
        this.freqs=freqs; // deep copy
        this.scos =new double[this.freqs.length];
        this.ssin =new double[this.freqs.length];
        this.scos2=new double[this.freqs.length];
        this.ssin2=new double[this.freqs.length];
        this.L    =new double[this.freqs.length];
        reset();
    }
    public void setHalfLife(double hl){
        alpha=Math.exp(Math.log(.5)/hl);
    }
    public double addPoint(double ts_ms){
        double ts=ts_ms;// /1000d;
        double maxL=0;
        int omaxLi=this.maxLi;
        // update the fourier spectrum info
        N = alpha*N + (1-alpha)*1d;
        for ( int fi=0; fi<freqs.length; fi++){
            double theta = ts*2d*Math.PI*freqs[fi];
            double costheta = Math.cos(theta);
            double sintheta = Math.sin(theta);
            scos[fi] = alpha*scos[fi] + (1-alpha)*costheta;
            ssin[fi] = alpha*ssin[fi] + (1-alpha)*sintheta;
            scos2[fi]= alpha*scos2[fi]+ (1-alpha)*costheta*costheta;
            ssin2[fi]= alpha*ssin2[fi]+ (1-alpha)*sintheta*sintheta;
            L[fi]    = scos[fi]*scos[fi] + ssin[fi]*ssin[fi];
            // get the max frequency
            if( L[fi]>maxL ) {
                maxLi=fi;
                maxL =L[fi];
            }
        }
        if( omaxLi != this.maxLi){
            System.out.println("FRAMETRACKER"+"max-freq change: "+freqs[omaxLi]+"->"+freqs[maxLi]);
        }
        // update the frequency and phase
        this.a  = 1./freqs[this.maxLi]; // N.B. convert back to ms
        // BODGE: lower bound b..
        double theta  = Math.atan2(ssin[this.maxLi],scos[this.maxLi]);
        // approx fraction of the circumfurance of unit circle spanned by variance in estimates
        double vartheta = (ssin2[this.maxLi] - ssin[this.maxLi]*ssin[this.maxLi]/this.N +
                           scos2[this.maxLi] - scos[this.maxLi]*scos[this.maxLi]/this.N)/this.N;
        this.b  = (theta)*a/(2d*Math.PI);
        return getPoint(ts_ms);
    }
    public double getPoint(double ts){
        return Math.round((ts-this.b)/this.a)*this.a + this.b;
    }
    public String toString(){
        String str="a:"+this.a+" b:"+this.b;
        int maxfi=0; double maxfL=L[0];
        for ( int i=0; i<L.length; i++){
            if( maxfL < L[i]) {
                maxfL = L[i];
                maxfi = i;
            }
        }
        str += " @"+freqs[maxfi]*1000d+ "Hz";
        return str;
    }

    /**
     * driver class for testing 
     */
    public static void main(String []argv){
        System.out.println("Usage: BoundLineTracker filename N lowerboundp halflife cpm");
        String filename="-";
        if ( argv.length>0 ){
            filename=argv[0];
        }
        System.out.println("filename="+filename);
        float halflife=10*1000;
        if( argv.length>1 ){
            try{
                halflife = Float.valueOf(argv[1]);
            } catch ( NumberFormatException e) {
            }
        }
        System.out.println("halflife="+halflife);
        float f0=60;
        if( argv.length>2 ) {
            try{
                f0 = Float.valueOf(argv[2]);
            } catch ( NumberFormatException e) {
            }
        }
        System.out.println("f0="+f0);
        int VERB=0;
        if( argv.length>3 ) {
            try{
                f0 = Integer.valueOf(argv[3]);
            } catch ( NumberFormatException e) {
            }
        }
        System.out.println("VERB="+VERB);
        
        
        // read the test data from stdin/file
        java.io.BufferedReader bfr=null;
        if( filename.equals("-") ) { // read from std-in
            bfr = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        } else { // read from filename
            try{
              bfr = new java.io.BufferedReader(new java.io.FileReader(filename));
            } catch ( java.io.IOException ex ) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
        java.util.ArrayList<Float> xs = readFrameTimes(bfr);

        FrameTracker ftt = new FrameTracker();
        ftt.setFreqs(f0-.2,.05,f0+.2);
        double xest=0,x;
        for ( int i=0; i<xs.size(); i++){
            x=xs.get(i);
            xest = ftt.addPoint(x);
            if( VERB> 0 ) {
                System.out.println(i + ") X ="+x+" X_f=" + xest + " diff="+(xest-x));
                System.out.println(i+") "+ftt);
            } else {
                System.out.println(xest);
            }
        }

        // print detailed final state.
        if( VERB>1 ) {
            for ( int i=0; i<ftt.freqs.length ; i++ ){
                System.out.println(i+") "+ftt.freqs[i]+" L="+ftt.L[i]+" ssin="+ftt.ssin[i] + " scos="+ftt.scos[i]);
            }
        }
    }    
    

    public static java.util.ArrayList<Float> readFrameTimes(java.io.BufferedReader bfr){
        String line;
        java.util.ArrayList<Float> xs=new java.util.ArrayList<Float>();
        try { 
            while( (line = bfr.readLine())!=null){
                if ( line == null || line.startsWith("#") || line.length()==0 )
                    continue;
                xs.add(Float.valueOf(line.trim()));
            }
        } catch ( java.io.IOException ex ) {
            ex.printStackTrace();
            System.out.println("error reading test matrix");
        }
        return xs;
    }
};
