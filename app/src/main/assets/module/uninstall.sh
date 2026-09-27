#!/system/bin/sh

MODPATH="${0%/*}"
PERSIST_DIR="/data/adb/ColorOSIconsPatch"
LEGACY_DIR="/data/adb/colorosiconspatch"

# Stop module-owned mounts before the root manager removes the module directory.
for target in \
  /my_product/media/theme/uxicons/hdpi \
  /data/oplus/uxicons \
  /my_stock/media/theme/uxicons/xhdpi \
  /my_stock/media/theme/uxicons/xxhdpi \
  /my_stock/media/theme/uxicons/xxxhdpi; do
  mountpoint -q "$target" 2>/dev/null && umount "$target" 2>/dev/null
done

# Remove generated links and persistent icon data from current and legacy locations.
[ -L "$MODPATH/uxicons" ] && rm -f "$MODPATH/uxicons"
rm -rf "$PERSIST_DIR" "$LEGACY_DIR"
