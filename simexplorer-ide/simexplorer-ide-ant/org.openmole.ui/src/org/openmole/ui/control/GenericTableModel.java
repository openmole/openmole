
package org.openmole.ui.control;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import org.openmole.ui.commons.Conversions;
import org.openmole.ui.control.model.ITableModel;


/**
 *
 * @author mathieu
 */
public class GenericTableModel extends AbstractTableModel implements ITableModel {
    private int rowCount = 0;
    private int columnCount = 0;
    private String name;
    private TableType.Name type;


    public GenericTableModel(TableType.Name tn) {
    super();
    type = tn;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public void setRowCount(int r) {
        rowCount = r;
    }

    @Override
    public void setColumcount(int c) {
        columnCount = c;
    }
    
    @Override
    public void tableChanged(TableModelEvent tme) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getValueAt(int i, int i1) {
        return 10;
    }

    @Override
public void setValueAt(Object value, int row, int col) {
        
    }

    @Override
    public String getName() {
        return name;
    }

    public String getHeader(){
        return TableType.toString(type);    
    }

    public String[] getHeaders(){
        return Conversions.collectionToStringArray(TableMapping.getInstance().getHeaders(type));
    }
}
