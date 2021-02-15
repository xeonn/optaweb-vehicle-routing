#!/bin/bash

sed -i 's/BACKEND_URL/'"$BACKEND_URL"'/g' CONFIG_FILE
exec "$@"
