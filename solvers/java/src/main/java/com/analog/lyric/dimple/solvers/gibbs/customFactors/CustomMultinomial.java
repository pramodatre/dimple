/*******************************************************************************
*   Copyright 2013 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.gibbs.customFactors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.analog.lyric.dimple.factorfunctions.Multinomial;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.VariableBase;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.DirichletParameters;
import com.analog.lyric.dimple.solvers.gibbs.SDiscreteVariable;
import com.analog.lyric.dimple.solvers.gibbs.SRealFactor;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.DirichletSampler;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.IRealJointConjugateSamplerFactory;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;

public class CustomMultinomial extends SRealFactor implements IRealJointConjugateFactor
{
	private Object[] _outputMsgs;
	private SDiscreteVariable[] _outputVariables;
	private int _dimension;
	private int _alphaParameterEdge;
	private int _numOutputEdges;
	private int[] _constantOutputCounts;
	private boolean _hasConstantOutputs;
	private boolean[] _hasConstantOutput;
	private static final int NO_PORT = -1;
	private static final int ALPHA_PARAMETER_INDEX_FIXED_N = 0;	// If N is in constructor then alpha is first index (0)
	private static final int OUTPUT_MIN_INDEX_FIXED_N = 1;		// If N is in constructor then output starts at second index (1)
	private static final int ALPHA_PARAMETER_INDEX = 1;			// If N is not in constructor then alpha is second index (1)
	private static final int OUTPUT_MIN_INDEX = 2;				// If N is not in constructor then output starts at third index (2)
	
	public CustomMultinomial(Factor factor)
	{
		super(factor);
	}

	@Override
	public void updateEdgeMessage(int portNum)
	{
		if (portNum == _alphaParameterEdge)
		{
			// Output port is the joint alpha parameter input
			// Determine sample alpha vector of the conjugate Dirichlet distribution
			
			DirichletParameters outputMsg = (DirichletParameters)_outputMsgs[portNum];

			// Clear the output counts
			outputMsg.setNull(_dimension);

			// Get the current output counts
			if (!_hasConstantOutputs)
			{
				for (int i = 0; i < _dimension; i++)
					outputMsg.add(i, _outputVariables[i].getCurrentSampleIndex());
			}
			else	// Some or all outputs are constant
			{
				for (int i = 0, iVar = 0, iConst = 0; i < _dimension; i++)
					outputMsg.add(i, _hasConstantOutput[i] ? _constantOutputCounts[iConst++] : _outputVariables[iVar++].getCurrentSampleIndex());
			}
		}
		else
			super.updateEdgeMessage(portNum);
	}
	
	
	@Override
	public Set<IRealJointConjugateSamplerFactory> getAvailableRealJointConjugateSamplers(int portNumber)
	{
		Set<IRealJointConjugateSamplerFactory> availableSamplers = new HashSet<IRealJointConjugateSamplerFactory>();
		if (isPortAlphaParameter(portNumber))						// Conjugate sampler if edge is alpha parameter input
			availableSamplers.add(DirichletSampler.factory);		// Parameter inputs have conjugate Dirichlet distribution
		return availableSamplers;
	}
	
	public boolean isPortAlphaParameter(int portNumber)
	{
		determineParameterConstantsAndEdges();	// Call this here since initialize may not have been called yet
		return (portNumber == _alphaParameterEdge);
	}

	
	
	@Override
	public void initialize()
	{
		super.initialize();
		
		// Determine what parameters are constants or edges, and save the state
		determineParameterConstantsAndEdges();
	}
	
	
	private void determineParameterConstantsAndEdges()
	{
		FactorFunction factorFunction = _factor.getFactorFunction();
		Multinomial specificFactorFunction = (Multinomial)factorFunction.getContainedFactorFunction();	// In case the factor function is wrapped

		
		// Pre-determine whether or not the parameters are constant
		List<? extends VariableBase> siblings = _factor.getSiblings();
		_hasConstantOutputs = false;
		_alphaParameterEdge = NO_PORT;
		_outputVariables = null;
		int alphaParameterIndex;
		int outputMinIndex;
		if (specificFactorFunction.hasConstantNParameter())		// N parameter is constructor constant
		{
			alphaParameterIndex = ALPHA_PARAMETER_INDEX_FIXED_N;
			outputMinIndex = OUTPUT_MIN_INDEX_FIXED_N;
		}
		else	// Variable or constant N parameter
		{
			alphaParameterIndex = ALPHA_PARAMETER_INDEX;
			outputMinIndex = OUTPUT_MIN_INDEX;
		}
		if (!factorFunction.isConstantIndex(alphaParameterIndex))
			_alphaParameterEdge = factorFunction.getEdgeByIndex(alphaParameterIndex);

		// Save the output constant or variables as well
		_numOutputEdges = _numPorts - factorFunction.getEdgeByIndex(outputMinIndex);
		_outputVariables = new SDiscreteVariable[_numOutputEdges];
		_hasConstantOutputs = factorFunction.hasConstantAtOrAboveIndex(outputMinIndex);
		_constantOutputCounts = null;
		_hasConstantOutput = null;
		_dimension = -1;
		if (_hasConstantOutputs)
		{
			int numConstantOutputs = factorFunction.numConstantsAtOrAboveIndex(outputMinIndex);
			_dimension = _numOutputEdges + numConstantOutputs;
			_hasConstantOutput = new boolean[_dimension];
			_constantOutputCounts = new int[numConstantOutputs];
			for (int i = 0, index = outputMinIndex; i < _dimension; i++, index++)
			{
				if (factorFunction.isConstantIndex(index))
				{
					_hasConstantOutput[i] = true;
					_constantOutputCounts[i] = (Integer)factorFunction.getConstantByIndex(index);
				}
				else
				{
					_hasConstantOutput[i] = false;
					int outputEdge = factorFunction.getEdgeByIndex(index);
					_outputVariables[i] = (SDiscreteVariable)((siblings.get(outputEdge)).getSolver());
				}
			}
		}
		else	// No constant outputs
		{
			_dimension = _numOutputEdges;
			for (int i = 0, index = outputMinIndex; i < _dimension; i++, index++)
			{
				int outputEdge = factorFunction.getEdgeByIndex(index);
				_outputVariables[i] = (SDiscreteVariable)((siblings.get(outputEdge)).getSolver());
			}
		}

	}
	
	
	@Override
	public void createMessages() 
	{
		super.createMessages();
		determineParameterConstantsAndEdges();	// Call this here since initialize may not have been called yet
		_outputMsgs = new Object[_numPorts];
		if (_alphaParameterEdge != NO_PORT)
			_outputMsgs[_alphaParameterEdge] = new DirichletParameters();
	}
	
	@Override
	public Object getOutputMsg(int portIndex) 
	{
		return _outputMsgs[portIndex];
	}
	
	@Override
	public void moveMessages(ISolverNode other, int thisPortNum, int otherPortNum)
	{
		super.moveMessages(other, thisPortNum, otherPortNum);
		_outputMsgs[thisPortNum] = ((CustomMultinomial)other)._outputMsgs[otherPortNum];
	}
}
