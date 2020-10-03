package cnuphys.ced.clasio.filter;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jlab.io.base.DataEvent;

import cnuphys.ced.alldata.DataManager;
import cnuphys.ced.alldata.graphics.AllBanksList;

/**
 * This class is to filter on the number of rows in a set banks
 * 
 * @author heddle
 *
 */
public class BankSizeFilter extends AEventFilter {
	
	// list for all known banks
	private AllBanksList _blist;
	
	//for entering the range
	private BankSizeRangePanel _bsPanel;
	
	
	private Hashtable<String, BankRangeRecord> _records;

	public BankSizeFilter() {
		super(true);
		setName("Bank Size Filter");
		setActive(false);
		_records = new Hashtable<String, BankRangeRecord>();
	}

	@Override
	public boolean pass(DataEvent event) {
		Collection<BankRangeRecord> recs = _records.values();

		if ((recs != null) && !recs.isEmpty()) {
			for (BankRangeRecord brec : recs) {
				if (brec.active) {
					int rowCount = DataManager.getInstance().getRowCount(event, brec.bankName);
					if ((rowCount <= brec.minCount) || (rowCount >= brec.maxCount)) {
						return false;
					}
				}
			}
		}

		return true;
	}


	public BankRangeRecord getRecord(String bname) {
		if (bname == null) {
			return null;
		}
		
		return _records.get(bname);
	}
	
	public BankRangeRecord addRecord(String bname, int minCount, int maxCount, boolean active) {
		
		if (bname == null) {
			return null;
		}

		
		_records.remove(bname);
		
		BankRangeRecord rec = new BankRangeRecord(bname, minCount, maxCount, active);
		_records.put(bname, rec);
		return rec;
	}
	
	/**
	 * Create the filter editor 
	 * @return the filter editor
	 */
	@Override
	public AFilterDialog createEditor() {
		
		final BankSizeFilter ffilter = this;
		
		AFilterDialog editor = new AFilterDialog("Bank Size Filter Settings", this) {
			/**
			 * Save the preferences to user pref
			 */
			@Override
			protected void savePreferences() {
				
			}
			
			@Override
			protected void handleCommand(String command) {
				_bsPanel.setName(null);
				setVisible(false);
			}

			
			/**
			 * Create the main component
			 * 
			 * @return the main component of the editor
			 */
			@Override
			public JComponent createMainComponent() {
				JPanel cp = new JPanel();
				_blist = new AllBanksList();
				
				
				
				ListSelectionListener lsl = new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}
						String bname = _blist.getSelectedValue();
						selectedBank(bname);
					}
					
				};
				
				_blist.addListSelectionListener(lsl);
				
				cp.add(_blist.getScrollPane(), BorderLayout.CENTER);
				
				
				
				_bsPanel = new BankSizeRangePanel(ffilter);
				cp.add(_bsPanel, BorderLayout.EAST);
				return cp;
			}

			
		};
		return editor;
	}
	
	//a bank has been selected from the list
	private void selectedBank(String bankname) {
		_bsPanel.setName(bankname);
	}

	
	/**
	 * Edit the filter
	 */
	@Override
	public void edit() {
		if (_editor == null) {
			_editor = createEditor();
		}
		
		if (_editor != null) {
			_editor.setVisible(true);
		}
	}

	
	/**
	 * A builder for a Trigger Filter
	 */
	public static class BankRangeRecord {
		
		public int minCount;
		public int maxCount;
		public String bankName;
		public boolean active;
		
		public BankRangeRecord(String bankName, int minCount, int maxCount, boolean active) {
			this.bankName = bankName;
			this.minCount = minCount;
			this.maxCount = maxCount;
			this.active = active;
		}
	}
	

}
