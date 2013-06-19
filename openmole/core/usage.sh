
#!/bin/bash

find ./ -type f | grep -i -E "\.java$|\.ini$|\.scala|$\.mf$|.groovy$" | xargs grep $1

