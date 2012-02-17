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

package org.openmole.ide.core.implementation.panel

import org.openide.util.ImageUtilities
import org.openmole.ide.misc.widget.PopupMenu
import scala.swing.Menu
import scala.swing.Button
import scala.swing.event.ButtonClicked

class PopupToolBarPresenter(t: String, menu: Menu) extends Button(t){
val popup = new PopupMenu {contents += menu}
icon = ImageUtilities.loadImageIcon("org/openide/awt/resources/arrow.png", true)
listenTo(mouse.clicks)
    reactions += {
      case x:ButtonClicked => popup.show(this, 0, size.height)
    }
}
//
//PopupToolbarPresenter extends JButton implements ActionListener {
//
//    private final JPopupMenu popup;
//
//    PopupToolbarPresenter(String text, JMenu menu) {
//        this.setText(text);
//        //Set the icon of the button:
//        this.setIcon(ImageUtilities.loadImageIcon("org/openide/awt/resources/arrow.png", true));
//        //Create a popup:
//        popup = new JPopupMenu();
//        popup.add(menu);
//        //Show the button when the popup is clicked:
//        this.addActionListener(PopupToolbarPresenter.this);
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        popup.show(PopupToolbarPresenter.this, 0, PopupToolbarPresenter.this.getHeight());
//    }