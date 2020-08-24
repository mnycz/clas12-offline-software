package org.jlab.rec.cvt.services;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.HashMap;
import java.util.Map;

import org.jlab.clas.reco.ReconstructionEngine;
import org.jlab.clas.swimtools.Swim;
import org.jlab.clas.tracking.kalmanfilter.Surface;
import org.jlab.clas.tracking.objects.Strip;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.geant4.v2.CTOFGeant4Factory;
import org.jlab.detector.geant4.v2.SVT.SVTConstants;
import org.jlab.detector.geant4.v2.SVT.SVTStripFactory;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.prim.Cylindrical3D;
import org.jlab.geom.prim.Plane3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.cvt.Constants;
import org.jlab.rec.cvt.banks.HitReader;
import org.jlab.rec.cvt.banks.RecoBankWriter;
import org.jlab.rec.cvt.bmt.CCDBConstantsLoader;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cluster.ClusterFinder;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.cross.CrossMaker;
import org.jlab.rec.cvt.hit.ADCConvertor;
import org.jlab.rec.cvt.hit.FittedHit;
import org.jlab.rec.cvt.hit.Hit;
import org.jlab.rec.cvt.track.Seed;
import org.jlab.rec.cvt.track.Track;
import org.jlab.rec.cvt.track.TrackListFinder;
import org.jlab.rec.cvt.track.TrackSeeder;
import org.jlab.rec.cvt.track.TrackSeederCA;

/**
 * Service to return reconstructed TRACKS
 * format
 *
 * @author ziegler
 *
 */
public class CVTRecNewKF extends ReconstructionEngine {

    org.jlab.rec.cvt.svt.Geometry SVTGeom;
    org.jlab.rec.cvt.bmt.Geometry BMTGeom;
    CTOFGeant4Factory CTOFGeom;
    Detector          CNDGeom ;
    SVTStripFactory svtIdealStripFactory;
    
    public CVTRecNewKF() {
        super("CVTTracks", "ziegler", "4.0");
        
        SVTGeom = new org.jlab.rec.cvt.svt.Geometry();
        BMTGeom = new org.jlab.rec.cvt.bmt.Geometry();
        
    }

    String FieldsConfig = "";
    int Run = -1;
    public boolean isSVTonly = false;
    
    public void setRunConditionsParameters(DataEvent event, String FieldsConfig, int iRun, boolean addMisAlignmts, String misAlgnFile) {
        if (event.hasBank("RUN::config") == false) {
            System.err.println("RUN CONDITIONS NOT READ!");
            return;
        }

        int Run = iRun;

        boolean isMC = false;
        boolean isCosmics = false;
        DataBank bank = event.getBank("RUN::config");
        //System.out.println("EVENTNUM "+bank.getInt("event",0));
        if (bank.getByte("type", 0) == 0) {
            isMC = true;
        }
        if (bank.getByte("mode", 0) == 1) {
            isCosmics = true;
        }

        

        // Load the fields
        //-----------------
        String newConfig = "SOLENOID" + bank.getFloat("solenoid", 0);

        if (FieldsConfig.equals(newConfig) == false) {
            // Load the Constants
            
            this.setFieldsConfig(newConfig);
        }
        FieldsConfig = newConfig;

        // Load the constants
        //-------------------
        int newRun = bank.getInt("run", 0);
        
        if (Run != newRun) {
            boolean align=false;
            //Load field scale
            double SolenoidScale =(double) bank.getFloat("solenoid", 0);
            Constants.setSolenoidscale(SolenoidScale);
            if(Math.abs(SolenoidScale)<0.001)
            Constants.setCosmicsData(true);
            
//            System.out.println(" LOADING CVT GEOMETRY...............................variation = "+variationName);
//            CCDBConstantsLoader.Load(new DatabaseConstantProvider(newRun, variationName));
//            System.out.println("SVT LOADING WITH VARIATION "+variationName);
//            DatabaseConstantProvider cp = new DatabaseConstantProvider(newRun, variationName);
//            cp = SVTConstants.connect( cp );
//            cp.disconnect();  
//            SVTStripFactory svtFac = new SVTStripFactory(cp, true);
//            SVTGeom.setSvtStripFactory(svtFac);
            float[]b = new float[3];
            Swim swimmer = new Swim();
            swimmer.BfieldLab(0, 0, org.jlab.rec.cvt.Constants.getZoffset()/10, b);
            Constants.setSolenoidVal(Math.abs(b[2]));
            Constants.Load(isCosmics, isSVTonly);
            this.setRun(newRun);

        }
      
        Run = newRun;
        this.setRun(Run);
    }

