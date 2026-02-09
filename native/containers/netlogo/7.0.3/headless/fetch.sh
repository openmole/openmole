
LIBS=`cs fetch com.thoughtworks.xstream:xstream:1.4.20`

mkdir libs

for lib in $LIBS
do
   cp $lib libs/
done

