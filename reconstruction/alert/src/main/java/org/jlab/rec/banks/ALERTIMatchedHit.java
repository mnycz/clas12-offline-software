package org.jlab.rec.banks;

import org.jlab.utils.groups.IndexedTable;

import java.util.ArrayList;
import java.util.List;

public interface ALERTIMatchedHit {
    public String DetectorName();

    public List<ALERTBaseHit> MatchHits(ArrayList<ALERTBaseHit> ADCandTDCLists, double timeJitter, IndexedTable tdcConv, IndexedTable ADCandTDCOffsets);

}
