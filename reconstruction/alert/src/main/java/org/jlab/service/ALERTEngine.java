package org.jlab.service;
import org.jlab.clas.reco.ReconstructionEngine;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.banks.ALERTRecoBankWriter;
import org.jlab.rec.banks.ALERTTrackReader;
import org.jlab.rec.cluster.ALERTCluster;
import org.jlab.rec.hit.Hit;

import java.util.ArrayList;
import java.util.List;

public class ALERTEngine extends ReconstructionEngine{


    public ALERTEngine() {
        super("ALERT","mpaolone","1.0");
    }

    @Override
    public boolean init() {
        // TODO Auto-generated method stub
        // Do not have constants loaded to read in

        ALERTRecoBankWriter ALERTrbc = new ALERTRecoBankWriter();
        return true;
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        //setRunConditionsParameters( event) ;
        if(event.hasBank("RUN::config")==false ) {
            System.err.println("RUN CONDITIONS NOT READ!");
            return true;
        }

        DataBank bank = event.getBank("RUN::config");

        // Get the list of track lines which will be used for matching the CTOF
        // hit to the CVT track
        ALERTTrackReader trkRead = new ALERTTrackReader();
        //ArrayList<ALERTTrack> tracks = trkRead.fetch_Trks(event);

        List<Hit> hits = new ArrayList<Hit>(); // all hits
        List<ALERTCluster> clusters = new ArrayList<ALERTCluster>(); // all clusters





        return true;
    }





    public static void main(String[] args){
        
    }
}
