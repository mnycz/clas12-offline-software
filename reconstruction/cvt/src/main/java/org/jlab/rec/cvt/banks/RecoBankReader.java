package org.jlab.rec.cvt.banks;

import java.util.ArrayList;
import java.util.List;

import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.hit.ADCConvertor;
import org.jlab.rec.cvt.hit.FittedHit;
import org.jlab.rec.cvt.hit.Hit;
import org.jlab.rec.cvt.hit.Strip;
import org.jlab.rec.cvt.svt.Constants;
import org.jlab.rec.cvt.svt.Geometry;
import org.jlab.rec.cvt.track.StraightTrack;
import org.jlab.rec.cvt.trajectory.Ray;

public class RecoBankReader {
	private List<StraightTrack> _cosmics;
	private List<Cross> _crosses;
	private List<Cluster> _clusters;
	private List<FittedHit> _SVTHits;



	/**
	 *
	 * @param event the event
	 * @param trkcands the list of reconstructed straight tracks
	 * @return cosmic bank
	 */
	public DataBank fillStraightTracksBank(DataEvent event,
			List<StraightTrack> cosmics, double zShift) {
		if (cosmics == null) {
			return null;
		}
		if (cosmics.size() == 0) {
			return null;
		}

		DataBank bank = event.createBank("CVTRec::Cosmics", cosmics.size());
		// an array representing the ids of the crosses that belong to the track: for a helical track with the current
		// 4 regions of SVT + 1 region of BMT there can be up to 4*2 (*2: for each hemisphere) crosses of type SVT and 2*2 of type PSEUDOBMT (1 for the C detector and 1 for the Z detector)

		for (int i = 0; i < cosmics.size(); i++) {
			List<Integer> crossIdxArray = new ArrayList<Integer>();


			bank.setShort("ID", i, (short) cosmics.get(i).get_Id());
			bank.setFloat("chi2", i, (float) cosmics.get(i).get_chi2());
			bank.setShort("ndf", i, (short) cosmics.get(i).get_ndf());
			bank.setFloat("trkline_yx_slope", i, (float) cosmics.get(i).get_ray().get_yxslope());
			bank.setFloat("trkline_yx_interc", i, (float) (cosmics.get(i).get_ray().get_yxinterc()/10.));
			bank.setFloat("trkline_yz_slope", i, (float) cosmics.get(i).get_ray().get_yzslope());
			bank.setFloat("trkline_yz_interc", i, (float) (cosmics.get(i).get_ray().get_yzinterc()/10.+zShift));

			// get the cosmics ray unit direction vector
			Vector3D u = new Vector3D(cosmics.get(i).get_ray().get_yxslope(), 1, cosmics.get(i).get_ray().get_yzslope()).asUnit();
			// calculate the theta and phi components of the ray direction vector in degrees
			bank.setFloat("theta", i, (float) Math.toDegrees(u.theta()));
			bank.setFloat("phi", i, (float) Math.toDegrees(u.phi()));

			// the array of cross ids is filled in order of the SVT cosmic region 1 to 8 starting from the bottom-most double layer
			for (int j = 0; j < cosmics.get(i).size(); j++) {
				crossIdxArray.add(cosmics.get(i).get(j).get_Id());

			}


			for (int j = 0; j < crossIdxArray.size(); j++) { 
				if(j<18) {
					//only 18 entries in bank
					String hitStrg = "Cross";
					hitStrg += (j + 1);
					hitStrg += "_ID";
					bank.setShort(hitStrg, i, (short) crossIdxArray.get(j).shortValue());
				}
			}
		}
		//bank.show();
		return bank;
	}


	public void fetch_SVTCrosses(DataEvent event, double zShift) {
		if(_clusters == null)
			fetch_SVTClusters(event);

		if (event.hasBank("BSTRec::Crosses") == false) {
			//System.err.println("there is no BST bank ");
			_crosses = new ArrayList<Cross>();

			return;
		}
		DataBank bank = event.getBank("BSTRec::Crosses");

		int index = 0;
		for (int j = 0; j < bank.rows(); j++) {
			int region = bank.getByte("region", j);
			int sector = bank.getByte("sector", j);
			int id = bank.getShort("ID",j);
			Cross cross = new Cross("SVT", "", sector, region, j);
			cross.set_Point(new Point3D(10.*bank.getFloat("x", j), 10.*bank.getFloat("y", j),10.*(bank.getFloat("z", j)-zShift)));
			cross.set_PointErr(new Point3D(10.*bank.getFloat("err_x", j), 10.*bank.getFloat("err_y", j),10.*bank.getFloat("err_z", j)));
			cross.set_AssociatedTrackID(bank.getShort("trkID",j));
			cross.set_Dir(new Vector3D(bank.getFloat("ux", j),bank.getFloat("uy", j),bank.getFloat("uz", j)));

			int cluster1id = bank.getShort("Cluster1_ID", j);
			for (Cluster cluster: _clusters)
				if (cluster.get_Id() == cluster1id)
					cross.set_Cluster2(cluster);
			int cluster2id = bank.getShort("Cluster2_ID", j);
			for (Cluster cluster: _clusters)
				if (cluster.get_Id() == cluster2id)
					cross.set_Cluster2(cluster);

			index++;
			_crosses.add(cross);
		}



	}

