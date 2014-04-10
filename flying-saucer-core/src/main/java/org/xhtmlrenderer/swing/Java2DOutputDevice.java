/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.swing.*;

import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient.StopValue;
import org.xhtmlrenderer.extend.FSGlyphVector;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.render.AbstractOutputDevice;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.BorderPainter;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.InlineLayoutBox;
import org.xhtmlrenderer.render.InlineText;
import org.xhtmlrenderer.render.JustificationInfo;
import org.xhtmlrenderer.render.RenderingContext;

public class Java2DOutputDevice extends AbstractOutputDevice implements OutputDevice {
    private final Graphics2D _graphics;

    public Java2DOutputDevice(final Graphics2D graphics) {
        _graphics = graphics;
    }

    public Java2DOutputDevice(final BufferedImage outputImage) {
        this(outputImage.createGraphics());
    }
    
    @Override
    public void drawSelection(final RenderingContext c, final InlineText inlineText) {
        if (inlineText.isSelected()) {
            final InlineLayoutBox iB = inlineText.getParent();
            final String text = inlineText.getSubstring();
            if (text != null && text.length() > 0) {
                final FSFont font = iB.getStyle().getFSFont(c);
                final FSGlyphVector glyphVector = c.getTextRenderer().getGlyphVector(
                        c.getOutputDevice(),
                        font,
                        inlineText.getSubstring());
                
                final Rectangle start = c.getTextRenderer().getGlyphBounds(
                        c.getOutputDevice(),
                        font,
                        glyphVector,
                        inlineText.getSelectionStart(),
                        iB.getAbsX() + inlineText.getX(),
                        iB.getAbsY() + iB.getBaseline());
                
                final Rectangle end = c.getTextRenderer().getGlyphBounds(
                        c.getOutputDevice(),
                        font,
                        glyphVector,
                        inlineText.getSelectionEnd() - 1,
                        iB.getAbsX() + inlineText.getX(),
                        iB.getAbsY() + iB.getBaseline());
                final Graphics2D graphics = getGraphics();
                final double scaleX = graphics.getTransform().getScaleX();
                final boolean allSelected = (text.length() == inlineText.getSelectionEnd()-inlineText.getSelectionStart());
                final int startX = (inlineText.getSelectionStart() == inlineText.getStart())?iB.getAbsX() + inlineText.getX():(int)Math.round(start.x/scaleX);
                final int endX = (allSelected)?startX+inlineText.getWidth():(int)Math.round((end.x + end.width)/scaleX);
                _graphics.setColor(UIManager.getColor("TextArea.selectionBackground"));  // FIXME
                fillRect(
                        startX,
                        iB.getAbsY(),
                        endX - startX,
                        iB.getHeight());
                
                _graphics.setColor(Color.WHITE); // FIXME
                setFont(iB.getStyle().getFSFont(c));                
                
                drawSelectedText(c, inlineText, iB, glyphVector);
            }
        }
    }

    private void drawSelectedText(final RenderingContext c, final InlineText inlineText, final InlineLayoutBox iB, final FSGlyphVector glyphVector) {
        final GlyphVector vector = ((AWTFSGlyphVector)glyphVector).getGlyphVector();
        
        // We'd like to draw only the characters that are actually selected, but 
        // unfortunately vector.getGlyphPixelBounds() doesn't give us accurate
        // results with the result that text can appear to jump around as it's
        // selected.  To work around this, we draw the whole string, but move
        // non-selected characters offscreen.
        for (int i = 0; i < inlineText.getSelectionStart(); i++) {
            vector.setGlyphPosition(i, new Point2D.Float(-100000, -100000));
        }
        for (int i = inlineText.getSelectionEnd(); i < inlineText.getSubstring().length(); i++) {
            vector.setGlyphPosition(i, new Point2D.Float(-100000, -100000));
        }
        if(inlineText.getParent().getStyle().isTextJustify()) {
            final JustificationInfo info = inlineText.getParent().getLineBox().getJustificationInfo();
            if(info!=null) {
                final String string = inlineText.getSubstring();
                float adjust = 0.0f;
                for (int i = inlineText.getSelectionStart(); i < inlineText.getSelectionEnd(); i++) {
                    final char ch = string.charAt(i);
                    if (i != 0) {
                        final Point2D point = vector.getGlyphPosition(i);
                        vector.setGlyphPosition(
                                i, new Point2D.Double(point.getX() + adjust, point.getY()));
                    }
                    if (ch == ' ' || ch == '\u00a0' || ch == '\u3000') {
                        adjust += info.getSpaceAdjust();
                    } else {
                        adjust += info.getNonSpaceAdjust();
                    }
                }

            }
        }
        c.getTextRenderer().drawGlyphVector(
                c.getOutputDevice(),
                glyphVector,
                iB.getAbsX() + inlineText.getX(),
                iB.getAbsY() + iB.getBaseline());
    }    

    @Override
    public void drawBorderLine(
            final Rectangle bounds, final int side, final int lineWidth, final boolean solid) {
    	
    	final int x = bounds.x;
        final int y = bounds.y;
        final int w = bounds.width;
        final int h = bounds.height;
        
        final int adj = solid ? 1 : 0;
        
        if (side == BorderPainter.TOP) {
            drawLine(x, y + (int) (lineWidth / 2), x + w - adj, y + (int) (lineWidth / 2));
        } else if (side == BorderPainter.LEFT) {
            drawLine(x + (int) (lineWidth / 2), y, x + (int) (lineWidth / 2), y + h - adj);
        } else if (side == BorderPainter.RIGHT) {
            int offset = (int)(lineWidth / 2);
            if (lineWidth % 2 != 0) {
                offset += 1;
            }
            drawLine(x + w - offset, y, x + w - offset, y + h - adj);
        } else if (side == BorderPainter.BOTTOM) {
            int offset = (int)(lineWidth / 2);
            if (lineWidth % 2 != 0) {
                offset += 1;
            }
            drawLine(x, y + h - offset, x + w - adj, y + h - offset);
        }
    }

