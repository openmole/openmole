/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.widget

import java.awt.Dimension
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.BoxPanel
import scala.swing.FileChooser
import scala.swing.FileChooser.Result.Approve
import scala.swing.Orientation
import scala.swing.TextField
import scala.swing.event.FocusGained

class ChooseFileTextField(filter: FileNameExtensionFilter, chooserTitle: String) extends BoxPanel(Orientation.Horizontal) {
  maximumSize = new Dimension(150,30)
  val tf = new TextField {
    reactions += {
      case FocusGained(tf,_,false) =>
        val fc = new FileChooser {
          fileFilter = filter
          title = chooserTitle
        }
        focusable = false
        if (fc.showDialog(this,"OK") == Approve) text = fc.selectedFile.getPath
        focusable = true
    }
  }
  contents.append(tf)
  listenTo(tf)
  
  def path = tf.text
}
