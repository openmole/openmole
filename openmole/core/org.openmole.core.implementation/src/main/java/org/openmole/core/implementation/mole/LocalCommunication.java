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
package org.openmole.core.implementation.mole;

import java.util.Collection;
import org.openmole.core.implementation.tools.RegistryWithTicket;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.transition.ITransition;
import org.openmole.core.model.data.IDataChannel;
import org.openmole.core.model.tools.IRegistryWithTicket;
import org.openmole.core.model.mole.ILocalCommunication;
import org.openmole.core.model.transition.IAggregationTransition;

public class LocalCommunication implements ILocalCommunication {

    IRegistryWithTicket<ITransition, IContext> transitionRegistry = new RegistryWithTicket<ITransition, IContext>();
    IRegistryWithTicket<IAggregationTransition, Collection<IContext>> aggregationTransitionRegistry = new RegistryWithTicket<IAggregationTransition, Collection<IContext>>();
    IRegistryWithTicket<IDataChannel, IContext> dataChannetContextRegistry = new RegistryWithTicket<IDataChannel, IContext>();

    @Override
    public IRegistryWithTicket<ITransition, IContext> getTransitionRegistry() {
        return transitionRegistry;
    }

    @Override
    public IRegistryWithTicket<IDataChannel, IContext> getDataChannelRegistry() {
        return dataChannetContextRegistry;
    }

    @Override
    public IRegistryWithTicket<IAggregationTransition, Collection<IContext>> getAggregationTransitionRegistry() {
        return aggregationTransitionRegistry;
    }

}
