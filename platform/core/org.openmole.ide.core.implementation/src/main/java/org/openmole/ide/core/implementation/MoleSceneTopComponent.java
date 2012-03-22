/*//GEN-LINE:initComponents
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the termoleScene of the GNU Affero General Public License as published by
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
import java.awt.Color;
import java.util.Set;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.WindowManager;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openmole.ide.core.implementation.control.TopComponentsManager;
import org.openide.awt.ActionID;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openmole.ide.core.implementation.action.ConnectionVsDataChannelAction;
import org.openmole.ide.core.implementation.action.BuildExecutionAction;
import org.openmole.ide.core.implementation.action.CleanAndBuildExecutionAction;
import org.openmole.ide.core.implementation.action.StartMoleAction;
import org.openmole.ide.core.implementation.action.StopMoleAction;
import org.openmole.ide.core.model.control.IMoleComponent;
import org.openmole.ide.core.implementation.dialog.DialogFactory;
import org.openmole.ide.core.implementation.data.CheckData;
import org.openmole.ide.core.implementation.panel.ConceptMenu;
import org.openmole.ide.core.model.workflow.IMoleScene;
import org.openmole.ide.misc.widget.ToolBarButton;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.openmole.ide.core.implementation//MoleScene//EN",
autostore = false)
@TopComponent.Description(preferredID = "MoleSceneTopComponent", iconBase = "img/addMole.png", persistenceType = TopComponent.PERSISTENCE_NEVER)
//iconBase="SET/PATH/TO/ICON/HERE", 
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "File", id = "org.openmole.ide.core.implementation.MoleSceneTopComponent")
//@ActionReferences({
// @ActionReference(path = "Menu/File", position = 30),
// @ActionReference(path = "Toolbars/File", position = 10),
// @ActionReference(path = "Shortcuts", name = "D-N")
//})
@TopComponent.OpenActionRegistration(displayName = "Add Mole")
public final class MoleSceneTopComponent extends CloneableTopComponent {

    private ToolBarButton buildButton;
    private ToolBarButton cleanAndBuildButton;
    private ToolBarButton startButton;
    private ToolBarButton stopButton;
    private JToggleButton connectionVsDataChannelButton;
    private final InstanceContent ic = new InstanceContent();
    private ExecutionTopComponent etc = ((ExecutionTopComponent) WindowManager.getDefault().findTopComponent("ExecutionTopComponent"));
    private IMoleScene moleScene;
    private IMoleComponent moleComponent;

    public MoleSceneTopComponent() {
    }

    public void initialize(IMoleScene ms,
            IMoleComponent mc) {
        moleScene = ms;
        // to be aware of the current edited mole
        associateLookup(Lookups.singleton(ms));
        moleComponent = mc;
        initComponents();
        setToolTipText(NbBundle.getMessage(MoleSceneTopComponent.class, "HINT_MoleSceneTopComponent"));

        connectionVsDataChannelButton = new JToggleButton(new ImageIcon(ImageUtilities.loadImage("img/connectMode.png")));
        connectionVsDataChannelButton.addActionListener(new ConnectionVsDataChannelAction());
        connectionVsDataChannelButton.setSelected(true);
        
        buildButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/build.png")),
                "Build the workflow",
                new BuildExecutionAction(this));
        cleanAndBuildButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/cleanAndBuild.png")),
                "Clean and build the workflow",
                new CleanAndBuildExecutionAction(this));
        startButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/startExe.png")), "Start the workflow",
                new StartMoleAction());
        stopButton = new ToolBarButton(new ImageIcon(ImageUtilities.loadImage("img/stopExe.png")), "Stop the workflow",
                new StopMoleAction());

        toolBar.add(connectionVsDataChannelButton);
        toolBar.add(buildButton.peer());
        toolBar.add(cleanAndBuildButton.peer());
        toolBar.add(startButton.peer());
        toolBar.add(stopButton.peer());
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        setDisplayName(moleScene.manager().name());
        setName(moleScene.manager().name());
        add(moleScene.graphScene().createView(), BorderLayout.CENTER);
        etc.close();
        repaint();
    }

    public IMoleComponent getMoleComponent() {
        return moleComponent;
    }

    public IMoleScene getMoleScene() {
        return moleScene;
    }

    public void refresh(Boolean b) {
        repaint();
        buildMode(b);
    }

    public void buildMode(Boolean b) {
        buildButton.visible_$eq(b);
        cleanAndBuildButton.visible_$eq(b);
        startButton.visible_$eq(!b);
        stopButton.visible_$eq(!b);
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
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
    }// </editor-fold>                        
    // Variables declaration - do not modify                     
    private javax.swing.JToolBar toolBar;
    // End of variables declaration                   

    public Set<TopComponent> getOpened() {
        return getRegistry().getOpened();
    }
    
    @Override
    public void componentActivated() {
        TopComponentsManager.setCurrentMoleSceneTopComponent(this);
        refresh(moleScene.isBuildScene());
        TopComponentsManager.displayExecutionView(moleComponent);
        if (!moleScene.isBuildScene()) {
            etc.open();
        }
        else {
        CheckData.checkMole(moleScene.manager());}
    }

    @Override
    public void componentOpened() {
        TopComponentsManager.setCurrentMoleSceneTopComponent(this);
    }

    @Override
    public boolean canClose() {
        if (moleScene.isBuildScene()) {
            return true;
        } else {
            return DialogFactory.closeExecutionTab(moleComponent);
        }
    }

    @Override
    public void componentClosed() {
        TopComponentsManager.stopAndCloseExecutions(moleComponent);
        TopComponentsManager.noCurrentMoleSceneTopComponent();
       // TopComponentsManager.removeTopComponent(moleComponent);
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
