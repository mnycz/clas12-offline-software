package cnuphys.bCNU.view;


import java.awt.Color;

import cnuphys.bCNU.graphics.container.BaseContainer;
import cnuphys.bCNU.graphics.container.ContainerPanel;
import cnuphys.bCNU.graphics.toolbar.ToolBarToggleButton;

/**
 * A view that contains a grid of BaseContainers.
 * @author heddle
 *
 */
public class ContainerGridView extends ScrollableGridView {
	
	// the containers
	protected ContainerPanel _containerPanel[][];
	protected int _numRow;
	protected int _numCol;

	// which 1-based cell is selected
	protected BaseContainer _hotContainer;

	/**
	 * Creat a crid of BaseContainers each with independent drawing.
	 * @param numRow
	 * @param numCol
	 * @param cellWidth
	 * @param cellHeight
	 * @param keyVals
	 */
	protected ContainerGridView(int numRow, int numCol, int cellWidth, int cellHeight, Object... keyVals) {
		super(numRow, numCol, cellWidth, cellHeight, keyVals);
		_numRow = numRow;
		_numCol = numCol;
		_containerPanel = new ContainerPanel[numRow][numCol];
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

	/**
	 * Set all three labels
	 * @param row the 1-base row
	 * @param col the 1-bases column
	 * @param title the title
	 * @param xlabel the x label
	 * @param yLabel the y label
	 */
	public void setLabels(int row, int col, String title, String xlabel, String ylabel) {
		ContainerPanel panel = getContainerPanel(row, col);
		
		if (panel != null) {
			panel.setLabels(title, xlabel, ylabel);
		}
	}
	


	/**
	 * Set the title for one of the cells
	 * @param row the 1-base row
	 * @param col the 1-bases column
	 * @param title the title
	 */
	public void setTitle(int row, int col, String title) {
		ContainerPanel panel = getContainerPanel(row, col);
		
		if (panel != null) {
			panel.setTitle(title);
		}
	}
	
	/**
	 * Set the x label for one of the cells
	 * @param row the 1-base row
	 * @param col the 1-bases column
	 * @param label the label
	 */
	public void setXLabel(int row, int col, String label) {
		ContainerPanel panel = getContainerPanel(row, col);
		
		if (panel != null) {
			panel.setXLabel(label);
		}
	}

	/**
	 * Set the y label for one of the cells
	 * @param row the 1-base row
	 * @param col the 1-bases column
	 * @param label the label
	 */
	public void setYLabel(int row, int col, String label) {
		ContainerPanel panel = getContainerPanel(row, col);
		
		if (panel != null) {
			panel.setYLabel(label);
		}
	}

	/**
	 * Get the container panel in the given cell
	 * 
	 * @param row the 1-based row
	 * @param col the 1-based column
	 * @return the container panel (might be <code>null</code>);
	 */
	public ContainerPanel getContainerPanel(int row, int col) {
		if ((row < 1) || (row > _numRow)) {
			return null;
		}
		if ((col < 1) || (col > _numCol)) {
			return null;
		}

		return _containerPanel[row - 1][col - 1];
	}

}