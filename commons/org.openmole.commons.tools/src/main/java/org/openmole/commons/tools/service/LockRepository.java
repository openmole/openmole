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

package org.openmole.commons.tools.service;

import org.openmole.commons.tools.structure.Duo;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LockRepository<T> {

    final Map<T, Duo<Lock, Integer>> locks = new HashMap<T, Duo<Lock, Integer>>();

    public void lock(T obj) {
        Duo<Lock, Integer> lock;

        synchronized (locks) {
            lock = locks.get(obj);

            if (lock == null) {
                lock = new Duo<Lock, Integer>(new ReentrantLock(), 1);
                locks.put(obj, lock);
            } else {
                lock.setRight(lock.getRight() + 1);
            }
        }

        lock.getLeft().lock();
    }

    public void unlock(T obj) {
        Duo<Lock, Integer> lock;
        synchronized (locks) {
            lock = locks.get(obj);
            if (lock == null) {
                Logger.getLogger(LockRepository.class.getName()).log(Level.SEVERE, "BUG: Unlocking an object that has not been locked.");
                return;
            }
            lock.setRight(lock.getRight() - 1);
            if (lock.getRight() <= 0) {
                locks.remove(obj);
            }
        }
        lock.getLeft().unlock();
    }
}
