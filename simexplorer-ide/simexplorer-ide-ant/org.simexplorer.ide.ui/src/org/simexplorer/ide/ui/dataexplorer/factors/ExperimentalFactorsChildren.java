/*
 *  Copyright © 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */

package org.simexplorer.ide.ui.dataexplorer.factors;

import java.util.Collection;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openmole.core.implementation.plan.Factor;
import org.openmole.core.model.plan.IFactor;

class ExperimentalFactorsChildren extends Children.Keys<IFactor<?,?>> {

    private Collection<? extends IFactor<?,?>> factors;
        
    public ExperimentalFactorsChildren(Collection<? extends IFactor<?,?>> factors) {
        this.factors = factors;
        setKeys(factors);
    }

    @Override
    protected Node[] createNodes(IFactor<?,?> factor) {
        return new Node[] {new ExperimentalFactorNode(factor, this)};
    }
    
    public void removeFactor(Factor factor) {
        factors.remove(factor);
        setKeys(factors);
    }

}
