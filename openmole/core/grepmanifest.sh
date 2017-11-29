
for i in `find . -name "*jar"`
do
  echo $i
  unzip -q -c $i META-INF/MANIFEST.MF | grep $1 
done

