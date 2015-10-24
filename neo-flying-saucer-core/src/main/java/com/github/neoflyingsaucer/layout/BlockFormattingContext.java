/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
 * Copyright (c) 2005 Wisconsin Court System
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

import java.awt.Point;

import com.github.neoflyingsaucer.css.style.CssContext;
import com.github.neoflyingsaucer.render.BlockBox;
import com.github.neoflyingsaucer.render.Box;
import com.github.neoflyingsaucer.render.LineBox;

/**
 * This class represents a block formatting context as defined in the CSS spec.
 * Its main purpose is to provide BFC relative coordinates for a {@link FloatManager}.
 * This coordinate space is used when positioning floats and calculating the
 * amount of space floated boxes take up at a given y position.
 *
 * <b>NOTE:</b> The {@link #translate(int, int)} method must be called when a
 * block box in the normal flow is moved (i.e. its static position changes)
 */
public class BlockFormattingContext {
    private int _x = 0;
    private int _y = 0;

    private final PersistentBFC _persistentBFC;

    public BlockFormattingContext(final BlockBox block, final LayoutContext c) {
        _persistentBFC = new PersistentBFC(block, c);
    }

    public Point getOffset() {
        return new Point(_x, _y);
    }

    public void translate(final int x, final int y) {
        _x -= x;
        _y -= y;
    }

    public FloatManager getFloatManager() {
        return _persistentBFC.getFloatManager();
    }

    public int getLeftFloatDistance(final CssContext cssCtx, final LineBox line, final int containingBlockWidth) {
        return getFloatManager().getLeftFloatDistance(cssCtx, this, line, containingBlockWidth);
    }

    public int getRightFloatDistance(final CssContext cssCtx, final LineBox line, final int containingBlockWidth) {
        return getFloatManager().getRightFloatDistance(cssCtx, this, line, containingBlockWidth);
    }

    public int getFloatDistance(final CssContext cssCtx, final LineBox line, final int containingBlockWidth) {
        return getLeftFloatDistance(cssCtx, line, containingBlockWidth) +
                    getRightFloatDistance(cssCtx, line, containingBlockWidth);
    }

    public int getNextLineBoxDelta(final CssContext cssCtx, final LineBox line, final int containingBlockWidth) {
        return getFloatManager().getNextLineBoxDelta(cssCtx, this, line, containingBlockWidth);
    }

    public void floatBox(final LayoutContext c, final BlockBox floated) {
        getFloatManager().floatBox(c, c.getLayer(), this, floated);
    }

    public void clear(final LayoutContext c, final Box current) {
        getFloatManager().clear(c, this, current);
    }

    public String toString() {
        return "BlockFormattingContext: (" + _x + "," + _y + ")";
    }
}