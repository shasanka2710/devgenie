#!/bin/bash

# === Configuration ===
MONGO_HOST="localhost"
MONGO_PORT="27017"
DATABASE_NAME="devgenie"

# === Collections to Clear ===
COLLECTIONS=("coverageData" "coverage_improvement_sessions" "dashboard_cache" "fileMetadata" "repositoryAnalysis" "sonarBaseComponentMetrics")

# === Script Execution ===
echo "Connecting to MongoDB at $MONGO_HOST:$MONGO_PORT"
echo "Target database: $DATABASE_NAME"

for COLLECTION in "${COLLECTIONS[@]}"
do
    echo "Clearing collection: $COLLECTION"
    mongo "$DATABASE_NAME" --host "$MONGO_HOST" --port "$MONGO_PORT" --eval "db.getCollection('$COLLECTION').deleteMany({})"
done

echo "✅ All specified collections cleared."