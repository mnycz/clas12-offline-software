package org.jlab.rec.tracks;

import org.jlab.detector.hits.DetHit;
import org.jlab.geometry.prim.Line3d;
import org.jlab.rec.hit.ALERT.ALERTHit;

public class ALERTTrack {
    private int    Id;
    private Line3d Line;
    private double Path;
    private DetHit Hit;

    public ALERTTrack() {
    }

    public ALERTTrack(int Id, Line3d Line, double Path) {
        this.Id = Id;
        this.Line = Line;
        this.Path = Path;
    }


        public Line3d getLine() {
            return Line;
        }

        public void setLine(Line3d Line) {
            this.Line = Line;
        }

        public double getPath() {
            return Path;
        }

        public void setPath(double Path) {
            this.Path = Path;
        }

        public int getId() {
            return Id;
        }

        public void setId(int Id) {
            this.Id = Id;
        }

        public DetHit getHit(){
            return ALERTHit;
        }


        public void setHit(DetHit Hit) {
            this.Hit = Hit;
        }
    }

