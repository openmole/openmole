package org.openmole.ui.control;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.*;
import org.openmole.ui.workflow.action.EnableTaskDetailedView;
import org.openmole.ui.workflow.action.MoveOrDrawTransitionAction;
import org.openmole.ui.workflow.implementation.MoleScene;

/**
 *
 * @author mathieu
 */
public class ControlPanel extends JPanel{
    JTabbedPane moleTabbedPane;
    JSplitPane splitPaneV;
    JTabbedPane tableTabbedPane;
    JPanel controlPane;

     JToggleButton moveButton = new JToggleButton();
     JToggleButton detailedViewButton = new JToggleButton();

    public ControlPanel(MoleScene moleScene) throws IllegalArgumentException, IllegalAccessException {
        super();

        BorderLayout layout = new BorderLayout();
        setLayout(layout);

        moleTabbedPane = new JTabbedPane();
        tableTabbedPane = new JTabbedPane();

    /*    splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                moleTabbedPane,
                tableTabbedPane);
*/
        controlPane = new JPanel(new GridLayout(0,1));
        controlPane.setPreferredSize(new Dimension(250,this.getHeight()));
        controlPane.setBorder(BorderFactory.createCompoundBorder(
                              BorderFactory.createTitledBorder("Styled Text"),
                              BorderFactory.createEmptyBorder(5,5,5,5)));

        moveButton.addActionListener(new MoveOrDrawTransitionAction());
        moveButton.setSelected(true);
        moveButton.setText("Draw connections");

        detailedViewButton.addActionListener(new EnableTaskDetailedView(moleScene));
        detailedViewButton.setText("Detailed view");
        
        MoleScenesManager.getInstance().setScenesMovable(true);

        controlPane.add(moveButton);
        controlPane.add(detailedViewButton);

        
        add(controlPane, BorderLayout.WEST);
       // add(splitPaneV,BorderLayout.CENTER);
    }

    public void addMoleView(String tabTitle,
                            Component moleView) {
        moleTabbedPane.add(tabTitle,moleView);
    }

    public void switchTableTabbedPane(JTabbedPane tTPane){
        splitPaneV.setBottomComponent(tTPane);
        splitPaneV.setDividerLocation(0.2);
    }
}
