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
import org.jlab.geometry.prim.Line3d;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.cvt.Constants;
import org.jlab.rec.cvt.align.AlignmentMatrixBuilder;
import org.jlab.rec.cvt.align.AlignmentMatrixIndices;

import static org.jlab.rec.cvt.Constants.setSolenoidscale;

import org.jlab.rec.cvt.banks.AlignmentBankWriter;
import org.jlab.rec.cvt.banks.HitReader;
import org.jlab.rec.cvt.banks.RecoBankReader;
import org.jlab.rec.cvt.bmt.CCDBConstantsLoader;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cluster.ClusterFinder;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.cross.CrossMaker;
import org.jlab.rec.cvt.hit.ADCConvertor;
import org.jlab.rec.cvt.hit.FittedHit;
import org.jlab.rec.cvt.hit.Hit;
import org.jlab.rec.cvt.svt.Geometry;
import org.jlab.rec.cvt.track.Seed;
import org.jlab.rec.cvt.track.StraightTrack;
import org.jlab.rec.cvt.track.Track;
import org.jlab.rec.cvt.track.TrackListFinder;
import org.jlab.rec.cvt.track.TrackSeeder;
import org.jlab.rec.cvt.track.TrackSeederCA;
import org.jlab.rec.cvt.track.fit.KFitter;
import org.jlab.rec.cvt.trajectory.Helix;
import org.jlab.rec.cvt.trajectory.Ray;
import org.jlab.rec.cvt.trajectory.Trajectory;
import org.jlab.rec.cvt.trajectory.TrajectoryFinder;

import Jama.Matrix;
import eu.mihosoft.vrl.v3d.Vector3d;

/**
 * Service to return reconstructed TRACKS
 * format
 *
 * @author ziegler
 *
 */
public class CVTAlignment extends ReconstructionEngine {

	org.jlab.rec.cvt.svt.Geometry SVTGeom;
	org.jlab.rec.cvt.bmt.Geometry BMTGeom;
	CTOFGeant4Factory CTOFGeom;
	Detector          CNDGeom ;
	SVTStripFactory svtIdealStripFactory;

	public CVTAlignment() {
		super("CVTAlignment", "spaul", "4.0");

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


		RecoBankReader reader = new RecoBankReader();

		reader.fetch_Cosmics(event, SVTGeom, 0);
		List<StraightTrack> straightTracks = reader.get_Cosmics();

		//System.out.println("H");
		List<Matrix> As = new ArrayList<Matrix>();
		List<Matrix> Bs = new ArrayList<Matrix>();
		List<Matrix> Vs = new ArrayList<Matrix>();
		List<Matrix> dms = new ArrayList<Matrix>();
		for (StraightTrack track : straightTracks) {
			int nCross = 0;
			for(Cross c : track) {
				if(c.get_Detector().equalsIgnoreCase("SVT"))
					nCross++;
			}
			Ray ray = track.get_ray();
			Matrix A = new Matrix(2*nCross, AlignmentMatrixIndices.getParameterCount());
			Matrix B = new Matrix(2*nCross, 4);
			Matrix V = new Matrix(2*nCross,2*nCross);
			Matrix dm = new Matrix(2*nCross,1);
			int i = 0;
			for(Cross cross : track) {
				if(!cross.get_Detector().equalsIgnoreCase("SVT"))
					continue;
				Cluster c1 = cross.get_Cluster1();
				fillMatrices(i,ray,c1,A,B,V,dm);
				i++;
				Cluster c2 = cross.get_Cluster2();
				fillMatrices(i,ray,c2,A,B,V,dm);
				i++;
			}
			As.add(A);
			Bs.add(B);
			Vs.add(V);
			dms.add(dm);
		}
		AlignmentBankWriter writer = new AlignmentBankWriter();
		writer.write_Matrix(event, "A", As);
		writer.write_Matrix(event, "B", Bs);
		writer.write_Matrix(event, "V", Vs);
		writer.write_Matrix(event, "dm", dms);
		
		//event.show();
		return true;

	}
	/*
	 * converts a Vector3D to a Vector3d.  
	 * These objects are from two different packages.
	 */
	private Vector3d convertVector(Vector3D v) {
		return new Vector3d(v.x(),v.y(),v.z());
	}

	private void fillMatrices(int i, Ray ray, Cluster c, Matrix A, Matrix B, Matrix V, Matrix dm) {
		int region = c.get_Region();
		int layer = c.get_Layer();
		int sector = c.get_Sector();
		double centroid = c.get_Centroid();
		Line3d line1 = SVTGeom.getStrip(layer, sector, (int)centroid);
		Line3d line2 = SVTGeom.getStrip(layer, sector, (int)centroid+1); 
		//take the weighted average of the directions of the two lines.
		Vector3d l = line1.diff().normalized().times(centroid%1).add(line2.diff().normalized().times(1-centroid%1)).normalized();
		Vector3d e = line1.end().times(centroid%1).add(line2.end().times(1-centroid%1));
		Vector3d s = line2.end().minus(line2.end());
		Vector3d xref = convertVector(ray.get_refPoint().toVector3D());
		Vector3d u = convertVector(ray.get_dirVec()); 
		s = s.minus(l.times(s.dot(l))).normalized();
		Vector3d n = l.cross(s);
		double udotn = u.dot(n);
		double sdotu = s.dot(u);

		Vector3d extrap = xref.plus(u.times(n.dot(e.minus(xref))/udotn));

		V.set(i, i, Math.pow(c.get_ResolutionAlongZ(extrap.z, SVTGeom),2));


		Vector3d sp = s.minus(n.times(sdotu/udotn));

		int offset = AlignmentMatrixIndices.getIndexSVT(region, sector);
		Vector3d cref = convertVector(SVTGeom.getPlaneModuleOrigin(sector, layer).toVector3D());
		Vector3d dmdr =sp.cross(extrap).plus(n.cross(cref).times(sdotu/udotn));
		A.set(i, offset + 0, -sp.x);
		A.set(i, offset + 1, -sp.y);
		A.set(i, offset + 2, -sp.z);
		A.set(i, offset + 3, dmdr.x);
		A.set(i, offset + 4, dmdr.y);
		A.set(i, offset + 5, dmdr.z);

		Vector3d dmdu = sp.times(e.minus(xref).dot(n)/udotn);
		B.set(i,0, sp.x);
		B.set(i,1, sp.z);
		B.set(i,2, dmdu.x);
		B.set(i,3, dmdu.z);
		dm.set(i,0, s.dot(e.minus(extrap)));
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
		SVTStripFactory svtFac = new SVTStripFactory(cp, false);
		SVTGeom.setSvtStripFactory(svtFac);



		return true;
	}


	private String variationName;


}
