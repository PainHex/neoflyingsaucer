/*
 * Copyright (c) 2005 Patrick Wright
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.css.style.derived;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.ValueConstants;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.DerivedValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.layout.SharedContext;

import com.github.neoflyingsaucer.extend.output.FontSpecificationI;

public class LengthValue extends DerivedValue {


    private static final Logger LOGGER = LoggerFactory.getLogger(LengthValue.class);
    private final static int MM__PER__CM = 10;
    private final static float CM__PER__IN = 2.54F;
    private final static float PT__PER__IN = 1f / 72f;
    private final static float PC__PER__PT = 12;

    /**
     * The specified length value, as a float; pulled from the CSS text
     */
    private final float _lengthAsFloat;
    
    private final CalculatedStyle _style;

    /**
     * The specified primitive SAC data type given for this length, from the CSS text
     */
    private final CSSPrimitiveUnit _lengthPrimitiveType;
    
    public LengthValue(final CalculatedStyle style, final CSSName name, final PropertyValue value) {
        super(name, value.getPrimitiveTypeN(), value.getCssText(), value.getCssText());
        
        _style = style;
        _lengthAsFloat = value.getFloatValue();
        _lengthPrimitiveType = value.getPrimitiveTypeN();
    }

    public float asFloat() {
        return _lengthAsFloat;
    }

    /**
     * Computes a relative unit (e.g. percentage) as an absolute value, using
     * the input value. Used for such properties whose parent value cannot be
     * known before layout/render
     *
     * @param cssName   Name of the property
     * @param baseValue
     * @param ctx
     * @return the absolute value or computed absolute value
     */
    public float getFloatProportionalTo(final CSSName cssName,
                                        final float baseValue,
                                        final CssContext ctx) {
        return calcFloatProportionalValue(getStyle(),
                cssName,
                getStringValue(),
                _lengthAsFloat,
                _lengthPrimitiveType,
                baseValue,
                ctx);
    }

    public boolean hasAbsoluteUnit() {
        return ValueConstants.isAbsoluteUnit(getCssSacUnitType());
    }
    
    public boolean isDependentOnFontSize() {
        return _lengthPrimitiveType == CSSPrimitiveUnit.CSS_EXS ||
               _lengthPrimitiveType == CSSPrimitiveUnit.CSS_EMS;
    }

    public static float calcFloatProportionalValue(final CalculatedStyle style,
                                                      final CSSName cssName,
                                                      final String stringValue,
                                                      final float relVal,
                                                      final CSSPrimitiveUnit primitiveType,
                                                      float baseValue,
                                                      final CssContext ctx) {

        float absVal = Float.MIN_VALUE;

        // NOTE: we used to cache absolute values, but have removed that to see if it
        // really makes a difference, since the calcs are so simple. In any case, for DPI-relative
        // values we shouldn't be caching, unless we also check if the DPI is changed, which
        // would seem to obviate the advantage of caching anyway.
        switch (primitiveType) {
        	// TODO: Other primitive types.
            case CSS_PX:
                absVal = relVal * ctx.getDotsPerPixel();
                break;
            case CSS_IN:
                absVal = (((relVal * CM__PER__IN) * MM__PER__CM) / ctx.getMmPerDot());
                break;
            case CSS_CM:
                absVal = ((relVal * MM__PER__CM) / ctx.getMmPerDot());
                break;
            case CSS_MM:
                absVal = relVal / ctx.getMmPerDot();
                break;
            case CSS_PT:
                absVal = (((relVal * PT__PER__IN) * CM__PER__IN) * MM__PER__CM) / ctx.getMmPerDot();
                break;
            case CSS_PC:
                absVal = ((((relVal * PC__PER__PT) * PT__PER__IN) * CM__PER__IN) * MM__PER__CM) / ctx.getMmPerDot();
                break;
            case CSS_EMS:
                // EM is equal to font-size of element on which it is used
                // The exception is when ?em? occurs in the value of
                // the ?font-size? property itself, in which case it refers
                // to the calculated font size of the parent element
                // http://www.w3.org/TR/CSS21/fonts.html#font-size-props
                if (cssName == CSSName.FONT_SIZE) {
                    final FontSpecification parentFont = style.getParent().getFont(ctx);
                    //font size and FontSize2D should be identical
                    absVal = relVal * parentFont.size;//ctx.getFontSize2D(parentFont);
                } else {
                    absVal = relVal * style.getFont(ctx).size;//ctx.getFontSize2D(style.getFont(ctx));
                }

                break;
            case CSS_EXS:
                // To convert EMS to pixels, we need the height of the lowercase 'Xx' character in the current
                // element...
                // to the font size of the parent element (spec: 4.3.2)
                float xHeight;
                if (cssName == CSSName.FONT_SIZE) {
                    final FontSpecificationI parentFont = style.getParent().getFont(ctx);
                    xHeight = ctx.getXHeight(parentFont);
                } else {
                    final FontSpecificationI font = style.getFont(ctx);
                    xHeight = ctx.getXHeight(font);
                }
                absVal = relVal * xHeight;

                break;
            case CSS_PERCENTAGE:
                // percentage depends on the property this value belongs to
                if (cssName == CSSName.VERTICAL_ALIGN) {
                    baseValue = style.getParent().getLineHeight(ctx);
                } else if (cssName == CSSName.FONT_SIZE) {
                    // same as with EM
                    final FontSpecification parentFont = style.getParent().getFont(ctx);
                    baseValue = parentFont.size;// WAS:   ctx.getFontSize2D(parentFont);
                } else if (cssName == CSSName.LINE_HEIGHT) {
                    final FontSpecificationI font = style.getFont(ctx);
                    baseValue = ctx.getFontSize2D(font);
                }
                absVal = (relVal / 100f) * baseValue;

                break;
            case CSS_NUMBER:
                absVal = relVal;
                break;
            default:
                // nothing to do, we only convert those listed above
                LOGGER.error("Asked to convert " + cssName + " from relative to absolute, " +
                        " don't recognize the datatype " +
                        "'" + ValueConstants.stringForSACPrimitiveType(primitiveType) + "' "
                        + primitiveType + "(" + stringValue + ")");
        }
        //assert (new Float(absVal).intValue() >= 0);

        if (LOGGER.isTraceEnabled()) {
            if (cssName == CSSName.FONT_SIZE) {
                LOGGER.trace(cssName + ", relative= " +
                        relVal + " (" + stringValue + "), absolute= "
                        + absVal);
            } else {
                LOGGER.trace(cssName + ", relative= " +
                        relVal + " (" + stringValue + "), absolute= "
                        + absVal + " using base=" + baseValue);
            }
        }

        final double d = Math.round((double) absVal);
        absVal = new Float(d).floatValue();
        return absVal;
    }
    
    private CalculatedStyle getStyle() {
        return _style;
    }

    /**
     * Convert a value to pixels. Used for media query values.
     */
	public static float calcFloatProportionalValue(PropertyValue cssValue, SharedContext ctx) 
	{
	       float absVal = Float.MIN_VALUE;

	       if (cssValue.getCssValueTypeN() == CSSValueType.CSS_PRIMITIVE_VALUE)
	       {
	    	   CSSPrimitiveUnit primitiveType = cssValue.getPrimitiveTypeN();
	    	   float relVal = cssValue.getFloatValue();
	    	   
	    	   switch (primitiveType) {
	    	   // TODO: Other types that are needed for media queries.
	           case CSS_PX:
	                absVal = relVal * ctx.getDotsPerPixel();
	                break;
	           case CSS_IN:
	                absVal = (((relVal * CM__PER__IN) * MM__PER__CM) / (ctx.getMmPerPx() * ctx.getDotsPerPixel()));
	                break;
	           case CSS_CM:
	                absVal = ((relVal * MM__PER__CM) / (ctx.getMmPerPx() * ctx.getDotsPerPixel()));
	                break;
	           case CSS_MM:
	                absVal = relVal / (ctx.getMmPerPx() * ctx.getDotsPerPixel());
	                break;
	           case CSS_PT:
	                absVal = (((relVal * PT__PER__IN) * CM__PER__IN) * MM__PER__CM) / (ctx.getMmPerPx() * ctx.getDotsPerPixel());
	                break;
	           case CSS_PC:
	                absVal = ((((relVal * PC__PER__PT) * PT__PER__IN) * CM__PER__IN) * MM__PER__CM) / (ctx.getMmPerPx() * ctx.getDotsPerPixel());
	                break;
	           case CSS_NUMBER:
	                absVal = relVal;
	                break;
	           case CSS_DPPX:
	        	    absVal = relVal;
	        	    break;
	           case CSS_DPI:
	        	    relVal = relVal / CM__PER__IN;
	        	    // Fallthrough
	           case CSS_DPCM:
	        	    absVal = (relVal / MM__PER__CM) / ctx.getMmPerPx();
	        	    break;
	           default:
	        	   LOGGER.warn("Asked to convert value for media query: {}", ValueConstants.stringForSACPrimitiveType(primitiveType));
	    	   }
	       }
	       else
	       {
	    	   LOGGER.warn("Asked to convert value for media query: {}", cssValue.getCssValueTypeN().toString());
	       }

	       return absVal;
	}
}
