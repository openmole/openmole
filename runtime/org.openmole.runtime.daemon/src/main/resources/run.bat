FOR /F "tokens=*" %%G IN ('DIR /B /AD /S org.*') DO RMDIR /S /Q %%G
java -ea -Dosgi.classloader.singleThreadLoads=true -Xmx92m -jar plugins/org.eclipse.equinox.launcher_1.1.0.jar  %*
