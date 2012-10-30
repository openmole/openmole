
#!/bin/bash

find ./ -type f | grep -i -E "\.java$|\.xml$|\.aj$|\.ini$|\.scala|$\.mf$|.groovy$" | xargs grep $1

