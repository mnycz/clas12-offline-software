package org.jlab.detector.swaps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
        put(DetectorType.FTCAL,new Table("/calibraion/ft/ftcal/status","status"));
        put(DetectorType.FTHODO,new Table("/calibraion/ft/fthodo/status","status"));
    }};

    private static class Table {
        public String tableName;
        public String varName;
        public Table(String tableName,String varName) {
            this.tableName=tableName;
            this.varName=varName;
        }
    }
   
    private StatusManager() {}
    
    public static StatusManager getInstance() {
        if (instance == null) {
            instance = new StatusManager();
        }
        return instance;
    }
    
    public Set<DetectorType> getDetectorTypes() {
        return DETECTORS.keySet();
    }

    public Set<String> getTables() {
        Set<String> ret = new HashSet<>();
        Iterator it = DETECTORS.values().iterator();
        while (it.hasNext()) {
            ret.add(((Table)it.next()).tableName);
        }
        return ret;
    }
    
    public int getStatus(int run,DetectorType type,int... slc) {
        if (conman == null || !DETECTORS.containsKey(type)) {
            return NOSTATUS;
        }
        IndexedTable table = conman.getConstants(run, DETECTORS.get(type).tableName);
        return table.getIntValue(DETECTORS.get(type).varName,slc);
    }

    public void initialize(ConstantsManager conman) {
        this.conman = conman;
    }
    
}
