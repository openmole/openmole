/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.example;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.openide.modules.ModuleInstall;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.control.ControlPanel;
import org.openmole.ui.control.MoleScenesManager;
import org.openmole.ui.control.TableMapping;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.implementation.Preferences;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@iscpif.fr>
 */
public class MoleExampleStarter extends ModuleInstall {

    @Override
    public void restored() {
        createAndShowGUI();
    }

    public static void createAndShowGUI() {

        try {
            TableMapping.getInstance().initialize();
            Preferences.getInstance().initialize();

            JFrame frame = new JFrame();
            MoleScene scene = new MoleScene();
            MoleScene scene2 = new MoleScene();

            MoleScenesManager.getInstance().addMoleScene(scene2);
            MoleScenesManager.getInstance().addMoleScene(scene);

            // SceneSupport.show(MoleSceneFactory.buildMoleScene(MoleExample.buildMole()));

            ControlPanel panel = ControlPanel.getInstance();
            panel.addMoleView("My mole!", scene.getView());
            panel.addMoleView("My second mole!", scene2.getView());

            frame.getContentPane().add(panel);
            frame.setSize(ApplicationCustomize.PANEL_WIDTH, ApplicationCustomize.PANEL_HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setVisible(true);
            scene2.build(MoleExample.buildMole2());
            scene.build(MoleExample.buildMole());
            

        } catch (Exception e) {
            Logger.getLogger(MoleExampleStarter.class.getName()).log(Level.SEVERE, "", e);
        }
    }
}
