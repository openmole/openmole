/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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


package org.openmole.plugin.tool.netlogo;

public interface NetLogo {
  public String[] globals() throws Exception;
  public String[] reporters() throws Exception;
  public void open(String script) throws Exception;
  void command(String cmd) throws Exception;
  boolean isNetLogoException(Throwable exception);
  Object report(String variable) throws Exception;
  void setGlobal(String variable,Object value) throws Exception;
  //void setRandomSeed(int seed) throws Exception;
  void dispose() throws Exception;
  ClassLoader getNetLogoClassLoader();
}
