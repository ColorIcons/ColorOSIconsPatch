#!/bin/sh

SKIPMOUNT=false
PROPFILE=true
POSTFSDATA=true
LATESTARTSERVICE=true

PERSIST_DIR="/data/adb/ColorOSIconsPatch"
PERSIST_ICONS="$PERSIST_DIR/uxicons"
MODULE_ICONS="$MODPATH/uxicons"
ACTION_SCRIPT="$MODPATH/action.sh"

mkdir -p "$PERSIST_ICONS" || abort "Create persistent icon directory failed"
rm -rf "$MODULE_ICONS"
ln -s "$PERSIST_ICONS" "$MODULE_ICONS" || abort "Link module icon directory failed"

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$ACTION_SCRIPT" 0 0 0755
ui_print "- ColorOS Icons Patch"
ui_print "- Persistent icons: $PERSIST_ICONS"
