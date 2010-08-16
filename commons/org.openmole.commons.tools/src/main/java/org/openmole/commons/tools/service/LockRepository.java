/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.commons.tools.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import scala.Tuple2;

public class LockRepository<T> {

    final Map<T, Tuple2<Lock, AtomicInteger>> locks = new HashMap<T, Tuple2<Lock, AtomicInteger>>();

    public void lock(T obj) {
        Tuple2<Lock, AtomicInteger> lock;

        synchronized (locks) {
            lock = locks.get(obj);

            if (lock == null) {
                lock = new Tuple2<Lock, AtomicInteger>(new ReentrantLock(), new AtomicInteger(1));
                locks.put(obj, lock);
            } else {
                lock._2().incrementAndGet();
            }
        }

        lock._1().lock();
    }

    public void unlock(T obj) {
        Tuple2<Lock, AtomicInteger> lock;
        synchronized (locks) {
            lock = locks.get(obj);
            if (lock == null) {
                Logger.getLogger(LockRepository.class.getName()).log(Level.SEVERE, "BUG: Unlocking an object that has not been locked.");
                return;
            }
            int value = lock._2().decrementAndGet();
            if (value <= 0) {
                locks.remove(obj);
            }
        }
        lock._1().unlock();
    }
}
