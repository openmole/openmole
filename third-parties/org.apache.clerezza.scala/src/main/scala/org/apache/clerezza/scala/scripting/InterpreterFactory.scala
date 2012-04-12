/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.scala.scripting;


import org.osgi.framework.BundleContext
import org.osgi.framework.Bundle
import scala.tools.nsc._;
import scala.tools.nsc.interpreter._;
import scala.tools.nsc.util._
import java.io.PrintWriter
import java.net._
import org.apache.clerezza.scala.Activator

class InterpreterFactory {
	
  def createInterpreter(out: PrintWriter) : Interpreter = {
    val i = new BundleContextScalaInterpreter(Activator.bundleContext, out)
    i
  }

	
}
