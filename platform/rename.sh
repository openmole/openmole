
#!/bin/bash

find ./ -type f | grep -E -i "\.java$|\.xml$|\.aj$|\.ini$|\.scala|$\.mf$|.groovy$" | xargs sed -i "s/$1/$2/g"

