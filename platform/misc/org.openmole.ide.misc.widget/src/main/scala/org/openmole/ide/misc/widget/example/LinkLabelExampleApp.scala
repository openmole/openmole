/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.widget.example

import java.awt.Dimension
import org.openmole.ide.misc.widget.{PluginPanel,ContentAction,MainLinkLabel,LinkLabel,EditableLinkLabel,ImageLinkLabel}
import scala.collection.JavaConversions._
import scala.swing.{ComboBox,Action,MainFrame,SimpleSwingApplication}
import javax.swing.JComboBox

object LinkLabelExampleApp extends SimpleSwingApplication
{
  def top = new MainFrame {
    title = "Link Label Demo"
    contents = new PluginPanel(""){
      contents +=new MainLinkLabel("Edit",
                                   new Action(""){def apply = println("My main link !")})
      contents +=new LinkLabel("My hyper label ",
                               new Action(""){def apply = println("My link !")})
      
      val li = List(new ContentAction("one",new Fake){override def apply = content.fakemethod(title)},new ContentAction("two",new Fake){def apply = content.fakemethod(title)})
      contents += new EditableLinkLabel(li.head,li)
      contents += new ImageLinkLabel("img/add.png",30,30,new Action(""){def apply = println("My image link !")} )
    }
    size = new Dimension(250,200)
  }
  
  class Fake {
    def fakemethod(s: String) = println("fake method from " + s)
  }
}