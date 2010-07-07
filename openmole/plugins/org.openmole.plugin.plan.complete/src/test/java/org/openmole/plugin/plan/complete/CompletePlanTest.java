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

package org.openmole.plugin.plan.complete;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.implementation.plan.Factor;
import org.openmole.core.implementation.plan.FactorsValues;
import org.openmole.core.model.data.IPrototype;
import static org.junit.Assert.*;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.plugin.domain.interval.RangeBigDecimal;
import org.openmole.plugin.domain.interval.RangeInteger;

/**
 *
 * @author reuillon
 */
public class CompletePlanTest {

    public CompletePlanTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of build method, of class CompletePlan.
     */
    @Test
    public void testBuild() throws Exception {
        System.out.println("build");
        IContext context = new Context();
        
        IPrototype f1 = new Prototype("f1", Integer.class);
        IPrototype f2 = new Prototype("f2", BigDecimal.class);
        
        IFactor f1Factor = new Factor(f1,new RangeInteger("1", "2"));
        IFactor f2Factor = new Factor(f2,new RangeBigDecimal("0.1", "0.2", "0.1"));
        
        CompletePlan instance = new CompletePlan(f1Factor, f2Factor);

        List<IFactorValues> expectedResult = new LinkedList<IFactorValues>();
        FactorsValues value = new FactorsValues();
        value.setValue(f1, 1);
        value.setValue(f2, new BigDecimal("0.1"));
        expectedResult.add(value);
                        
        value = new FactorsValues();
        value.setValue(f1, 2);
        value.setValue(f2, new BigDecimal("0.1"));
        expectedResult.add(value);
        
        value = new FactorsValues();
        value.setValue(f1, 1);
        value.setValue(f2, new BigDecimal("0.2"));
        expectedResult.add(value);

        
        value = new FactorsValues();
        value.setValue(f1, 2);
        value.setValue(f2, new BigDecimal("0.2"));
        expectedResult.add(value);

        
        IExploredPlan result = instance.build(context);
        Iterator<IFactorValues> resIt = result.iterator();
        while(resIt.hasNext()) {
            IFactorValues res = resIt.next();
            for(String name: res.getNames()) {
                System.out.print(name + " " + res.getValue(name) + " ");
            }
            System.out.println();
        }
        

        System.out.println(expectedResult.size() + " " + result.size());
        assertEquals(expectedResult.size(), result.size());
        
        resIt = result.iterator();
        Iterator<IFactorValues> expIt = expectedResult.iterator();
        
        while(expIt.hasNext()) {
            IFactorValues curRes = resIt.next();
            IFactorValues expRes = expIt.next();
            
            for(String varName: expRes.getNames()) {
                System.out.println(curRes.getValue(varName) + " " + expRes.getValue(varName));  
                assertEquals(curRes.getValue(varName), expRes.getValue(varName));
            }
        }
    }

}