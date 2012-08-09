awk '
{ 
    for (i=1; i<=NF; i++)  {
    	if (i==1) {h[NR]=$1}
        a[NR,i] = $i
    }
}
NF>p { p = NF }
END {    
#column headers
	str="x"
    for(j=1; j<=NR; j++) {
        str=str"	"h[j]
    }
    print str
#lines
    for(i=1; i<=NR; i++){
		str=a[i,2]
    	for (j=1;j<=i;j++) {
    		str=str"	0"
    	}
    	str=str""a[i,3]
    	print str    	
    }

}' 
