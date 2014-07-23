/*
 * StylesheetInfo.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
package org.xhtmlrenderer.css.sheet;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.css.mediaquery.MediaQueryList;
import org.xhtmlrenderer.layout.SharedContext;

/**
 * A reference to a stylesheet. If no stylesheet is set, the matcher will try to
 * find the stylesheet by uri, first from the StylesheetFactory cache, then by
 * loading the uri if it is not cached. <p>
 *
 * Therefore, either a stylesheet must be set, or a uri must be set <p>
 *
 * Origin defaults to USER_AGENT and media defaults to "all"
 *
 * @author   Torbjoern Gannholm
 */
public class StylesheetInfo {

	private static final Logger LOGGER = LoggerFactory.getLogger(StylesheetInfo.class);
	
	// Just to be able to attach "dummy" stylesheets. Also might save a lookup if it's already looked up
	private Stylesheet stylesheet = null;

	private String title;
    private Optional<String> uri;
    private CSSOrigin origin = CSSOrigin.USER_AGENT;
    private String type;
    private MediaQueryList mediaQueryList;
    private String content;

    public static enum CSSOrigin
    {
    	/** Origin of stylesheet - user agent  */
    	USER_AGENT,

    	/** Origin of stylesheet - user  */
    	USER,

    	/** Origin of stylesheet - author  */
    	AUTHOR,
    }

    /**
     * @param m  a single media identifier
     * @param _context 
     * @return   true if the stylesheet referenced applies to the medium
     */
    public boolean appliesToMedia(SharedContext context) 
    {
    	// mediaQueryList may be null.
    	if (mediaQueryList == null)
    		return true;
    	else
    		return mediaQueryList.eval(context);
    }

    /**
     * Sets the uri attribute of the StylesheetInfo object
     *
     * @param uri  The new uri value
     */
    public void setUri( final Optional<String> uri ) {
        this.uri = uri;
    }

    /**
     * Sets the origin attribute of the StylesheetInfo object
     *
     * @param origin  The new origin value
     */
    public void setOrigin( final CSSOrigin origin ) {
        this.origin = origin;
    }

    /**
     * Sets the type attribute of the StylesheetInfo object
     *
     * Currently we only support "text/css".
     */
    public void setType( final String type ) {
        this.type = type;
    }

    /**
     * Sets the title attribute of the StylesheetInfo object
     *
     * @param title  The new title value
     */
    public void setTitle( final String title ) {
        this.title = title;
    }

    /**
     * Sets the stylesheet attribute of the StylesheetInfo object
     *
     * @param stylesheet  The new stylesheet value
     */
    public void setStylesheet( final Stylesheet stylesheet ) {
        this.stylesheet = stylesheet;
    }

    /**
     * Gets the uri attribute of the StylesheetInfo object
     *
     * @return   The uri value
     */
    public Optional<String> getUri() {
        return uri;
    }

    public MediaQueryList getMediaQueryList() {
        return mediaQueryList;
    }

    /**
     * Gets the origin attribute of the StylesheetInfo object
     *
     * @return   The origin value
     */
    public CSSOrigin getOrigin() {
        return origin;
    }

    /**
     * Gets the type attribute of the StylesheetInfo object
     *
     * @return   The type value
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the title attribute of the StylesheetInfo object
     *
     * @return   The title value
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the stylesheet attribute of the StylesheetInfo object
     *
     * @return   The stylesheet value
     */
    public Optional<Stylesheet> getStylesheet() {
        return Optional.ofNullable(stylesheet);
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }
    
    public boolean isInline() {
        return this.content != null;
    }

	public void setMediaQueryList(MediaQueryList mediaQueryList) 
	{
		this.mediaQueryList = mediaQueryList;
	}
}

