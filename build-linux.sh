#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk
VERSION="${1:-1.8.1}"

if [ ! -x appimagetool.AppImage ]; then
  curl -sSL -o appimagetool.AppImage https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x appimagetool.AppImage
fi

touch local.properties
chmod +x gradlew
rm -rf dist desktopApp/build/compose/binaries/main/rpm
mkdir -p dist

./gradlew :desktopApp:packageRpm :desktopApp:createDistributable -Pbitchord.version="$VERSION"

desktopApp/packaging/appimage.sh desktopApp/build/compose/binaries/main/app/BitChord "dist/BitChord-$VERSION-linux-x86_64.AppImage" "$PWD/appimagetool.AppImage"
cp desktopApp/build/compose/binaries/main/rpm/*.rpm dist/
ls -lh dist
