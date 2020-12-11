package org.jlab.rec.cluster;

import org.jlab.rec.hit.Hit;

import java.util.ArrayList;

public class ALERTCluster extends ArrayList<Hit> implements Comparable<ALERTCluster> {


    private int _Id;
    private int _Layer; // Layer is likely more applicable to the ATOF
    private int _Sector;

    private double _Energy; // the total energy of the cluster
    private double _t; // the cluster time

    public ALERTCluster(int sector, int layer, int id) {
        _Id = id;
        _Layer = layer;
        _Sector = sector;


    }



    /* Issue related to get_ID() in ALERTHitReader
    But this is already defined in ALERT/hit/Hit.java -- Why is it declared again?
    Error will go away when using ALERTCluster (and not Cluster)
    */
   /*
    public int get_Id() {
        return _Id;
    }*/
    public int get_Layer() {
        return _Layer;
    }

    public void set_Layer(int _Layer) { this._Layer = _Layer; }

    public int get_Sector() {
        return _Sector;
    }
    public void set_Sector(int _Sector) {
        this._Sector = _Sector;
    }


    public double get_Energy() {
        return _Energy;
    }
    public void set_Energy(double _Energy) {
        this._Energy = _Energy;
    }

@Override
    public int compareTo(ALERTCluster arg){
        /*
        Missing
         */

        int return_val =0;
        return return_val;
}

}
