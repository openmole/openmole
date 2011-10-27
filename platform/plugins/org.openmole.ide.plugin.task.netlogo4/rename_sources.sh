
#!/bin/bash

find ./ -type f | grep -E -i "\.java$|\.scala|\.xml" | xargs sed -i "s/$1/$2/g"

