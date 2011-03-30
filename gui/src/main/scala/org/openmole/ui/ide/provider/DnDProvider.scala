/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import java.awt.geom.AffineTransform
import org.netbeans.api.visual.action.AcceptProvider
import org.netbeans.api.visual.widget.Widget
import org.openmole.ui.ide.workflow.implementation.MoleScene
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