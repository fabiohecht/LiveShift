#!/bin/bash

#made for 4 curves (could be adapted to adapt)

#example usage plot-lag.sh "ppn29bc0-skipstall" ".0 .5 .75" 2|R --vanilla  --slave 

EXP=$1 #e.g. "ppn29bc0-skipstall"
PARS=$2 #e.g. ".0 .5 .75"
S=$3 #e.g. 2


echo "setEPS()"
echo "postscript(file=\"$EXP-s$S-lag-boxplot.eps\", pointsize=28, width=14, height=10)"


i=1
for p in $PARS
do
  echo "LAG$i <- read.table(\"$EXP-$p-s$S-lag.data\", header=FALSE, colClasses=c(\"integer\",\"double\",\"double\",\"double\"))"
  i=$(($i+1))
  if [[ -n $c ]]
  then
    c=$c,
  fi
  c=$c\"$p\"
done

groups=(1 3 10 20)
groupsjoined=$(printf ",%s" "${groups[@]}")
groupsjoined=${groupsjoined:1}


spacing="-.4"
echo "boxplot(V2 ~ V1, LAG1, outline=FALSE, boxwex=0.15, at=1:${#groups[*]}+$spacing, xaxt=\"n\", ylab=\"Playback Lag (s)\", xlab=\"Session Holding Time (min)\", pars=list(whisklty=1), ylim=c(0,100))"

echo "grid(lty=1)";

for j in `seq 2 $(($i-1))`
do
  spacing=`echo $spacing+.2|bc`
  echo "boxplot(V2 ~ V1, LAG$j, outline=FALSE, at=1:${#groups[*]}+$spacing, axes=FALSE, boxwex=0.15, col=\"white\", add = TRUE, pars=list(whisklty=$j))"
done

echo "axis(1, at=0:${#groups[*]}, lab=c(\"\",$groupsjoined))"

echo "legend(\"topleft\", c($c),lty = c(1,2,3,4))"

echo "title(main=\"$EXP-s$S\")"
