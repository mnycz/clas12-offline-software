package cnuphys.ced.ced3d;



import java.awt.Color;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

import com.jogamp.opengl.GLAutoDrawable;

import bCNU3D.Support3D;
import cnuphys.ced.clasio.ClasIoEventManager;
import cnuphys.ced.frame.CedColors;
import item3D.Item3D;

public class RecDrawer3D extends Item3D {
	
	// the event manager
	ClasIoEventManager _eventManager = ClasIoEventManager.getInstance();
	
	//the current event
	private DataEvent _currentEvent;

	private static final float POINTSIZE = 5f;
	private CedPanel3D _cedPanel3D;
	

	
	public RecDrawer3D(CedPanel3D panel3D) {
		super(panel3D);
		_cedPanel3D = panel3D;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		
		_currentEvent = _eventManager.getCurrentEvent();
		if (_currentEvent == null) {
			return;
		}
		
		if (_panel3D instanceof ForwardPanel3D) { // forward detectors
			
			//show any data from REC::Calorimiter?
			if (((ForwardPanel3D) _panel3D).showRecCal()) {
				showEconCalorimeter(drawable);
			}
		}
	}
	
	
	//show data from REC::Calorimeter
	private void showEconCalorimeter(GLAutoDrawable drawable) {
		
		if (!_currentEvent.hasBank("REC::Calorimeter")) {
			return;
		}
		
		DataBank bank = _currentEvent.getBank("REC::Calorimeter");
		byte[] sector = bank.getByte("sector");

		int len = (sector == null) ? 0 : sector.length;
		if (len == 0) {
			return;
		}

		byte[] layer = bank.getByte("layer");
		float[] energy = bank.getFloat("energy");
		float[] x = bank.getFloat("x"); // CLAS system
		float[] y = bank.getFloat("y");
		float[] z = bank.getFloat("z");

		for (int i = 0; i < len; i++) {
			
			float radius = (float) (Math.log((energy[i] + 1.0e-8) / 1.0e-8));
			radius = Math.max(1, Math.min(40f, radius));

			if ((layer[i] <= 3) && _cedPanel3D.showPCAL()) {
				Support3D.drawPoint(drawable, x[i], y[i], z[i], Color.black, POINTSIZE, true);
				Support3D.solidSphere(drawable, x[i], y[i], z[i], radius, 40, 40, CedColors.RECPcalFill);
			} else if ((layer[i] > 3) && _cedPanel3D.showECAL()) {
				Support3D.drawPoint(drawable, x[i], y[i], z[i], Color.black, POINTSIZE, true);
				Support3D.solidSphere(drawable, x[i], y[i], z[i], radius, 40, 40, CedColors.RECEcalFill);
			}
		}

	}

}
