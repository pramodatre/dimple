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

package com.analog.lyric.dimple.model.domains;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.collect.WeakInterner;
import com.analog.lyric.math.Utilities;
import com.google.common.collect.Interner;

import net.jcip.annotations.Immutable;

/**
 * A finite field of characteristic 2 (GF(2<sup>N</sup>)).  The domain is determined by the length, N,
 * as well as the primitive polynomial.
 */
@Immutable
public class FiniteFieldDomain extends TypedDiscreteDomain<FiniteFieldNumber>
{
	private static final long serialVersionUID = 1L;
	
	private int _primitivePolynomial;
	private int _N;
	private int _size;

	private static enum InternedDomains
	{
		INSTANCE;
		
		private final Interner<FiniteFieldDomain> interner = WeakInterner.create();
	}
	
	/*--------------
	 * Construction
	 */
	
	/**
	 * Create finite field domain with specified primitive polynomial.
	 * @since 0.07
	 */
	public static FiniteFieldDomain create(int primitivePolynomial)
	{
		return new FiniteFieldDomain(primitivePolynomial).intern();
	}
	
	FiniteFieldDomain(int primitivePolynomial)
	{
		super(computeHashCode(primitivePolynomial));
		
		_primitivePolynomial = primitivePolynomial;
		_N = Utilities.findMSB(primitivePolynomial) - 1;
		_size = 1 << _N;
	}
		
	private static int computeHashCode(int primitivePolynomial)
	{
		return primitivePolynomial;
	}
	
	@Override
	protected FiniteFieldDomain intern()
	{
		return InternedDomains.INSTANCE.interner.intern(this);
	}
	
	public final int getPrimitivePolynomial()
	{
		return _primitivePolynomial;
	}
	
	public int getN()
	{
		return _N;
	}
	
	/*----------------
	 * Object methods
	 */
	
	@Override
	public final boolean equals(@Nullable Object that)
	{
		if (this == that)
			return true;
		
		if (that instanceof FiniteFieldDomain)
		{
			FiniteFieldDomain thatFF = (FiniteFieldDomain)that;
			return _primitivePolynomial == thatFF._primitivePolynomial;
		}
		
		return false;
	}
	
	/*------------------------
	 * DiscreteDomain methods
	 */

	@Override
	public final int size()
	{
		return _size;
	}


	/*------------------------
	 * TypedDiscreteDomain methods
	 */

	@Override
	public final FiniteFieldNumber getElement(int i)
	{
		assertIndexInBounds(i, _size);
		return new FiniteFieldNumber(i, this);
	}

	@Override
	public final Class<FiniteFieldNumber> getElementClass()
	{
		return FiniteFieldNumber.class;
	}

	@Override
	public final int getIndex(@Nullable Object value)
	{
		if (value instanceof FiniteFieldNumber)
			return ((FiniteFieldNumber)value).intValue();
		else if (value instanceof Integer)
			return (Integer)value;	// Already an index
		else
			return -1;
	}
	
	/*----------------
	 * Domain methods
	 */
	
	@Override
	public boolean hasIntCompatibleValues()
	{
		return true;
	}
	
	@Override
	public boolean isIntegral()
	{
		return true;
	}

}
