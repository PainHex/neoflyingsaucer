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
package org.xhtmlrenderer.css.parser.property;

import java.util.ArrayList;
import java.util.List;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import org.xhtmlrenderer.util.LangId;

import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public class ListStylePropertyBuilder implements PropertyBuilder {
    private static final CSSName[] ALL = new CSSName[] {
        CSSName.LIST_STYLE_TYPE, CSSName.LIST_STYLE_POSITION, CSSName.LIST_STYLE_IMAGE }; 
    
    public List<PropertyDeclaration> buildDeclarations(final CSSName cssName, final List<PropertyValue> values, final CSSOrigin origin, final boolean important, final boolean inheritAllowed) {
        List<PropertyDeclaration> result = checkInheritAll(ALL, values, origin, important, inheritAllowed);
        if (result != null) {
            return result;
        }
        
        PropertyDeclaration listStyleType = null;
        PropertyDeclaration listStylePosition = null;
        PropertyDeclaration listStyleImage = null;
        
        for (final PropertyValue value : values) {
            checkInheritAllowed(value, false);
            final CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type == CSSPrimitiveUnit.CSS_IDENT) {
                final IdentValue ident = checkIdent(CSSName.LIST_STYLE_SHORTHAND, value);
                
                if (ident == IdentValue.NONE) {
                    if (listStyleType == null) {
                        listStyleType = new PropertyDeclaration(
                                CSSName.LIST_STYLE_TYPE, value, important, origin);
                    }
                    
                    if (listStyleImage == null) {
                        listStyleImage = new PropertyDeclaration(
                                CSSName.LIST_STYLE_IMAGE, value, important, origin);
                    }
                } else if (IdentSet.LIST_STYLE_POSITIONS.contains(ident)) {
                    if (listStylePosition != null) {
                        throw new CSSParseException(LangId.NO_TWICE, -1, "list-style-position");
                    }
                    
                    listStylePosition = new PropertyDeclaration(
                            CSSName.LIST_STYLE_POSITION, value, important, origin);
                } else if (IdentSet.LIST_STYLE_TYPES.contains(ident)) {
                    if (listStyleType != null) {
                        throw new CSSParseException(LangId.NO_TWICE, -1, "list-style-type");
                    }
                    
                    listStyleType = new PropertyDeclaration(
                            CSSName.LIST_STYLE_TYPE, value, important, origin);
                }
            } else if (type == CSSPrimitiveUnit.CSS_URI) {
                if (listStyleImage != null) {
                    throw new CSSParseException(LangId.NO_TWICE, -1, "list-style-image");
                }
                
                listStyleImage = new PropertyDeclaration(
                        CSSName.LIST_STYLE_IMAGE, value, important, origin);
            }
        }
        
        result = new ArrayList<PropertyDeclaration>(3);
        if (listStyleType != null) {
            result.add(listStyleType);
        }
        if (listStylePosition != null) {
            result.add(listStylePosition);
        }
        if (listStyleImage != null) {
            result.add(listStyleImage);
        }
        
        return result;
    }
}
