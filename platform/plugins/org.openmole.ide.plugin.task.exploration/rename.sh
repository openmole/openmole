
#!/bin/bash

find ./ -type f | grep -i -E "\.java$|\.xml$|\.aj$|\.ini$|\.scala|\.form|\.mf|\.properties|.groovy$" | xargs sed -i "s/$1/$2/g"

