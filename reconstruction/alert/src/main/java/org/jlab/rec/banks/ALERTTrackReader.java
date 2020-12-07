package org.jlab.rec.banks;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.tracks.ALERTTrack;

import java.util.ArrayList;

public class ALERTTrackReader {
    public ALERTTrackReader(){
        //
    }

    public ArrayList<ALERTTrack> fetch_Trks(DataEvent event) {

        ArrayList<ALERTTrack> Tracks = new ArrayList<ALERTTrack>();


        DataBank bank = event.getBank("AHDC::Trajectory"); // I am not sure?


    return Tracks;
    }

}
