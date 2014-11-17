/*
 * Copyright (C) 2008 Permeance Technologies Pty Ltd. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.xhtmlrenderer.pdf.util;

import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.pdf.DefaultPDFCreationListener;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFCreationListener;
import org.xhtmlrenderer.util.NodeHelper;

import com.github.neoflyingsaucer.extend.useragent.Optional;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfString;


/**
 * <h1>Description</h1>
 * <p>
 * This PDF Creation Listener parses meta data elements from an (X)HTML document and appends them to 
 * the info dictionary of a PDF document.
 * </p>
 * 
 * <p> 
 * The XHTML document is parsed for relevant PDF meta data during construction, then adds the meta
 * data to the PDF document when the PDF document is closed by the calling ITextRenderer. 
 * </p>
 * 
 * <p>
 * Valid (X)HTML tags are:
 * <ul>
 * <li>TITLE</li>
 * </ul>
 * </p>
 *  
 * <p>
 * Valid (X)HTML meta tag attribute names are:
 * <ul>
 * <li>TITLE (optional), DC.TITLE</li>
 * <li>CREATOR, AUTHOR, DC.CREATOR</li>
 * <li>SUBJECT, DC.SUBJECT</li>
 * <li>KEYWORDS</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Valid PDF meta names are defined in Adobe's PDF Reference (Sixth Edition), 
 * section "10.2.1 - Document Information Dictionary", table 10.2, pg.844
 * http://www.adobe.com/devnet/pdf/pdf_reference.html
 * </p>
 * 
 * <h1>Usage</h1>
 * <pre>
 * // Setup output stream
 * OutputStream outputStream = ... 
 * 
 * // Create W3C document model
 * Document doc = ...
 * 
 * // Create new PDF renderer
 * ITextRenderer renderer = new ITextRenderer();
 * 
 * // Add PDF creation listener
 * PDFCreationListener pdfCreationListener = new XHtmlMetaToPdfInfoAdapter( doc );
 * renderer.setListener( pdfCreationListener);
 * 
 * // Add W3C document to renderer
 * renderer.setDocument( doc, null );
 *       
 * // Layout PDF document
 * renderer.layout();
 * 
 * // Write PDF document
 * renderer.createPDF( outputStream, true );
 * </pre>
 * 
 * <h1>Notes</h1>
 * This class was derived from a sample PDF creation listener
 * at "http://markmail.org/message/46t3bw7q6mbhvra2"
 * by Jesse Keller <jesse.keller@roche.com>.
 *
 * @author Tim Telcik <tim.telcik@permeance.com.au> 
 * 
 * @see DefaultPDFCreationListener
 * @see PDFCreationListener
 * @see ITextRenderer 
 * @see http://markmail.org/message/46t3bw7q6mbhvra2
 * @see http://www.adobe.com/devnet/pdf/pdf_reference.html
 * @see http://www.seoconsultants.com/meta-tags/dublin/
 */
