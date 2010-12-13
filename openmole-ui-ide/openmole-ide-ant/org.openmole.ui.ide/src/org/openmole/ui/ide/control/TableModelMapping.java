package org.openmole.ui.ide.control;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JTabbedPane;
import org.openmole.ui.ide.control.TableType.Name;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author mathieu
 */
public class TableModelMapping {

    private static TableModelMapping instance = null;
    private Map<IGenericTaskModelUI, JTabbedPane> tableModelMap = new HashMap();


    public synchronized JTabbedPane getTabbedPane(IGenericTaskModelUI tm) {

        if (!tableModelMap.containsKey(tm)) {
            buildTabbedPane(tm);
        }

        return tableModelMap.get(tm);
    }

    private void buildTabbedPane(IGenericTaskModelUI tm) {

//        JTabbedPane tabbedPane = new JTabbedPane();
//        Iterator<TableType.Name> it = tm.getFields().iterator();
//        while (it.hasNext()) {
//            Name n = it.next();
//            GenericTableModel model = new GenericTableModel(n);
//            GenericTableView gtv = new GenericTableView(model);
//            tabbedPane.add(model.getHeader(), new TableScrollPane(gtv));
//        }
//        tableModelMap.put(tm, tabbedPane);
//    }
//
//    public static TableModelMapping getInstance() {
//        if (instance == null) {
//            instance = new TableModelMapping();
//        }
//        return instance;
    }
}