    public int getRun() {
        return Run;
    }

    public void setRun(int run) {
        Run = run;
    }

    public String getFieldsConfig() {
        return FieldsConfig;
    }

    public void setFieldsConfig(String fieldsConfig) {
        FieldsConfig = fieldsConfig;
    }
    @Override
    public boolean processDataEvent(DataEvent event) {
        
        this.setRunConditionsParameters(event, FieldsConfig, Run, false, "");
        double shift = org.jlab.rec.cvt.Constants.getZoffset();

        this.FieldsConfig = this.getFieldsConfig();
        
        Swim swimmer = new Swim();
        ADCConvertor adcConv = new ADCConvertor();

        RecoBankWriter rbc = new RecoBankWriter();

        HitReader hitRead = new HitReader();
        hitRead.fetch_SVTHits(event, adcConv, -1, -1, SVTGeom);
        if(isSVTonly==false)
          hitRead.fetch_BMTHits(event, adcConv, BMTGeom);

        List<Hit> hits = new ArrayList<Hit>();
        //I) get the hits
        List<Hit> svt_hits = hitRead.get_SVTHits();
        if(svt_hits.size()>org.jlab.rec.cvt.svt.Constants.MAXSVTHITS)
            return true;
        if (svt_hits != null && svt_hits.size() > 0) {
            hits.addAll(svt_hits);
        }

        List<Hit> bmt_hits = hitRead.get_BMTHits();
        if (bmt_hits != null && bmt_hits.size() > 0) {
            hits.addAll(bmt_hits);

            if(bmt_hits.size()>org.jlab.rec.cvt.bmt.Constants.MAXBMTHITS)
                 return true;
        }

        //II) process the hits		
        List<FittedHit> SVThits = new ArrayList<FittedHit>();
        List<FittedHit> BMThits = new ArrayList<FittedHit>();
        //1) exit if hit list is empty
        if (hits.size() == 0) {
            return true;
        }
       
        List<Cluster> clusters = new ArrayList<Cluster>();
        List<Cluster> SVTclusters = new ArrayList<Cluster>();
        List<Cluster> BMTclusters = new ArrayList<Cluster>();

        //2) find the clusters from these hits
        ClusterFinder clusFinder = new ClusterFinder();
        clusters.addAll(clusFinder.findClusters(svt_hits, BMTGeom));     
        if(bmt_hits != null && bmt_hits.size() > 0)
            clusters.addAll(clusFinder.findClusters(bmt_hits, BMTGeom)); 
        
        if (clusters.size() == 0) {
            rbc.appendCVTBanks(event, SVThits, BMThits, null, null, null, null, shift);
            return true;
        }
        
        // fill the fitted hits list.
        if (clusters.size() != 0) {
            for (int i = 0; i < clusters.size(); i++) {
                if (clusters.get(i).get_Detector() == 0) {
                    SVTclusters.add(clusters.get(i));
                    SVThits.addAll(clusters.get(i));
                }
                if (clusters.get(i).get_Detector() == 1) {
                    BMTclusters.add(clusters.get(i));
                    BMThits.addAll(clusters.get(i));
                }
            }
        }

        List<ArrayList<Cross>> crosses = new ArrayList<ArrayList<Cross>>();
        CrossMaker crossMake = new CrossMaker();
        crosses = crossMake.findCrosses(clusters, SVTGeom);
         if(crosses.get(0).size() > org.jlab.rec.cvt.svt.Constants.MAXSVTCROSSES ) {
            rbc.appendCVTBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, null, null, shift);
            return true; 
         }
         {//System.out.println(" FITTING SEED......................");
           
            List<Seed> seeds = null;
            
            if(this.isSVTonly) {
                TrackSeeder trseed = new TrackSeeder();
                seeds = trseed.findSeed(crosses.get(0), crosses.get(1), SVTGeom, BMTGeom, swimmer);
            
            } else {
                TrackSeederCA trseed = new TrackSeederCA();  // cellular automaton seeder
                seeds = trseed.findSeed(crosses.get(0), crosses.get(1), SVTGeom, BMTGeom, swimmer);
                
            }
            if(seeds ==null) {
                this.CleanupSpuriousCrosses(crosses, null) ;
                rbc.appendCVTBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, null, shift);
                return true;
            }   
            if(seeds ==null) {
                this.CleanupSpuriousCrosses(crosses, null) ;
                rbc.appendCVTBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, null, shift);
                return true;
            }   
            org.jlab.clas.tracking.kalmanfilter.helical.KFitter kf;
            List<Track> trkcands = new ArrayList<Track>();
 
            for (Seed seed : seeds) { 
                if(Constants.LIGHTVEL * seed.get_Helix().radius() *Constants.getSolenoidVal()<Constants.PTCUT)
                    continue;
                org.jlab.clas.tracking.trackrep.Helix hlx = null ;
               
                double xr =  -seed.get_Helix().get_dca()*Math.sin(seed.get_Helix().get_phi_at_dca());
                double yr =  seed.get_Helix().get_dca()*Math.cos(seed.get_Helix().get_phi_at_dca());
                double zr =  seed.get_Helix().get_Z0();
                double pt = Constants.LIGHTVEL * seed.get_Helix().radius() * Constants.getSolenoidVal();
                double pz = pt*seed.get_Helix().get_tandip();
                double px = pt*Math.cos(seed.get_Helix().get_phi_at_dca());
                double py = pt*Math.sin(seed.get_Helix().get_phi_at_dca());
                hlx = new org.jlab.clas.tracking.trackrep.Helix(xr,yr,zr,px,py,pz, 
                        (int) (Math.signum(Constants.getSolenoidscale())*seed.get_Helix().get_charge()), Constants.getSolenoidVal(), org.jlab.clas.tracking.trackrep.Helix.Units.MM);
                
                Matrix cov = seed.get_Helix().get_covmatrix();
                kf = new org.jlab.clas.tracking.kalmanfilter.helical.KFitter( hlx, cov, event,  swimmer, 
                        org.jlab.rec.cvt.Constants.getXb(), 
                        org.jlab.rec.cvt.Constants.getYb(),
                        org.jlab.rec.cvt.Constants.getZoffset(), 
                        this.setMeasVecs(seed, SVTGeom)) ;
                
                kf.runFitter(swimmer);
                
            //System.out.println(" OUTPUT SEED......................");
            
            if (kf.setFitFailed == false) {
                trkcands.add(this.OutputTrack(seed, kf));
                trkcands.get(trkcands.size() - 1).set_TrackingStatus(2);
           //} else {
            //    trkcands.add(this.OutputTrack(seed, SVTGeom, swimmer));
            //    trkcands.get(trkcands.size() - 1).set_TrackingStatus(1);
           }
        }
        
        if (trkcands.size() == 0) {
            this.CleanupSpuriousCrosses(crosses, null) ;
            rbc.appendCVTBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, null, shift);
            return true;
        }
        //This last part does ELoss C
        TrackListFinder trkFinder = new TrackListFinder();
        List<Track> trks = trkFinder.getTracks(trkcands, SVTGeom, BMTGeom, CTOFGeom, CNDGeom, swimmer);
        for( int i=0;i<trks.size();i++) { 
            trks.get(i).set_Id(i+1);
        }
        
        //System.out.println( " *** *** trkcands " + trkcands.size() + " * trks " + trks.size());
        trkFinder.removeOverlappingTracks(trks); //turn off until debugged

        
