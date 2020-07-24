/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.tracking;

/**
 *
 * @author ziegler
 */
public class Constants {

    /**
     * @return the unitScale
     */
    public static double getUnitScale() {
        return unitScale;
    }

    /**
     * @param aUnitScale the unitScale to set
     */
    public static void setUnitScale(double aUnitScale) {
        unitScale = aUnitScale;
    }

    /**
     * @return the LightVel
     */
    public static double getLIGHTVEL() {
        return LightVel;
    }

    /**
     * @param aLIGHTVEL the LightVel to set
     */
    public static void setLIGHTVEL(double aLIGHTVEL) {
        LightVel = aLIGHTVEL;
    }

    public static final double LIGHTVEL = 0.0000299792458;       // velocity of light (cm/ns) - conversion factor from radius in mm to momentum in GeV/c 
    
    private static double LightVel = 0.0000299792458;       // velocity of light (cm/ns) - conversion factor from radius in mm to momentum in GeV/c 
    private static double unitScale = 1;
}
