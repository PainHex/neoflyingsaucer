/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
 * }}}
 */
package com.github.neoflyingsaucer.j2dout;

import com.github.neoflyingsaucer.extend.output.FSFont;
import com.github.neoflyingsaucer.extend.output.FontResolver;
import com.github.neoflyingsaucer.extend.output.FontSpecificationI;
import com.github.neoflyingsaucer.extend.output.FontSpecificationI.FontStyle;
import com.github.neoflyingsaucer.extend.output.FontSpecificationI.FontVariant;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.Serializable;
import java.util.HashMap;


/**
 * Description of the Class
 *
 * @author Joshua Marinacci
 */
public class Java2DFontResolver implements FontResolver {
    /**
     * Description of the Field
     */
    HashMap<String, Font> instance_hash;
    /**
     * Description of the Field
     */
    HashMap<String, Serializable> available_fonts_hash;

    /**
     * Constructor for the FontResolverTest object
     */
    public Java2DFontResolver() {
        init();
    }
    
    private void init() {
        final GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final String[] available_fonts = gfx.getAvailableFontFamilyNames();
        //Uu.p("available fonts =");
        //Uu.p(available_fonts);
        instance_hash = new HashMap<String, Font>();

        // preload the font map with the font names as keys
        // don't add the actual font objects because that would be a waste of memory
        // we will only add them once we need to use them
        // put empty strings in instead
        available_fonts_hash = new HashMap<String, Serializable>();
        for (final String available_font : available_fonts) {
            available_fonts_hash.put(available_font, "");
        }

        // preload sans, serif, and monospace into the available font hash
        available_fonts_hash.put("Serif", new Font("Serif", Font.PLAIN, 1));
        available_fonts_hash.put("SansSerif", new Font("SansSerif", Font.PLAIN, 1));
        //Uu.p("put in sans serif");
        available_fonts_hash.put("Monospaced", new Font("Monospaced", Font.PLAIN, 1));
    }
    
    public void flushCache() {
        init();
    }

    /**
     * Description of the Method
     *
     * @param ctx
     * @param families PARAM
     * @param size     PARAM
     * @param weight   PARAM
     * @param style    PARAM
     * @param variant  PARAM
     * @return Returns
     */
    public FSFont resolveFont(final String[] families, final float size, final int weight, final FontStyle style, final FontVariant variant) 
    {
        //Uu.p("familes = ");
        //Uu.p(families);
        // for each font family
        if (families != null) {
            for (final String family : families) {
                final Font font = resolveFont(family, size, weight, style, variant);
                if (font != null) {
                    return new Java2DFont(font);
                }
            }
        }

        // if we get here then no font worked, so just return default sans
        //Uu.p("pulling out: -" + available_fonts_hash.get("SansSerif") + "-");
        String family = "SansSerif";
        if (style == FontStyle.ITALIC) {
            family = "Serif";
        }

        final Font fnt = createFont((Font) available_fonts_hash.get(family), size, weight, style, variant);
        instance_hash.put(getFontInstanceHashName(family, size, weight, style, variant), fnt);
        //Uu.p("subbing in base sans : " + fnt);
        return new Java2DFont(fnt);
    }

    /**
     * Sets the fontMapping attribute of the FontResolver object
     *
     * @param name The new fontMapping value
     * @param font The new fontMapping value
     */
    public void setFontMapping(final String name, final Font font) {
        available_fonts_hash.put(name, font.deriveFont(1f));
    }

