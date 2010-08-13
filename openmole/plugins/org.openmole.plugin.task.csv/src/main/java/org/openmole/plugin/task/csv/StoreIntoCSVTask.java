/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.plugin.task.csv;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.openmole.core.implementation.task.Task;
import org.openmole.core.implementation.tools.VariableExpansion;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import scala.Tuple2;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 *
 * The StoreIntoCSVTask task is dedicated to the storage of data of the workflow
 * into CSV files. It is in particular possible to store data (as prototypes) aggregated in an
 * array. The number of data to store in columns is not limited.
 */
public class StoreIntoCSVTask extends Task {

    private String fileName;
    private char delimiter;
    private char quotechar;
    private List<Tuple2<IPrototype<? extends Collection>, String>> columns = new LinkedList<Tuple2<IPrototype<? extends Collection>, String>>();

    /**
     * Creates an instance of StoreIntoCSVTask with a default delimiter (',') and
     * quote character (none)
     *
     * @param name, the name of the task
     * @param fileName, the path of the CSV file to be stored
     * @throws UserBadDataError
     * @throws InternalProcessingError
     */
    public StoreIntoCSVTask(String name,
            String fileName) throws UserBadDataError, InternalProcessingError {
        this(name, fileName, ',', CSVWriter.NO_QUOTE_CHARACTER);
    }

    /**
     * Creates an instance of StoreIntoCSVTask with a specific delimiter and default
     * quote character (none)
     *
     * @param name, the name of the task
     * @param fileName, the path of the CSV file to be stored
     * @param delimiter, the char to be used to separate values
     * @throws UserBadDataError
     * @throws InternalProcessingError
     */
    public StoreIntoCSVTask(String name,
            String fileName,
            char delimiter) throws UserBadDataError, InternalProcessingError {
        this(name, fileName, delimiter, CSVWriter.NO_QUOTE_CHARACTER);
    }

    /**
     * Creates an instance of StoreIntoCSVTask with a specific delimiter and
     * quote character
     *
     * @param name, the name of the task
     * @param fileName, the path of the CSV file to be stored
     * @param delimiter, the char to be used to separate values
     * @param quotechar, the char to be used to quote  values
     * @throws UserBadDataError
     * @throws InternalProcessingError
     */
    public StoreIntoCSVTask(String name,
            String fileName,
            char delimiter,
            char quotechar) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.fileName = fileName;
        this.delimiter = delimiter;
        this.quotechar = quotechar;
    }

    /**
     * Add a prototype to be stored
     *
     * @param prototype
     */
    public void addColumn(IPrototype<? extends Collection> prototype) {
        addColumn(prototype, prototype.getName());
    }

    /**
     * Add a prototype to be stored, specifying explicitely the name of the column header to be saved
     *
     * @param prototype
     * @param columnName, the name of the column header
     */
    public void addColumn(IPrototype<? extends Collection> prototype, String columnName) {
        columns.add(new Tuple2<IPrototype<? extends Collection>, String>(prototype, columnName));
        addInput(prototype);
    }

    @Override
    protected void process(IContext global, IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {

        try {
            List<Iterator<Object>> valueList = new ArrayList<Iterator<Object>>();
            int listSize = 0;

            File file = new File(VariableExpansion.expandData(global, context, fileName));
            CSVWriter writer = new CSVWriter(new BufferedWriter(new FileWriter(file)), delimiter, quotechar);

            try {
                //header
                Iterator<Tuple2<IPrototype<? extends Collection>, String>> columnIt = columns.iterator();

                String[] header = new String[columns.size()];
                int ind = 0;
                while (columnIt.hasNext()) {
                    Tuple2<IPrototype<? extends Collection>, String> column = columnIt.next();
                    header[ind] = column._2();

                    Collection li = context.getValue(column._1());

                    if (listSize < li.size()) {
                        listSize = li.size();
                    }

                    valueList.add(li.iterator());
                    ind++;
                }

                if (header != null) {
                    writer.writeNext(header);
                }

                //body
                for (int i = 0; i < listSize; ++i) {
                    String[] line = new String[valueList.size()];
                    for (int j = 0; j < valueList.size(); ++j) {
                        line[j] = valueList.get(j).next().toString();
                    }
                    writer.writeNext(line);
                }
            } finally {
                writer.close();
            }


        } catch (IOException ex) {
            throw new UserBadDataError(ex);
        }
    }
}
