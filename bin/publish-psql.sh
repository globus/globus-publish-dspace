#!/bin/bash

# Got this function from:
# http://nitinkhola.com/how-to-use-properties-file-in-shell-script/
function getPropertyFromFile()
{
# substitute “.” with “\.” so that we can use it as sed expression
propertyName=`echo $1 | sed -e 's/\./\\\./g'`
fileName=$2;
cat $fileName | sed -n -e "s/^[ ]*//g;/^#/d;s/^$propertyName=//p" | tail -1
}

pathToScript=`readlink -f $0`
echo I am $pathToScript
scriptDir="$(cd "$(dirname "$0")" && pwd)"
echo I am in $scriptDir
devroot=`dirname $scriptDir`
echo devroot is $devroot

if ! [ -z "$1" ]; then
    dspaceSrc=$1
fi
if [ -z "$dspaceSrc" ]; then
    dspaceSrc=$DSPACE_SRC
fi
if [ -z "$dspaceSrc" ]; then
    dspaceSrc=.
fi

dspaceInstall=`getPropertyFromFile dspace.install.dir $dspaceSrc/build.properties`

dspaceConfig=$dspaceInstall/config/dspace.cfg

echo dspace Config is: $dspaceConfig

dspaceDBURL=`getPropertyFromFile db.driver $dspaceSrc/build.propertes`

echo dspace URL is: $dspaceDBURL

dspaceDBUser=`getPropertyFromFile db.username $dspaceConfig`

echo dspace user is: $dspaceDBUser

dspaceDBPW=`getPropertyFromFile db.password $dspaceConfig`

echo dspace pw is: $dspaceDBPW

