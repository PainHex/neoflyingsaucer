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
package org.xhtmlrenderer.css.sheet;

import java.util.ArrayList;
import java.util.List;

import org.xhtmlrenderer.css.mediaquery.MediaQueryList;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import org.xhtmlrenderer.layout.SharedContext;

public class MediaRule implements RulesetContainer 
{
    private final List<Ruleset> _contents = new ArrayList<Ruleset>();
    private MediaQueryList mediaQueryList;
    private final CSSOrigin _origin;
    
    public MediaRule(final CSSOrigin origin) {
        _origin = origin;
    }
    
    public void setMediaQueryList(MediaQueryList mediaQueryList)
    {
    	this.mediaQueryList = mediaQueryList;
    }
    
    public boolean matches(final SharedContext ctx) 
    {
    	return mediaQueryList.eval(ctx);
    }
    
    @Override
    public void addContent(final Ruleset ruleset) {
        _contents.add(ruleset);
    }
    
    public List<Ruleset> getContents() {
        return _contents;
    }
    
    @Override
    public CSSOrigin getOrigin() {
        return _origin;
    }
}
