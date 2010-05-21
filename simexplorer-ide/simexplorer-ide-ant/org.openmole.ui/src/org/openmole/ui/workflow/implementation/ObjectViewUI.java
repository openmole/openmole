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
import org.openmole.ui.workflow.provider.WidgetSetlectionProvider;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class ObjectViewUI extends Widget implements IObjectViewUI {

    protected MoleScene scene;
    private final Color defaultBorderColor;
    private final Color defaultBackgroundColor;

    protected Image backgroundImage= null;
    protected Color borderColor;
    protected Color backgroundColor;

    public ObjectViewUI(MoleScene scene,
            Color defaultBackgroundColor,
            Color defaultBorderColor) {
        super(scene);
        this.scene = scene;
        this.defaultBorderColor = defaultBorderColor;
        this.defaultBackgroundColor = defaultBackgroundColor;
        this.borderColor = defaultBorderColor;
        this.backgroundColor = defaultBackgroundColor;
        createActions(MoleScene.MOVE).addAction (ActionFactory.createMoveAction());
        getActions().addAction(ActionFactory.createSelectAction(new WidgetSetlectionProvider()));
    }

    public ObjectViewUI(MoleScene scene,
            Color defaultBackgroundColor,
            Color defaultBorderColor,
            Image image) {
        this(scene, defaultBackgroundColor, defaultBorderColor);
        this.backgroundImage = image;
    }



    @Override
    public void setBackgroundColor(Color col) {
        this.backgroundColor = col;
    }

    @Override
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setBorderColor(Color col) {
        this.borderColor = col;
    }

    @Override
    public void setDefaultBackgroundColor() {
        borderColor = defaultBorderColor;
    }

    @Override
    public void setDefaultBorderColor() {
        backgroundColor = defaultBackgroundColor;
    }

    @Override
    public Color getBorderColor() {
        return borderColor;
    }

    @Override
    public Image getBackgroundImage() {
        return backgroundImage;
    }
}
