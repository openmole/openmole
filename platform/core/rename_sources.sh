
#!/bin/bash

find ./ -type f | grep -E -i "\.java$|\.scala" | xargs sed -i "s/$1/$2/g"

