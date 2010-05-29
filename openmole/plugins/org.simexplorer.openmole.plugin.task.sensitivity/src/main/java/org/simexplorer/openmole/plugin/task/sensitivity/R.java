/*
 *  Copyright (C) 2010 Cemagref
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
package org.simexplorer.openmole.plugin.task.sensitivity;

import org.nuiton.j2r.REngine;
import org.nuiton.j2r.RException;
import org.nuiton.j2r.RProxy;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class R {

    private REngine rengine;
    private static R instance;

    private R() throws RException {
        rengine = new RProxy();
    }

    public static R getInstance() throws RException {
        if (instance == null) {
            instance = new R();
        }
        return instance;
    }

    public static REngine getRengine() throws RException {
        return getInstance().rengine;
    }

    public static Object eval(String command) throws RException {
        return getRengine().eval(command);
    }

    public static void voidEval(String command) throws RException {
        getRengine().voidEval(command);
    }
}
