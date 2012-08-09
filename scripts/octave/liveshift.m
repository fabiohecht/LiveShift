disp(['Loading data...']);
data = load(argv(){1});
disp(['Finished.']);

tHold = data(:,1);
lag = data(:,2);

maxThold = max(tHold);
minThold = min(tHold);

maxLag = max(lag);
minLag = 0; %min(data(:,2)); There are -1's, ignore

tHoldSamples = 40;
lagSamples = maxLag/1000;


tHoldSweep = linspace(minThold, maxThold, tHoldSamples);
lagSweep = linspace(minLag, maxLag, lagSamples);

output2DCDF = zeros(length(tHoldSweep), length(lagSweep));
for currTholdIndex = 1:length(tHoldSweep)
        
    currThold = tHoldSweep(currTholdIndex);
    auxTHold = find(tHold <= currThold);
            
    disp(['Working on Holding Time: ' num2str(currThold) ' - (' num2str(currTholdIndex) ' of ' num2str(length(tHoldSweep)) ')']);
    
    for currLagIndex = 1:length(lagSweep)
       
        currLag = lagSweep(currLagIndex);
        
        % find numSamples for which these two are satisfied
        
        auxLag = find(lag <= currLag);      
        
        currNumSamples = length(intersect(auxTHold, auxLag));
        
        output2DCDF(currTholdIndex, currLagIndex) = currNumSamples;

% disp(currTholdIndex),disp(";"),disp(currLagIndex),disp(";"),disp(currNumSamples);

    end

end

output2DCDF = output2DCDF ./ max(max(output2DCDF));

save (argv(){2}, "output2DCDF", "lagSweep", "tHoldSweep");

