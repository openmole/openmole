/*
 *  Copyright (C) 2010 Cemagref
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

package org.simexplorer.ide.ui.applicationexplorer;

import org.openide.util.lookup.ServiceProvider;
import org.simexplorer.ui.ide.workflow.model.ApplicationsExplorerService;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
@ServiceProvider(service=ApplicationsExplorerService.class)
public class ApplicationsExplorer implements ApplicationsExplorerService {

    @Override
    public void setApplication(ExplorationApplication application) {
        ApplicationsTopComponent.findInstance().setApplication(application);
    }

    @Override
    public ExplorationApplication getExplorationApplication() {
        return ApplicationsTopComponent.findInstance().getExplorationApplication();
    }

}
