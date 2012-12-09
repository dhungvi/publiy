#\!/bin/bash; 

if [ -z "$1" ]; then
    red "Missing working dir ($0);"
    exit -1; fi
workingdir="$1";

# "DT{zipf-MULT1.0-SKW1.0-PG6-PC10-SG6}-TICK5-d3-sSTR3-W1000-MP-FOUT12-CAND4-BW_METRIC_INOUT_BYTES{20000-20000}__2"

for dir in `ls -d $workingdir/DT*/`;do
    if [ `expr match "$dir" ".*DELAY.*"` -ne 0 ]; then
        red "SKIPPING `basename $dir`"; continue; fi

    blue `basename $dir`;

    first=$(echo $dir | awk '{printf gensub(/(.*)TICK.*/,"\\1",1)}');
    last=$(echo $dir|awk '{printf gensub(/.*(TICK.*)/,"\\1",1)}');
    sdelay=$(cat $dir/.sdelay);
    pdelay=$(cat $dir/.pdelay);

    mv $dir $first"DELAY{$pdelay-$sdelay}-"$last;
    # green `basename $first"DELAY{$pdelay-$sdelay}-"$last`;
    echo;
done
