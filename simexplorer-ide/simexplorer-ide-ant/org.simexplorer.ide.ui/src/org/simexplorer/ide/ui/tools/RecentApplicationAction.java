/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simexplorer.ide.ui.tools;

import org.openide.util.Exceptions;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.BeanInfo;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;
import org.openide.windows.WindowManager;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import org.simexplorer.ide.ui.tools.RecentApplications.HistoryItem;

/**
 * Action that presents list of recently closed files/documents.
 *
 * adapted from @author Dafe Simonek by @author thierry
 */
public class RecentApplicationAction extends AbstractAction implements Presenter.Menu, PopupMenuListener {

    /** property of menu items where we store fileobject to open */
    private static final String SimExplorerApp_PROP = "RecentApplicationAction.Recent_SimExplorerApp";
    /** number of maximum shown items in submenu */
    private static final int MAX_COUNT = 15;
    private JMenu menu;

    public RecentApplicationAction() {
        super(NbBundle.getMessage(RecentApplicationAction.class, "LBL_RecentFileAction_Name")); // NOI18N
    }

    /********* Presenter.Menu impl **********/
    public JMenuItem getMenuPresenter() {
        if (menu == null) {
            menu = new UpdatingMenu(this);
            menu.setMnemonic(NbBundle.getMessage(RecentApplicationAction.class,
                    "MNE_RecentFileAction_Name").charAt(0));
            menu.getPopupMenu().addPopupMenuListener(this);
        }
        return menu;
    }

    /******* PopupMenuListener impl *******/
    public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
        fillSubMenu();
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
        menu.removeAll();
    }

    public void popupMenuCanceled(PopupMenuEvent arg0) {
    }

    /** Fills submenu with recently closed files got from RecentFiles support */
    private void fillSubMenu() {
        List<RecentApplications.HistoryItem> files = RecentApplications.getRecentFiles();

        int counter = 0;
        for (HistoryItem hItem : files) {
            // obtain file object
            // note we need not check for null or validity, as it is ensured
            // by RecentFiles.getRecentFiles()
            FileObject fo = RecentApplications.convertURL2File(hItem.getURL());
            // allow only up to max items
            if (++counter > MAX_COUNT) {
                break;
            }

            /*
            // obtain icon for fileobject
            Image icon = null;
            try {
            DataObject dObj = DataObject.find(fo);
            icon = dObj.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16);
            } catch (DataObjectNotFoundException ex) {
            // should not happen, log and skip to next
            Logger.getLogger(RecentApplications.class.getName()).log(
            Level.INFO, ex.getMessage(), ex);
            continue;
            }
             */
            // create and configure menu item
            JMenuItem jmi = null;
//            if (icon != null) {
//                jmi = new JMenuItem(fo.getNameExt(), new ImageIcon(icon));
//            } else {
            jmi = new JMenuItem(fo.getNameExt());
//            }
            jmi.putClientProperty(SimExplorerApp_PROP, fo);
            jmi.addActionListener(this);
            menu.add(jmi);
        }
    }

    /** Opens recently closed file, using OpenFile support.
     *
     * Note that method works as action handler for individual submenu items
     * created in fillSubMenu, not for whole RecentFileAction.
     */
    public void actionPerformed(ActionEvent evt) {
        JMenuItem source = (JMenuItem) evt.getSource();
        FileObject fo = (FileObject) source.getClientProperty(SimExplorerApp_PROP);
        if (fo != null) {
            int n = JOptionPane.YES_OPTION;
            ExplorationApplication application = ApplicationsTopComponent.findInstance().getExplorationApplication();
            if (application != null) {
                n = JOptionPane.showOptionDialog(WindowManager.getDefault().getMainWindow(),
                        "Do you want to erase the existing Exploration application?",
                        "Erase warning",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.NO_OPTION);

            }
            ExplorationApplication explorationApplication = null;
            try {
                explorationApplication = new ExplorationApplication(fo.getPath());
            } catch (InternalProcessingError ex) {
                Exceptions.printStackTrace(ex);
            } catch (UserBadDataError ex) {
                Exceptions.printStackTrace(ex);
            }
            ApplicationsTopComponent.findInstance().setApplication(explorationApplication);
        }
    }

    /** Menu that checks its enabled state just before is populated */
    private static class UpdatingMenu extends JMenu implements DynamicMenuContent {

        private final JComponent[] content = new JComponent[]{this};

        public UpdatingMenu(Action action) {
            super(action);
        }

        public JComponent[] getMenuPresenters() {
            setEnabled(!RecentApplications.getRecentFiles().isEmpty());
            return content;
        }

        public JComponent[] synchMenuPresenters(JComponent[] items) {
            return getMenuPresenters();
        }
    }
}

