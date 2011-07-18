/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import java.awt.geom.AffineTransform
import org.netbeans.api.visual.action.AcceptProvider
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.netbeans.api.visual.action.ConnectorState
import java.awt.Graphics
import java.awt.Graphics2D

abstract class DnDProvider(molescene: MoleScene) extends AcceptProvider{

  override def isAcceptable(widget: Widget, point: Point,transferable:Transferable): ConnectorState= {
    val view= molescene.getView
    val visRect = view.getVisibleRect
    val g2= view.getGraphics.asInstanceOf[Graphics2D]
    view.paintImmediately(visRect.x, visRect.y, visRect.width, visRect.height)
    
    g2.drawImage(molescene.getImageFromTransferable(transferable),
                               AffineTransform.getTranslateInstance(point.getLocation().getX(),point.getLocation().getY()),
                               null)
    return ConnectorState.ACCEPT
  }
}
//abstract class DnDProvider implements AcceptProvider {
//    protected MoleScene scene; 
//
//    public DnDProvider(MoleScene molescene) {
//        this.scene = molescene;
//    }
//
//    @Override
//    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
//        Image dragImage = scene.getImageFromTransferable(transferable);
//        JComponent view = scene.getView();
//        Graphics2D g2 = (Graphics2D) view.getGraphics();
//        Rectangle visRect = view.getVisibleRect();
//        view.paintImmediately(visRect.x, visRect.y, visRect.width, visRect.height);
//        g2.drawImage(dragImage,
//                AffineTransform.getTranslateInstance(point.getLocation().getX(),
//                point.getLocation().getY()),
//                null);
//        return ConnectorState.ACCEPT;
//    }
//}