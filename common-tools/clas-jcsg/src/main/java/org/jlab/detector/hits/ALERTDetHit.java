package org.jlab.detector.hits;


import static org.jlab.detector.hits.DetId.ALERTID;

/**
 *
 * @author Hamza
 */
public final class ALERTDetHit extends DetHit {

    public ALERTDetHit(DetHit hit) {
        super(hit.origin(), hit.end(), hit.detId);
        detectorComponent = hit.detectorComponent;

        if (hit.detId.length != 2 || hit.getId()[0] != ALERTID) {
            throw new IllegalArgumentException("Hit is not ALERT Detector Hit!");
        }
    }

    public int getPaddle() {
        return detId[1];
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(String.format("ALERT detector,"));
        str.append(String.format("paddle#: %d", detId[1]));

        return str.toString();
    }
}


