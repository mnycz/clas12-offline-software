package cnuphys.ced.event.data;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

import cnuphys.ced.clasio.ClasIoEventManager;

/**
 * This is for caching the relevant data from the REC::Calorimeter bank
 * @author davidheddle
 *
 */
public class RECCalorimeter extends DetectorData {
	
	public static final int NOPID = -999999;
	
	//the singleton
	private static RECCalorimeter _instance;
	
	//the number of rows in the current 
	public int count;
	
	public byte sector[];
	
	public byte layer[];
	
	public float energy[];
	
	public float x[];
	
	public float y[];
	
	public float z[];
	
	public int pid[];
		
	private short pindex[];
	
	//private constructor for singleton
	private RECCalorimeter() {
		super();
	}
	
	/**
	 * public access to the singleton
	 * @return the singleton
	 */
	public static RECCalorimeter getInstance() {
		if (_instance == null) {
			_instance = new RECCalorimeter();
		}
		return _instance;
	}
	
	@Override
	public void newClasIoEvent(DataEvent event) {
		update(event);
	}

	private void nullify() {
		count = 0;
		sector = null;
		layer = null;
		energy = null;
		x = null;
		y = null;
		z = null;
		pid = null;		
	}

	private void update(DataEvent event) {
		
		if (event == null) {
			nullify();
			return;
		}
		
		DataBank bank = event.getBank("REC::Calorimeter");
		if (bank == null) {
			nullify();
			return;
		}

		sector = bank.getByte("sector");
		count = (sector == null) ? 0 : sector.length;

		layer = bank.getByte("layer");
		energy = bank.getFloat("energy");
		x = bank.getFloat("x"); // CLAS system
		y = bank.getFloat("y");
		z = bank.getFloat("z");

		pindex = bank.getShort("pindex");

		getPID(event);

	} //update
	
	/**
	 * Get the cluster drawing radius from the energy
	 * @param energy the energy in GeV
	 * @return the radius in cm
	 */
	public float getRadius(double energy) {
		if (energy < 0.05) {
			return 0;
		}
		
		float radius = (float) (Math.log((energy + 1.0e-8) / 1.0e-8));
		radius = Math.max(1, Math.min(40f, radius));
		return radius;
	}
	
	

	//get the pids from the REC::Particle bank
	//the pindex array points to rows in this bank
	private void getPID(DataEvent event) {

		pid = null;

		if (count > 0) {
			DataBank particleBank = event.getBank("REC::Particle");
			if (particleBank != null) {
				pid = particleBank.getInt("pid");
			}
		}

	}
	
	/**
	 * Get the feedback string for the PID
	 * @param index the row
	 * @return the pid string
	 */
	public String getPIDStr(int index) {
		int pidval = NOPID;
		
		if (pid != null) {
			pidval = pid[pindex[index]];
		}
		
		if (pidval == NOPID) {
			return "REC PID not available";
		}
		else {
			return "REC PID " + pidval;
		}
	}
	
	/**
	 * Check whether there is any data at all
	 * @return <code>true</code> if there are no data.
	 */
	public boolean isEmpty() {
		return (count  < 1);
	}
	
	/**
	 * Try to get a pid associated with this index
	 * @param index the index of the row in the REC::Calorimeter table
	 * @return the pid from REC::Particle, or NOPID if fails
	 */
	public int getPID(int index) {
		if ((pid == null) || (index < 0) || (index >= count)) {
			return NOPID;
		}
		return pid[pindex[index]];
	}
}