    @Override
    public void paintReplacedElement(final RenderingContext c, final BlockBox box) {
        final ReplacedElement replaced = box.getReplacedElement();
//      if (replaced instanceof SwingReplacedElement) {
// TODO
//            Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
//            JComponent component = ((SwingReplacedElement)box.getReplacedElement()).getJComponent();
//            RootPanel canvas = (RootPanel)c.getCanvas();
//            CellRendererPane pane = canvas.getCellRendererPane();
//            pane.paintComponent(_graphics, component, canvas, contentBounds.x,  contentBounds.y, contentBounds.width, contentBounds.height,true);
//        }
    if (replaced instanceof ImageReplacedElement) {
            final Image image = ((ImageReplacedElement)replaced).getImage();
            
            final Point location = replaced.getLocation();
            _graphics.drawImage(
                    image, (int)location.getX(), (int)location.getY(), null);
        }
    }
    
    @Override
    public void setColor(final FSColor color) {
        if (color instanceof FSRGBColor) {
            final FSRGBColor rgb = (FSRGBColor) color;
            _graphics.setColor(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(),(int) (rgb.getAlpha() * 255)));
        } else {
            throw new RuntimeException("internal error: unsupported color class " + color.getClass().getName());
        }
    }
    
    @Override
    protected void drawLine(final int x1, final int y1, final int x2, final int y2) {
        _graphics.drawLine(x1, y1, x2, y2);
    }
    
    @Override
    public void drawRect(final int x, final int y, final int width, final int height) {
        _graphics.drawRect(x, y, width, height);
    }
    
    @Override
    public void fillRect(final int x, final int y, final int width, final int height) {
        _graphics.fillRect(x, y, width, height);
    }
    
    @Override
    public void setClip(final Shape s) {
        _graphics.setClip(s);
    }
    
    @Override
    public Shape getClip() {
        return _graphics.getClip();
    }

    @Override    
    public void clip(final Shape s) {
        _graphics.clip(s);
    }
    
    @Override
    public void translate(final double tx, final double ty) {
        _graphics.translate(tx, ty);
    }
    
    public Graphics2D getGraphics() {
        return _graphics;
    }

    @Override
    public void drawOval(final int x, final int y, final int width, final int height) {
        _graphics.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(final int x, final int y, final int width, final int height) {
        _graphics.fillOval(x, y, width, height);
    }

    @Override
    public Object getRenderingHint(final Key key) {
        return _graphics.getRenderingHint(key);
    }

    @Override
    public void setRenderingHint(final Key key, final Object value) {
        _graphics.setRenderingHint(key, value);
    }
    
    @Override
    public void setFont(final FSFont font) {
        _graphics.setFont(((AWTFSFont)font).getAWTFont());
    }

    @Override
    public void setStroke(final Stroke s) {
        _graphics.setStroke(s);
    }

    @Override
    public Stroke getStroke() {
        return _graphics.getStroke();
    }

    @Override
    public void fill(final Shape s) {
        _graphics.fill(s);
    }

    @Override
    public void drawImage(final FSImage image, final int x, final int y) {
        _graphics.drawImage(((AWTFSImage)image).getImage(), x, y, null);
    }
    
    @Override
    public boolean isSupportsSelection() {
        return true;
    }
    
    @Override
    public boolean isSupportsCMYKColors() {
        return true;
    }

	@Override
	public void drawBorderLine(final Shape bounds, final int side, final int width, final boolean solid) {
		draw(bounds);
	}

	@Override
	public void draw(final Shape s) {
		_graphics.draw(s);
	}

	@Override
	public void drawLinearGradient(final FSLinearGradient gradient, final int x, final int y, final int width, final int height) 
	{
		final float[] fractions = new float[gradient.getStopPoints().size()];
		final Color[] colors = new Color[gradient.getStopPoints().size()];

		final float range = gradient.getStopPoints().get(gradient.getStopPoints().size() - 1).getLength() -
				gradient.getStopPoints().get(0).getLength();
		
		int i = 0;
		for (final StopValue pt : gradient.getStopPoints())
		{
	        if (pt.getColor() instanceof FSRGBColor) 
	        {
	            final FSRGBColor rgb = (FSRGBColor) pt.getColor();
	            colors[i] = new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), (int) (rgb.getAlpha() * 255));
	        }
	        else {
	        	assert(false);
	        	throw new RuntimeException("internal error: unsupported color class " + pt.getColor().getClass().getName());
	        }

	        if (range != 0)
	        	fractions[i] = (pt.getLength() / range);
	        
	        i++;
		}

		final LinearGradientPaint paint = new LinearGradientPaint(
				gradient.getStartX() + x, gradient.getStartY() + y,
				gradient.getEndX() + x, gradient.getEndY() + y, fractions, colors);
		
		_graphics.setPaint(paint);
		_graphics.fillRect(x, y, x + width, y + width);
		_graphics.setPaint(null);
	}

	@Override
	public void setOpacity(final float opacity) 
	{
		if (opacity == 1)
		{
			_graphics.setComposite(AlphaComposite.SrcOver);
		}
		else
		{
			_graphics.setComposite(AlphaComposite.SrcOver.derive(opacity));
		}
	}
}
