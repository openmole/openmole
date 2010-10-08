/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.ui.ide.control.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class IOContainer extends JPanel implements ITaskSettingContainer {

    private JScrollPane listScrollPane;
    private String[] imageNames = {"Bird", "Cat"};
    private JList list = new JList(imageNames);


    public IOContainer() {
        super(new BorderLayout());
        list.addListSelectionListener(this);


        listScrollPane = new JScrollPane(list);

        add(new JLabel("Task input"), BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);

        setMinimumSize(new Dimension(getWidth(),200));
        addItem();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // Ask parent to paint background.
        g.setColor(Color.orange);
        g.fillRect(0, 0, getWidth(),getHeight());
    }

    public void addItem() {
        String[] uu = {"Birdee", "Cateiei"};
        list.add(new JList(uu));
    }

    @Override
    public void valueChanged(ListSelectionEvent lse) {
        System.out.println("selected ");
    }
}
