
package org.openmole.ui.ide.control.model;

import javax.swing.event.TableModelListener;

/**
 *
 * @author mathieu
 */
public interface ITableModel extends TableModelListener{
    public void setRowCount(int r);
    public void setColumcount(int c);
    public String getName();
}
