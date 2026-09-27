#!/system/bin/sh

am start -n com.immortal521.colorosiconspatch/.MainActivity >/dev/null 2>&1 ||
  am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.immortal521.colorosiconspatch >/dev/null 2>&1