//      FIXME: workaround to properly assign the position and direction to the BMT crosses. to be understood where it comes from the not correct one  
        for( Track t : trks ) {
	    	for( Cross c : t ) {
	        	if (Double.isNaN(c.get_Point0().x())) {
	        		double r = org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[c.get_Region()-1]+org.jlab.rec.cvt.bmt.Constants.hStrip2Det;
	        		Point3D p = t.get_helix().getPointAtRadius(r);
	                c.set_Point(new Point3D(p.x(), p.y(), c.get_Point().z()));
	                Vector3D v = t.get_helix().getTrackDirectionAtRadius(r);
	                c.set_Dir(v);
	            }
	            if (Double.isNaN(c.get_Point0().z())) {
	        		double r = org.jlab.rec.cvt.bmt.Constants.getCRZRADIUS()[c.get_Region()-1]+org.jlab.rec.cvt.bmt.Constants.hStrip2Det;
	        		Point3D p = t.get_helix().getPointAtRadius(r);
	                c.set_Point(new Point3D(c.get_Point().x(), c.get_Point().y(), p.z()));
	                Vector3D v = t.get_helix().getTrackDirectionAtRadius(r);
	                c.set_Dir(v);
	            }
	    	}
        }
        
        
        for (int c = 0; c < trks.size(); c++) {
            trks.get(c).set_Id(c + 1);
            for (int ci = 0; ci < trks.get(c).size(); ci++) {

                if (crosses.get(0) != null && crosses.get(0).size() > 0) {
//                    for (Cross crsSVT : crosses.get(0)) {
                	for (int jj=0 ; jj < crosses.get(0).size(); jj++) {
                		Cross crsSVT = crosses.get(0).get(jj);
                        if (crsSVT.get_Sector() == trks.get(c).get(ci).get_Sector() && crsSVT.get_Cluster1()!=null && crsSVT.get_Cluster2()!=null 
                                && trks.get(c).get(ci).get_Cluster1()!=null && trks.get(c).get(ci).get_Cluster2()!=null
                                && crsSVT.get_Cluster1().get_Id() == trks.get(c).get(ci).get_Cluster1().get_Id()
                                && crsSVT.get_Cluster2().get_Id() == trks.get(c).get(ci).get_Cluster2().get_Id()) {  
                            crsSVT.set_Point(trks.get(c).get(ci).get_Point());
                            trks.get(c).get(ci).set_Id(crsSVT.get_Id());
                            crsSVT.set_PointErr(trks.get(c).get(ci).get_PointErr());
                            crsSVT.set_Dir(trks.get(c).get(ci).get_Dir());
                            crsSVT.set_DirErr(trks.get(c).get(ci).get_DirErr());
                            crsSVT.set_AssociatedTrackID(c + 1);
                            crsSVT.get_Cluster1().set_AssociatedTrackID(c + 1);
                            for (FittedHit h : crsSVT.get_Cluster1()) {
                                h.set_AssociatedTrackID(c + 1);
                            }
                            for (FittedHit h : crsSVT.get_Cluster2()) {
                                h.set_AssociatedTrackID(c + 1);
                            }
                            crsSVT.get_Cluster2().set_AssociatedTrackID(c + 1);

                        }
                    }
                }
                if (crosses.get(1) != null && crosses.get(1).size() > 0) {
//                    for (Cross crsBMT : crosses.get(1)) {
                	for (int jj=0 ; jj < crosses.get(1).size(); jj++) {
                		Cross crsBMT = crosses.get(1).get(jj);
                        if (crsBMT.get_Id() == trks.get(c).get(ci).get_Id()) {
                            crsBMT.set_Point(trks.get(c).get(ci).get_Point());
                            crsBMT.set_PointErr(trks.get(c).get(ci).get_PointErr());
                            crsBMT.set_Dir(trks.get(c).get(ci).get_Dir());
                            crsBMT.set_DirErr(trks.get(c).get(ci).get_DirErr());
                            crsBMT.set_AssociatedTrackID(c + 1);
                            crsBMT.get_Cluster1().set_AssociatedTrackID(c + 1);
                            for (FittedHit h : crsBMT.get_Cluster1()) {
                                h.set_AssociatedTrackID(c + 1);
                            }
                        }
                    }
                }
            }
        }
        
        /// remove direction information from crosses that were part of duplicates, now removed. TODO: Should I put it in the clone removal?  
        for( Cross c : crosses.get(1) ) {
        	if( c.get_AssociatedTrackID() < 0 ) {
        		c.set_Dir( new Vector3D(0,0,0));
        		c.set_DirErr( new Vector3D(0,0,0));
        		if( c.get_DetectorType().equalsIgnoreCase("C")) {
//        			System.out.println(c + " " + c.get_AssociatedTrackID());
        			c.set_Point(new Point3D(Double.NaN,Double.NaN,c.get_Point().z()));
//        			System.out.println(c.get_Point());
        		}
        		else {
        			c.set_Point(new Point3D(c.get_Point().x(),c.get_Point().y(),Double.NaN));
        		}
        	}
        }
        for( Cross c : crosses.get(0) ) {
        	if( c.get_AssociatedTrackID() < 0 ) {
        		c.set_Dir( new Vector3D(0,0,0));
        		c.set_DirErr( new Vector3D(0,0,0));
        	}
        }
        
        
        //------------------------
        // set index associations
        if (trks.size() > 0) {
            this.CleanupSpuriousCrosses(crosses, trks) ;
            rbc.appendCVTBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, trks, shift);
        }
        //System.out.println("H");
    } 
    //event.show();
    return true;

    }
    private void CleanupSpuriousCrosses(List<ArrayList<Cross>> crosses, List<Track> trks) {
        List<Cross> rmCrosses = new ArrayList<Cross>();
        
        for(Cross c : crosses.get(0)) {
            double z = SVTGeom.transformToFrame(c.get_Sector(), c.get_Region()*2, c.get_Point().x(), c.get_Point().y(),c.get_Point().z(), "local", "").z();
            if(z<-0.1 || z>SVTConstants.MODULELEN) {
                rmCrosses.add(c);
            }
        }
       
        
        for(int j = 0; j<crosses.get(0).size(); j++) {
            for(Cross c : rmCrosses) {
                if(crosses.get(0).get(j).get_Id()==c.get_Id())
                    crosses.get(0).remove(j);
            }
        } 
        
       
        if(trks!=null && rmCrosses!=null) {
            List<Track> rmTrks = new ArrayList<Track>();
            for(Track t:trks) {
                boolean rmFlag=false;
                for(Cross c: rmCrosses) {
                    if(c!=null && t!=null && c.get_AssociatedTrackID()==t.get_Id())
                        rmFlag=true;
                }
                if(rmFlag==true)
                    rmTrks.add(t);
            }
            trks.removeAll(rmTrks);
        }
    }
    
    private List<Surface> setMeasVecs(Seed trkcand, org.jlab.rec.cvt.svt.Geometry sgeo) {
        List<Surface> KFSites = new ArrayList<Surface>();
        Plane3D pln0 = new Plane3D(new Point3D(Constants.getXb(),Constants.getYb(),Constants.getZoffset()),
        new Vector3D(0,0,1));
        Surface meas0 = new Surface(pln0,new Point3D(Constants.getXb(),Constants.getYb(),0),
        new Point3D(Constants.getXb()-300,Constants.getYb(),0), new Point3D(Constants.getXb()+300,Constants.getYb(),0));
        meas0.setSector(0);
        meas0.setLayer(0);
        meas0.setError(1);
        KFSites.add(meas0); 
        for (int i = 0; i < trkcand.get_Clusters().size(); i++) { //SVT
            if(trkcand.get_Clusters().get(i).get_Detector()==0) {
                int id = trkcand.get_Clusters().get(i).get_Id();
                double ce = trkcand.get_Clusters().get(i).get_Centroid();
                Point3D endPt1 = trkcand.get_Clusters().get(i).getEndPoint1();
                Point3D endPt2 = trkcand.get_Clusters().get(i).getEndPoint2();
                Strip strp = new Strip(id, ce, endPt1.x(), endPt1.y(), endPt1.z(),
                                        endPt2.x(), endPt2.y(), endPt2.z());
                Plane3D pln = new Plane3D(endPt1,sgeo.findBSTPlaneNormal(trkcand.get_Clusters().get(i).get_Sector(), 
                        trkcand.get_Clusters().get(i).get_Layer()));
                Point3D Or = sgeo.getPlaneModuleOrigin(trkcand.get_Clusters().get(i).get_Sector(), trkcand.get_Clusters().get(i).get_Layer());
                Point3D En = sgeo.getPlaneModuleEnd(trkcand.get_Clusters().get(i).get_Sector(), trkcand.get_Clusters().get(i).get_Layer());
                Surface meas = new Surface(pln, strp, Or, En);
                meas.setSector(trkcand.get_Clusters().get(i).get_Sector());
                meas.setLayer(trkcand.get_Clusters().get(i).get_Layer());
                double err = trkcand.get_Clusters().get(i).get_Error();
                meas.setError(err*err); // CHECK THIS .... DOES KF take e or e^2?
                if(i>0 && KFSites.get(KFSites.size()-1).getLayer()==meas.getLayer())
                    continue;
                KFSites.add(meas);
            }
        }

        // adding the BMT
        for (int c = 0; c < trkcand.get_Crosses().size(); c++) {
            if (trkcand.get_Crosses().get(c).get_Detector().equalsIgnoreCase("BMT")) {
                Cylindrical3D cyl = new Cylindrical3D();
                cyl.baseArc().setCenter(new Point3D(0, 0, 0));
                cyl.highArc().setCenter(new Point3D(0, 0, 0));
                cyl.baseArc().setNormal(new Vector3D(0,1,0));
                cyl.highArc().setNormal(new Vector3D(0,1,0));
                
                int id = trkcand.get_Clusters().get(c).get_Id();
                double ce = trkcand.get_Crosses().get(c).get_Cluster1().get_Centroid();
                if (trkcand.get_Crosses().get(c).get_DetectorType().equalsIgnoreCase("Z")) {
                    double x = trkcand.get_Crosses().get(c).get_Point().x();
                    double y = trkcand.get_Crosses().get(c).get_Point().y();
                    double phi = Math.atan2(y,x);
                    double err = trkcand.get_Crosses().get(c).get_Cluster1().get_PhiErr();
                    
                    Strip strp = new Strip(id, ce, x, y, phi);
                    //cyl.baseArc().setRadius(Math.sqrt(x*x+y*y));
                    //cyl.highArc().setRadius(Math.sqrt(x*x+y*y));
                    cyl.baseArc().setRadius(org.jlab.rec.cvt.bmt.Constants.getCRZRADIUS()[(trkcand.get_Crosses().get(c).get_Cluster1().get_Layer() + 1) / 2 - 1]+org.jlab.rec.cvt.bmt.Constants.hStrip2Det);
                    cyl.highArc().setRadius(org.jlab.rec.cvt.bmt.Constants.getCRZRADIUS()[(trkcand.get_Crosses().get(c).get_Cluster1().get_Layer() + 1) / 2 - 1]+org.jlab.rec.cvt.bmt.Constants.hStrip2Det);                   
                    
                    Surface meas = new Surface(cyl, strp);
                    meas.setSector(trkcand.get_Crosses().get(c).get_Sector());
                    meas.setLayer(trkcand.get_Crosses().get(c).get_Cluster1().get_Layer()+6);
                    meas.setError(err*err); // CHECK THIS .... DOES KF take e or e^2?
                    if(c>0 && KFSites.get(KFSites.size()-1).getLayer()==meas.getLayer())
                        continue;
                    KFSites.add(meas);
                }
                if (trkcand.get_Crosses().get(c).get_DetectorType().equalsIgnoreCase("C")) {
                    double z = trkcand.get_Crosses().get(c).get_Point().z();
                    double err = trkcand.get_Crosses().get(c).get_Cluster1().get_ZErr();
                    
                    Strip strp = new Strip(id, ce, z);
                    cyl.baseArc().setRadius(org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[(trkcand.get_Crosses().get(c).get_Cluster1().get_Layer() + 1) / 2 - 1]+org.jlab.rec.cvt.bmt.Constants.hStrip2Det);
                    cyl.highArc().setRadius(org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[(trkcand.get_Crosses().get(c).get_Cluster1().get_Layer() + 1) / 2 - 1]+org.jlab.rec.cvt.bmt.Constants.hStrip2Det);                   
                    Surface meas = new Surface(cyl, strp);
                    meas.setSector(trkcand.get_Crosses().get(c).get_Sector());
                    meas.setLayer(trkcand.get_Crosses().get(c).get_Cluster1().get_Layer()+6);
                    meas.setError(err*err); // CHECK THIS .... DOES KF take e or e^2?
                    if(c>0 && KFSites.get(KFSites.size()-1).getLayer()==meas.getLayer())
                        continue;
                    KFSites.add(meas);
                }
            }
        }
        return KFSites;
    }

    
    private void MatchTrack2Traj(Seed trkcand, Map<Integer, 
            org.jlab.clas.tracking.kalmanfilter.helical.KFitter.HitOnTrack> traj, 
            org.jlab.rec.cvt.svt.Geometry sgeo) {
        
        for (int i = 0; i < trkcand.get_Clusters().size(); i++) { //SVT
            if(trkcand.get_Clusters().get(i).get_Detector()==0) {
                Cluster cluster = trkcand.get_Clusters().get(i);
                int layer = trkcand.get_Clusters().get(i).get_Layer();
                int sector = trkcand.get_Clusters().get(i).get_Sector();
                Point3D p = new Point3D(traj.get(layer).x, traj.get(layer).y, traj.get(layer).z);
                double doca2Cls = sgeo.getDOCAToStrip(sector, layer, cluster.get_Centroid(), p);
                double doca2Seed = sgeo.getDOCAToStrip(sector, layer, (double) cluster.get_SeedStrip(), p);
                cluster.set_SeedResidual(doca2Seed); 
                cluster.set_CentroidResidual(doca2Cls);
            
                for (FittedHit hit : cluster) {
                    double doca1 = sgeo.getDOCAToStrip(sector, layer, (double) hit.get_Strip().get_Strip(), p);
                    double sigma1 = sgeo.getSingleStripResolution(layer, hit.get_Strip().get_Strip(), traj.get(layer).z);
                    hit.set_stripResolutionAtDoca(sigma1);
                    hit.set_docaToTrk(doca2Cls);  

                }
            }
        }

        // adding the BMT
        for (int c = 0; c < trkcand.get_Crosses().size(); c++) {
            if (trkcand.get_Crosses().get(c).get_Detector().equalsIgnoreCase("BMT")) {
                double ce = trkcand.get_Crosses().get(c).get_Cluster1().get_Centroid();
                if (trkcand.get_Crosses().get(c).get_DetectorType().equalsIgnoreCase("Z")) {
                    //double x = trkcand.get_Crosses().get(c).get_Point().x();
                    //double y = trkcand.get_Crosses().get(c).get_Point().y();
                    //double phi = Math.atan2(y,x);
                    //double err = trkcand.get_Crosses().get(c).get_Cluster1().get_PhiErr();
                    //int sector = trkcand.get_Crosses().get(c).get_Sector();
                    int layer = trkcand.get_Crosses().get(c).get_Cluster1().get_Layer()+6;
                    Cluster cluster = trkcand.get_Crosses().get(c).get_Cluster1();
                    Point3D p = new Point3D(traj.get(layer).x, traj.get(layer).y, traj.get(layer).z);
                    double doca2Cls = Math.atan2(p.y(), p.x())-cluster.get_Phi()*
                            (org.jlab.rec.cvt.bmt.Constants.getCRZRADIUS()[cluster.get_Region() - 1] 
                            + org.jlab.rec.cvt.bmt.Constants.hStrip2Det);
                    
                    cluster.set_CentroidResidual(doca2Cls);

                    for (FittedHit hit : cluster) {
                        double doca1 = Math.atan2(p.y(), p.x())-hit.get_Strip().get_Phi()*
                            (org.jlab.rec.cvt.bmt.Constants.getCRZRADIUS()[cluster.get_Region() - 1] 
                            + org.jlab.rec.cvt.bmt.Constants.hStrip2Det);
                       
                        hit.set_docaToTrk(doca2Cls);  

                    }
                }
                if (trkcand.get_Crosses().get(c).get_DetectorType().equalsIgnoreCase("C")) {
                    double z = trkcand.get_Crosses().get(c).get_Point().z();
                    double err = trkcand.get_Crosses().get(c).get_Cluster1().get_ZErr();
                    int layer = trkcand.get_Crosses().get(c).get_Cluster1().get_Layer()+6;
                    Cluster cluster = trkcand.get_Crosses().get(c).get_Cluster1();
                    Point3D p = new Point3D(traj.get(layer).x, traj.get(layer).y, traj.get(layer).z);
                    double doca2Cls = p.z()-cluster.get_Z();
                    
                    cluster.set_CentroidResidual(doca2Cls);

                    for (FittedHit hit : cluster) {
                        double doca1 = p.z()-hit.get_Strip().get_Z();
                        hit.set_docaToTrk(doca2Cls);  

                    }
                }
            }
        }
    }
    
    private Track OutputTrack(Seed seed, org.jlab.clas.tracking.kalmanfilter.helical.KFitter kf) {
        org.jlab.rec.cvt.trajectory.Helix helix = new org.jlab.rec.cvt.trajectory.Helix(kf.KFHelix.getD0(), 
                kf.KFHelix.getPhi0(), kf.KFHelix.getOmega(), 
                kf.KFHelix.getZ0(), kf.KFHelix.getTanL());
        helix.B = kf.KFHelix.getB();
        Track cand = new Track(helix);
        cand.setNDF(kf.NDF);
        cand.setChi2(kf.chi2);
        
        for (Cross c : seed.get_Crosses()) {
            if (c.get_Detector().equalsIgnoreCase("SVT")) {
                continue;
            }
        }
        cand.addAll(seed.get_Crosses());
        this.MatchTrack2Traj(seed, kf.TrjPoints, SVTGeom);
        return cand;
        
    }
    private Track OutputTrack(Seed seed) {
        
        Track cand = new Track(seed.get_Helix());
        
        for (Cross c : seed.get_Crosses()) {
            if (c.get_Detector().equalsIgnoreCase("SVT")) {
                continue;
            }
        }
        cand.addAll(seed.get_Crosses());
        return cand;
        
    }
    @Override
    public boolean init() {
        // Load config
        String rmReg = this.getEngineConfigString("removeRegion");
        
        if (rmReg!=null) {
            System.out.println("["+this.getName()+"] run with region "+rmReg+"removed config chosen based on yaml");
            Constants.setRmReg(Integer.valueOf(rmReg));
        }
        else {
            rmReg = System.getenv("COAT_CVT_REMOVEREGION");
            if (rmReg!=null) {
                System.out.println("["+this.getName()+"] run with region "+rmReg+"removed config chosen based on env");
                Constants.setRmReg(Integer.valueOf(rmReg));
            }
        }
        if (rmReg==null) {
             System.out.println("["+this.getName()+"] run with all region (default) ");
        }
        //svt stand-alone
        String svtStAl = this.getEngineConfigString("svtOnly");
        
        if (svtStAl!=null) {
            System.out.println("["+this.getName()+"] run with SVT only "+svtStAl+" config chosen based on yaml");
            this.isSVTonly= Boolean.valueOf(svtStAl);
        }
        else {
            svtStAl = System.getenv("COAT_SVT_ONLY");
            if (svtStAl!=null) {
                System.out.println("["+this.getName()+"] run with SVT only "+svtStAl+" config chosen based on env");
                this.isSVTonly= Boolean.valueOf(svtStAl);
            }
        }
        if (svtStAl==null) {
             System.out.println("["+this.getName()+"] run with both CVT systems (default) ");
        }
        // Load other geometries
        
        variationName = Optional.ofNullable(this.getEngineConfigString("variation")).orElse("default");
        System.out.println(" CVT YAML VARIATION NAME + "+variationName);
        ConstantProvider providerCTOF = GeometryFactory.getConstants(DetectorType.CTOF, 11, variationName);
        CTOFGeom = new CTOFGeant4Factory(providerCTOF);        
        CNDGeom =  GeometryFactory.getDetector(DetectorType.CND, 11, variationName);
        //
          
        
        System.out.println(" LOADING CVT GEOMETRY...............................variation = "+variationName);
        CCDBConstantsLoader.Load(new DatabaseConstantProvider(11, variationName));
        System.out.println("SVT LOADING WITH VARIATION "+variationName);
        DatabaseConstantProvider cp = new DatabaseConstantProvider(11, variationName);
        cp = SVTConstants.connect( cp );
        cp.disconnect();  
        SVTStripFactory svtFac = new SVTStripFactory(cp, true);
        SVTGeom.setSvtStripFactory(svtFac);

        return true;
    }
  
    private String variationName;
    

}