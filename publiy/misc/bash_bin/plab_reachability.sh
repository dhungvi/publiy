#! /bin/bash

# Local Variables
localrepo="/home/$ft/localrepo";
javatar="$localrepo/java32.tar.gz";
setup="$localrepo/setup/";
sshprivatekey="$localrepo/.ssh/pl.id_rsa";
sshpubkey="$localrepo/.ssh/pl.id_rsa.pub";
ft=utoronto_jacobsen_ft;
SSH_TIMEOUT=20;
result="";

# Remote Variables
for remote in $*; do 
	output=`ssh -o ConnectTimeout=$SSH_TIMEOUT -o StrictHostKeyChecking=no -i $sshprivatekey $ft@$remote "echo HI"`
	if [ "$output" != "HI" ]; then 
		if [ `expr match "$result" "[^ ]*"` -ne 0 ]; then 
			result=$result" "$remote;
		else
			result=$remote;
		fi;
	fi;
done

echo $result;
