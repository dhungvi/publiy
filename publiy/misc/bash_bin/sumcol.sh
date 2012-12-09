awk '{if(line[0]==""){line[0]=$0; for(i=1;i<=NF;i++) line[i]=$i} else {for (i=1 ; i<=NF ; i++) {tot[i]+=$(i); if(i>colCount)colCount=i;}}} END{for (i=1 ; i<=colCount ; i++) printf line[i]": "tot[i]"\n"}' $1

