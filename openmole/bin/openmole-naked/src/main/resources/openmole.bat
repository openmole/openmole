set startdir=%cd%
set PWD=%~dp0
if "%PWD:~-1%"=="\" set PWD=%PWD:~0,-1%
cd /d %~dp0
cd %cd%
mkdir "%UserProfile%\.openmole\.tmp"
set ran="%UserProfile%\.openmole\.tmp\%random%"

set MEMORY=1500

java -d64 -version >nul 2>&1
if errorlevel 1 goto is32bit
set MEMORY=2000
set FLAG="-XX:+UseCompressedOops"
:is32bit

set SCRIPT_ARGS=

:run
java -Xss8M -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.country=US -Duser.language=en -Dopenmole.location="%PWD%" -Dosgi.classloader.singleThreadLoads=true -Dosgi.configuration.area=%ran% -Djdk.util.zip.disableZip64ExtraFieldValidation=true --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio.file=ALL-UNNAMED -XX:+UseG1GC -XX:CICompilerCount=2 -XX:ParallelGCThreads=2 -XX:ConcGCThreads=2 -XX:G1ConcRefinementThreads=2 -XX:+UseStringDeduplication -Xmx%MEMORY%m %FLAG% -cp "%PWD%/launcher/*" org.openmole.launcher.Launcher  --plugins "%PWD%/plugins/" --run org.openmole.ui.Application --priority "logging" --osgi-directory %ran% -- %* %SCRIPT_ARGS%
set ret=%errorlevel%
rmdir /s /q %ran%

if %ret% EQU 254 (
  set SCRIPT_ARGS=--no-browser
  goto run
)

EXIT /B %ret%
