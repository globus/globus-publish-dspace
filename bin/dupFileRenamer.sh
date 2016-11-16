#!/bin/sh

usage() {
echo "$0: keep_directory replace_directory [replace_extension]"
echo "Find all files in the keep_directory tree and look for files with the"
echo "same name in the replace_directory tree. Any files found in"
echo "replace_directory with the same name will be renamed with an additional"
echo "extension replace_extension. If no replace_extension is given, the"
echo "default extension .orig will be used."
}

# SrcDir is first param with any trailing / stripped
srcDir=${1%/}

# Directory to replace in is 2nd param with any trailing / stripped
replDir=${2%/}

# Extension to be used when adding is from param 3 though we default to .orig
addOnExtension=$3

if [ -z "$srcDir" ]; then
    usage
    echo "\nkeep_directory must be provided"
    exit 1
fi

if [ -z "$replDir" ]; then
    usage
    echo "\nreplace_directory must be provided"
    exit 1
fi

if [ ! -d $srcDir ]; then
    echo "value for keep_directory: $srcDir does not exist."
    exit 1
fi

if [ ! -d $replDir ]; then
    echo "value for replace_directory: $replDir does not exist."
    exit 1
fi

if [ -z "$addOnExtension" ]; then
    addOnExtension=".orig"
fi

# Print these one per line to allow for spaces in names
srcFiles=`find $srcDir -follow -type f -print`

# Make the loop iterator sep. char a newline. Note the funky syntax that puts
# the open ' on one line and the close ' on the next line. That's not a mistake

IFS='
'

for file in $srcFiles
do
    # Get the file name without the leading directory name
    suffix=${file#$srcDir}
    # remove a leading slash if present
    newSuffix=${suffix#\/}
    # Potential destination file name is the suffix part of the original name
    # with the replacement directory pre-pended
    destFile=$replDir/$newSuffix
    if [ -f $destFile ]; then
	echo "renaming $destFile to $destFile$addOnExtension"
	mv $destFile $destFile$addOnExtension
    fi    
done


#
# now, we find any .orig files and make sure the new files are there, else we
# we rename them back to the name without extension
#


extensionFiles=`find $replDir -follow -type f -iname \*$addOnExtension`

for file in $extensionFiles
do
    #Get file name without extension
    origName=${file%$addOnExtension}
    # The name without the leading directory
    suffix=${origName#$replDir}
    # remove a leading slash
    newSuffix=${suffix#\/}
    # Now we should be looking for this file in the source directory
    srcFile=$srcDir/$newSuffix
    if [ ! -f $srcFile ]; then
	echo "renaming $file to $origName"
	mv $file $origName
    fi
done
# Set all other files in the replace directory to READONLY so that we don't
# accidently change them later.

find $replDir -type f -exec chmod a-w '{}' \;
