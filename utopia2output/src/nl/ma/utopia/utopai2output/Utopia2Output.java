package nl.ma.utopia.utopia2output;
import nl.ma.utopiaserver.UtopiaClient;
import nl.ma.utopiaserver.messages.NewTarget;
import nl.ma.utopiaserver.messages.UtopiaMessage;
import nl.ma.utopiaserver.messages.PredictedTargetProb;
import java.io.*;
import java.util.List;
import java.util.LinkedList;


public class Utopia2Output implements Runnable {
    public static String DEFAULTHOST="localhost";
    public static int    DEFAULTPORT=8400;
    
    volatile UtopiaClient client=null;
    public double outputPressThreshold=.1;
    public double outputReleaseThreshold=.1;
    
    /**
	  * construct a utopai2output object and initialize the connections
     */    
    public Utopia2Output() { client= new UtopiaClient(); }

    /**
     * try *forevery* to connect to the utopia server, and initialize the clock alignment for this connection
     *
     * @param  host  the utopia host to connect to 
     * @param  port  the port on the utopia host to connect to
     */    
    public void connect(String host, int port) throws IOException { connect(host,port,-1); }

    /**
     * connect to the utopia server, and initialize the clock alignment for this connection
     *
     * @param  host  the utopia host to connect to 
     * @param  port  the port on the utopia host to connect to
     * @param  timeout_ms maximum time in milliseconds to wait for successful connection
     */    
    public void connect(String host, int port, int timeout_ms) throws IOException {
        // make the initial connection
        int elapsed_ms=0; // how long have we been trying to connect
        while(!client.isConnected() && (timeout_ms<0 || timeout_ms > elapsed_ms)){ 
            try {
                client.connect(host,port);
            } catch ( IOException ex ) {
                System.out.println("Couldnt connect to " + host + ":" + port + " waiting...");
                System.out.println(ex);
            }
            if( !client.isConnected() ) {
                elapsed_ms+=1000;
                try { Thread.sleep(1000); } catch( InterruptedException ex ){}
            }
        }
        client.initClockAlign();        
    }
    public void setOutputThresholds(double oet){ setOutputThresholds(oet,oet*2); }
    public void setOutputThresholds(double pt, double rt){ outputPressThreshold=pt; outputReleaseThreshold=rt; }    
    
    /**
	  *  mainloop of the utopia-to-output mapping
     * 
     *  runs a infinite loop, waiting for new messages from utopia, filtering 
     *  out those messages which contain an output prediction (i.e.. PREDICTEDTARGETPROB message)
     *  and if the output prediction is sufficiently confident forwarding this
     *  to the output device and sending a NEWTARGET to the recogniser to indicate
     *  the output has been sent.
     *
     */    
    public void run(){
        System.out.println("Waiting for messages");
        boolean outputActivated=false;
        while ( true ){
            try { 
                List<UtopiaMessage> inmsgs=client.getNewMessages(1000);
                for( UtopiaMessage msg : inmsgs ) {
                    System.out.println("Got Message: " + msg.toString() + " <- server");
                    // Identify which messages are TargetProb messages
                    if( msg.msgID()==PredictedTargetProb.MSGID ) {
                        PredictedTargetProb ptp= (PredictedTargetProb)msg;
                        if( ptp.Perr < outputPressThreshold && !outputActivated ) {
                            doOutput(ptp.Yest);
                            // Tell the recogniser we've generated an output and to start processing
                            // a new target
                            client.sendMessage(new NewTarget(client.gettimeStamp()));
                            // mark the output as activated
                            outputActivated=true; 
                        } else if ( ptp.Perr > outputReleaseThreshold && outputActivated ) {
                            // release the output
                            outputActivated=false;
                        }
                    }
                }
            } catch ( IOException ex ) {
                System.out.println("Problem reading from stream"); 
                System.exit(-1);
            }
            System.out.print('.');System.out.flush();
        }
    }

    /*
     * Generic function to generate an indicated output.  Override with your application specific output
     */
    public void doOutput(int objID){        
        System.out.println("Target " + objID + " is good engough!");        
    }
    

    /**
	  *  driver method for stand-along usage.  Uses the command-line arguments
     *  to connect to given utopia server (utopiahost:utopiaport) and
     *  forwards outputs with sufficiently low error rates (i.e. Perr <
     *  outputPerrThreshold) to the output
     *
     *  @parm args command-line arguments: utopiahost:utopiaport outputPerrThreshold
     */    
    public static void main(String[] args) throws java.io.IOException,InterruptedException {
			if ( args.length==0 ) {
				System.out.println("utopia2output utopiahost:utopiaport outputPerrThreshold");
         }
         // Argument processing
         String host = Utopia2Output.DEFAULTHOST;
		  int port = Utopia2Output.DEFAULTPORT;
        double outputPerrThreshold=-1;
		  if (args.length>=1) {
				host = args[0];
				int sep = host.indexOf(':');
				if ( sep>0 ) {
					 port=Integer.parseInt(host.substring(sep+1,host.length()));
					 host=host.substring(0,sep);
				}
				System.out.println("Utopia Host" + host + ":" + port);
		  }			
        if( args.length>1 ) {
            try { 
                outputPerrThreshold = Double.parseDouble(args[1]);
            } catch ( NumberFormatException ex ) {}
        }

        // Initialize everything
		  Utopia2Output u2o=new Utopia2Output();
        try {
            u2o.connect(host,port);
        } catch ( IOException ex ) {
            System.out.println("Error: couldnt connect to utopia!");
            System.exit(-1);
        }
        if( outputPerrThreshold>=0 ) u2o.setOutputThresholds(outputPerrThreshold);

        // run the message forwarding thread
        u2o.run();
	  }    
}
