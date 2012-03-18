
#!/bin/bash

find ./ -type f | grep -i -E "\.java$|\.scal|" | xargs sed -i "s/$1/$2/g"

