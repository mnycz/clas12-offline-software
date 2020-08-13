package cnuphys.ced.cedview.magfieldview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Double;
import java.util.List;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cnuphys.bCNU.drawable.DrawableAdapter;
import cnuphys.bCNU.drawable.IDrawable;
import cnuphys.bCNU.graphics.GraphicsUtilities;
import cnuphys.bCNU.graphics.container.BaseContainer;
import cnuphys.bCNU.graphics.container.IContainer;
import cnuphys.bCNU.layer.LogicalLayer;
import cnuphys.bCNU.util.PropertySupport;
import cnuphys.bCNU.util.UnicodeSupport;
import cnuphys.bCNU.util.X11Colors;
import cnuphys.ced.cedview.CedView;
import cnuphys.ced.component.ControlPanel;
import cnuphys.ced.component.DisplayBits;
import cnuphys.ced.item.BeamLineItem;

/**
 * The mag field view is used for testing the magnetic field
 * @author heddle
 *
 */
public class MagfieldView extends CedView implements ChangeListener {
	
	private static int CLONE_COUNT = 0;
	
	//phi in degrees
	private double _phi;
	

	
	private MagfieldView(Object... keyVals) {
		super(false, keyVals);
		_phi = 0;
		addItems();
		setBeforeDraw();
		setAfterDraw();

	}
	
	public static MagfieldView createMagfieldView() {
		MagfieldView view = null;

		double xo = -500; // cm. Think of sector 1. x is "vertical"
		double zo = -300.0; // cm. Think of sector 1. z is "horizontal"
		double wheight = 1000;
		double wwidth = 900;

		Dimension d = GraphicsUtilities.screenFraction(0.7);
		// give container same aspect ratio
		// give container same aspect ratio
		int height = d.height;
		int width = (int) ((wwidth * height) / wheight);
		
		String title = "Magnetic Field Testing View";
		if (CLONE_COUNT > 0) {
			title += "_(" + CLONE_COUNT + ")";
		}

		// create the view
		view = new MagfieldView(PropertySupport.WORLDSYSTEM, new Rectangle2D.Double(zo, xo, wwidth, wheight),

				PropertySupport.WIDTH, width,
				PropertySupport.HEIGHT, height, PropertySupport.TOOLBAR, true, PropertySupport.TOOLBARBITS,
				CedView.TOOLBARBITS, PropertySupport.VISIBLE, true, PropertySupport.BACKGROUND,
				Color.white, PropertySupport.TITLE, title,
				PropertySupport.STANDARDVIEWDECORATIONS, true);
		
		view._controlPanel = new ControlPanel(view,
				ControlPanel.PHISLIDER + ControlPanel.FEEDBACK + ControlPanel.FIELDLEGEND, DisplayBits.MAGFIELD, 3, 5);

		view.add(view._controlPanel, BorderLayout.EAST);

		view.pack(); //IMPORTANT!!

		CLONE_COUNT++;
		return view;
	}
	
	/**
	 * Add all the items on this view
	 */
	private void addItems() {
		
		LogicalLayer layer = getContainer().getLogicalLayer(_magneticFieldLayerName);
		new BeamLineItem(layer);

	}
	
	/**
	 * Set the view's before draw
	 */
	private void setBeforeDraw() {
		IDrawable beforeDraw = new DrawableAdapter() {

			@Override
			public void draw(Graphics g, IContainer container) {
//				Rectangle bounds = container.getComponent().getBounds();
//				g.setColor(container.getComponent().getBackground());
//				g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

			}

		};
		getContainer().setBeforeDraw(beforeDraw);
	}

	/**
	 * Set the view's after draw
	 */
	private void setAfterDraw() {
		IDrawable afterDraw = new DrawableAdapter() {

			@Override
			public void draw(Graphics g, IContainer container) {

			}

		};
		getContainer().setAfterDraw(afterDraw);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
	}

	@Override
	public int getSector(IContainer container, Point screenPoint, Double worldPoint) {
		return 0;
	}
	
	
	/**
	 * Some view specific feedback. Should always call super.getFeedbackStrings
	 * first.
	 * 
	 * @param container the base container for the view.
	 * @param pp        the pixel point
	 * @param wp        the corresponding world location.
	 */
	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Point2D.Double wp, List<String> feedbackStrings) {
		
		double rho = wp.y;
		double z = wp.x;
		double phi = Math.toRadians(_phi);
		double x = rho*Math.cos(phi);
		double y = rho*Math.sin(phi);
		double theta = Math.toDegrees(Math.atan2(rho, z));
		
		String xyz = String.format("%s (%9.6f, %9.6f, %9.6f) cm", CedView.xyz, x, y, z);
		String rzp = String.format("%s (%9.6f, %9.6f, %6.3f%s) cm", CedView.rhoZPhi, rho, z, _phi, UnicodeSupport.DEGREE);
		String rtp = String.format("%s (%9.6f, %9.6f%s, %6.3f%s) cm", 
				CedView.rThetaPhi, rho, theta, UnicodeSupport.DEGREE, _phi, UnicodeSupport.DEGREE);

		feedbackStrings.add(xyz);
		feedbackStrings.add(rzp);
		feedbackStrings.add(rtp);
		
		feedbackStrings.add("Bounds " + ((BaseContainer)container).getBounds());
		
		
	}
	
	
}
