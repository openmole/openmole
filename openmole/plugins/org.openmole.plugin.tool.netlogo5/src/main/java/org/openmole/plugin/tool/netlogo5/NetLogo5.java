/*
 * Copyright (C) 2011 Romain Reuillon
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
package org.openmole.plugin.tool.netlogo5;

import org.nlogo.OpenMOLEAdapter;
import org.nlogo.api.LogoList;
import org.openmole.plugin.tool.netlogo.NetLogo;

/**
 * @author Romain Reuillon
 */
public class NetLogo5 implements NetLogo {
    final private OpenMOLEAdapter adapter = new OpenMOLEAdapter();

    @Override
    public void open(String s, boolean b) throws Exception {
        adapter.open(s, b);
    }

    @Override
    public void command(String s) throws Exception {
        adapter.command(s);
    }

    @Override
    public boolean isNetLogoException(Throwable throwable) {
        return adapter.isNetLogoException(throwable);
    }

    @Override
    public Object report(String s) throws Exception {
        return adapter.report(s);
    }

    @Override
    public void setGlobal(String s, Object o) throws Exception {
        adapter.setGlobal(s, o);
    }

    @Override
    public void dispose() throws Exception {
        adapter.dispose();
    }

    @Override
    public String[] globals() {
        return adapter.globals();
    }

    @Override
    public String[] reporters() {
        return adapter.reporters();
    }

    @Override
    public ClassLoader getNetLogoClassLoader() {
        return adapter.getNetLogoClassLoader();
    }

    public static LogoList arrayToList(Object[] objects) {
        return OpenMOLEAdapter.arrayToList(objects);
    }
}
