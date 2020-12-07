package org.jlab.rec.banks;

import org.jlab.detector.base.DetectorType;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.hit.ALERTHit;

import java.util.List;

public class ALERTRecoBankWriter {

public ALERTRecoBankWriter(){
    // Blank
}
public DataBank fillRawHitsBank(DataEvent event, List<ALERTHit> hitlist){
    // Need to decide if we want to combine AHit.java and ALERTHit.java into a single JAVA class
    // in TOF - ctof and hit Hit class extends AHit (but we do not need to separate
    DataBank bank = event.createBank("ALERT::rawhits", hitlist.size());

    int rows = bank.rows();
    for (int i = 0; i < rows; i++) {
            // DetectorType is UNDEFINED! Needs to be updated in DetectorType to include ALERT!*
        if (bank.getByte("detector", i) == DetectorType.UNDEFINED.getDetectorId()) {
            int id = bank.getShort("id", i);
            double x = bank.getFloat("x", i);
            double y = bank.getFloat("y", i);
            double z = bank.getFloat("z", i);
        }
    }


    return bank;
}


}
