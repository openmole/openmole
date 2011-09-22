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
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openmole.ide.core.implementation.palette.PaletteSupport;
import org.openmole.ide.core.implementation.control.TabManager;
import org.openmole.ide.core.implementation.action.AddMoleSceneAction;
import org.openmole.ide.core.implementation.action.EnableTaskDetailedViewAction;
import org.openmole.ide.core.implementation.action.RemoveAllMoleSceneAction;
import org.netbeans.spi.palette.PaletteController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.ImageUtilities;
import org.openmole.ide.core.implementation.control.ExecutionBoard;
import org.openmole.ide.core.implementation.action.BuildExecutionAction;
import org.openmole.ide.core.implementation.action.CleanAndBuildExecutionAction;
import org.openmole.ide.core.implementation.action.RemoveMoleSceneAction;
import scala.swing.MenuItem;
import org.openmole.ide.misc.widget.ToolBarButton;

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
    private ToolBarButton buildButton;
    private ToolBarButton cleanAndBuildButton;
    private ToolBarButton addMoleButton;
    private ToolBarButton removeMoleButton;
    private ToolBarButton removeAllMoleButton;
    private JToggleButton detailedViewButton;
    private PaletteController palette;
    private final InstanceContent ic = new InstanceContent();
    private ExecutionTopComponent etc = ((ExecutionTopComponent) WindowManager.getDefault().findTopComponent("ExecutionTopComponent"));

    public MoleSceneTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(MoleSceneTopComponent.class, "CTL_MoleSceneTopComponent"));
        setToolTipText(NbBundle.getMessage(MoleSceneTopComponent.class, "HINT_MoleSceneTopComponent"));

        associateLookup(new AbstractLookup(ic));
        addPalette();

        detailedViewButton = new JToggleButton(new ImageIcon(ImageUtilities.loadImage("img/detailedView.png")));
        detailedViewButton.addActionListener(new EnableTaskDetailedViewAction());

        addMoleButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/addMole.png")),
                "Add a workflow scene",
                new AddMoleSceneAction(""));
        removeMoleButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/removeMole.png")),
                "Remove the current workflow",
                new RemoveMoleSceneAction(""));
        removeAllMoleButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/removeAll.png")),
                "Remove all the workflows",
                new RemoveAllMoleSceneAction(""));

        buildButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/build.png")),
                "Build the workflow",
                new BuildExecutionAction(""));
        cleanAndBuildButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/cleanAndBuild.png")),
                "Clean and build the workflow",
                new CleanAndBuildExecutionAction(""));

        toolBar.add(detailedViewButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(addMoleButton.peer());
        toolBar.add(removeMoleButton.peer());
        toolBar.add(removeAllMoleButton.peer());
        toolBar.add(new JToolBar.Separator());
        toolBar.add(buildButton.peer());
        toolBar.add(cleanAndBuildButton.peer());
        toolBar.add(new JToolBar.Separator());
        toolBar.add(ExecutionBoard.peer());
        //  add(toolBar);
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(TabManager.tabbedPane().peer(), BorderLayout.CENTER);
        etc.close();
        repaint();
    }

    public void addPalette() {
        palette = PaletteSupport.createPalette();
        ic.add(palette);
    }

    public PaletteController getPalette() {
        return palette;
    }

    public void refresh(Boolean b) {
        ic.remove(palette);
        addPalette();
        repaint();
        updateMode(b);
    }

    public void updateMode(Boolean b) {
        buildButton.enabled_$eq(b);
        cleanAndBuildButton.enabled_$eq(b);
        ExecutionBoard.activate(!b);
        detailedViewButton.setEnabled(b);
        if (b) {
            etc.close();
        } else {
            etc.open();
        }
        repaint();
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
