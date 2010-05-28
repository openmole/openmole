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
package org.openmole.ui.workflow.implementation;

import java.awt.Color;
import java.awt.Image;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.ui.workflow.model.IObjectViewUI;
import java.util.Properties;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class ObjectViewUI extends Widget implements IObjectViewUI {

    protected MoleScene scene;
    protected Properties properties;

    public ObjectViewUI(MoleScene scene,
                        Properties properties){
        super(scene);
        this.scene = scene;
        this.properties = properties;
        createActions(MoleScene.MOVE).addAction (ActionFactory.createMoveAction());
    }

    @Override
    public Color getBackgroundColor() {
        return getColor(PropertyManager.BG_COLOR);
    }

    @Override
    public Color getBorderColor() {
        return getColor(PropertyManager.BORDER_COLOR);
    }

    private Color getColor(String colorString){
        String[] colors = properties.getProperty(colorString).split(",");
        return new Color(Integer.parseInt(colors[0]),
                         Integer.parseInt(colors[1]),
                         Integer.parseInt(colors[2]));
    }

    @Override
    public Image getBackgroundImage() {
      return ImageUtilities.loadImage(properties.getProperty(PropertyManager.BG_IMG));
    }
}
