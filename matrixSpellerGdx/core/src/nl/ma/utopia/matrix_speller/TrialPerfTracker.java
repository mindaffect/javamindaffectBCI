package nl.ma.utopia.matrix_speller;

public class TrialPerfTracker {
    public class StatsTracker {
        int _N;
        int sT;
        int sT2;
        int minT;
        int maxT;
        public void addPoint(int t){
            _N =_N+1;
            sT = sT+t;
            sT2=sT2+t*t;
            minT=minT<t?minT:t;
            maxT=maxT>t?maxT:t;
        }
        public void reset(){ _N=0; sT=0; sT2=0; minT=999999; maxT=-999999; }
        public int N() { return _N; }
        public float mean(){ return sT/((float)_N); }
        public float std() { return (float) Math.sqrt((sT2 - sT*sT/((float)_N))/((float)_N)); }
        public int min() { return minT; }
        public int max() { return maxT; }
        public String toString(){
            return N() + "Trials   T = " + mean() + " ( " + min() + "> " + std() + " <"+ max() + ")";
        }
    }
    public StatsTracker allTrials = new StatsTracker();
    public StatsTracker correctTrials = new StatsTracker();
    public StatsTracker wrongTrials = new StatsTracker();

    void addTrial(int tgt, int pred, int time){
        allTrials.addPoint(time);
        if( tgt==pred || pred==0 ) correctTrials.addPoint(time);
        else            wrongTrials.addPoint(time);
    }
    void reset(){
        allTrials.reset(); correctTrials.reset(); wrongTrials.reset();
    }
};

