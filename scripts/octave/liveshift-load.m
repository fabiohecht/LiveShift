disp(['Loading data...']);
load '/tmp/data.mat' output2DCDF lagSweep tHoldSweep
disp(['Finished.']);



figure(1)
clf
pcolor(lagSweep, tHoldSweep, output2DCDF);
colorbar
xlabel('Lag');
ylabel('Holding Time');
print('-dpsc','/tmp/figure.ps')

