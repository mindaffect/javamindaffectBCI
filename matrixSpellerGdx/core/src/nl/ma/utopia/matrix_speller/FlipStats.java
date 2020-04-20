package nl.ma.utopia.matrix_speller;

public class FlipStats {
    // Class for recording flip-times
    long [] fliptimes;
    long lasttime=0;
    int N=0; // N.B. N<0 -> fliptimes-N pts in set, N>0 -> cursor
    public FlipStats() { init(200); }
    public FlipStats(int buffsize) { init(buffsize); }
    public void init(int buffsize){
        fliptimes=new long[buffsize];
        reset();
    }
    public void reset(){
        N=-fliptimes.length;
        lasttime=0;
        for ( int i=0; i<fliptimes.length; i++){ fliptimes[i]=0; }
    }
    public void addFlipTime(long time){
        int cursor = N<0? fliptimes.length+N: N;
        fliptimes[cursor]=time-lasttime;
        lasttime=time;
        N= N+1<fliptimes.length?N+1:0;
    }
    public void addDeltaTime(long time){
        int cursor = N<0? fliptimes.length+N: N;
        fliptimes[cursor]=time;
        N= N+1<fliptimes.length?N+1:0;
    }

    public int Npts;
    public long medf;
    public float madf;
    public long minf;
    public long maxf;
    public void getStats(){
        Npts = N<0? fliptimes.length+N:fliptimes.length;
        if( Npts==0 ) return;
        // print the flip stats
        // get the median of the times
        long [] ac = new long[Npts];
        for ( int i=0; i < ac.length; i++) ac[i]=fliptimes[i];
        // convert to differences
        System.out.print("dframe ["+Npts+"]:");
        for ( int i=0 ; i<ac.length; i++) {
            //ac[i] = ac[i + 1] - ac[i];
            System.out.print(ac[i]+",");
        }
        System.out.println();
        // sort to get middle
        java.util.Arrays.sort(ac);
        medf = ac[(int)ac.length/2];
        // get min/max mean abs distance (with outlier removal)
        minf=9999;
        maxf=-9999;
        float madf0=0;
        // pass 1, est with all points
        for ( int i=0; i<ac.length; i++) {
            madf0=madf0+Math.abs(medf-ac[i]);
        }
        madf0=Math.max(1,madf0/ac.length);
        // pass 2, est excluding outliers
        madf=0;
        int nvalid=0;
        for ( int i=0 ; i<ac.length; i++ ){
            if ( Math.abs(medf-ac[i])>2*madf0 ) continue;
            nvalid=nvalid+1;
            minf = ac[i]<minf ? ac[i] : minf;
            maxf = ac[i]>maxf ? ac[i] : maxf;
            madf = madf + Math.abs(medf-ac[i]);
        }
        madf = madf/nvalid;
    }
    public String toString(){
        getStats();
        return medf + " +/-" + madf + " [" + minf + "," + maxf +"]" + "/" + Npts;
    }
}
