/*
 *  Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.workflow.implementation.paint;

import java.awt.Dimension;
import javax.swing.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.router.RouterFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.openmole.ui.ide.commons.ApplicationCustomize;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class LabeledConnectionWidget extends ConnectionWidget {

    LabelWidget conditionLabel;

    public LabeledConnectionWidget(Scene scene, String condition) {
        super(scene);
        conditionLabel = new LabelWidget(scene, condition);
        conditionLabel.setBackground(ApplicationCustomize.getInstance().getColor(ApplicationCustomize.CONDITION_LABEL_BACKGROUND_COLOR));
        conditionLabel.setBorder(BorderFactory.createLineBorder(ApplicationCustomize.getInstance().getColor(ApplicationCustomize.CONDITION_LABEL_BORDER_COLOR), 2));
        conditionLabel.setOpaque(true);
        addChild(conditionLabel);
        setConstraint(conditionLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f);
        setMinimumSize(new Dimension(10, 25));
        setLabelVisible();
    }

    public void setConditionLabel(String cond) {
        conditionLabel.setLabel(cond);
        setLabelVisible();
    }

    private void setLabelVisible() {
        conditionLabel.setVisible(!conditionLabel.getLabel().isEmpty());
    }
}
