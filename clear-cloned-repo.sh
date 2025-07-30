#!/bin/bash

TARGET_DIR="/tmp/coverage-workspaces"

# Check if the directory exists
if [ -d "$TARGET_DIR" ]; then
    echo "Deleting contents of $TARGET_DIR..."
    rm -rf "${TARGET_DIR:?}/"*
    echo "All files and directories inside $TARGET_DIR have been deleted."
else
    echo "Directory $TARGET_DIR does not exist."
fi