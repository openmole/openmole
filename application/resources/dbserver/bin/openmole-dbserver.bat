@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\lib

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\org\openmole\com.db4o\0.8.0-SNAPSHOT\com.db4o-0.8.0-SNAPSHOT.jar;"%REPO%"\org\openmole\com.thoughtworks.xstream\0.8.0-SNAPSHOT\com.thoughtworks.xstream-0.8.0-SNAPSHOT.jar;"%REPO%"\org\openmole\core\org.openmole.misc.replication\0.8.0-SNAPSHOT\org.openmole.misc.replication-0.8.0-SNAPSHOT.jar;"%REPO%"\org\openmole\org.scala-lang.scala-library\0.8.0-SNAPSHOT\org.scala-lang.scala-library-0.8.0-SNAPSHOT.jar;"%REPO%"\org\scala-lang\scala-library\2.10.1\scala-library-2.10.1.jar;"%REPO%"\org\scala-lang\scala-reflect\2.10.1\scala-reflect-2.10.1.jar;"%REPO%"\com\typesafe\akka\akka-transactor_2.10\2.1.1\akka-transactor_2.10-2.1.1.jar;"%REPO%"\org\scala-stm\scala-stm_2.10\0.7\scala-stm_2.10-0.7.jar;"%REPO%"\org\scala-lang\scala-actors\2.10.1\scala-actors-2.10.1.jar;"%REPO%"\org\scala-lang\scala-compiler\2.10.1\scala-compiler-2.10.1.jar;"%REPO%"\org\scala-lang\jline\2.10.1\jline-2.10.1.jar;"%REPO%"\org\fusesource\jansi\jansi\1.4\jansi-1.4.jar;"%REPO%"\com\typesafe\akka\akka-actor_2.10\2.1.1\akka-actor_2.10-2.1.1.jar;"%REPO%"\com\typesafe\config\1.0.0\config-1.0.0.jar;"%REPO%"\org\openmole\core\org.openmole.runtime.dbserver\0.8.0-SNAPSHOT\org.openmole.runtime.dbserver-0.8.0-SNAPSHOT.jar
set EXTRA_JVM_ARGUMENTS=
goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% %EXTRA_JVM_ARGUMENTS% -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Dapp.name="openmole-dbserver" -Dapp.repo="%REPO%" -Dbasedir="%BASEDIR%" org.openmole.runtime.dbserver.DBServer %CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
