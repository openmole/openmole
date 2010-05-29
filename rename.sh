
#!/bin/bash

find ./ -type f | grep -i -E "\.java$|\.xml$|\.aj$|\.ini$|\.scala$\.mf$" | xargs sed -i "s/$1/$2/g"

