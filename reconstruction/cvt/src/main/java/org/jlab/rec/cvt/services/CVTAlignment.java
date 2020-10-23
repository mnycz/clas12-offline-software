package org.jlab.rec.cvt.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jlab.clas.reco.ReconstructionEngine;
//import org.jlab.clas.swimtools.Swim;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.geant4.v2.CTOFGeant4Factory;
import org.jlab.detector.geant4.v2.SVT.SVTAlignmentFactory;
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
import org.jlab.rec.cvt.align.AlignmentMatrixIndices;

import org.jlab.rec.cvt.banks.AlignmentBankWriter;
import org.jlab.rec.cvt.banks.RecoBankReader;
import org.jlab.rec.cvt.bmt.CCDBConstantsLoader;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.track.StraightTrack;
import org.jlab.rec.cvt.track.Track;
import org.jlab.rec.cvt.trajectory.Helix;
import org.jlab.rec.cvt.trajectory.Ray;
import org.jlab.rec.cvt.trajectory.Trajectory;

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
	private Boolean svtTopBottomSep;
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

	boolean isCosmics = false;

	@Override
	public boolean processDataEvent(DataEvent event) {
		int runNum = event.getBank("RUN::config").getInt("run", 0);
		int eventNum = event.getBank("RUN::config").getInt("event", 0);
		this.setRunConditionsParameters(event, FieldsConfig, Run, false, "");

		double shift = org.jlab.rec.cvt.Constants.getZoffset();;

		this.FieldsConfig = this.getFieldsConfig();


		RecoBankReader reader = new RecoBankReader();

		//reader.fetch_Cosmics(event, SVTGeom, 0);

		List<? extends Trajectory> tracks;
		if(isCosmics) {
			reader.fetch_Cosmics(event, SVTGeom, shift);
			tracks = reader.get_Cosmics();
		} else {
			reader.fetch_Tracks(event, SVTGeom, shift);
			tracks = reader.get_Tracks();
		}

		//System.out.println("H");
		List<Matrix> Is = new ArrayList<Matrix>();
		List<Matrix> As = new ArrayList<Matrix>();
		List<Matrix> Bs = new ArrayList<Matrix>();
		List<Matrix> Vs = new ArrayList<Matrix>();
		List<Matrix> ms = new ArrayList<Matrix>();
		List<Matrix> cs = new ArrayList<Matrix>();
		List<Integer> trackIDs = new ArrayList<Integer>();

		for (Trajectory track : tracks) {
			if(Math.abs(getDoca(track))>maxDocaCut)
				continue;
			/*System.out.println("track read: ");
			System.out.println("track chi2: "+ track.get_chi2());
			System.out.println("ndf: "+ track.get_ndf());
			System.out.println("ncrosses: "+ track.size());
			System.out.println("ray: "+ track.get_ray().get_refPoint() + 
					" + lambda*" + track.get_ray().get_dirVec());
			System.out.println();*/
			int nCross = 0;
			for(Cross c : track) {
				if(c.get_Detector().equalsIgnoreCase("SVT"))
					nCross++;
			}
			if(nCross <= 2)
				continue;
			Ray ray = track.get_ray();
			if(ray == null) {
				ray = getRay(track.get_helix());
				//System.out.println("curvature " +  track.get_helix().get_curvature());
				//System.out.println("doca " +  track.get_helix().get_dca());
				if(Math.abs(track.get_helix().get_curvature())>0.0001) {
					continue;
				}
			}
			//getRay(track);
			//System.out.println(ray.get_dirVec().toString());
			//System.out.println(ray.get_refPoint().toString());
			Matrix A = new Matrix(2*nCross, svtTopBottomSep ? 2*nAlignVars*nCross : nAlignVars*nCross);//not sure why there aren't 6 columns
			Matrix B = new Matrix(2*nCross, 4);
			Matrix V = new Matrix(2*nCross,2*nCross);
			Matrix m = new Matrix(2*nCross,1);
			Matrix c = new Matrix(2*nCross,1);
			Matrix I = new Matrix(2*nCross,1);

			int i = 0;
			for(Cross cross : track) {
				//System.out.println("cross " +cross.get_Point());
				if(!cross.get_Detector().equalsIgnoreCase("SVT") && isSVTonly)
					continue;
				Cluster cl1 = cross.get_Cluster1();
				fillMatrices(i,ray,cl1,A,B,V,m,c,I);
				i++;
				Cluster cl2 = cross.get_Cluster2();
				fillMatrices(i,ray,cl2,A,B,V,m,c,I);
				i++;
				
			}

			/*System.out.println("dm: ");
			dm.print(6, 2);
			System.out.println("V:  ");
			V.print(7, 4);
			System.out.println("B:  ");
			B.print(7, 4);
			System.out.println("A:  ");
			A.print(7, 4);
			System.out.println("track chi2: " + dm.transpose().times(V.inverse()).times(dm).get(0, 0));
			System.out.println();*/
			As.add(A);
			Bs.add(B);
			Vs.add(V);
			ms.add(m);
			cs.add(c);
			Is.add(I);
			

			c.print(7, 4);
			m.print(7, 4);

			trackIDs.add(track.get_Id());
		}
		AlignmentBankWriter writer = new AlignmentBankWriter();
		writer.write_Matrix(event, "I", Is);
		writer.write_Matrix(event, "A", As);
		writer.write_Matrix(event, "B", Bs);
		writer.write_Matrix(event, "V", Vs);
		writer.write_Matrix(event, "m", ms);
		writer.write_Matrix(event, "c", cs);
		fillMisc(event,runNum,eventNum,trackIDs,As,Bs,Vs,ms,cs,Is);

		//event.show();
		return true;

	}

	private Ray getRay(Helix h) {

		double d = h.get_dca();
		double z = h.get_Z0();
		double phi = h.get_phi_at_dca();
		double td = h.get_tandip();
		double cd = 1/Math.hypot(td, 1);
		double sd = td*cd;
		//Vector3D u = new Vector3D(-cd*Math.sin(phi), cd*Math.cos(phi), sd);
		//Point3D x = new Point3D(d*Math.cos(phi),d*Math.sin(phi), z);
		Vector3D u = new Vector3D(cd*Math.cos(phi), cd*Math.sin(phi), sd);
		Point3D x = new Point3D(-d*Math.sin(phi),d*Math.cos(phi), z);
		//if(u.y() <0)
		//	u = u.multiply(-1);
		//x = x.toVector3D().add(u.multiply(-x.y()/u.y())).toPoint3D();
		Ray ray = new Ray(x, u);
		//System.out.println("doca " + d);
		//System.out.println("td " + td);

		return ray;
	}



	private double getDoca(Trajectory track) {
		if(track instanceof StraightTrack) {
			// TODO Auto-generated method stub
			Ray ray = track.get_ray();
			double intercept = ray.get_yxinterc();
			double slope = ray.get_yxslope();
			return Math.abs(intercept)/Math.hypot(1, slope);
		} else return track.get_helix().get_dca();
	}

	private void fillMisc(DataEvent event, int runNum, int eventNum, List<Integer> trackIDs, 
			List<Matrix> As, List<Matrix> Bs, List<Matrix> Vs, List<Matrix> ms, List<Matrix> cs,
			List<Matrix> is) {
		DataBank bank = event.createBank("Align::misc", trackIDs.size());
		for(int i = 0; i<trackIDs.size(); i++) {
			bank.setInt("run", i, runNum);
			bank.setInt("event", i, eventNum);
			Matrix c = cs.get(i), m = ms.get(i), V = Vs.get(i);
			bank.setFloat("chi2", i, (float)(m.minus(c)).transpose().times(V.inverse()).times(m.minus(c)).get(0, 0));
			
			bank.setShort("ndof", i, (short)(Vs.get(i).getRowDimension()-4));
			bank.setShort("track", i, (short)(int)trackIDs.get(i));
			bank.setShort("nalignables", i, (short)(this.svtTopBottomSep ? 2*42 : 42));
			bank.setShort("nparameters", i, (short)this.nAlignVars);
		}

		event.appendBank(bank);
	}

	/*
	 * converts a Vector3D to a Vector3d.  
	 * These objects are from two different packages.
	 */
	private Vector3d convertVector(Vector3D v) {
		return new Vector3d(v.x(),v.y(),v.z());
	}

	private void fillMatrices(int i, Ray ray, Cluster cl, Matrix A, Matrix B, Matrix V, Matrix m, Matrix c, Matrix I) {
		int region = cl.get_Region();
		int layer = cl.get_Layer();
		int sector = cl.get_Sector();
		//System.out.println("RLS " + region + " " + layer + " " + sector);
		//System.out.println("th" + c.get_Phi());
		double centroid = cl.get_Centroid();
		
		// this avoids a certain bug that only occurs if
		// there is a single-hit cluster on the last strip,
		// in which obtaining the next strip (line2) gives 
		// an IllegalArgumentException
		
		
		if(centroid == SVTConstants.NSTRIPS)
			centroid = SVTConstants.NSTRIPS-.001;
		Line3d line1 = SVTGeom.getStrip(layer-1, sector-1, (int)Math.floor(centroid)-1);
		Line3d line2 = SVTGeom.getStrip(layer-1, sector-1, (int)Math.floor(centroid)-0); 
		
		
		
		
		//take the weighted average of the directions of the two lines.
		Vector3d l = line1.diff().normalized().times(1-(centroid%1)).add(line2.diff().normalized().times((centroid%1))).normalized();

		Vector3d e1 = line1.origin();
		Vector3d e2 = line2.origin();
		Vector3d e = e1.times(1-(centroid%1)).add(e2.times((centroid%1)));
		Vector3d s = e2.minus(e1);
		Vector3d xref = convertVector(ray.get_refPoint().toVector3D());
		Vector3d u = convertVector(ray.get_dirVec()); 
		s = s.minus(l.times(s.dot(l))).normalized();
		Vector3d n = l.cross(s);
		double udotn = u.dot(n);
		double sdotu = s.dot(u);
		Vector3d extrap = xref.plus(u.times(n.dot(e.minus(xref))/udotn));
		//System.out.println(extrap.toStlString());
		double resolution = cl.get_ResolutionAlongZ(extrap.z, SVTGeom);
		/*System.out.println("sector: " + sector + " of " + SVTConstants.NSECTORS[region-1]);
		System.out.println("xref: " + xref.toStlString());
		System.out.println("e: " + e.toStlString());

		System.out.println("u: " + u.magnitude() + " " + u.toStlString());
		System.out.println("l: " + l.magnitude() + " " + l.toStlString());
		System.out.println("n: " + n.magnitude() + " " + n.toStlString());
		System.out.println("s: " + s.magnitude() + " " + s.toStlString());
		System.out.println("extrap: " + extrap.toStlString());
		System.out.println("resolution: " + resolution);


		//extrap should be on ths same plane as e., 
		//basis vectors must be perp to one another.
		System.out.println("these should be zero:  " + extrap.minus(e).dot(n)+" " + n.dot(s) 
				+ " " + s.dot(l) + " "+ l.dot(n) + " " + extrap.minus(xref).cross(u).magnitude());
		 */
		V.set(i, i, Math.pow(resolution,2));


		Vector3d sp = s.minus(n.times(sdotu/udotn));

		int index = getIndexSVT(region-1, sector-1);
		if(svtTopBottomSep && (layer-1)%2==1) {
			index += 42;
		}
		
		//Use the same reference point for both inner and outer layer of region
		Vector3d cref = getModuleReferencePoint(sector,layer);
		
		
		
		//for debugging
		/*
		double phi1 = Math.atan2(n.y, n.x), phi2 = Math.atan2(cref.y, cref.x);
		double dphi = phi1-phi2;
		while (dphi < -Math.PI)
			dphi += 2*Math.PI;
		while (dphi > Math.PI)
			dphi -= 2*Math.PI;
		System.out.println(layer + " "+phi1 + " " + phi2 + " " + dphi);
		*/
		
		Vector3d dmdr =sp.cross(extrap).plus(n.cross(cref).times(sdotu/udotn));
		dmdr = dmdr.minus(n.cross(u).times(n.dot(e.minus(extrap))*sdotu/(udotn*udotn)));
		/*A.set(i, (svtTopBottomSep? i : i/2)*6 + 0, -sp.x);
		A.set(i, (svtTopBottomSep? i : i/2)*6 + 1, -sp.y);
		A.set(i, (svtTopBottomSep? i : i/2)*6 + 2, -sp.z);
		A.set(i, (svtTopBottomSep? i : i/2)*6 + 3, dmdr.x);
		A.set(i, (svtTopBottomSep? i : i/2)*6 + 4, dmdr.y);
		A.set(i, (svtTopBottomSep? i : i/2)*6 + 5, dmdr.z);*/
		if(orderTx >= 0)
			A.set(i, (svtTopBottomSep? i : i/2)*nAlignVars + orderTx, -sp.x);
		if(orderTy >= 0)
			A.set(i, (svtTopBottomSep? i : i/2)*nAlignVars + orderTy, -sp.y);
		if(orderTz >= 0)
			A.set(i, (svtTopBottomSep? i : i/2)*nAlignVars + orderTz, -sp.z);
		if(orderRx >= 0)
			A.set(i, (svtTopBottomSep? i : i/2)*nAlignVars + orderRx, dmdr.x);
		if(orderRy >= 0)
			A.set(i, (svtTopBottomSep? i : i/2)*nAlignVars + orderRy, dmdr.y);
		if(orderRz >= 0)
			A.set(i, (svtTopBottomSep? i : i/2)*nAlignVars + orderRz, dmdr.z);
		
		

		I.set(i, 0, index);

		Vector3d dmdu = sp.times(e.minus(xref).dot(n)/udotn);
		B.set(i,0, sp.x);
		B.set(i,1, sp.z);
		B.set(i,2, dmdu.x);
		B.set(i,3, dmdu.z);
		//dm.set(i,0, s.dot(e.minus(extrap)));
		c.set(i,0,s.dot(extrap));
		m.set(i,0,s.dot(e));
		
	}



	int getIndexSVT(int region, int sect){
		if (region == 0)
			return sect;
		if (region == 1)
			return org.jlab.rec.cvt.svt.Constants.NSECT[0] + sect;
		if (region == 2)
			return org.jlab.rec.cvt.svt.Constants.NSECT[0] +
					org.jlab.rec.cvt.svt.Constants.NSECT[2] + sect;
		return -1;
	}

	private Vector3d getModuleReferencePoint(int sector, int layer) {
		return SVTAlignmentFactory.getIdealFiducialCenter((layer-1)/2, sector-1);
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
			System.out.println("["+this.getName()+"] align SVT only "+svtStAl+" config chosen based on yaml");
			this.isSVTonly= Boolean.valueOf(svtStAl);
		}
		else {
			svtStAl = System.getenv("COAT_ALIGN_SVT_ONLY");
			if (svtStAl!=null) {
				System.out.println("["+this.getName()+"] align SVT only "+svtStAl+" config chosen based on env");
				this.isSVTonly= Boolean.valueOf(svtStAl);
			}
		}
		if (svtStAl==null) {
			System.out.println("["+this.getName()+"] align SVT only (default) ");
			this.isSVTonly = true;
		}
		// Load other geometries

		variationName = Optional.ofNullable(this.getEngineConfigString("variation")).orElse("default");
		System.out.println(" CVT YAML VARIATION NAME + "+variationName);
		
		System.out.println("SVT LOADING WITH VARIATION "+variationName);
		DatabaseConstantProvider cp = new DatabaseConstantProvider(11, variationName);
		cp = SVTConstants.connect( cp );
		cp.disconnect();  
		SVTStripFactory svtFac = new SVTStripFactory(cp, false);
		SVTGeom.setSvtStripFactory(svtFac);

		String svtTopBottomSep = this.getEngineConfigString("svtAlignTopBottomSeparately");
		if (svtTopBottomSep!=null) {
			System.out.println("["+this.getName()+"] run with SVT alignment for top and bottom as separate modules "+svtTopBottomSep+" config chosen based on yaml");
			this.svtTopBottomSep= Boolean.valueOf(svtTopBottomSep);
		}
		else {
			svtTopBottomSep = System.getenv("COAT_SVT_TOP_BOTTOM");
			if (svtTopBottomSep!=null) {
				System.out.println("["+this.getName()+"] run with SVT alignment for top and bottom as separate modules "+svtTopBottomSep+" config chosen based on env");
				this.svtTopBottomSep= Boolean.valueOf(svtTopBottomSep);
			}
		}
		if (svtTopBottomSep==null) {
			System.out.println("["+this.getName()+"] run with SVT top and bottom as a single module (default) ");
			this.svtTopBottomSep = false;
		}
		
		String alignVars = this.getEngineConfigString("alignVariables");
		if (alignVars!=null) {
			System.out.println("["+this.getName()+"] obtain alignment derivatives for the following variables "+svtTopBottomSep+" config chosen based on yaml");
			this.setAlignVars(alignVars);
		}
		else {
			alignVars = System.getenv("COAT_ALIGN_VARS");
			if (alignVars!=null) {
				System.out.println("["+this.getName()+"] obtain alignment derivatives for the following variables "+svtTopBottomSep+" config chosen based on env");
				this.setAlignVars(alignVars);
			}
		}
		if (alignVars==null) {
			System.out.println("["+this.getName()+"] obtain alignment derivatives for all 6 variables (default) ");
			this.setAlignVars("Tx Ty Tz Rx Ry Rz");
		}
		
		
		String maxDocaCut = this.getEngineConfigString("maxDocaCut");
		
		if(maxDocaCut != null) {
			System.out.println("["+this.getName()+"] max doca cut "+ maxDocaCut + " mm");
			this.maxDocaCut = Double.parseDouble(maxDocaCut);
		}
		else {
			System.out.println("["+this.getName()+"] no max doca cut set (default)");
			this.maxDocaCut = Double.MAX_VALUE;
		}
		
		String cosmics = this.getEngineConfigString("cosmics");

		if(cosmics != null) {
			System.out.println("["+this.getName()+"] use cosmics bank instead of tracks bank? "+ cosmics );
			this.isCosmics = Boolean.parseBoolean(cosmics);
		}
		else {
			System.out.println("["+this.getName()+"] using tracks bank (default)");
			this.isCosmics = false;
		}
		return true;
	}

	double maxDocaCut;
	


	private void setAlignVars(String alignVars) {
		String split[] = alignVars.split("[ \t]+");
		int i = 0;
		orderTx = -1;
		orderTy = -1;
		orderTz = -1;
		orderRx = -1;
		orderRy = -1;
		orderRz = -1;
		for(String s : split) {
			if(s.equals("Tx")) {
				orderTx = i; i++;
			} else if(s.equals("Ty")) {
				orderTy = i; i++;
			} else if(s.equals("Tz")) {
				orderTz = i; i++;
			} else if(s.equals("Rx")) {
				orderRx = i; i++;
			} else if(s.equals("Ry")) {
				orderRy = i; i++;
			} else if(s.equals("Rz")) {
				orderRz = i; i++;
			}
		}
		nAlignVars = i;
		System.out.println(nAlignVars + " alignment variables requested");
	}
	private int nAlignVars;
	private int orderTx;
	private int orderTy;
	private int orderTz;
	private int orderRx;
	private int orderRy;
	private int orderRz; 

	private String variationName;


}