	public void fetch_Cosmics(DataEvent event, org.jlab.rec.cvt.svt.Geometry geo, double zShift) {

		if(_crosses == null)
			fetch_SVTCrosses(event, zShift);
		if (event.hasBank("CVTRec::Cosmics") == false) {
			//System.err.println("there is no BST bank ");
			_cosmics = new ArrayList<StraightTrack>();

			return;
		}

		List<Hit> hits = new ArrayList<Hit>();

		DataBank bank = event.getBank("CVTRec::Cosmics");

		int rows = bank.rows();;

		short ids[] = bank.getShort("ID");
		float chi2s[] = bank.getFloat("chi2");
		short ndfs[] = bank.getShort("ndf");
		float yx_slopes[] = bank.getFloat("trkline_yx_slope");
		float yx_intercs[] = bank.getFloat("trkline_yx_interc");
		float yz_slopes[] = bank.getFloat("trkline_yz_slope");
		float yz_intercs[] = bank.getFloat("trkline_yz_interc");


		_cosmics = new ArrayList<StraightTrack>();


		for(int i = 0; i<rows; i++) {
			// get the cosmics ray unit direction vector
			Vector3D u = new Vector3D(yx_slopes[i], 1, yz_slopes[i]).asUnit();
			Point3D point = new Point3D(10.*yx_intercs[i], 0, 10.*(yz_intercs[i]-zShift));
			Ray ray = new Ray(point, u);
			StraightTrack track = new StraightTrack(ray);
			track.set_Id(ids[i]);
			track.set_chi2(chi2s[i]);
			track.set_ndf(ndfs[i]);

			List<Integer> crossIdxArray = new ArrayList<Integer>();

			for (int j = 0; j < crossIdxArray.size(); j++) { 
				if(j<18) {
					//only 18 entries in bank
					String hitStrg = "Cross";
					hitStrg += (j + 1);
					hitStrg += "_ID";
					if(!hasColumn(bank,hitStrg))
						continue;
					int crossid = bank.getShort(hitStrg, i);
					for(Cross cross : _crosses)
						if(cross.get_Id() == crossid)
							track.add(cross);
				}
			}
			_cosmics.add(track);

		}
	}

	private boolean hasColumn(DataBank bank, String name) {
		for(String n : bank.getColumnList()) {
			if (name.equalsIgnoreCase(n))
				return true;
		}
		return false;
	}

	public void fetch_SVTClusters(DataEvent event) {
		_clusters = new ArrayList<Cluster>();
		if(_SVTHits == null)
			this.fetch_SVTHits(event);
		DataBank bank = event.getBank("BSTRec::Clusters");

		for (int i = 0; i < bank.rows(); i++) {


			int id = bank.getByte("ID", i);
			int layer = bank.getByte("layer", i);
			int sector = bank.getByte("sector", i);
			Cluster cluster = new Cluster(0, 0, sector, layer, id);

			int size = bank.getInt("size", i);
			cluster.set_TotalEnergy(bank.getFloat("ETot", i));
			cluster.set_SeedStrip(bank.getInt("seedStrip", i));
			cluster.set_Centroid(bank.getFloat("centroid",i));
			cluster.set_SeedEnergy(bank.getFloat("seedE",i));
			cluster.set_SeedEnergy(bank.getFloat("seedE",i));
			cluster.set_CentroidResidual(bank.getFloat("centroidResidual",i));
			cluster.set_SeedResidual(bank.getFloat("seedResidual",i));
			cluster.set_AssociatedTrackID(bank.getShort("trkID",i));

			//Since only up to 5 hits per track are written...
			for (int j = 0; j < 5; j++) {
				String hitStrg = "Hit";
				hitStrg += (j + 1);
				hitStrg += "_ID";
				if(!hasColumn(bank,hitStrg))
					continue;
				int hitId = bank.getShort(hitStrg, i);
				for(FittedHit hit : _SVTHits) {
					if (hit.get_Id() == hitId) {
						cluster.add(hit);
					}
				}
			}
			_clusters.add(cluster);

		}

	}


	private void fetch_SVTHits(DataEvent event) {
		DataBank bank = event.getBank("BSTRec::Hits");

		_SVTHits = new ArrayList<FittedHit>();
		for (int i = 0; i < bank.rows(); i++) {
			int layer = bank.getByte("layer", i);
			int sector = bank.getByte("sector", i);
			int strip = bank.getInt("strip", i);
			int id = bank.getShort("ID", i);
			FittedHit hit = new FittedHit(0, 0, sector, layer, new Strip(strip, 0));
			
			hit.set_Id(id);
			
			

			hit.set_docaToTrk(bank.getFloat("fitResidual", i));
			hit.set_TrkgStatus(bank.getInt("trkingStat", i));

			hit.set_AssociatedClusterID(bank.getShort("clusterID", i));
			hit.set_AssociatedTrackID(bank.getShort("trkID", i));
			_SVTHits.add(hit);
		}
		//bank.show();
	}


	public List<StraightTrack> get_Cosmics() {
		
		return _cosmics;
	}



}	
