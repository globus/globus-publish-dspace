#!/bin/sh

outFile=$3

for i in 10 20 30 40 50 60 70 80 90 100
do
    echo $i >> $outFile
    sleep 10
done
