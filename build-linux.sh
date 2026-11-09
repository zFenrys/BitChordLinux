#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk
STATE=".build-state"
LAST_VERSION="1.9.1"; LAST_BUILD=33
[ -f "$STATE" ] && read -r LAST_VERSION LAST_BUILD < "$STATE"
if [ -n "${1:-}" ]; then
  VERSION="$1"
elif [[ "$LAST_VERSION" =~ ^([0-9]+\.[0-9]+)\.([0-9]+)$ ]]; then
  VERSION="${BASH_REMATCH[1]}.$(( BASH_REMATCH[2] + 1 ))"
else
  echo "Last version '$LAST_VERSION' is not X.Y.Z: pass the version explicitly." >&2
  exit 1
fi
PACKAGE_BUILD=$(( LAST_BUILD + 1 ))
export LISTEN_TOGETHER_SERVER="${LISTEN_TOGETHER_SERVER:-https://bitchord-listen-together.onrender.com}"

if [ ! -x appimagetool.AppImage ]; then
  curl -sSL -o appimagetool.AppImage https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x appimagetool.AppImage
fi

touch local.properties
chmod +x gradlew
rm -rf dist desktopApp/build/compose/binaries/main/rpm
mkdir -p dist

./gradlew :desktopApp:packageRpm :desktopApp:createDistributable -Pbitchord.version="$VERSION" -Pbitchord.packageBuild="$PACKAGE_BUILD"

desktopApp/packaging/appimage.sh desktopApp/build/compose/binaries/main/app/BitChord "dist/BitChord-$VERSION-linux-x86_64.AppImage" "$PWD/appimagetool.AppImage"
cp desktopApp/build/compose/binaries/main/rpm/*.rpm dist/
ls -lh dist
echo "$VERSION $PACKAGE_BUILD" > "$STATE"
echo "Built $VERSION (package build $PACKAGE_BUILD)"
