FOR /F "tokens=*" %%G IN ('DIR /B /AD /S org.*') DO RMDIR /S /Q %%G
java -ea -XX:+CMSClassUnloadingEnabled -XX:+UseParallelGC -Xmx92m -jar plugins\org.eclipse.equinox.launcher.jar  %*
