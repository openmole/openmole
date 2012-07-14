cd %1
sc delete openmole-dbserver
dbserver\bin\openmole-dbserver.bat install
dbserver\bin\openmole-dbserver.bat start
