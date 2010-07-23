/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.plugin.plan.csv;

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
import org.openmole.core.implementation.plan.ExploredPlan;
import org.openmole.core.implementation.plan.FactorsValues;
import org.openmole.core.implementation.plan.Plan;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.core.model.data.IPrototype;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import au.com.bytecode.opencsv.CSVReader;
import java.util.Iterator;
import org.openmole.core.implementation.data.DataSet;
import org.openmole.core.model.data.IData;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 *
 * The CSVPlan enables to generates a design of experiment from a CSV file.
 * Each line of the file describes an experimentation. It is possible to restrict
 * the number of parameters to be used. Actually all the parameters to be taken into account
 * have to be specified using the method addColumn()
 */
public class CSVPlan extends Plan {

    final private File csvFile;


    final private Map<IPrototype<? extends File>, File> pathMapping = new HashMap<IPrototype<? extends File>, File>();
    final private Map<String, IPrototype> prototypes = new TreeMap<String, IPrototype>();

    /**
     * Creates an intstance of CSVPlan.
     *
     * @param csvFilePath, the path of the CSV file as a String
     */
    public CSVPlan(String csvFilePath) {
        this(new File(csvFilePath));
    }

    /**
     * Creates an intstance of CSVPlan.
     *
     * @param csvFilePath, the path of the CSV file as a File
     */
    public CSVPlan(File csvFile) {
        this.csvFile = csvFile;
    }

    /**
     * Adds a prototype to be takken into account in the DoE
     *
     * @param proto, the prototyde to be added
     */
    public void addColumn(IPrototype proto) {
        prototypes.put(proto.getName(), proto);
    }

    /**
     * 
     * @param dataset
     */
    public void addColumn(DataSet dataset){
        Iterator<IData<?>> it = dataset.iterator();
        while(it.hasNext()){
            IData d = it.next();
            prototypes.put(d.getPrototype().getName(),d.getPrototype());
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
        prototypes.put(proto.getName(), proto);
        pathMapping.put(proto, basePath);
    }

    /**
     * Builds the plan.
     *
     */
    @Override
    public IExploredPlan build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        Collection<IFactorValues> listOfListOfValues = new ArrayList<IFactorValues>();
        Map<IPrototype, IStringMapping> convertorMapping = new HashMap<IPrototype, IStringMapping>();

        try {
            CSVReader reader = new CSVReader(new FileReader(csvFile));

            try {
                //parse header
                List<String> header = Arrays.asList(reader.readNext());
                Map<Integer, IPrototype> headerPrototypes = new HashMap<Integer, IPrototype>();
                int index;
                for (IPrototype p : prototypes.values()) {
                    if ((index = header.indexOf(p.getName())) == -1) {
                        throw new UserBadDataError("Unknown header " + p.getName());
                    }
                    headerPrototypes.put(index, p);
                    try {
                        if (pathMapping.containsKey(p)) {
                            File deployedPath = pathMapping.get(p);
                            convertorMapping.put(p, StringConvertor.getInstance().getConvertor(p.getType(), deployedPath));
                        } else {
                            convertorMapping.put(p, StringConvertor.getInstance().getConvertor(p.getType()));
                        }
                    } catch (IllegalArgumentException ex) {
                        throw new UserBadDataError(ex);
                    } catch (NoSuchMethodException ex) {
                        throw new UserBadDataError(ex);
                    } catch (InstantiationException ex) {
                        throw new UserBadDataError(ex);
                    } catch (IllegalAccessException ex) {
                        throw new UserBadDataError(ex);
                    } catch (InvocationTargetException ex) {
                        throw new UserBadDataError(ex);
                    }
                }


                //parse values
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    FactorsValues fv = new FactorsValues();
                    for (int i : headerPrototypes.keySet()) {
                        IPrototype p = headerPrototypes.get(i);
                        fv.setValue(p, convertorMapping.get(p).convert(nextLine[i]));
                    }
                    listOfListOfValues.add(fv);
                }
                reader.close();
            } catch (IOException ex) {
                throw new InternalProcessingError(ex);
            }
        } catch (FileNotFoundException ex) {
            throw new UserBadDataError(ex);
        }
        return new ExploredPlan(listOfListOfValues);
    }
}
