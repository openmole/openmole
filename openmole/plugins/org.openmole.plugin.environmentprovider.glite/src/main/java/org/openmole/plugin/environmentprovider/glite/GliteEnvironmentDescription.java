/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.plugin.environmentprovider.glite;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.execution.batch.BatchEnvironmentDescription;

public class GliteEnvironmentDescription extends BatchEnvironmentDescription<GliteEnvironment> {

    String voName;
    String vomsURL;
    String bdiiURL;

    public GliteEnvironmentDescription(String voName, String vomsURL, String bdiiURL) {
        super();
        this.voName = voName;
        this.bdiiURL = bdiiURL;
        this.vomsURL = vomsURL;
    }


    String getVomsURL() {
        return vomsURL;
    }

    String getVoName() {
        return voName;
    }

    void setVoName(String voName) {
        this.voName = voName;
    }

    String getBDII() {
        return bdiiURL;
    }

    void setBDII(String bdii) {
        bdiiURL = bdii;
    }
   
    @Override
    public GliteEnvironment createEnvironment() throws InternalProcessingError {
        return new GliteEnvironment(this);
    }

    @Override
    public boolean equals(Object obj) {
        //Logger.getLogger(GliteEnvironmentDescription.class.getName()).info(this.toString() + " " + obj.toString() + "O");

        if (obj == null) {
            return false;
        }


       // Logger.getLogger(GliteEnvironmentDescription.class.getName()).info(this.toString() + " " + obj.toString() + "1");
        if (getClass() != obj.getClass()) {

            return false;
        }

       // Logger.getLogger(GliteEnvironmentDescription.class.getName()).info(this.toString() + " " + obj.toString() + "2");
        final GliteEnvironmentDescription other = (GliteEnvironmentDescription) obj;
        if ((this.voName == null) ? (other.voName != null) : !this.voName.equals(other.voName)) {
            return false;
        }

       // Logger.getLogger(GliteEnvironmentDescription.class.getName()).info(this.toString() + " " + obj.toString() + "3");
        if ((this.vomsURL == null) ? (other.vomsURL != null) : !this.vomsURL.equals(other.vomsURL)) {
            return false;
        }

       // Logger.getLogger(GliteEnvironmentDescription.class.getName()).info(this.toString() + " " + obj.toString() + "4");
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.voName != null ? this.voName.hashCode() : 0);
        hash = 71 * hash + (this.vomsURL != null ? this.vomsURL.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return GliteEnvironmentDescription.class.getName() + ": " + voName + "," + vomsURL;
    }



}
