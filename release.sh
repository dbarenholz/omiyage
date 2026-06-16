#!/bin/bash
set -e

# Extract current version from frontend/package.json
CURRENT_VERSION=$(grep '"version":' frontend/package.json | sed -E 's/.*"version": "([^"]+)".*/\1/')

if [ -n "$1" ]; then
    NEW_VERSION="$1"
else
    # Increment patch version automatically
    IFS='.' read -r major minor patch <<< "$CURRENT_VERSION"
    NEW_PATCH=$((patch + 1))
    NEW_VERSION="${major}.${minor}.${NEW_PATCH}"
fi

echo "Releasing version $NEW_VERSION (previous: $CURRENT_VERSION)..."

# increment version in backend/pom.xml
sed -i "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/" backend/pom.xml

# increment version in frontend/package.json
sed -i "s/\"version\": \"$CURRENT_VERSION\"/\"version\": \"$NEW_VERSION\"/" frontend/package.json

echo "Files updated. Committing and tagging..."

# create git tag
git add backend/pom.xml frontend/package.json
git commit -m "chore: release $NEW_VERSION"
git tag -a "$NEW_VERSION" -m "Release $NEW_VERSION"

# push git tag
echo "Pushing commits and tags to origin..."
git push origin HEAD
git push origin "$NEW_VERSION"

echo "Done! Version $NEW_VERSION released."