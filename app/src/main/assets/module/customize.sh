#!/bin/sh

SKIPMOUNT=false
PROPFILE=true
POSTFSDATA=true
LATESTARTSERVICE=true

PERSIST_DIR="/data/adb/colorosiconspatch"
PERSIST_ICONS="$PERSIST_DIR/uxicons"
MODULE_ICONS="$MODPATH/uxicons"

mkdir -p "$PERSIST_ICONS" || abort "Create persistent icon directory failed"
rm -rf "$MODULE_ICONS"
ln -s "$PERSIST_ICONS" "$MODULE_ICONS" || abort "Link module icon directory failed"

set_perm_recursive "$MODPATH" 0 0 0755 0644
ui_print "- ColorOS Icons Patch"
ui_print "- Persistent icons: $PERSIST_ICONS"
