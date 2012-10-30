
#!/bin/bash

find ./ -type f | grep -i -E "\.scala|" | xargs sed -i "s/$1/$2/g"

