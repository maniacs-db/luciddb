/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2005-2005 The Eigenbase Project
// Copyright (C) 2005-2005 Disruptive Tech
// Copyright (C) 2005-2005 LucidEra, Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

#ifndef Fennel_AggComputerImpl_Included
#define Fennel_AggComputerImpl_Included

#include "fennel/exec/AggComputer.h"
#include "fennel/tuple/TupleData.h"

FENNEL_BEGIN_NAMESPACE

/**
 * CountAggComputer is an abstract base for computing COUNT().
 */
class CountAggComputer : public AggComputer
{
protected:
    inline uint64_t &interpretDatum(TupleDatum &);
    
    inline void clearAccumulatorImpl(
        TupleDatum &accumulatorDatum);
    inline void updateAccumulatorImpl(
        TupleDatum &accumulatorDatum);
    
public:
    // implement AggComputer
    virtual void computeOutput(
        TupleDatum &outputDatum,
        TupleDatum const &accumulatorDatum);
};

/**
 * CountStarAggComputer computes COUNT(*), which counts tuples without
 * regard for null values.
 */
class CountStarAggComputer : public CountAggComputer
{
public:
    // implement AggComputer
    virtual void clearAccumulator(
        TupleDatum &accumulatorDatum);

    // implement AggComputer
    virtual void updateAccumulator(
        TupleDatum &accumulatorDatum,
        TupleData const &inputTuple);
};

/**
 * CountNullableAggComputer computes COUNT(X), which does not count tuples
 * for which X IS NULL.
 */
class CountNullableAggComputer : public CountAggComputer
{
public:
    // implement AggComputer
    virtual void clearAccumulator(
        TupleDatum &accumulatorDatum);

    // implement AggComputer
    virtual void updateAccumulator(
        TupleDatum &accumulatorDatum,
        TupleData const &inputTuple);
};

/**
 * ExtremeAggComputer computes MIN/MAX, ignoring null values but returning
 * null if the input is empty.
 */
class ExtremeAggComputer : public AggComputer
{
    /**
     * Type descriptor used as comparison functor.
     */
    StoredTypeDescriptor const *pTypeDescriptor;
    
    /**
     * false for MAX, true for MIN
     */
    bool isMin;
    
    // FIXME jvs 7-Oct-2005: This violates the rule in AggComputer's class
    // description about where data state is stored.
    bool isResultNull;

    inline void copyInputToAccumulator(
        TupleDatum &accumulatorDatum, 
        TupleDatum const &inputDatum);

public:
    explicit ExtremeAggComputer(
        TupleAttributeDescriptor const &attrDesc,
        bool isMin);
    
    // implement AggComputer
    virtual void clearAccumulator(
        TupleDatum &accumulatorDatum);

    // implement AggComputer
    virtual void updateAccumulator(
        TupleDatum &accumulatorDatum,
        TupleData const &inputTuple);
    
    // implement AggComputer
    virtual void computeOutput(
        TupleDatum &outputDatum,
        TupleDatum const &accumulatorDatum);
};

/**
 * SumAggComputer computes SUM(x), ignoring null values but returning
 * null if the input is empty.
 */
template <class T>
class SumAggComputer : public AggComputer
{
    // FIXME jvs 7-Oct-2005: This violates the rule in AggComputer's class
    // description about where data state is stored.
    bool isResultNull;
    
    inline T &interpretDatum(TupleDatum &datum)
    {
        assert(datum.cbData == sizeof(T));
        assert(datum.pData);
        return *reinterpret_cast<T *>(const_cast<PBuffer>(datum.pData));
    }
    
    inline T const &interpretDatum(TupleDatum const &datum)
    {
        assert(datum.cbData == sizeof(T));
        assert(datum.pData);
        return *reinterpret_cast<T const *>(datum.pData);
    }
    
public:
    // implement AggComputer
    virtual void clearAccumulator(TupleDatum &accumulatorDatum)
    {
        T &sum = interpretDatum(accumulatorDatum);
        sum = 0;

        isResultNull = true;
    }

    // implement AggComputer
    virtual void updateAccumulator(
        TupleDatum &accumulatorDatum,
        TupleData const &inputTuple)
    {
        assert(iInputAttr != -1);
        TupleDatum const &inputDatum = inputTuple[iInputAttr];
        if (!inputDatum.pData) {
            // SQL2003 Part 2 Section 10.9 General Rule 4.a
            // TODO jvs 6-Oct-2005:  we're supposed to queue a warning
            // for null value eliminated in set function
            return;
        } else {
            isResultNull = false;
        }
        T &sum = interpretDatum(accumulatorDatum);
        if (inputDatum.pData) {
            T input = interpretDatum(inputDatum);
            // TODO jvs 6-Oct-2005:  overflow check
            sum += input;
        }
    }

    // implement AggComputer
    virtual void computeOutput(
        TupleDatum &outputDatum,
        TupleDatum const &accumulatorDatum)
    {
        // Set output to alias accumulator value directly.
        outputDatum = accumulatorDatum;
        if (isResultNull) {
            outputDatum.pData = NULL;
        }
    }
};
    
FENNEL_END_NAMESPACE

#endif

// End AggComputerImpl.h