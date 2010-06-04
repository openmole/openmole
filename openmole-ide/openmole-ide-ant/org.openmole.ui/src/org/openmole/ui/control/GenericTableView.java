package org.openmole.ui.control;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.control.model.ITableView;

/**
 *
 * @author mathieu
 */
public class GenericTableView extends JTable implements ITableView {

    private GenericTableModel model;

    public GenericTableView(GenericTableModel itm) {
        super(new Object[3][itm.getHeaders().length], itm.getHeaders());
        model = itm;

        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getTableHeader().setBackground(ApplicationCustomize.getInstance().getColor(ApplicationCustomize.TABLE_HEADER_COLOR));
    }

    @Override
    public GenericTableModel getTableModel() {
        return model;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer,
            int Index_row,
            int Index_col) {
        Color col = ApplicationCustomize.getInstance().getColor(ApplicationCustomize.TABLE_ROW_COLOR);
        Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
        if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
            comp.setBackground(col);
        } else {
            comp.setBackground(Color.white);
        }
        return comp;
    }
}
