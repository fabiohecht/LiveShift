


egrep 'DELAY|holding' pass2 |sed 's/\// /'| awk '{

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
#print "+",$4,$6
}
else {
print sprintf("%d" ,cat/60/1000),sumlag/cnt/1000,sumskip/cnt,sumplay/cnt#, "b"
catlength*=2;
sumlag=0;
sumskip=0;
sumplay=0;
cnt=0;
cat=-1;
}
}
else {
#print "-",$4,$6
}
}
}
'>lag&

|egrep '^-'

|less

>lag    &


|head



>lag 

