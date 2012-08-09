{

if ($11=="holding") {

if (cnt>0) {
print sprintf("%d" ,cat/60/1000),sumlag/cnt/1000,sumskip/cnt,sumplay/cnt#, "a"
}
catlength=1*60000;
sumlag=0;
sumskip=0;
sumplay=0;
cnt=0;
cat=-1;
a=1;
}
else
{
if (!(cnt==0 && $4>4000 && a==1)) {
a=0;
x=sprintf("%d",($4/catlength)); x=(x+1)*catlength;
if (cat==-1) {
cat=x;
}
if (cat==x) {
sumlag+=$6;
sumskip+=$10;
sumplay+=$11;
cnt++;
#print ">",$4,$6
if ($4<$6) {
#print "putaqueopariu"
}
}
else {
print sprintf("%d" ,cat/60/1000),sumlag/cnt/1000,sumskip/cnt,sumplay/cnt#, "b"
if (catlength==60000) {
catlength*=3;
}
else if (catlength==3*60000) {
catlength=10*60000;
}
else if (catlength==10*60000) {
catlength*=2;
}
sumlag=0;
sumskip=0;
sumplay=0;
cnt=0;
cat=-1;
}
}
else {
#print "x ",$4,$6
}
}
}

