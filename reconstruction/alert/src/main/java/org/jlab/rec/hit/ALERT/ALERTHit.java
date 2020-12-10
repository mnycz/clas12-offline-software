package org.jlab.rec.hit.ALERT;

import org.jetbrains.annotations.NotNull;
import org.jlab.rec.hit.Hit;

public class ALERTHit extends Hit {

    public ALERTHit(int id, int sector, int superlayer, int layer, int paddle,
                    int adc1, int adc2, int adc3, int tdc1, int tdc2, int tdc3) {
        super(id,sector, superlayer,layer,paddle, adc1, adc2, adc3, tdc1, tdc2, tdc3);
    }

    //Need to create ALERTDetHit --- But where is this clas located? org.jlab.det.hits? I do not
    // see such a file
    //private ALERTDetHit _matchedTrackHit; // matched hit information from


    @Override
    public int compareTo(@NotNull Hit o) {
        return 0;
    }
}