public class XHtmlMetaToPdfInfoAdapter extends DefaultPDFCreationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(XHtmlMetaToPdfInfoAdapter.class);
    private static final String HTML_TAG_TITLE = "title";    
    private static final String HTML_TAG_HEAD = "head";    
    private static final String HTML_TAG_META = "meta";    
    private static final String HTML_META_KEY_TITLE = "title";    
    private static final String HTML_META_KEY_DC_TITLE = "DC.title";    
    private static final String HTML_META_KEY_CREATOR = "creator";    
    private static final String HTML_META_KEY_DC_CREATOR = "DC.creator";
    private static final String HTML_META_KEY_SUBJECT = "subject";    
    private static final String HTML_META_KEY_DC_SUBJECT = "DC.subject";    
    private static final String HTML_META_KEY_KEYWORDS = "keywords";    
    private static final String HTML_META_ATTR_NAME = "name";    
    private static final String HTML_META_ATTR_CONTENT = "content";    

    private final java.util.Map<PdfName, PdfString> pdfInfoValues = new HashMap<PdfName, PdfString>();    

    
    /**
     * Creates a new adapter from the given XHTML document.
     * 
     * @param doc XHTML document
     */
    public XHtmlMetaToPdfInfoAdapter( final Document doc ) {
        parseHtmlTags( doc );        
    }
    
    /**
     * PDFCreationListener onClose event handler.
     * 
     * @see PDFCreationListener
     */
    public void onClose( final ITextRenderer renderer ) {
        LOGGER.trace( "handling onClose event ..." );
        addPdfMetaValuesToPdfDocument( renderer );        
    }

    private void parseHtmlTags( final Document doc ) {
        LOGGER.trace( "parsing (X)HTML tags ..." );
        parseHtmlTitleTag( doc );
        parseHtmlMetaTags( doc );
        if ( LOGGER.isTraceEnabled() ) {
            LOGGER.trace( "PDF info map = " + pdfInfoValues );
        }
    }
    
    private void parseHtmlTitleTag( final Document doc ) 
    {
        final Optional<Element> rootHeadNodeElement = NodeHelper.getHead(doc);

        if (!rootHeadNodeElement.isPresent())
        	return;
        
        final Optional<Element> titleElement = NodeHelper.getFirstMatchingChildByTagName(rootHeadNodeElement.get(), "title");
        
        if ( titleElement.isPresent() ) {
            final String titleContent = titleElement.get().getTextContent();
            final PdfName pdfName = PdfName.TITLE;
            final PdfString pdfString = new PdfString( titleContent );
            this.pdfInfoValues.put( pdfName, pdfString );
        }
    }
    
    private void parseHtmlMetaTags( final Document doc ) {
        
        final Optional<Element> rootHeadNodeElement = NodeHelper.getHead(doc);

        if (!rootHeadNodeElement.isPresent())
        	return;
        
        NodeList nl = rootHeadNodeElement.get().getChildNodes();
        int length = nl.getLength();
        
        for (int i = 0; i < length; i++)
        {
        	Node n = nl.item(i);
        	if (!(n instanceof Element) ||
        		!n.getNodeName().equals("meta"))
        		continue;
        		
        	Element elem = (Element) n;
        	
        	if (!elem.hasAttribute("name") || 
        		!elem.hasAttribute("content"))
        		continue;
        	
        	final String metaName = elem.getAttribute("name");
            final String metaContent = elem.getAttribute("content");

            PdfName pdfName;
            PdfString pdfString;

            if ( HTML_META_KEY_TITLE.equalsIgnoreCase( metaName ) 
              || HTML_META_KEY_DC_TITLE.equalsIgnoreCase( metaName ) ) 
            {
                    pdfName = PdfName.TITLE;
                    pdfString = new PdfString( metaContent, PdfObject.TEXT_UNICODE );                    
                    this.pdfInfoValues.put( pdfName, pdfString );
            } 
            else if ( HTML_META_KEY_CREATOR.equalsIgnoreCase( metaName ) 
                     || HTML_META_KEY_DC_CREATOR.equalsIgnoreCase( metaName ) ) 
            {
                    pdfName = PdfName.AUTHOR;
                    pdfString = new PdfString( metaContent, PdfObject.TEXT_UNICODE );                    
                    this.pdfInfoValues.put( pdfName, pdfString );
            }
            else if ( HTML_META_KEY_SUBJECT.equalsIgnoreCase( metaName ) 
                     || HTML_META_KEY_DC_SUBJECT.equalsIgnoreCase( metaName ) ) 
            {
                    pdfName = PdfName.SUBJECT;
                    pdfString = new PdfString( metaContent, PdfObject.TEXT_UNICODE );                    
                    this.pdfInfoValues.put( pdfName, pdfString );
            }
            else if ( HTML_META_KEY_KEYWORDS.equalsIgnoreCase( metaName ) ) 
            {
                    pdfName = PdfName.KEYWORDS;
                    pdfString = new PdfString( metaContent, PdfObject.TEXT_UNICODE );                    
                    this.pdfInfoValues.put( pdfName, pdfString );
            }                
        }
    }
    
    /**
     * Add PDF meta values to the target PDF document.
     */
    private void addPdfMetaValuesToPdfDocument( final ITextRenderer renderer ) {
        
        final Iterator<PdfName> pdfNameIter = this.pdfInfoValues.keySet().iterator();

        while (pdfNameIter.hasNext()) {
            final PdfName pdfName = pdfNameIter.next();
            final PdfString pdfString = pdfInfoValues.get( pdfName );
            LOGGER.trace( "pdfName=" + pdfName + ", pdfString=" + pdfString );
            renderer.getOutputDevice().getWriter().getInfo().put( pdfName, pdfString );            
        }
        if ( LOGGER.isTraceEnabled()) {
            LOGGER.trace( "added " + renderer.getOutputDevice().getWriter().getInfo().getKeys() );            
        }
    }
    
}
