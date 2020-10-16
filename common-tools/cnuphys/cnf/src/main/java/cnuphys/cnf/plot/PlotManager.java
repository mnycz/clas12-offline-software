package cnuphys.cnf.plot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.jlab.io.base.DataEvent;

import cnuphys.bCNU.drawable.DrawableAdapter;
import cnuphys.bCNU.graphics.colorscale.ColorScaleModel;
import cnuphys.bCNU.graphics.container.BaseContainer;
import cnuphys.bCNU.graphics.container.ContainerPanel;
import cnuphys.bCNU.graphics.container.IContainer;
import cnuphys.bCNU.util.Fonts;
import cnuphys.bCNU.util.X11Colors;
import cnuphys.cnf.alldata.ColumnData;
import cnuphys.cnf.alldata.DataManager;

import cnuphys.cnf.event.EventManager;
import cnuphys.cnf.event.IEventListener;

public class PlotManager implements IEventListener {

	private static PlotManager _instance;

	private DefGridView _view;

	private static final float TINY = (float) 1.0e-5;
	// menu related
	private JMenuBar _menuBar;
	private JMenu _plotMenu;
	private JMenuItem _clearItem;

	// indices
	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;
	private static final int BX = 3;
	private static final int BY = 4;
	private static final int BZ = 5;
	private static final int DEL = 6;
	private static final int R = 7;
	private static final int BMAG = 8;

	// color scales
	private ColorScaleModel _redModel = ColorScaleModel.createColorModel(0, 1, 100, new Color(255, 250, 250),
			Color.red);
	private ColorScaleModel _blueModel = ColorScaleModel.createColorModel(0, 1, 100, new Color(250, 250, 255),
			Color.blue);

	// just counts "events"
	private int _totalCount;
	private DataRanges _dataRanges = new DataRanges();

	// data related
	private ArrayList<ColumnData> _columnDataList = new ArrayList<ColumnData>();
	private String _bankName = "CNF::nucleon_map";
	private Hashtable<String, ArrayList<Float>> _data;

	private PlotManager() {
		EventManager.getInstance().addEventListener(this, 2);
		_data = new Hashtable<String, ArrayList<Float>>();
		initializeColumnData();
	}

	/**
	 * public access to the singleton
	 * 
	 * @return
	 */
	public static PlotManager getInstance() {
		if (_instance == null) {
			_instance = new PlotManager();
		}

		return _instance;
	}

	// initialize the column data
	private void initializeColumnData() {
		String columnNames[] = DataManager.getInstance().getColumnNames(_bankName);

		if (columnNames != null) {
			for (String columnName : columnNames) {
				String fullName = DataManager.getInstance().fullName(_bankName, columnName);
				ColumnData cd = DataManager.getInstance().getColumnData(fullName);
				if (cd != null) {
					_columnDataList.add(cd);
				}
			}
		}
	}

	// inside or outside test
	public boolean inside(float[] a) {

		float dot = 0;
		for (int i = 0; i < 3; i++) {
			dot += a[i] * a[i + 3];
		}
		return (dot > 0); // outside
	}

	// clear all data
	private void clear() {
		_data.clear();
		_dataRanges = new DataRanges();
		_view.refresh();
	}

	private void setPlotLimits() {
		BaseContainer container;
		double pad = 1.05;
		double w, h, w2, h2;

		// plot (0, 0) x vs y
		container = _view.getContainer(0, 0);
		double xc = (_dataRanges.xmin + _dataRanges.xmax) / 2;
		double yc = (_dataRanges.ymin + _dataRanges.ymax) / 2;
		w = pad * (_dataRanges.xmax - _dataRanges.xmin);
		h = pad * (_dataRanges.ymax - _dataRanges.ymin);
		w2 = w / 2;
		h2 = h / 2;
		container.reworld(xc - w2, xc + w2, yc - h2, yc + h2);

		// plot (0, 1) r vs theta (deg)
		container = _view.getContainer(0, 1);
		container.reworld(0, 180, 0, _dataRanges.rmax);

		// plot (1, 0) r vs phi (deg)
		container = _view.getContainer(1, 0);
		container.reworld(-180, 180, 0, _dataRanges.rmax);

	}

