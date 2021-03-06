/*******************************************************************************
*   Copyright 2014 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.core.proxy;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.events.SolverEventSource;
import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.model.core.INode;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;

import net.jcip.annotations.NotThreadSafe;

/**
 * @since 0.05
 * @deprecated No longer used.
 */
@Deprecated
@NotThreadSafe
public abstract class ProxySolverNode<Delegate extends ISolverNode>
	extends SolverEventSource
	implements ISolverNode, IProxySolverNode<Delegate>
{
	/*-------
	 * State
	 */
	
	protected @Nullable ISolverFactorGraph _parent = null;
	
	/*--------------
	 * Construction
	 */
	
	protected ProxySolverNode()
	{
	}

	/*---------------------
	 * ISolverNode methods
	 */
	
	@Override
	public double getBetheEntropy()
	{
		return requireDelegate("getBetheEntropy").getBetheEntropy();
	}

	@Override
	public double getInternalEnergy()
	{
		return requireDelegate("getInternalEnergy").getInternalEnergy();
	}

	@Deprecated
	@Override
	public Object getInputMsg(int portIndex)
	{
		throw unsupported("getInputMsg");
	}

	@Deprecated
	@Override
	public Object getOutputMsg(int portIndex)
	{
		throw unsupported("getOutputMsg");
	}

	@Override
	public @Nullable ISolverFactorGraph getParentGraph()
	{
		return _parent;
	}

	@Override
	public ISolverFactorGraph getRootSolverGraph()
	{
		ISolverFactorGraph root = getContainingSolverGraph();
		
		for (ISolverFactorGraph parent = root; parent != null; parent = parent.getParentGraph())
		{
			root = parent;
		}
		
		return root;
	}
	
	@SuppressWarnings("null")
	@Override
	public ISolverNode getSibling(int edge)
	{
		final INode sibling = getModelObject().getSibling(edge);
		return getSolverMapping().getSolverNode(sibling);
	}
	
	@SuppressWarnings("null")
	@Override
	public int getSiblingCount()
	{
		return getModelObject().getSiblingCount();
	}
	
	@Deprecated
	@Override
	public double getScore()
	{
		return requireDelegate("getScore").getScore();
	}

	@Override
	public void initialize()
	{
		clearFlags();
		requireDelegate("initialize").initialize();
	}

	@Deprecated
	@Override
	public void setInputMsg(int portIndex, Object obj)
	{
		throw unsupported("setInputMsg");
	}

	@Deprecated
	@Override
	public void setOutputMsg(int portIndex, Object obj)
	{
		throw unsupported("setOutputMsg");
	}

	@Deprecated
	@Override
	public void setInputMsgValues(int portIndex, Object obj)
	{
		throw unsupported("setInputMsgValues");
	}

	@Deprecated
	@Override
	public void setOutputMsgValues(int portIndex, Object obj)
	{
		throw unsupported("setOutputMsgValues");
	}

	@Override
	public void update()
	{
		requireDelegate("update").update();
	}

	@Override
	public void updateEdge(int outPortNum)
	{
		throw unsupported("updateEdge");
	}

	/*---------------
	 * Local methods
	 */
	
	@Override
	public abstract @Nullable Delegate getDelegate();

	/**
	 * Returns non-null delegate or throws an error indicating method requires that
	 * delegate solver has been set.
	 * @since 0.06
	 */
	protected Delegate requireDelegate(String method)
	{
		Delegate delegate = getDelegate();
		if (delegate == null)
		{
			throw new DimpleException("Delegate solver required by '%s' has not been set.", method);
		}
		return delegate;
	}
	
	protected RuntimeException unsupported(String method)
	{
		return DimpleException.unsupportedMethod(getClass(), method,
			"Not supported for proxy solver because graph topology may be different.");
	}
}
