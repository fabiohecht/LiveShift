rm results_$1/messages.sql
for file in results_$1/*.tgz; do
	echo 'Processing '$file
	~/untar.sh $file > /dev/null
	~/analyze/messages_sql.sh $file $1 >> results_$1/messages.sql
done
