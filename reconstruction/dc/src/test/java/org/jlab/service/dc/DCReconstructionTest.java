package org.jlab.service.dc;

import cnuphys.magfield.MagneticFields;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

import org.jlab.io.base.DataEvent;
import org.jlab.service.dc.DCHBEngine;
import org.jlab.service.dc.DCTBEngine;

import org.jlab.analysis.physics.TestEvent;
import org.jlab.analysis.math.ClasMath;

import org.jlab.clas.swimtools.MagFieldsEngine;
import org.jlab.geom.prim.Point3D;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.utils.CLASResources;
import org.jlab.utils.system.ClasUtilsFile;

/**
 *
 * @author nharrison, marmstr
 */
public class DCReconstructionTest {

  @Test
  public void testDCReconstruction() {
    System.setProperty("CLAS12DIR", "../../");

    String mapDir = CLASResources.getResourcePath("etc")+"/data/magfield";
        try {
            MagneticFields.getInstance().initializeMagneticFields(mapDir,
                    "Symm_torus_r2501_phi16_z251_24Apr2018.dat","Symm_solenoid_r601_phi1_z1201_13June2018.dat");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    String dir = ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4");
    SchemaFactory schemaFactory = new SchemaFactory();
    schemaFactory.initFromDirectory(dir);

    DataEvent testEvent = TestEvent.getDCSector1ElectronEvent(schemaFactory);

    MagFieldsEngine enf = new MagFieldsEngine();
    enf.init();
    enf.processDataEvent(testEvent);
    DCHBEngine engineHB = new DCHBEngine();
    engineHB.init();
    engineHB.processDataEvent(testEvent); 
    if(testEvent.hasBank("HitBasedTrkg::HBTracks")) {
        testEvent.getBank("HitBasedTrkg::HBTracks").show();
    }
    
    //Compare HB momentum to expectation
    assertEquals(testEvent.hasBank("HitBasedTrkg::HBTracks"), true);
    assertEquals(testEvent.getBank("HitBasedTrkg::HBTracks").rows(), 1);
    assertEquals(testEvent.getBank("HitBasedTrkg::HBTracks").getByte("q", 0), -1);
    assertEquals(ClasMath.isWithinXPercent(16.0, testEvent.getBank("HitBasedTrkg::HBTracks").getFloat("p0_x", 0), 1.057), true);
    assertEquals(testEvent.getBank("HitBasedTrkg::HBTracks").getFloat("p0_y", 0) > -0.1, true);
    assertEquals(testEvent.getBank("HitBasedTrkg::HBTracks").getFloat("p0_y", 0) < 0.1, true);
    assertEquals(ClasMath.isWithinXPercent(16.0, testEvent.getBank("HitBasedTrkg::HBTracks").getFloat("p0_z", 0), 2.266), true);

    //TB reconstruction
    DCTBEngine engineTB = new DCTBEngine();
    engineTB.init();
    engineTB.processDataEvent(testEvent); 
    if(testEvent.hasBank("TimeBasedTrkg::TBTracks")) {
        testEvent.getBank("TimeBasedTrkg::TBTracks").show();
    }

    
    assertEquals(testEvent.hasBank("TimeBasedTrkg::TBTracks"), true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").rows(), 1);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getByte("q", 0), -1);

    assertEquals(ClasMath.isWithinXPercent(5, testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("p0_x", 0), 1.05), true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("p0_y", 0) > -0.05, true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("p0_y", 0) <  0.05, true);
    assertEquals(ClasMath.isWithinXPercent(5, testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("p0_z", 0), 2.26), true);

    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("Vtx0_x", 0) < 0.4, true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("Vtx0_x", 0) > -0.4, true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("Vtx0_y", 0) < 0.4, true);  
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("Vtx0_y", 0) > -0.4, true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("Vtx0_z", 0) < 0.8, true);
    assertEquals(testEvent.getBank("TimeBasedTrkg::TBTracks").getFloat("Vtx0_z", 0) > -0.8, true);
    

    Point3D cross1 = new Point3D( -3.5, -21.7, 237.7);
    Point3D cross2 = new Point3D( -8.8, -33.2, 369.0);
    Point3D cross3 = new Point3D(-27.2, -41.9, 510.6);
    double crossErr = 0.5;

    //Region 1
    assertEquals(Math.abs(testEvent.getBank("TimeBasedTrkg::TBCrosses").getFloat("x", 0 ) - cross1.x())<crossErr, true); 
    assertEquals(Math.abs(testEvent.getBank("TimeBasedTrkg::TBCrosses").getFloat("y", 0 ) - cross1.y())<crossErr, true); 

    //Region 2
    assertEquals(Math.abs(testEvent.getBank("TimeBasedTrkg::TBCrosses").getFloat("x", 1 ) - cross2.x())<crossErr, true); 
    assertEquals(Math.abs(testEvent.getBank("TimeBasedTrkg::TBCrosses").getFloat("y", 1 ) - cross2.y())<crossErr, true); 

    //Region 3
    assertEquals(Math.abs(testEvent.getBank("TimeBasedTrkg::TBCrosses").getFloat("x", 2 ) - cross3.x())<crossErr, true); 
    assertEquals(Math.abs(testEvent.getBank("TimeBasedTrkg::TBCrosses").getFloat("y", 2 ) - cross3.y())<crossErr, true); 
    
  }
  
}
