# Local Variables
localrepo="/home/Reza/localrepo";
setup="$localrepo/setup/";
sshprivatekey="$localrepo/../.ssh/id_rsa";
sshpubkey="$localrepo/../.ssh/id_rsa.pub";

###########################
## CL
## Remote Variables
CL_USER="reza";
CL_HOME="/home/master/$CL_USER";
CL_REPO="$CL_HOME/localrepo";
CL_LIB="$CL_REPO/lib";
CL_SETUP="$CL_REPO/setup";
CL_SDIR="$CL_HOME/System";
CL_SJAR="$CL_SDIR/System.jar";
CL_BINDIR="$SDIR";
CL_PATH=".:$CL_JAVADIR/bin";
CL_CLASSPATH="$CL_SJAR";
CL_DAEMON="$CL_LIB/daemon";
CL_DATA="$CL_SDIR/data";
CL_DATA_DESCRIPTOR="$CL_DATA/data_descriptor";
CL_MASTER="10.0.0.1";

###########################
## PL
## Remote Variables
PL_USER="utoronto_jacobsen_ft";

COMMON_RESPONSE_TIMEOUT=10;

function just_wait() {
    COMMON_MAIN_LOOP_PID="$*";
    trap "on_die $COMMON_MAIN_LOOP_PID" 2;
    yellow "WAITING for subshell(s): $*";
    wait $*;
}

function on_die() {
    pids_to_kill=$*;
    red -en "\n\nReally terminate? Give an answer in $RESPONSE_TIMEOUT seconds.\n";
    read -t $COMMON_RESPONSE_TIMEOUT line;
    if [ "$line" == "YES" -o "$line" == "yes" ]; then
        kill -9 $pids_to_kill;
        exit -1;
    else
        green "We continue then ...";
        just_wait $*;
    fi
}

function wait_on_stop() {
    if [ -z "$1" ]; then
        stopdir="$workingdir";
    elif [ ! -d "$1" ]; then
        stopdir="$workingdir";
    else
        stopdir="$1"; fi

    while :; do
       if [ -f "$stopdir/.stop" ]; then
           if [ "`cat $stopdir/.stop`" != "STOP" ]; then
                break;
           elif [ "$stopped" = "1" ]; then
                yellow -en ".";
                sleep 1;
           else
                stopped="1";
                yellow "STOPPING @`date`";
           fi;
       else
           break;
       fi;
    done;
    if [ "$stopped" = "1" ]; then
         stopped="0";
         yellow -en "\nWAKINGUP @`date`\n";
    fi
}
