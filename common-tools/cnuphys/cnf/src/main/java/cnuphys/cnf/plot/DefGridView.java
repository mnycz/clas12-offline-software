package cnuphys.cnf.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.JPanel;

import org.jlab.io.base.DataEvent;

import cnuphys.bCNU.graphics.GraphicsUtilities;
import cnuphys.bCNU.graphics.container.BaseContainer;
import cnuphys.bCNU.graphics.container.ContainerPanel;
import cnuphys.bCNU.graphics.toolbar.BaseToolBar;
import cnuphys.bCNU.graphics.toolbar.ToolBarToggleButton;
import cnuphys.bCNU.util.Environment;
import cnuphys.bCNU.util.PropertySupport;
import cnuphys.bCNU.util.X11Colors;
import cnuphys.bCNU.view.ContainerGridView;
import cnuphys.bCNU.view.ScrollableGridView;
import cnuphys.cnf.event.EventManager;
import cnuphys.cnf.event.IEventListener;

/**
 * A view that contains a grid of BaseContainers.
 * @author heddle
 *
 */
public class DefGridView extends ContainerGridView implements IEventListener {
	
	/**
	 * Creat a crid of BaseContainers each with independent drawing.
	 * @param numRow
	 * @param numCol
	 * @param cellWidth
	 * @param cellHeight
	 * @param keyVals
	 */
	protected DefGridView(int numRow, int numCol, int cellWidth, int cellHeight, Object... keyVals) {
		super(numRow, numCol, cellWidth, cellHeight, keyVals);
		EventManager.getInstance().addEventListener(this, 2);	
	}
	
	
	/**
	 * Create a ContainerGridView
	 * 
	 * @param title
	 * @param numRow
	 * @param numCol
	 * @param cellWidth
	 * @param cellHeight
	 * @param screenFraction
	 * @return a PlotGridView object
	 */
	public static DefGridView createDefGridView(String title, int numRow, 
			int numCol, double screenFraction) {

		Dimension d = GraphicsUtilities.screenFraction(screenFraction);
		int width = d.width;
		int height = d.height;
		
		int cellWidth = width/numCol;
		int cellHeight = height/numRow;



		final DefGridView view = new DefGridView(numRow, numCol, cellWidth, cellHeight, PropertySupport.WIDTH,
				width, PropertySupport.HEIGHT, height, PropertySupport.TOOLBAR, false,
				PropertySupport.VISIBLE, true, PropertySupport.TITLE,
				title, PropertySupport.SCROLLABLE, true, PropertySupport.STANDARDVIEWDECORATIONS, true);

		
		JPanel p = new JPanel();
		
		p.setLayout(new GridLayout(numRow, numCol, 4, 4));
		p.setOpaque(true);
		p.setBackground(Environment.getCommonPanelBackground());
		
		int tbarbits = BaseToolBar.NODRAWING & ~BaseToolBar.TEXTFIELD & ~BaseToolBar.CONTROLPANELBUTTON
				& ~BaseToolBar.RECTGRIDBUTTON & ~BaseToolBar.MAGNIFYBUTTON & ~BaseToolBar.PANBUTTON
				& ~BaseToolBar.RANGEBUTTON & ~BaseToolBar.CENTERBUTTON & ~BaseToolBar.UNDOZOOMBUTTON
				& ~BaseToolBar.TEXTBUTTON & ~BaseToolBar.DELETEBUTTON & ~BaseToolBar.CLONEBUTTON;

		
		
		for (int row = 0; row < numRow; row++) {
			for (int col = 0; col < numCol; col++) {
				view._containerPanel[row][col] = new ContainerPanel(tbarbits);
				p.add(view._containerPanel[row][col]);
			}
		}
		view.add(p);

		return view;
	}


	/**
	 * Check whether the pointer bar is active on the tool bar
	 * 
	 * @return <code>true</code> if the Pointer button is active.
	 */
	protected boolean isPointerButtonActive() {
		ToolBarToggleButton mtb = getContainer().getActiveButton();
		return (mtb == getContainer().getToolBar().getPointerButton());
	}


	
	@Override
	public void newEvent(DataEvent event, boolean isStreaming) {
	}

	@Override
	public void openedNewEventFile(File file) {
	}

	@Override
	public void rewoundFile(File file) {
	}

	@Override
	public void streamingStarted(File file, int numToStream) {
	}

	@Override
	public void streamingEnded(File file, int reason) {
	}

}
