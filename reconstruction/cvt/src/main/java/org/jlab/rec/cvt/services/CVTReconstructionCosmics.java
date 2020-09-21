package org.jlab.rec.cvt.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jlab.clas.reco.ReconstructionEngine;
import org.jlab.clas.swimtools.Swim;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.geant4.v2.CTOFGeant4Factory;
import org.jlab.detector.geant4.v2.SVT.SVTConstants;
import org.jlab.detector.geant4.v2.SVT.SVTStripFactory;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.cvt.Constants;
import static org.jlab.rec.cvt.Constants.setSolenoidscale;
import org.jlab.rec.cvt.banks.HitReader;
import org.jlab.rec.cvt.banks.RecoBankWriter;
import org.jlab.rec.cvt.bmt.CCDBConstantsLoader;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cluster.ClusterFinder;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.cross.CrossList;
import org.jlab.rec.cvt.cross.CrossMaker;
import org.jlab.rec.cvt.cross.StraightTrackCrossListFinder;
import org.jlab.rec.cvt.hit.ADCConvertor;
import org.jlab.rec.cvt.hit.FittedHit;
import org.jlab.rec.cvt.hit.Hit;
import org.jlab.rec.cvt.track.Seed;
import org.jlab.rec.cvt.track.StraightTrack;
import org.jlab.rec.cvt.track.Track;
import org.jlab.rec.cvt.track.TrackCandListFinder;
import org.jlab.rec.cvt.track.TrackListFinder;
import org.jlab.rec.cvt.track.TrackSeeder;
import org.jlab.rec.cvt.track.TrackSeederCA;
import org.jlab.rec.cvt.track.fit.KFitter;
import org.jlab.rec.cvt.trajectory.Helix;
import org.jlab.rec.cvt.trajectory.Trajectory;
import org.jlab.rec.cvt.trajectory.TrajectoryFinder;

/**
 * Service to return reconstructed TRACKS
 * format
 *
 * @author ziegler
 *
 */
public class CVTReconstructionCosmics extends ReconstructionEngine {

	org.jlab.rec.cvt.svt.Geometry SVTGeom;
	org.jlab.rec.cvt.bmt.Geometry BMTGeom;
	CTOFGeant4Factory CTOFGeom;
	Detector          CNDGeom ;
	SVTStripFactory svtIdealStripFactory;

	public CVTReconstructionCosmics() {
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

			System.out.println(" LOADING BMT GEOMETRY...............................variation = "+variationName);
			CCDBConstantsLoader.Load(new DatabaseConstantProvider(newRun, variationName));
			//            System.out.println("SVT LOADING WITH VARIATION "+variationName);
			//            DatabaseConstantProvider cp = new DatabaseConstantProvider(newRun, variationName);
			//            cp = SVTConstants.connect( cp );
			//            cp.disconnect();  
			//            SVTStripFactory svtFac = new SVTStripFactory(cp, true);
			//            SVTGeom.setSvtStripFactory(svtFac);
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
		double shift = 0;

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


		TrackCandListFinder tclf = new TrackCandListFinder();
		StraightTrackCrossListFinder stclf = new StraightTrackCrossListFinder();
		CrossList crossList = stclf.findCosmicsCandidateCrossLists(crosses, SVTGeom, BMTGeom, 3);
		if(crossList ==null || crossList.size() == 0) {
			this.CleanupSpuriousCrosses(crosses, null) ;
			rbc.appendCVTCosmicsBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, null, shift);
			return true;
		} 
		List<StraightTrack> tracks = tclf.getStraightTracks(crossList, crosses.get(1), SVTGeom, BMTGeom);


