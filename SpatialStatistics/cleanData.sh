head -n 1 $1 > finalOutput
tail -q -n +2 *.csv >> finalOutput
sed 's/[T|Z]/ /g' finalOutput > finalOutput.csv
rm finalOutput
