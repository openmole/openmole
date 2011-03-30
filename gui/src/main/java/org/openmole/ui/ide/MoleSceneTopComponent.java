/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ui.ide;

import java.awt.BorderLayout;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
//import org.netbeans.ProxyClassLoader;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openmole.ui.ide.control.MoleScenesManager;
import org.openmole.ui.ide.control.TaskSettingsManager;
import org.openmole.ui.ide.dialog.PrototypeManager;
import org.openmole.ui.ide.dialog.SamplingManager;
import org.openmole.ui.ide.dialog.TaskManager;
import org.openmole.ui.ide.palette.PaletteSupport;
import org.openmole.ui.ide.workflow.action.AddMoleSceneAction;
import org.openmole.ui.ide.workflow.action.EnableTaskDetailedView;
import org.openmole.ui.ide.workflow.action.ManageEntityAction;
import org.openmole.ui.ide.workflow.action.RemoveAllMoleSceneAction;
import org.openmole.ui.ide.workflow.action.RemoveMoleSceneAction;
import org.netbeans.spi.palette.PaletteController;
import org.openmole.misc.pluginmanager.PluginManager;
import org.openmole.plugin.task.netlogo.NetLogoTask;
import org.openmole.ui.ide.commons.ApplicationCustomize;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.openmole.ui.ide//MoleScene//EN",
autostore = false)
public final class MoleSceneTopComponent extends TopComponent {

    private static MoleSceneTopComponent instance;
    private PaletteController palette;
    private final InstanceContent ic = new InstanceContent();
    /** path to the icon used by the component and its open action */
    private static final String PREFERRED_ID = "MoleSceneTopComponent";

    public MoleSceneTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(MoleSceneTopComponent.class, "CTL_MoleSceneTopComponent"));
        setToolTipText(NbBundle.getMessage(MoleSceneTopComponent.class, "HINT_MoleSceneTopComponent"));

//        System.out.println("avant pm");
//        PluginManager.load("/home/mathieu/Work/cogit/openmole-core/openmole/plugins/org.openmole.plugin.task.netlogo/target/org.openmole.plugin.task.netlogo-0.3-SNAPSHOT.jar");
//        System.out.println("apres pm");
//        PluginManager.getPluginForClass(NetLogoTask.class);
//        System.out.println("après pm " + PluginManager.getPluginForClass(NetLogoTask.class).getAbsolutePath());

        MoleScenesManager.getInstance().setTabbedPane(tabbedPane);
        TaskSettingsManager.getInstance().setTabbedPane(tabbedPane);
        MoleScenesManager.getInstance().display(MoleScenesManager.getInstance().addMoleScene());

        palette = PaletteSupport.createPalette();

        associateLookup(new AbstractLookup(ic));
        ic.add(palette);

        JToggleButton detailedViewButton = new JToggleButton("Detailed view");
        detailedViewButton.addActionListener(new EnableTaskDetailedView());

        JButton newPrototypeButton = new JButton("Prototypes");
        newPrototypeButton.addActionListener(new ManageEntityAction(new PrototypeManager()));

        JButton newTaskButton = new JButton("Tasks");
        newTaskButton.addActionListener(new ManageEntityAction(new TaskManager()));

        JButton newSamplingButton = new JButton("Samplings");
        newSamplingButton.addActionListener(new ManageEntityAction(new SamplingManager()));

        JPopupMenu molePopupMenu = new JPopupMenu("Mole");

        JMenuItem itAdd = new JMenuItem("Add");
        itAdd.addActionListener(new AddMoleSceneAction());

        JMenuItem itRem = new JMenuItem("Remove");
        itRem.addActionListener(new RemoveMoleSceneAction());

        JMenuItem itRemAll = new JMenuItem("Remove All");
        itRemAll.addActionListener(new RemoveAllMoleSceneAction());

        molePopupMenu.add(itAdd);
        molePopupMenu.add(itRem);
        molePopupMenu.add(itRemAll);

        MenuToogleButton bMole = new MenuToogleButton("Mole");
        bMole.setPopupMenu(molePopupMenu);

        toolBar.add(detailedViewButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(newPrototypeButton);
        toolBar.add(newTaskButton);
        toolBar.add(newSamplingButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(bMole);
        //  add(toolBar);
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        //toolBar.setVisible(true);


    }

    public void refreshPalette() {
        ic.remove(palette);
        palette = PaletteSupport.createPalette();
        ic.add(palette);
        repaint();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tabbedPane = new javax.swing.JTabbedPane();
        toolBar = new javax.swing.JToolBar();

        jScrollPane1.setViewportView(tabbedPane);

        toolBar.setRollover(true);
        toolBar.setMaximumSize(new java.awt.Dimension(30, 15));
        toolBar.setMinimumSize(new java.awt.Dimension(30, 15));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jScrollPane1, toolBar});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized MoleSceneTopComponent getDefault() {
        if (instance == null) {
            instance = new MoleSceneTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the MoleSceneTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized MoleSceneTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(MoleSceneTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof MoleSceneTopComponent) {
            return (MoleSceneTopComponent) win;
        }
        Logger.getLogger(MoleSceneTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

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

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }
}
