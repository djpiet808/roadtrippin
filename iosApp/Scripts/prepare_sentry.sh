#!/bin/sh
set -eu

sentry_version="8.58.2"
sentry_checksum="08b0fa226894b146d8bb76d057df245807cd4113a189d0fec8bc7cd69678c9c6"
sentry_cache="$SRCROOT/.build/sentry"
sentry_archive="$sentry_cache/Sentry-$sentry_version.xcframework.zip"
sentry_framework="$sentry_cache/Sentry.xcframework"
sentry_url="https://github.com/getsentry/sentry-cocoa/releases/download/$sentry_version/Sentry.xcframework.zip"

if [ -f "$sentry_framework/Info.plist" ]; then
  exit 0
fi

mkdir -p "$sentry_cache"

if [ ! -f "$sentry_archive" ]; then
  curl --fail --location --silent --show-error "$sentry_url" --output "$sentry_archive"
fi

actual_checksum=$(xcrun swift package compute-checksum "$sentry_archive")
if [ "$actual_checksum" != "$sentry_checksum" ]; then
  echo "Sentry archive checksum mismatch" >&2
  exit 1
fi

extract_dir=$(mktemp -d "$sentry_cache/.extract.XXXXXX")
trap 'rm -rf "$extract_dir"' EXIT
unzip -q "$sentry_archive" -d "$extract_dir"

if [ ! -f "$extract_dir/Sentry.xcframework/Info.plist" ]; then
  echo "Sentry XCFramework was not present in the verified archive" >&2
  exit 1
fi

rm -rf "$sentry_framework"
mv "$extract_dir/Sentry.xcframework" "$sentry_framework"
