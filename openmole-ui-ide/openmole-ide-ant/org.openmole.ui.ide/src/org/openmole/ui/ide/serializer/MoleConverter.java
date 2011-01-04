/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.serializer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.Exceptions;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.implementation.capsule.Capsule;
import org.openmole.core.implementation.mole.Mole;
import org.openmole.core.model.capsule.IGenericCapsule;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.transition.IGenericTransition;
import org.openmole.core.model.transition.ISlot;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class MoleConverter implements Converter {

    private Map<Object, Integer> slotmapping;
    private Collection<IPrototype> prototypes = new ArrayList<IPrototype>();

    public MoleConverter() {
        this.slotmapping = new HashMap<Object, Integer>();
    }

    public Collection<IPrototype> getPrototypes() {
        return prototypes;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(Mole.class);

    }

    @Override
    public void marshal(Object o, final HierarchicalStreamWriter writer, MarshallingContext mc) {
        final Mole mole = (Mole) o;
        try {
            mole.visit(new IVisitor<IGenericCapsule>() {

                int slotcount = 0;

                @Override
                public void action(IGenericCapsule t) throws Throwable {
                    //Capsule
                    writer.startNode("capsule");
                    writer.addAttribute("start", t.equals(mole.root()) ? "true" : "false");
                    writer.addAttribute("impl", t.getClass().toString());


                        System.out.println("-- task is defined "+ t.task().isDefined() );
                    //Task
                    if (t.task().isDefined()) {
                        writer.startNode("task");
                        writer.addAttribute("name", t.task().get().name());
                        writer.addAttribute("impl", t.task().get().getClass().toString());
                        writer.endNode();
                    }

                    //Input slot
                    for (scala.collection.Iterator<ISlot> slots = t.intputSlots().iterator(); slots.hasNext();) {
                        ISlot slot = slots.next();
                        slotcount++;

                        writer.startNode("islot");
                        writer.addAttribute("id", String.valueOf(slotcount));
                        writer.endNode();

                        slotmapping.put(slot, slotcount);
                    }

                    //Output slot
                    slotcount++;
                    writer.startNode("oslot");
                    writer.addAttribute("id", String.valueOf(slotcount));
                    writer.endNode();

                    slotmapping.put(t, slotcount);
                    writer.endNode();
                }
            });

            for (scala.collection.Iterator<IGenericCapsule> caps = mole.capsules().iterator(); caps.hasNext();) {
                for (scala.collection.Iterator<IGenericTransition> trans = caps.next().outputTransitions().iterator(); trans.hasNext();) {
                    IGenericTransition t = trans.next();
                    writer.startNode("transition");
                    writer.addAttribute("source", slotmapping.get(t.start()).toString());
                    writer.addAttribute("target", slotmapping.get(t.end()).toString());
                    writer.endNode();
                }
            }

        } catch (Throwable ex) {
            Exceptions.printStackTrace(ex);
        }




    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        return new Mole(new Capsule());
    }
}
