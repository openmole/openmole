/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
/**
 *
 * @author Mathieu leclaire <mathieu.leclaire at openmole.org>
 */
public class MenuToogleButton extends JToggleButton {

    private final Icon i = new MenuArrowIcon();
    private JPopupMenu pop;

    public MenuToogleButton() {
        this("", null);
    }

    public MenuToogleButton(Icon icon) {
        this("", icon);
    }

    public MenuToogleButton(String text) {
        this(text, null);
    }

    public MenuToogleButton(String text, Icon icon) {
        super();
        Action a = new AbstractAction(text) {

            @Override
            public void actionPerformed(ActionEvent ae) {
                MenuToogleButton b = (MenuToogleButton) ae.getSource();
                if (pop != null) {
                    pop.show(b, 0, b.getHeight());
                }
            }
        };
        a.putValue(Action.SMALL_ICON, icon);
        setAction(a);
        setFocusable(false);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4 + i.getIconWidth()));

    }

    public void setPopupMenu(final JPopupMenu pop) {
        this.pop = pop;
        pop.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                setSelected(false);
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = getSize();
        Insets ins = getInsets();
        int x = dim.width - ins.right;
        int y = ins.top + (dim.height - ins.top - ins.bottom - i.getIconHeight()) / 2;
        i.paintIcon(this, g, x, y);
    }

    class MenuArrowIcon implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(Color.BLACK);
            g2.translate(x, y);
            g2.drawLine(2, 3, 6, 3);
            g2.drawLine(3, 4, 5, 4);
            g2.drawLine(4, 5, 4, 5);
            g2.translate(-x, -y);
        }

        @Override
        public int getIconWidth() {
            return 9;
        }

        @Override
        public int getIconHeight() {
            return 9;
        }
    }
    
}
