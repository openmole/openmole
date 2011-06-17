/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core;

import java.util.logging.Handler;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MyHandler extends Handler {

    private Formatter formatter = null;
    private Level level = null;
    private static MyHandler handler = null;
    private OutputTopComponent outComponent = null;

    private MyHandler() {
        LogManager manager = LogManager.getLogManager();
        String className = this.getClass().getName();
        String level = manager.getProperty(className + ".level");
        setLevel(level != null ? Level.parse(level) : Level.INFO);
    }

    public void setOutComponent(OutputTopComponent outComponent) {
        this.outComponent = outComponent;
    }

    public synchronized void publish(LogRecord record) {
        String message = null;
        if (!isLoggable(record)) {
            return;
        }
        //  message = getFormatter().format(record);
        if (outComponent != null) {
            outComponent.appendLog(record.getMessage());
        }
    }

    public void close() {
    }

    public void flush() {
    }

    public static synchronized MyHandler getInstance() {
        if (handler == null) {
            handler = new MyHandler();
        }
        return handler;
    }
}
