#!/bin/bash

if [ -z "$1" ]; then
    red "No working dir specified ($0)" >&2;
    exit -1; fi
workingdir="$1";
plotdir="$workingdir/plots";

if [ ! -d "$plotdir" ]; then mkdir -p $plotdir; fi

green "Plot dir is $plotdir";

for eXpr in `ls -d $workingdir/*/`; do

    extraction=$(echo -en $eXpr | awk '/STR/&&/MULT/ {printf gensub(/.*MULT([[:digit:]\.]*).*STR([[:digit:]]*).*/, "\\1 \\2", 1)}');
    pdelay=`cat $eXpr/.pdelay`;
    pubs_count=`count_publishings $eXpr/`;
    dels_count=`count_deliveries $eXpr/`;

    echo -e "$extraction $pdelay\t=> $pubs_count\t$dels_count";
done > $plotdir/deliveries_stat;

for std in 1 2 3; do
    awk "{if(\$2==$std) print;}" $plotdir/deliveries_stat > $plotdir/deliveries_stat_std$std;
done

for pdelay in 1000 500 250 150 100; do
    awk "{if(\$3==$pdelay) print;}" $plotdir/deliveries_stat > $plotdir/deliveries_stat_delay$pdelay;
done