    /**
     * Description of the Method
     *
     * @param ctx
     * @param root_font PARAM
     * @param size      PARAM
     * @param weight    PARAM
     * @param style     PARAM
     * @param variant   PARAM
     * @return Returns
     */
    protected static Font createFont(final Font root_font, float size, final int weight, final FontStyle style, final FontVariant variant) 
    {
        //Uu.p("creating font: " + root_font + " size = " + size +
        //    " weight = " + weight + " style = " + style + " variant = " + variant);
        int font_const = Font.PLAIN;
        if (weight >= 600) 
        {
            font_const = font_const | Font.BOLD;
        }

        if (style != null && (style == FontStyle.ITALIC || style == FontStyle.OBLIQUE)) 
        {
            font_const = font_const | Font.ITALIC;
        }

        // scale vs font scale value too
        // TODO: Take font scaling into consideration.
        //size *= ctx.getTextRenderer().getFontScale();

        Font fnt = root_font.deriveFont(font_const, size);
        if (variant != null) {
            if (variant == FontVariant.SMALL_CAPS) {
                fnt = fnt.deriveFont((float) (((float) fnt.getSize()) * 0.6));
            }
        }

        return fnt;
    }

    /**
     * Description of the Method
     *
     * @param ctx
     * @param font    PARAM
     * @param size    PARAM
     * @param weight  PARAM
     * @param style   PARAM
     * @param variant PARAM
     * @return Returns
     */
    protected Font resolveFont(String font, final float size, final int weight, final FontStyle style, final FontVariant variant) {
        //Uu.p("here");
        // strip off the "s if they are there
        if (font.startsWith("\"")) {
            font = font.substring(1);
        }
        if (font.endsWith("\"")) {
            font = font.substring(0, font.length() - 1);
        }

        //Uu.p("final font = " + font);
        // normalize the font name
        if (font.equals("serif")) {
            font = "Serif";
        }
        if (font.equals("sans-serif")) {
            font = "SansSerif";
        }
        if (font.equals("monospace")) {
            font = "Monospaced";
        }

        if (font.equals("Serif") && style == FontStyle.OBLIQUE) font = "SansSerif";
        if (font.equals("SansSerif") && style == FontStyle.ITALIC) font = "Serif";

        // assemble a font instance hash name
        final String font_instance_name = getFontInstanceHashName(font, size, weight, style, variant);
        //Uu.p("looking for font: " + font_instance_name);
        // check if the font instance exists in the hash table
        if (instance_hash.containsKey(font_instance_name)) {
            // if so then return it
            return instance_hash.get(font_instance_name);
        }

        //Uu.p("font lookup failed for: " + font_instance_name);
        //Uu.p("searching for : " + font + " " + size + " " + weight + " " + style + " " + variant);


        // if not then
        //  does the font exist
        if (available_fonts_hash.containsKey(font)) {
            //Uu.p("found an available font for: " + font);
            final Object value = available_fonts_hash.get(font);
            // have we actually allocated the root font object yet?
            Font root_font = null;
            if (value instanceof Font) {
                root_font = (Font) value;
            } else {
                root_font = new Font(font, Font.PLAIN, 1);
                available_fonts_hash.put(font, root_font);
            }

            // now that we have a root font, we need to create the correct version of it
            final Font fnt = createFont(root_font, size, weight, style, variant);

            // add the font to the hash so we don't have to do this again
            instance_hash.put(font_instance_name, fnt);
            return fnt;
        }

        // we didn't find any possible matching font, so just return null
        return null;
    }

    /**
     * Gets the fontInstanceHashName attribute of the FontResolverTest object
     *
     * @param ctx
     *@param name    PARAM
     * @param size    PARAM
     * @param weight  PARAM
     * @param style   PARAM
     * @param variant PARAM @return The fontInstanceHashName value
     */
    protected static String getFontInstanceHashName(final String name, final float size, final int weight, final FontStyle style, final FontVariant variant) 
    {
       // return name + "-" + (size * ctx.getTextRenderer().getFontScale()) + "-" + weight + "-" + style + "-" + variant;
    	 return name + "-" + (size) + "-" + weight + "-" + style + "-" + variant;
    }

    @Override
    public FSFont resolveFont(FontSpecificationI spec) 
    {
        return resolveFont(spec.getFamilies(), spec.getSize(), spec.getFontWeight(), spec.getStyle(), spec.getVariant());
    }
}

