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
    for (j=1; j<=NR; j++) {
        for (k=1;k<=(p-2);k++) {
            str=str"	"h[j]
        }
    }
    print str
#lines as colunms
    for(i=1; i<=NR; i++){
        str=a[i,2]
        for (j=1;j<=(i-1)*(p-2);j++) {
            str=str"	0"
        }
        for (j=3;j<=p;j++) {
            str=str" "a[i,j]
        }
        print str
    }

}' 
