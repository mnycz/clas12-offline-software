package org.jlab.detector.geant4.v2;

import eu.mihosoft.vrl.v3d.Vector3d;
import org.jlab.detector.geant4.v2.Geant4Factory;
import org.jlab.geom.base.ConstantProvider;

public final class ALERTGeant4Factory extends Geant4Factory {
    private final int paddles = 64;


    public ALERTGeant4Factory(ConstantProvider cp) {

    }


    //public Vector3d getCenter(double radius, double thickness, double angle){
    public Vector3d getCenter( double thickness, double angle){
        // ** Removed thickness radius**
        Vector3d cent = new Vector3d(thickness/2.,0, 0);
        cent.rotateZ(Math.toRadians(angle));
        return cent;
    }

}
