package cnuphys.cnf.grid;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jlab.io.base.DataEvent;

import cnuphys.cnf.event.EventManager;
import cnuphys.cnf.event.IEventListener;

public class GridManager implements IEventListener {

	// grid related menu
	private JMenu _gridMenu;
	
	//gridify the data
	private JMenuItem _gridifyItem;

	//the singleton
	private static GridManager _instance;
	
	//private constructor
	private GridManager() {
		EventManager.getInstance().addEventListener(this, 2);
	}

	/**
	 * public access to the singleton
	 * 
	 * @return the GridManager
	 */
	public static GridManager getInstance() {
		if (_instance == null) {
			_instance = new GridManager();
		}

		return _instance;
	}
	
	//fix the menu state
	private void fixState() {
		
	}

	/**
	 * Get the grid related menu
	 * @return the grid menu
	 */
	public JMenu getGridMenu() {
		if (_gridMenu == null) {
			_gridMenu = new JMenu("Grid");
			_gridifyItem = new JMenuItem("Gridify current data");
			_gridifyItem.addActionListener(event->gridify());
			_gridMenu.add(_gridifyItem);
		}
		
		fixState();
		return _gridMenu;
	}
	
	//gridify any data in memory
	private void gridify() {
		
	}
	
	/**
	 * Check whether the data in memory is gridable.
	 * @return true if the data in memory is gridable.
	 */
	public boolean isGridable() {
		return true;
	}

	@Override
	public void newEvent(DataEvent event, boolean isStreaming) {
		// TODO Auto-generated method stub

	}

	@Override
	public void openedNewEventFile(File file) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rewoundFile(File file) {
		// TODO Auto-generated method stub

	}

	@Override
	public void streamingStarted(File file, int numToStream) {
		// TODO Auto-generated method stub

	}

	@Override
	public void streamingEnded(File file, int reason) {
		// TODO Auto-generated method stub

	}
}
