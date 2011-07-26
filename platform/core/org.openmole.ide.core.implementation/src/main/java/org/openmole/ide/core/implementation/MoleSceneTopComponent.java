/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core.implementation;

import java.awt.BorderLayout;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openmole.ide.core.implementation.palette.PaletteSupport;
import org.openmole.ide.core.implementation.control.MoleScenesManager;
import org.openmole.ide.core.implementation.action.AddMoleSceneAction;
import org.openmole.ide.core.implementation.action.EnableTaskDetailedViewAction;
import org.openmole.ide.core.implementation.action.RemoveAllMoleSceneAction;
import org.netbeans.spi.palette.PaletteController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.nodes.AbstractNode;
import org.openmole.ide.core.implementation.action.BuildExecutionAction;
import org.openmole.ide.core.implementation.palette.CategoryBuilder;
import org.openmole.ide.core.implementation.display.MenuToggleButton2;
import org.openmole.ide.core.implementation.action.RemoveMoleSceneAction;
import scala.swing.Menu;
import scala.swing.MenuItem;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.openmole.ide.core.implementation//MoleScene//EN",
autostore = false)
@TopComponent.Description(preferredID = "MoleSceneTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "org.openmole.ide.core.implementation.MoleSceneTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "MoleScene",
preferredID = "MoleSceneTopComponent")
public final class MoleSceneTopComponent extends TopComponent {

    private static MoleSceneTopComponent instance;
    private PaletteController palette;
    private final InstanceContent ic = new InstanceContent();

    public MoleSceneTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(MoleSceneTopComponent.class, "CTL_MoleSceneTopComponent"));
        setToolTipText(NbBundle.getMessage(MoleSceneTopComponent.class, "HINT_MoleSceneTopComponent"));

        MoleScenesManager.displayBuildMoleScene(MoleScenesManager.addBuildMoleScene());

        createPalette();
        associateLookup(new AbstractLookup(ic));
        ic.add(palette);

        //  palette.addPropertyChangeListener(this);

        JToggleButton detailedViewButton = new JToggleButton("Detailed view");
        detailedViewButton.addActionListener(new EnableTaskDetailedViewAction());

        MenuToggleButton2 moleButton = new MenuToggleButton2("Mole");
        moleButton.addItem(new MenuItem(new AddMoleSceneAction("Add")));
        moleButton.addItem(new MenuItem(new RemoveMoleSceneAction("Remove")));
        moleButton.addItem(new MenuItem(new RemoveAllMoleSceneAction("Remove All")));

        
        MenuToggleButton2 executionButton = new MenuToggleButton2("Execution");
        executionButton.addItem(new MenuItem(new BuildExecutionAction("Build")));
        executionButton.addItem(new MenuItem(new BuildExecutionAction("Clean and Build")));
        
        toolBar.add(detailedViewButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(moleButton.peer());
        toolBar.add(executionButton.peer());
        //  add(toolBar);
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(MoleScenesManager.tabbedPane().peer(), BorderLayout.CENTER);

    }

    public void refreshPalette() {
        ic.remove(palette);
        createPalette();
        ic.add(palette);
        repaint();
    }

    public void createPalette() {
        AbstractNode paletteRoot = new AbstractNode(new CategoryBuilder());
        paletteRoot.setName("Entities");
        palette = PaletteSupport.createPalette(paletteRoot);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toolBar = new javax.swing.JToolBar();

        toolBar.setRollover(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                .addGap(264, 264, 264))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
