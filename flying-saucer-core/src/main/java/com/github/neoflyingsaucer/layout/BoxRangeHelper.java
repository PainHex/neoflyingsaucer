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

import java.util.LinkedList;
import java.util.List;

import com.github.neoflyingsaucer.extend.OutputDevice;
import com.github.neoflyingsaucer.render.RenderingContext;
import com.github.neoflyingsaucer.util.XRRuntimeException;

public class BoxRangeHelper {
    private final LinkedList<BoxRangeData> _clipRegionStack = new LinkedList<BoxRangeData>();
    
    private final OutputDevice _outputDevice;
    private final List<BoxRangeData> _rangeList;
    
    private int _rangeIndex = 0;
    private BoxRangeData _current = null;
    
    public BoxRangeHelper(final OutputDevice outputDevice, final List<BoxRangeData> rangeList) {
        _outputDevice = outputDevice;
        _rangeList = rangeList;
        
        if (rangeList.size() > 0) {
            _current = rangeList.get(0);
        }
    }
    
    public void checkFinished() {
        if (_clipRegionStack.size() != 0)
        {
        	assert(false);
        	throw new XRRuntimeException("internal error");
        }
    }
    
    public void pushClipRegion(final RenderingContext c, final int contentIndex) {
        while (_current != null && _current.getRange().getStart() == contentIndex) {
            _current.setClip(_outputDevice.getClip());
            _clipRegionStack.add(_current);
            
            _outputDevice.clip(_current.getBox().getChildrenClipEdge(c));
            
            if (_rangeIndex == _rangeList.size() - 1) {
                _current = null;
            } else {
                _current = _rangeList.get(++_rangeIndex);
            }
        }
    }
    
    public void popClipRegions(final RenderingContext c, final int contentIndex) {
        while (_clipRegionStack.size() > 0) {
            final BoxRangeData data = _clipRegionStack.getLast();
            if (data.getRange().getEnd() == contentIndex) {
                _outputDevice.setClip(data.getClip());
                _clipRegionStack.removeLast();
            } else {
                break;
            }
        }
    }
}

