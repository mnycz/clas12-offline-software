package org.jlab.detector.geant4.v2;

import eu.mihosoft.vrl.v3d.Vector3d;
import org.jlab.detector.geant4.v2.Geant4Factory;
import org.jlab.detector.volume.G4World;
import org.jlab.detector.volume.Geant4Basic;
import org.jlab.geom.base.ConstantProvider;

public final class ALERTGeant4Factory extends Geant4Factory {
    private final int npaddles = 64;


    public ALERTGeant4Factory(ConstantProvider cp) {
        motherVolume = new G4World("root");

    }


    //public Vector3d getCenter(double radius, double thickness, double angle){
    public Vector3d getCenter( double thickness, double angle){
        // ** Removed thickness radius**
        Vector3d cent = new Vector3d(thickness/2.,0, 0);
        cent.rotateZ(Math.toRadians(angle));
        return cent;
    }

    public Geant4Basic getPaddle(int ipaddle) {
        if (ipaddle < 1 || ipaddle > npaddles) {
            System.err.println("ERROR!!!");
            System.err.println("ALERT Paddle #" + ipaddle + " doesn't exist");
            System.exit(111);
        }
        return motherVolume.getChildren().get(ipaddle - 1);
    }


}
