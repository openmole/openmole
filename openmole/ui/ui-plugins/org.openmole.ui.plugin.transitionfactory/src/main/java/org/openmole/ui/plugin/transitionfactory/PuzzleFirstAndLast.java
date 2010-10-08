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

package org.openmole.ui.plugin.transitionfactory;

import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.plugin.transitionfactory.IPuzzleFirst;
import org.openmole.ui.plugin.transitionfactory.IPuzzleFirstAndLast;
import org.openmole.ui.plugin.transitionfactory.IPuzzleLast;

/**
 *
 * @author mathieu
 */
public class PuzzleFirstAndLast <T1 extends IGenericTaskCapsule,T2 extends IGenericTaskCapsule> extends Puzzle implements IPuzzleFirstAndLast<T1,T2>, IPuzzleFirst<T1>, IPuzzleLast<T2> {

    /*protected IPuzzleFirst<T1> puzzleFirst;
    protected IPuzzleLast<T2> puzzleLast;*/
    protected PuzzleFirst<T1> puzzleFirst = new PuzzleFirst<T1>();
    protected PuzzleLast<T2> puzzleLast = new PuzzleLast<T2>();

    public PuzzleFirstAndLast(T1 firstC, T2 lastC) {
        setLastCapsule(lastC);
        setFirstCapsule(firstC);
    }

    @Override
    public T1 getFirstCapsule() {
        return puzzleFirst.getFirstCapsule();
    }

    @Override
    public void setFirstCapsule(T1 firstTCapsule) {
        puzzleFirst.setFirstCapsule(firstTCapsule);
    }

    @Override
    public T2 getLastCapsule() {
        return puzzleLast.getLastCapsule();
    }

    @Override
    public void setLastCapsule(T2 lastTCapsule) {
        puzzleLast.setLastCapsule(lastTCapsule);
    }
}
