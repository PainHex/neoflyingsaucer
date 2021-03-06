/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.github.neoflyingsaucer.layout;

import com.github.neoflyingsaucer.css.constants.IdentValue;

import java.util.List;

public class CounterFunction {
    private final IdentValue _listStyleType;
    private int _counterValue;
    private List<Integer> _counterValues;
    private String _separator;

    public CounterFunction(final int counterValue, final IdentValue listStyleType) {
        _counterValue = counterValue;
        _listStyleType = listStyleType;
    }

    public CounterFunction(final List<Integer> counterValues, final String separator, final IdentValue listStyleType) {
        _counterValues = counterValues;
        _separator = separator;
        _listStyleType = listStyleType;
    }

    public String evaluate() 
    {
        if (_counterValues == null) 
        {
            return createCounterText(_listStyleType, _counterValue);
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < _counterValues.size(); i++)
        {
        	Integer value = _counterValues.get(i);
        	
        	sb.append(createCounterText(_listStyleType, value.intValue()));

        	if (i != _counterValues.size() - 1)
        		sb.append(_separator);
        }

        return sb.toString();
    }

    public static String createCounterText(final IdentValue listStyle, final int listCounter) {
        String text;
        if (listStyle == IdentValue.LOWER_LATIN || listStyle == IdentValue.LOWER_ALPHA) {
            text = toLatin(listCounter).toLowerCase();
        } else if (listStyle == IdentValue.UPPER_LATIN || listStyle == IdentValue.UPPER_ALPHA) {
            text = toLatin(listCounter).toUpperCase();
        } else if (listStyle == IdentValue.LOWER_ROMAN) {
            text = toRoman(listCounter).toLowerCase();
        } else if (listStyle == IdentValue.UPPER_ROMAN) {
            text = toRoman(listCounter).toUpperCase();
        } else if (listStyle == IdentValue.DECIMAL_LEADING_ZERO) {
            text = (listCounter >= 10 ? "" : "0") + listCounter;
        } else { // listStyle == IdentValue.DECIMAL or anything else
            text = Integer.toString(listCounter);
        }
        return text;
    }


    private static String toLatin(int val) {
        String result = "";
        while (val > 0) {
            final int letter = val % 26;
            val = val / 26;
            result = ((char) (letter + 64)) + result;
        }
        return result;
    }

    private static String toRoman(int val) {
        final int[] ints = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        final String[] nums = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ints.length; i++) {
            final int count = (int) (val / ints[i]);
            for (int j = 0; j < count; j++) {
                sb.append(nums[i]);
            }
            val -= ints[i] * count;
        }
        return sb.toString();
    }
}
