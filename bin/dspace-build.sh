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

dspaceSrcModules="dspace-api dspace-jspui"
dspaceConfigModules="dspace"
dspaceModules="$dspaceSrcModules $dspaceConfigModules"

for module in $dspaceModules
do
    if ! [ -d "$dspaceSrc/$module" ]; then
	echo Module $module not found in dspace source directory $dspaceSrc
	abort=true
    fi
done

if ! [ -z "$abort" ]; then
    echo exiting...
    exit 1
fi

echo Using DSpace Source directory $dspaceSrc

dspaceInstall=`getPropertyFromFile dspace.install.dir $dspaceSrc/build.properties`

if [ -z $dspaceInstall ]; then
    echo Unable to find dspace.install.dir in $dspaceSrc/build.properties
    exit 1
fi

if [ -z "$CATALINA_HOME" ]; then
    if hash catalina 2>/dev/null; then
	CATALINA_HOME=`catalina version | fgrep CATALINA_HOME | awk '{print $3}'`
    fi
fi

if [ -z "$CATALINA_HOME" ]; then
    echo Cannot determine home location of Tomcat installation, please set CATALINA_HOME
else
    if [ -z "$CATALINA_USER" ]; then
	catalina stop
    fi
fi



for module in $dspaceSrcModules
do
    globusLink=$dspaceSrc/$module/src/globus
    globusSrc=$devroot/src/$module/src/globus
    pomLink=$dspaceSrc/$module/pom.xml
    globusPom=$devroot/src/$module/pom.xml

    if [ -d $globusSrc ]; then
	rm -v $globusLink
	ln -sfv $globusSrc $globusLink
    fi

    if [ -f $globusPom ]; then
	rm -v $pomLink
	ln -sfv $globusPom $pomLink
    fi

    (cd $dspaceSrc/$module/src && $devroot/bin/dupFileRenamer.sh globus main)
done

configFiles=`find $devroot/src/dspace -type f -print`
oldIFS=$IFS
IFS='
'

for devConfigFile in $configFiles
do
    configFile=${devConfigFile#$devroot/src/}
    # if there's  a directory in the path called globus, change that to main
    dspaceFile=`echo $dspaceSrc/$configFile | sed 's/\/globus\//\/main\//g'`
    echo devConfigFile is $devConfigFile
    echo dspaceFile is $dspaceFile
    if [ -L $dspaceFile ] ; then
	echo dspaceFile exists as a symlink removing...
	rm $dspaceFile
    fi
    if [ -f $dspaceFile ] ; then
	echo dspaceFile exists as a file renaming to $dspaceFile.orig
	mv $dspaceFile $dspaceFile.orig
    fi
    echo linking $devConfigFile to $dspaceFile
    ln -s $devConfigFile $dspaceFile
done

IFS=$oldIFS

dspaceTargetDir=$dspaceSrc/dspace/target/dspace-build

(cd $devroot/lib && ./install-jars-to-maven.sh)

(cd $devroot/src/globus-client-java && mvn install -DskipTests)

(cd $dspaceSrc && mvn -ff clean package -P '!dspace-xmlui,!dspace-lni,!dspace-oai,!dspace-sword,!dspace-swordv2,!dspace-rest')

if [ $? = 1 ]; then
    echo Build Failed, exiting
    exit 1
fi

(cd $dspaceSrc/dspace-api && mvn install -Dlicense.skip=true -DskipTests=true)

if [ $? = 1 ]; then
    echo Build dspace-api.jar Failed, exiting
    exit 1
fi

cp -vf $dspaceSrc/dspace-jspui/src/main/webapp/static/css/fonts/glyph* $dspaceTargetDir/webapps/jspui/static/css/fonts

(cd $dspaceTargetDir && ant update update_configs)

if ! [ -z "$CATALINA_USER" ]; then
    if ! [ -z "$CATALINA_PASSWORD" ]; then
	echo Waiting for Tomcat to reload JSPUI...
	curl -u $CATALINA_USER:$CATALINA_PASSWORD http://localhost:8080/manager/text/reload?path=/jspui
	if [ $? = 7 ]; then
	    echo Unable to connect to Tomcat, attempting to start
	    catalina jpda start
	    sleep 40
	fi
	sleep 2
    else
	echo CATALINA_USER is set without CATALINA_PASSWORD, don\'t know how to talk to server
    fi
elif ! [ -z "$CATALINA_HOME" ]; then
    catalina jpda start
    echo Waiting for Tomcat to start...
    sleep 65
fi

if hash tab.sh 2>/dev/null; then
    logDate=`date "+%Y-%m-%d"`
    tab.sh . "less +F $dspaceInstall/log/dspace.log.$logDate; exit"
fi

if ! [ -z "$CATALINA_HOME" ]; then
    less +F $CATALINA_HOME/logs/catalina.out
fi
