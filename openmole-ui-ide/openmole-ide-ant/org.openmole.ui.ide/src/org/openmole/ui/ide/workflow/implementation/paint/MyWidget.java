/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import java.awt.Color;
import java.awt.Container;
import org.netbeans.api.visual.widget.*;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.workflow.implementation.MoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MyWidget extends Widget {

    private Color backgroundCol;
    protected Color borderCol;
    private Image backgroundImaqe;
    protected Rectangle bodyArea = new Rectangle();
    protected Rectangle widgetArea = new Rectangle();
    private Rectangle titleArea = new Rectangle();
    private boolean title = false;
    private boolean image = false;
    private String titleString;
  //  private Container dataTableContainer = new Container();
    protected int taskWidth = ApplicationCustomize.TASK_CONTAINER_WIDTH;
    protected int taskImageOffset = ApplicationCustomize.TASK_IMAGE_WIDTH_OFFSET;
    protected MoleScene scene;

    public MyWidget(MoleScene scene,
                    Color col) {
        super(scene);
        this.backgroundCol = col;
        this.scene = scene;
        setWidthHint();
     //   dataTableContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setWidthHint() {
        if (scene.isDetailedView()) {
            taskWidth = ApplicationCustomize.EXPANDED_TASK_CONTAINER_WIDTH;
            taskImageOffset = ApplicationCustomize.EXPANDED_TASK_IMAGE_WIDTH_OFFSET;
        }
        else {
            taskWidth = ApplicationCustomize.TASK_CONTAINER_WIDTH;
            taskImageOffset = ApplicationCustomize.TASK_IMAGE_WIDTH_OFFSET;
        }

        Rectangle bodyrect = new Rectangle(0, 0,
                taskWidth,
                ApplicationCustomize.TASK_CONTAINER_HEIGHT);

        Rectangle widgetrect = new Rectangle(-8, -1,
                taskWidth + 16,
                ApplicationCustomize.TASK_CONTAINER_HEIGHT + 2);
        bodyArea.setBounds(bodyrect);
        widgetArea.setBounds(widgetrect);
        titleArea.setBounds(new Rectangle(0, 0,
                            taskWidth,
                            ApplicationCustomize.TASK_TITLE_HEIGHT));
        setPreferredBounds(widgetArea);
        revalidate();
        repaint();
    }

    public MyWidget(MoleScene scene,
            Color backgroundCol,
            Color borderCol) {
        this(scene, backgroundCol);
        this.borderCol = borderCol;
    }

    public MyWidget(MoleScene scene,
            Color backgroundCol,
            Color borderCol,
            Image backgroundImaqe) {
        this(scene, backgroundCol, borderCol);
        this.image = true;
        this.backgroundImaqe = backgroundImaqe;
    }

    //private void enlargeWidgetArea(Rectangle addedArea) {
    protected void enlargeWidgetArea(int y,
            int height) {
        //widgetArea.height += addedArea.height;
        //widgetArea.y -= addedArea.y;
        widgetArea.height += height;
        widgetArea.y -= y;
    }

    public void setBackgroundCol(Color backgroundCol) {
        this.backgroundCol = backgroundCol;
    }

    public void setBackgroundImaqe(Image backgroundImaqe) {
        this.backgroundImaqe = backgroundImaqe;
    }

    public void setBorderCol(Color borderCol) {
        this.borderCol = borderCol;
    }

    public void setTitle(String title) {
        this.titleString = title;
    }

    public String getTitleString() {
        return titleString;
    }

    public void addTitle(String titleString) {
        titleArea = new Rectangle(0, 0,
                taskWidth,
                ApplicationCustomize.TASK_TITLE_HEIGHT);

        this.title = true;
        this.titleString = titleString;

        setPreferredBounds(widgetArea);
    }

    @Override
    protected void paintWidget() {
        Graphics2D graphics = getGraphics();
        graphics.setColor(backgroundCol);
        graphics.fill(bodyArea);

        graphics.setColor(borderCol);

        if (title) {
            graphics.fill(titleArea);
            graphics.setColor(Color.WHITE);
            graphics.drawString(titleString, 10, 15);
        }

        if (image) {
            graphics.drawImage(backgroundImaqe,
                    taskImageOffset,
                    ApplicationCustomize.TASK_IMAGE_HEIGHT_OFFSET,
                    ApplicationCustomize.TASK_IMAGE_WIDTH,
                    ApplicationCustomize.TASK_IMAGE_HEIGHT,
                    backgroundCol,
                    new Container());
        }
    }
}
