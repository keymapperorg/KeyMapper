#!/system/bin/sh

SOURCE_PATH="%%%STARTER_PATH%%%"
STARTER_PATH="/data/local/tmp/keymapper_sysbridge_starter"

echo "info: start.sh begin"

if [ ! -f $SOURCE_PATH ]; then
  echo "fatal: source file does not exist: $SOURCE_PATH"
  exit 1
fi

recreate_tmp() {
  echo "info: /data/local/tmp is possible broken, recreating..."
  rm -rf /data/local/tmp
  mkdir -p /data/local/tmp
}

broken_tmp() {
  echo "fatal: /data/local/tmp is broken, please try reboot the device or manually recreate it..."
  exit 1
}

echo "info: attempt to copy starter from $SOURCE_PATH to $STARTER_PATH"
rm -f $STARTER_PATH

cp "$SOURCE_PATH" $STARTER_PATH
res=$?
if [ $res -ne 0 ]; then
  recreate_tmp
  cp "$SOURCE_PATH" $STARTER_PATH

  res=$?
  if [ $res -ne 0 ]; then
    broken_tmp
  fi
fi

chmod 700 $STARTER_PATH
chown 2000 $STARTER_PATH
chgrp 2000 $STARTER_PATH

if [ -f $STARTER_PATH ]; then
  echo "info: exec $STARTER_PATH"
    # Pass apk path, library path, package name, version code
    $STARTER_PATH --apk="%%%APK_PATH%%%" --lib="%%%LIB_PATH%%%" --package="%%%PACKAGE_NAME%%%" --version="%%%VERSION_CODE%%%"
    result=$?
    if [ ${result} -ne 0 ]; then
        echo "info: keymapper_sysbridge_starter exit with non-zero value $result"
    else
        echo "info: keymapper_sysbridge_starter exit with 0"
    fi
else
    echo "Starter file not exist, please open Key Mapper and try again."
fi
