package org.openmole.ui.workflow.model;

import java.util.Collection;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.netbeans.api.visual.action.PopupMenuProvider;

/**
 *
 * @author mathieu
 */
public interface IGenericMenuProvider extends PopupMenuProvider{

   // public IObjectModelUI getModel();
    public Collection<JMenuItem> getItems();
    public Collection<JMenu> getMenus();
}
