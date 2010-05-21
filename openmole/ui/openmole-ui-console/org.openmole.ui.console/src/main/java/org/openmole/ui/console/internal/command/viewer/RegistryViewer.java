/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.ui.console.internal.command.viewer;

import java.util.ArrayList;
import java.util.List;
import org.openmole.ui.console.internal.command.registry.Registry;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class RegistryViewer implements IViewer<Registry>{

    @Override
    public void view(Registry object, List<Object> args) {
        List<Object> toVisualize;

        switch(args.size()) {
            case 0:
                toVisualize = Registry.getInstance().getRegistred();
                break;
            case 1:
                if(Integer.class.isAssignableFrom(args.get(0).getClass())) {
                    toVisualize = new ArrayList<Object>(1);
                    toVisualize.add(Registry.getInstance().getRegistred((Integer) args.get(0)));
                } else {
                    toVisualize = Registry.getInstance().getRegistred((Class) args.get(0));
                }
                break;
            default:
                toVisualize = new ArrayList<Object>(1);
                toVisualize.add(Registry.getInstance().getRegistred((Class) args.get(0),(Integer) args.get(1)));
                break;
        }

        int i = 0;
        for(Object o: toVisualize) {
            System.out.println(i + " = " + o.toString());
            i++;
        }
    }

}