		if(tracks ==null) {
			this.CleanupSpuriousCrosses(crosses, null) ;
			rbc.appendCVTCosmicsBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, null, shift);
			return true;
		}   



		if (tracks.size() == 0) {
			this.CleanupSpuriousCrosses(crosses, null) ;
			rbc.appendCVTCosmicsBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, null, shift);
			return true;
		}
		for( int i=0;i<tracks.size();i++) { 
			tracks.get(i).set_Id(i+1);
		}
		removeOverlappingTracks(tracks); //turn off until debugged


		//      FIXME: workaround to properly assign the position and direction to the BMT crosses. to be understood where it comes from the not correct one  
		/*for( StraightTrack t : tracks ) {
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
		}*/


		for (int c = 0; c < tracks.size(); c++) {
			tracks.get(c).set_Id(c + 1);
			for (int ci = 0; ci < tracks.get(c).size(); ci++) {

				if (crosses.get(0) != null && crosses.get(0).size() > 0) {
					//                    for (Cross crsSVT : crosses.get(0)) {
					for (int jj=0 ; jj < crosses.get(0).size(); jj++) {
						Cross crsSVT = crosses.get(0).get(jj);
						if (crsSVT.get_Sector() == tracks.get(c).get(ci).get_Sector() && crsSVT.get_Cluster1()!=null && crsSVT.get_Cluster2()!=null 
								&& tracks.get(c).get(ci).get_Cluster1()!=null && tracks.get(c).get(ci).get_Cluster2()!=null
								&& crsSVT.get_Cluster1().get_Id() == tracks.get(c).get(ci).get_Cluster1().get_Id()
								&& crsSVT.get_Cluster2().get_Id() == tracks.get(c).get(ci).get_Cluster2().get_Id()) {  
							crsSVT.set_Point(tracks.get(c).get(ci).get_Point());
							tracks.get(c).get(ci).set_Id(crsSVT.get_Id());
							crsSVT.set_PointErr(tracks.get(c).get(ci).get_PointErr());
							crsSVT.set_Dir(tracks.get(c).get(ci).get_Dir());
							crsSVT.set_DirErr(tracks.get(c).get(ci).get_DirErr());
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
						if (crsBMT.get_Id() == tracks.get(c).get(ci).get_Id()) {
							crsBMT.set_Point(tracks.get(c).get(ci).get_Point());
							crsBMT.set_PointErr(tracks.get(c).get(ci).get_PointErr());
							crsBMT.set_Dir(tracks.get(c).get(ci).get_Dir());
							crsBMT.set_DirErr(tracks.get(c).get(ci).get_DirErr());
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
		if (tracks.size() > 0) {
			this.CleanupSpuriousCrosses(crosses, tracks) ;
			rbc.appendCVTCosmicsBanks(event, SVThits, BMThits, SVTclusters, BMTclusters, crosses, tracks, shift);
		}
		//System.out.println("H");

		//event.show();
		return true;

	}
	private void CleanupSpuriousCrosses(List<ArrayList<Cross>> crosses, List<StraightTrack> trks) {
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
			List<StraightTrack> rmTrks = new ArrayList<StraightTrack>();
			for(StraightTrack t:trks) {
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

		System.out.println("SVT LOADING WITH VARIATION "+variationName);
		DatabaseConstantProvider cp = new DatabaseConstantProvider(11, variationName);
		cp = SVTConstants.connect( cp );
		cp.disconnect();  
		SVTStripFactory svtFac = new SVTStripFactory(cp, true);
		SVTGeom.setSvtStripFactory(svtFac);

		return true;
	}

	private String variationName;

	private void removeOverlappingTracks(List<StraightTrack> trkcands) {
		if(trkcands==null)
			return;

		List<StraightTrack> selectedTracks =new ArrayList<StraightTrack>();
		List<StraightTrack> list = new  ArrayList<StraightTrack>();
		List<StraightTrack> rejected = new  ArrayList<StraightTrack>();
		for(int i =0; i<trkcands.size(); i++) { 

			list.clear();
			if(trkcands.get(i)==null) {
				continue;
			}
			if( rejected.contains( trkcands.get(i) ) ) continue;

			this.getOverlapLists(trkcands.get(i), trkcands, list);

			StraightTrack selectedTrk = this.FindBestTrack(list);
			if(selectedTrk==null)
				continue;
			if(selectedTracks.contains(selectedTrk)==false)
				selectedTracks.add(selectedTrk);

			list.remove(selectedTrk);

			for(StraightTrack t : list ) {
				if( ! rejected.contains(t) ) rejected.add(t);
			}

		}
		if( rejected != null )
			selectedTracks.removeAll(rejected);
		if(trkcands!=null)
			trkcands.removeAll(trkcands);
		if(selectedTracks!=null)
			trkcands.addAll(selectedTracks);
	}


	private void getOverlapLists(StraightTrack track, List<StraightTrack> trkcands, List<StraightTrack> list) {
		// --------------------------------------------------------------------
		//  two tracks are considered the same if they share at least 2 crosses
		// --------------------------------------------------------------------

		for( StraightTrack t : trkcands ) {
			int N = 0;
			for( Cross c : t ) {

				// do not check on BMTC
				if( c.get_DetectorType().equalsIgnoreCase("C") == true ) continue;

				if( track.contains(c) ) { N++;  }
			}
			if( N >= 2 ) list.add( t );
		}

	}

	private StraightTrack FindBestTrack(List<StraightTrack> trkList) {
		// --------------------------------------------------------------------
		//  Select the candidate with the highest number of NDF
		//    if two have the same ndf, get the one with the better chi2/ndf
		// --------------------------------------------------------------------
		double bestChi2 = 9999999;
		int ndf = 0;
		StraightTrack bestTrk = null;
		for(int i =0; i<trkList.size(); i++) {

			if( Double.isNaN(trkList.get(i).get_chi2()) == false && 
					trkList.get(i).get_chi2() < 1e4 &&
					trkList.get(i).get_ndf()>=ndf) {

				ndf = (int) trkList.get(i).get_ndf();
				if( trkList.get(i).get_ndf()==ndf ) {
					if(trkList.get(i).get_chi2()/(double)trkList.get(i).get_ndf()<bestChi2) {
						bestChi2 = trkList.get(i).get_chi2()/(double)trkList.get(i).get_ndf();
						bestTrk = trkList.get(i);
					}
				}
				else {
					bestChi2 = trkList.get(i).get_chi2()/(double)trkList.get(i).get_ndf();
					bestTrk = trkList.get(i);
				}
			}
		}
		return bestTrk;
	}
}
