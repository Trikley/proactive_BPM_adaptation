set datafile separator ','
set key autotitle columnhead
set key outside left bottom horizontal Left box lt -1 lw 0.5 
set yrange [-1:1]
if (exists("xbound")) set xrange [0:xbound]
#set term pdf
set term png transparent truecolor size 1500,750
set xtics nomirror
set ytics nomirror
set ytics 0.2
set mxtics 4
set mytics 2
set style line 81 lt 0 lc rgb "#808080" lw 0.75
set grid xtics
set grid ytics
set grid mxtics
set grid mytics
set grid back ls 81
if (!exists("poutput")) set output "output.png"
if (!exists("axiscolumn")) axiscolumn = 0
if (exists("poutput")) set output poutput
plot filename using axiscolumn:(($2)/1) with lines title 'earliness',\
												'' using axiscolumn:(($3)/1) with lines title 'true avg 100',\
												'' using axiscolumn:(($4)/1) with lines title 'true avg 1000',\
												'' using axiscolumn:(($5)/1) with lines title 'adaption rate',\
												'' using axiscolumn:(($7)/100) with lines title 'costs',\
												'' using axiscolumn:(($9)) with lines title 'rewards'