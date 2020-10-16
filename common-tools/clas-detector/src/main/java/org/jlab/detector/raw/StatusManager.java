package org.jlab.detector.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.utils.groups.IndexedTable;

/**
 *
 * @author baltzell
 */
public class StatusManager {
  
    public final static int NOSTATUS = 999999;
    
    private ConstantsManager conman = null;
    private static StatusManager instance = null;

    // since it looks like a consistent nameing convention wasn't followed, we'd need this:
    private static final Map<DetectorType,Table> DETECTORS = new HashMap<DetectorType,Table>() {{
        put(DetectorType.FTCAL, new Table("/calibration/ft/ftcal/status"));
        put(DetectorType.FTHODO,new Table("/calibration/ft/fthodo/status"));
        put(DetectorType.LTCC,  new Table("/calibration/ltcc/status"));
        put(DetectorType.ECAL,  new Table("/calibration/ec/status"));
        put(DetectorType.HTCC,  new Table("/calibration/htcc/status"));
        put(DetectorType.DC,    new Table("/calibration/dc/status_tables/Bad_Wires"));
        put(DetectorType.CTOF,  new Table("/calibration/ctof/status","status_upstream","status_downstream"));
        put(DetectorType.FTOF,  new Table("/calibration/ftof/status","status_left","status_right"));
        put(DetectorType.BST,   new Table("/calibration/cvt/status"));
    }};

    private static class Table {
        public String tableName;
        public List<String> varName = new ArrayList<>();
        public Table(String tableName,String... varName) {
            this.tableName=tableName;
            this.varName.addAll(Arrays.asList(varName));
        }
        public Table(String tableName) {
            this.tableName = tableName;
            this.varName.add("status");
        }
        @Override
        public String toString() {
            return tableName+"."+varName;
        }
    }
   
    private StatusManager() {}
    
    public static StatusManager getInstance() {
        if (instance == null) {
            instance = new StatusManager();
        }
        return instance;
    }
    
    public static Set<DetectorType> getDetectorTypes() {
        return DETECTORS.keySet();
    }

    public static Set<String> getTables() {
        Set<String> ret = new HashSet<>();
        Iterator it = DETECTORS.values().iterator();
        while (it.hasNext()) {
            ret.add(((Table)it.next()).tableName);
        }
        return ret;
    }
    
    public int getStatus(int run,DetectorType type,String varName,int... slc) {
        if (conman == null || !DETECTORS.containsKey(type)) {
            return NOSTATUS;
        }
        IndexedTable table = conman.getConstants(run, DETECTORS.get(type).tableName);
        return table.getIntValue(varName,slc);
    }

    public int getStatus(int run,DetectorType type,int... slc) {
        return this.getStatus(run,type,DETECTORS.get(type).varName.get(0),slc);
    }

    public void initialize(ConstantsManager conman) {
        this.conman = conman;
    }
    
    public static void main(String[] args) {
        ConstantsManager conman = new ConstantsManager();
        conman.setVariation("default");
        conman.init(new ArrayList<>(StatusManager.getTables()));
        StatusManager statman = StatusManager.getInstance();
        statman.initialize(conman);
        for (int c=50; c<65; c++) {
            System.out.println(String.format("%d/%d/%d : %d",
                    1,1,c,statman.getStatus(17,DetectorType.FTCAL,1,1,c)));
        }
    }
}
