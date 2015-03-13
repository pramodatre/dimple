/*******************************************************************************
*   Copyright 2012 Analog Devices, Inc.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************/

package com.analog.lyric.dimple.solvers.minsum;

import java.util.Arrays;

import com.analog.lyric.collect.ArrayUtil;
import com.analog.lyric.dimple.environment.DimpleEnvironment;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.domains.JointDomainIndexer;
import com.analog.lyric.dimple.model.factors.Factor;

/*
 * Provides the update and updateEdge logic for minsum
 */
public class TableFactorEngine
{
	MinSumTableFactor _tableFactor;
	Factor _factor;

	public TableFactorEngine(MinSumTableFactor tableFactor)
	{
		_tableFactor = tableFactor;
		_factor = _tableFactor.getFactor();
	}
	
	public void updateEdge(int outPortNum)
	{
	    int[][] table = _tableFactor.getFactorTable().getIndicesSparseUnsafe();
	    double[] values = _tableFactor.getFactorTable().getEnergiesSparseUnsafe();
	    int tableLength = table.length;
	    final int numPorts = _factor.getSiblingCount();


        double[] outputMsgs = _tableFactor.getOutPortMsg(outPortNum);
        final int outputMsgLength = outputMsgs.length;
		double[] saved = ArrayUtil.EMPTY_DOUBLE_ARRAY;
        
        if (_tableFactor._dampingInUse)
        {
        	double damping = _tableFactor._dampingParams[outPortNum];
        	if (damping != 0)
        	{
				saved = DimpleEnvironment.doubleArrayCache.allocateAtLeast(outputMsgLength);
				System.arraycopy(outputMsgs, 0, saved, 0, outputMsgLength);
        	}
        }

        
        for (int i = 0; i < outputMsgLength; i++)
        	outputMsgs[i] = Double.POSITIVE_INFINITY;

        double [][] inPortMsgs = _tableFactor.getInPortMsgs();
        
	    // Run through each row of the function table
        for (int tableIndex = 0; tableIndex < tableLength; tableIndex++)
        {
        	double L = values[tableIndex];
        	int[] tableRow = table[tableIndex];
        	int outputIndex = tableRow[outPortNum];

        	for (int inPortNum = 0; inPortNum < numPorts; inPortNum++)
        		if (inPortNum != outPortNum)
        			L += inPortMsgs[inPortNum][tableRow[inPortNum]];
        	
        	if (L < outputMsgs[outputIndex])
        		outputMsgs[outputIndex] = L;				// Use the minimum value
        }

	    // Normalize the outputs
        double minPotential = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < outputMsgLength; i++)
        {
        	double msg = outputMsgs[i];
        	if (msg < minPotential)
        		minPotential = msg;
        }
        
        // Damping
        if (_tableFactor._dampingInUse)
        {
        	double damping = _tableFactor._dampingParams[outPortNum];
        	if (damping != 0)
        	{
        		for (int i = 0; i < outputMsgLength; i++)
        			outputMsgs[i] = (1-damping)*outputMsgs[i] + damping*saved[i];
        	}
        }
        
		if (saved.length > 0)
		{
			DimpleEnvironment.doubleArrayCache.release(saved);
		}

		// Normalize min value
        for (int i = 0; i < outputMsgLength; i++)
        	outputMsgs[i] -= minPotential;
	}
	
	
	public void update()
	{
		final IFactorTable table = _tableFactor.getFactorTable();
		final JointDomainIndexer indexer = table.getDomainIndexer();
	    final int[][] tableIndices = table.getIndicesSparseUnsafe();
	    final double[] values = table.getEnergiesSparseUnsafe();
	    final int tableLength = tableIndices.length;
	    final int numPorts = _factor.getSiblingCount();
	    double [][] outPortMsgs = _tableFactor.getOutPortMsgs();

	    final boolean useDamping = _tableFactor._dampingInUse;
	    
	    final double[] saved =
	    	useDamping ?
	    		DimpleEnvironment.doubleArrayCache.allocateAtLeast(indexer.getSumOfDomainSizes()) :
	    			ArrayUtil.EMPTY_DOUBLE_ARRAY;
	    	
	    for (int port = 0, savedOffset = 0; port < numPorts; port++)
	    {
	    	final double[] outputMsgs = outPortMsgs[port];
	    	final int outputMsgLength = outputMsgs.length;
	    	
	    	if (useDamping)
	    	{
	    		double damping = _tableFactor._dampingParams[port];
	    		if (damping != 0)
	    		{
	    			System.arraycopy(outputMsgs, 0, saved, savedOffset, outputMsgLength);
	    		}
	    	}

	    	Arrays.fill(outputMsgs, Double.POSITIVE_INFINITY);
    		savedOffset += outputMsgLength;
	    }
	    
	    final double [][] inPortMsgs = _tableFactor.getInPortMsgs();

	    
	    // Run through each row of the function table
	    for (int tableIndex = 0; tableIndex < tableLength; tableIndex++)
	    {
	    	final int[] tableRow = tableIndices[tableIndex];
	    	
	    	// Sum up the function value plus the messages on all ports
	    	double L = values[tableIndex];
	    	for (int port = 0; port < numPorts; port++)
	    		L += inPortMsgs[port][tableRow[port]];

			// Run through each output port
	    	for (int outPortNum = 0; outPortNum < numPorts; outPortNum++)
	    	{
	    		final double[] outputMsgs = outPortMsgs[outPortNum];
	    		final int outputIndex = tableRow[outPortNum];											// Index for the output value
	    		final double LThisPort = L - inPortMsgs[outPortNum][outputIndex];			// Subtract out the message from this output port
	    		if (LThisPort < outputMsgs[outputIndex])
	    			outputMsgs[outputIndex] = LThisPort;	// Use the minimum value
	    	}
	    }
	   
	    // Damping
	    if (useDamping)
	    {
	    	for (int port = 0, savedOffset = 0; port < numPorts; port++)
	    	{
		    	final double[] outputMsgs = outPortMsgs[port];
		    	final int outputMsgLength = outputMsgs.length;
	    		final double damping = _tableFactor._dampingParams[port];

	    		if (damping != 0)
	    		{
	    			for (int i = 0; i < outputMsgLength; i++)
	    				outputMsgs[i] = (1-damping)*outputMsgs[i] + damping*saved[i+savedOffset];
	    		}
	    		
	    		savedOffset += outputMsgLength;
	    	}
	    	
	    	DimpleEnvironment.doubleArrayCache.release(saved);
	    }
    	
	    
    	
	    // Normalize the outputs
	    for (int port = 0; port < numPorts; port++)
	    {
    		double[] outputMsgs = outPortMsgs[port];
    		int outputMsgLength = outputMsgs.length;
	    	double minPotential = Double.POSITIVE_INFINITY;
	    	for (int i = 0; i < outputMsgLength; i++)
	    	{
	    		double msg = outputMsgs[i];
	    		if (msg < minPotential)
	    			minPotential = msg;
	    	}
	    	for (int i = 0; i < outputMsgLength; i++)
	    		outputMsgs[i] -= minPotential;			// Normalize min value
	    }
	}
}
