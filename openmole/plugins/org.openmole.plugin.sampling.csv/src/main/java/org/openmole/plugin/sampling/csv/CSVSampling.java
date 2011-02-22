/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.plugin.sampling.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import scala.collection.Iterator;

import au.com.bytecode.opencsv.CSVReader;

import org.openmole.core.model.data.IPrototype;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.data.DataSet;
import org.openmole.core.implementation.data.Variable;
import org.openmole.core.implementation.sampling.Sampling;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IContext;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.sampling.ISampling;
import scala.collection.Iterable;
import scala.collection.JavaConversions;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 *
 * The CSVPlan enables to generates a design of experiment from a CSV file.
 * Each line of the file describes an experimentation. It is possible to restrict
 * the number of parameters to be used. Actually all the parameters to be taken into account
 * have to be specified using the method addColumn()
 */
public class CSVSampling implements ISampling {

    final private File csvFile;
    final private Map<IPrototype<? extends File>, File> pathMapping = new HashMap<IPrototype<? extends File>, File>();
    final private Map<String, IPrototype> prototypes = new TreeMap<String, IPrototype>();

    /**
     * Creates an intstance of CSVPlan.
     *
     * @param csvFilePath, the path of the CSV file as a String
     */
    public CSVSampling(String csvFilePath) {
        this(new File(csvFilePath));
    }

    /**
     * Creates an intstance of CSVPlan.
     *
     * @param csvFilePath, the path of the CSV file as a File
     */
    public CSVSampling(File csvFile) {
        this.csvFile = csvFile;
    }

    /**
     * Adds a prototype to be takken into account in the DoE
     *
     * @param proto, the prototyde to be added
     */
    public void addColumn(IPrototype proto) {
        prototypes.put(proto.name(), proto);
    }

    /**
     * 
     * @param dataset
     */
    public void addColumn(DataSet dataset) {
        Iterator<IData<?>> it = dataset.iterator();
        while (it.hasNext()) {
            IData d = it.next();
            prototypes.put(d.prototype().name(), d.prototype());
        }
    }

    /**
     * Adds a prototype extended from a File to be takken into account in the DoE
     *
     * @param proto, the prototyde to be added
     * @param basePath, the base path of the considered file (which is thus considered relativaly to this path) as a String
     */
    public void addColumn(IPrototype<? extends File> proto, String basePath) {
        addColumn(proto, new File(basePath));
    }

    /**
     * Adds a prototype extended from a File to be takken into account in the DoE
     *
     * @param proto, the prototyde to be added
     * @param basePath, the base path of the considered file (which is thus considered relativaly to this path) as a File
     */
    public void addColumn(IPrototype<? extends File> proto, File basePath) {
        prototypes.put(proto.name(), proto);
        pathMapping.put(proto, basePath);
    }

    /**
     * Builds the plan.
     *
     */
    @Override
    public Iterable<Iterable<IVariable<?>>> build(IContext context) throws Throwable {
        Collection<Iterable<IVariable<?>>> listOfListOfValues = new ArrayList<Iterable<IVariable<?>>>();
        //Map<IPrototype, IStringMapping> convertorMapping = new HashMap<IPrototype, IStringMapping>();
        CSVReader reader = new CSVReader(new FileReader(csvFile));

        try {
            //parse header
            List<String> header = Arrays.asList(reader.readNext());
            Map<Integer, IPrototype> headerPrototypes = new HashMap<Integer, IPrototype>();
            int index;
            for (IPrototype p : prototypes.values()) {
                if ((index = header.indexOf(p.name())) == -1) {
                    throw new UserBadDataError("Unknown header " + p.name());
                }
                headerPrototypes.put(index, p);

            }

            //parse values
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                List<IVariable<?>> values = new ArrayList<IVariable<?>>(headerPrototypes.size());
                for (int i : headerPrototypes.keySet()) {
                    IPrototype p = headerPrototypes.get(i);
                    if (pathMapping.containsKey(p)) values.add(new Variable(p, new FileMapping(pathMapping.get(p)).convert(nextLine[i])));
                    else values.add(new Variable(p, StringConvertor.getConvertor(p.type().erasure()).convert(nextLine[i])));
                }
                listOfListOfValues.add(JavaConversions.asScalaIterable(values));
            }
        } finally {
            reader.close();
        }

        return scala.collection.JavaConversions.asScalaIterable(listOfListOfValues);
    }
}
