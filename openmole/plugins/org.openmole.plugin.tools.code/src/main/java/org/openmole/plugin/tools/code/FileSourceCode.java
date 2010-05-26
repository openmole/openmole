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
package org.openmole.plugin.tools.code;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.file.URIFile;
import org.openmole.commons.exception.InternalProcessingError;

public class FileSourceCode implements ISourceCode {

    File file;

    public FileSourceCode(String file) {
        this(new File(file));
    }

    public FileSourceCode(File file) {
        this.file = file;
    }

    @Override
    public String getCode() throws InternalProcessingError {
        StringBuilder code = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line + "\n");
            }
            reader.close();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }

        return code.toString();
    }
}