	/**
	 * Get the grid view with a grid of plots
	 * 
	 * @return the grid view
	 */
	public DefGridView getDefGridView() {
		if (_view == null) {

			int nrow = 2;
			int ncol = 4;

			_view = DefGridView.createDefGridView("Plots", nrow, ncol, 0.7);

			for (int row = 0; row < nrow; row++) {
				for (int col = 0; col < ncol; col++) {
					ContainerPanel panel = _view.getContainerPanel(row, col);
					new CellDrawer(this, panel, row, col);
				}

			}

			_menuBar = new JMenuBar();
			_view.setJMenuBar(_menuBar);
			addMenu();
			setupPlots();
		}

		return _view;
	}

	// setup the plots
	private void setupPlots() {
		_view.setLabels(0, 0, "Force Magnitude (z = 0)", "x (fm)", "y (fm)");
		_view.setLabels(0, 1, "Force Magnitude (phi = 0)", "theta (deg)", "r (fm)");
		_view.setLabels(1, 0, "Force Magnitude (z = 0)", "phi (deg)", "r (fm)");

	}

	// add the plots menu
	private void addMenu() {
		_plotMenu = new JMenu("Plots");

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if (source == _clearItem) {
					clear();
				}

			}

		};

		_clearItem = new JMenuItem("Clear all plots");
		_clearItem.addActionListener(al);
		_plotMenu.add(_clearItem);

		_view.getJMenuBar().add(_plotMenu);
	}

	// get the data list for a given column
	private ArrayList<Float> getDataArray(String colName) {
		return _data.get(DataManager.getInstance().fullName(_bankName, colName));
	}

	// get a data row
	private void getDataRow(int index, float[] array) {
		array[X] = getDataArray("x").get(index);
		array[Y] = getDataArray("y").get(index);
		array[Z] = getDataArray("z").get(index);
		array[BX] = getDataArray("Bx").get(index);
		array[BY] = getDataArray("By").get(index);
		array[BZ] = getDataArray("Bz").get(index);
		array[DEL] = getDataArray("Del").get(index);

		array[R] = getMag(array[X], array[Y], array[Z]);
		array[BMAG] = getMag(array[BX], array[BY], array[BZ]);
	}

	private float getMag(float x, float y, float z) {
		double magsq = x * x + y * y + z * z;
		return (float) (Math.sqrt(magsq));
	}

	@Override
	public void newEvent(DataEvent event, boolean isStreaming) {

		_totalCount++;

		if ((_totalCount % 1000) == 0) {
			System.err.println("Data read count: " + _totalCount);
		}

		if ((_columnDataList != null) && !_columnDataList.isEmpty()) {

			for (ColumnData cdata : _columnDataList) {
				// add to the data for that column
				ArrayList<Float> dal = _data.get(cdata.getFullName());
				if (dal == null) {
					dal = new ArrayList<Float>();
					_data.put(cdata.getFullName(), dal);
					System.err.println("Added column [" + cdata.getFullName() + "]");
				}

				for (double v : cdata.getAsDoubleArray(event)) {
					dal.add((float) v);
				}
			}

		}
	}

	@Override
	public void openedNewEventFile(File file) {
		System.err.println("Opened new event file [" + file.getPath() + "]");
		_totalCount = 0;
	}

	@Override
	public void rewoundFile(File file) {
		System.err.println("Rewound [" + file.getPath() + "]");
		_totalCount = 0;
	}

	@Override
	public void streamingStarted(File file, int numToStream) {
		System.err.println("Streaming Started [" + file.getPath() + "] num: " + numToStream);
	}

	@Override
	public void streamingEnded(File file, int reason) {
		System.err.println(
				"Streaming Ended [" + file.getPath() + "] reason: " + ((reason == 0) ? "completed" : "interrupted"));

		int count = 0;
		for (String key : _data.keySet()) {
			count = _data.get(key).size();
			System.err.println(String.format("column[%s] has %d values", key, count));
		}

		System.err.println("Checking inside or outside and getting data ranges");
		int insideCount = 0;
		int outsideCount = 0;

		float[] array = new float[9];
		for (int index = 0; index < count; index++) {
			getDataRow(index, array);
			_dataRanges.newValue(array);
			if (inside(array)) {
				insideCount++;
			} else {
				outsideCount++;
			}
		}

		System.err.println(_dataRanges.toString());
		System.err.println("Inside count: " + insideCount + "  Outside count: " + outsideCount);

		setPlotLimits();

		_view.refresh();
	}

	// are two values essentially the same
	private boolean sameValue(float v1, float v2) {
		return Math.abs(v1 - v2) < TINY;
	}

	// are two values essentially the same
	private boolean sameValue(double v1, double v2) {
		return Math.abs(v1 - v2) < TINY;
	}

	class CellDrawer extends DrawableAdapter {

		/** 0-based row */
		public int row;

		/** 0-based column */
		public int column;

		private PlotManager _plotManager;

		private ContainerPanel _panel;

		public CellDrawer(PlotManager pm, ContainerPanel panel, int row, int column) {
			this.row = row;
			this.column = column;
			_plotManager = pm;
			_panel = panel;

			_panel.getBaseContainer().noModel(this);
		}

		@Override
		public void draw(Graphics g, IContainer container) {

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			BaseContainer bc = (BaseContainer) container;

			Rectangle bounds = bc.getBounds();
			g.setColor(X11Colors.getX11Color("alice blue"));

			g.fillRect(0, 0, bounds.width, bounds.height);

			ArrayList<Float> testArray = getDataArray("x");
			int count = (testArray == null) ? 0 : testArray.size();

			if (count == 0) {
				return;
			}

			float data[] = new float[9];
			Point2D.Double wp = new Point2D.Double();
			Point pp = new Point();

			for (int index = 0; index < count; index++) {

				_plotManager.getDataRow(index, data);

				if ((row == 0) && (column == 0)) {
					if (sameValue(data[Z], 0)) {
						wp.setLocation(data[X], data[Y]);
						bc.worldToLocal(pp, wp);
						double mag = data[BMAG];
						double fract = mag / _dataRanges.bmax;

						Color color;
						if (inside(data)) {
							color = _redModel.getColor(fract);
						} else {
							color = _blueModel.getColor(fract);
						}

						g.setColor(color);

						g.fillRect(pp.x - 2, pp.y - 2, 4, 4);
					}
				}

				else if ((row == 0) && (column == 1)) {
					double phi = Math.toDegrees(Math.atan2(data[Y], data[X]));
					if (sameValue(phi, 0) || sameValue(phi, 360)) {
						double theta = Math.toDegrees(Math.acos(data[Z] / data[R]));

						wp.setLocation(theta, data[R]);
						bc.worldToLocal(pp, wp);
						double mag = data[BMAG];
						double fract = mag / _dataRanges.bmax;

						Color color;
						if (inside(data)) {
							color = _redModel.getColor(fract);
						} else {
							color = _blueModel.getColor(fract);
						}

						g.setColor(color);

						g.fillRect(pp.x - 2, pp.y - 2, 4, 4);

					}
				}

				else if ((row == 1) && (column == 0)) {
					if (sameValue(data[Z], 0)) {
						double phi = Math.toDegrees(Math.atan2(data[Y], data[X]));

						wp.setLocation(phi, data[R]);
						bc.worldToLocal(pp, wp);
						double mag = data[BMAG];
						double fract = mag / _dataRanges.bmax;

						Color color;
						if (inside(data)) {
							color = _redModel.getColor(fract);
						} else {
							color = _blueModel.getColor(fract);
						}

						g.setColor(color);

						g.fillRect(pp.x - 2, pp.y - 2, 4, 4);

					}

				}

			}

			// draw axis values
			g.setColor(Color.black);
			g.setFont(Fonts.smallMono);
			int numtick = 5;
			Rectangle2D.Double wr = bc.getWorldSystem();
			double dx = wr.width / (numtick + 1);
			double dy = wr.height / (numtick + 1);

			FontMetrics fm = bc.getFontMetrics(Fonts.smallMono);

			for (int i = 0; i < numtick; i++) {
				double yt = wr.y + (i + 1) * dy;
				wp.setLocation(wr.x, yt);
				bc.worldToLocal(pp, wp);
				g.drawLine(pp.x, pp.y, pp.x + 4, pp.y);

				String vstr = String.format("%-6.2f", yt);
				g.setColor(Color.white);
				g.drawString(vstr, pp.x + 5, pp.y + fm.getHeight() / 2 - 1);
				g.setColor(Color.black);
				g.drawString(vstr, pp.x + 6, pp.y + fm.getHeight() / 2);
			}

			for (int i = 0; i < numtick; i++) {
				double xt = wr.x + (i + 1) * dx;
				wp.setLocation(xt, wr.y);
				bc.worldToLocal(pp, wp);
				g.drawLine(pp.x, pp.y, pp.x, pp.y - 4);

				String vstr = String.format("%-6.2f", xt);
				g.setColor(Color.white);
				g.drawString(vstr, pp.x - fm.stringWidth(vstr) / 2 - 1, pp.y - 7);
				g.setColor(Color.black);
				g.drawString(vstr, pp.x - fm.stringWidth(vstr) / 2, pp.y - 6);

			}

		}

	}

	class DataRanges {
		float rmin = Float.POSITIVE_INFINITY;
		float rmax = Float.NEGATIVE_INFINITY;

		float xmin = Float.POSITIVE_INFINITY;
		float xmax = Float.NEGATIVE_INFINITY;
		float ymin = Float.POSITIVE_INFINITY;
		float ymax = Float.NEGATIVE_INFINITY;
		float zmin = Float.POSITIVE_INFINITY;
		float zmax = Float.NEGATIVE_INFINITY;
		float bmin = Float.POSITIVE_INFINITY;
		float bmax = Float.NEGATIVE_INFINITY;
		float dmin = Float.POSITIVE_INFINITY;
		float dmax = Float.NEGATIVE_INFINITY;

		public DataRanges() {
		}

		public void newValue(float[] array) {

			float x = array[X];
			float y = array[Y];
			float z = array[Z];
			float bx = array[BX];
			float by = array[BY];
			float bz = array[BZ];

			xmin = Math.min(xmin, x);
			xmax = Math.max(xmax, x);
			ymin = Math.min(ymin, y);
			ymax = Math.max(ymax, y);
			zmin = Math.min(zmin, z);
			zmax = Math.max(zmax, z);

			float rsq = x * x + y * y + z * z;
			float r = (float) Math.sqrt(rsq);

			rmin = Math.min(rmin, r);
			rmax = Math.max(rmax, r);

			double magSq = bx * bx + by * by + bz * bz;
			float mag = (float) Math.sqrt(magSq);

			bmin = Math.min(bmin, mag);
			bmax = Math.max(bmax, mag);

			dmin = Math.min(dmin, array[DEL]);
			dmax = Math.max(dmax, array[DEL]);
		}

		@Override
		public String toString() {
			return String.format(
					"Ranges:\n x:[%6.3f, %6.3f]\n y:[%6.3f, %6.3f]\n z:[%6.3f, %6.3f]\n r:[%6.3f, %6.3f]\n b:[%6.3f, %6.3f]\n d:[%6.3f, %6.3f]",
					xmin, xmax, ymin, ymax, zmin, zmax, rmin, rmax, bmin, bmax, dmin, dmax);

		}

	}

}
