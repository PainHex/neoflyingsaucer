/*
 * NaiveUserAgent.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
 *
 */
package com.github.danfickle.flyingsaucer.swing;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.HTMLResource;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.swing.StylesheetCache;
import org.xhtmlrenderer.util.ImageUtil;

import com.github.neoflyingsaucer.defaultuseragent.StylesheetCacheImpl;

/**
 * <p>NaiveUserAgent is a simple implementation of {@link UserAgentCallback} which places no restrictions on what
 * XML, CSS or images are loaded, and reports visited links without any filtering. The most straightforward process
 * available in the JDK is used to load the resources in question--either using java.io or java.net classes.
 *
 * <p>The NaiveUserAgent has a small cache for images,
 * the size of which (number of images) can be passed as a constructor argument. There is no automatic cleaning of
 * the cache; call {@link #shrinkImageCache()} to remove the least-accessed elements--for example, you might do this
 * when a new document is about to be loaded. The NaiveUserAgent is also a DocumentListener; if registered with a
 * source of document events (like the panel hierarchy), it will respond to the
 * {@link org.xhtmlrenderer.event.DocumentListener#documentStarted()} call and attempt to shrink its cache.
 *
 * <p>This class is meant as a starting point--it will work out of the box, but you should really implement your
 * own, tuned to your application's needs.
 *
 * @author Torbjoern Gannholm
 */
public class NaiveUserAgent implements UserAgentCallback, DocumentListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NaiveUserAgent.class);

    private static final int DEFAULT_IMAGE_CACHE_SIZE = 16;

    /**
     * a (simple) LRU cache
     */
    protected LinkedHashMap<String, ImageResource> _imageCache;

    protected StylesheetCache _styleCache = new StylesheetCacheImpl();
    
    private final int _imageCacheCapacity;
    private String _baseURL;

    /**
     * Creates a new instance of NaiveUserAgent with a max image cache of 16 images.
     */
    public NaiveUserAgent() {
        this(DEFAULT_IMAGE_CACHE_SIZE);
    }

    /**
     * Creates a new NaiveUserAgent with a cache of a specific size.
     *
     * @param imgCacheSize Number of images to hold in cache before LRU images are released.
     */
    public NaiveUserAgent(final int imgCacheSize) {
        this._imageCacheCapacity = imgCacheSize;

        // note we do *not* override removeEldestEntry() here--users of this class must call shrinkImageCache().
        // that's because we don't know when is a good time to flush the cache
        this._imageCache = new java.util.LinkedHashMap<String, ImageResource>(_imageCacheCapacity, 0.75f, true);
    }

    /**
     * If the image cache has more items than the limit specified for this class, the least-recently used will
     * be dropped from cache until it reaches the desired size.
     */
    public void shrinkImageCache() {
        int ovr = _imageCache.size() - _imageCacheCapacity;
        final Iterator<String> it = _imageCache.keySet().iterator();
        while (it.hasNext() && ovr-- > 0) {
            it.next();
            it.remove();
        }
    }

    /**
     * Empties the image cache entirely.
     */
    public void clearImageCache() {
        _imageCache.clear();
    }

    /**
     * Gets a Reader for the resource identified
     *
     * @param uri PARAM
     * @return The stylesheet value
     */
    //TOdO:implement this with nio.
    protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        uri = resolveURI(uri);
        try {
            is = new URL(uri).openStream();
        } catch (final java.net.MalformedURLException e) {
           LOGGER.error("bad URL given: " + uri, e);
        } catch (final java.io.FileNotFoundException e) {
           LOGGER.error("item at URI " + uri + " not found");
        } catch (final java.io.IOException e) {
           LOGGER.error("IO problem for " + uri, e);
        }
        return is;
    }

    /**
     * Retrieves the CSS located at the given URI.  It's assumed the URI does point to a CSS file--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the CSS parser.
     * The result is packed up into an CSSResource for later consumption.
     *
     * @param uri Location of the CSS source.
     * @return A CSSResource containing the parsed CSS.
     */
    public CSSResource getCSSResource(final String uri) {
        return new CSSResource(resolveAndOpenStream(uri));
    }

    /**
     * Retrieves the image located at the given URI. It's assumed the URI does point to an image--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the JDK image-parsing routines.
     * The result is packed up into an ImageResource for later consumption.
     *
     * @param uri Location of the image source.
     * @return An ImageResource containing the image.
     */
    public ImageResource getImageResource(String uri) {
        ImageResource ir;
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            final BufferedImage image = ImageUtil.loadEmbeddedBase64Image(uri);
            ir = createImageResource(null, image);
        } else {
            uri = resolveURI(uri);
            ir = _imageCache.get(uri);
            //TODO: check that cached image is still valid
            if (ir == null) {
                final InputStream is = resolveAndOpenStream(uri);
                if (is != null) {
                    try {
                        final BufferedImage img = ImageIO.read(is);
                        if (img == null) {
                            throw new IOException("ImageIO.read() returned null");
                        }
                        ir = createImageResource(uri, img);
                        _imageCache.put(uri, ir);
                    } catch (final FileNotFoundException e) {
                       LOGGER.error("Can't read image file; image at URI '" + uri + "' not found");
                    } catch (final IOException e) {
                       LOGGER.error("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                    } finally {
                        try {
                            is.close();
                        } catch (final IOException e) {
                            // ignore
                        }
                    }
                }
            }
            if (ir == null) {
                ir = createImageResource(uri, null);
            }
        }
        return ir;
    }

    /**
     * Factory method to generate ImageResources from a given Image. May be overridden in subclass. 
     *
     * @param uri The URI for the image, resolved to an absolute URI.
     * @param img The image to package; may be null (for example, if image could not be loaded).
     *
     * @return An ImageResource containing the image.
     */
    protected ImageResource createImageResource(final String uri, final Image img) {
        return new ImageResource(uri, AWTFSImage.createImage(img));
    }

    /**
     * Retrieves the XML located at the given URI. It's assumed the URI does point to a XML--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the XML parser (XMLReader)
     * configured for Flying Saucer. The result is packed up into an XMLResource for later consumption.
     *
     * @param uri Location of the XML source.
     * @return An XMLResource containing the image.
     */
    public HTMLResource getXMLResource(final String uri) {
        final InputStream inputStream = resolveAndOpenStream(uri);
        HTMLResource xmlResource;
        try {
            xmlResource = HTMLResource.load(inputStream, uri);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    // swallow
                }
            }
        }
        return xmlResource;
    }

    public byte[] getBinaryResource(final String uri) {
        InputStream is = resolveAndOpenStream(uri);
        try {
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            final byte[] buf = new byte[10240];
            int i;
            while ((i = is.read(buf)) != -1) {
                result.write(buf, 0, i);
            }
            is.close();
            is = null;

            return result.toByteArray();
        } catch (final IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Returns true if the given URI was visited, meaning it was requested at some point since initialization.
     *
     * @param uri A URI which might have been visited.
     * @return Always false; visits are not tracked in the NaiveUserAgent.
     */
    @Override
    public boolean isVisited(final String uri) {
        return false;
    }

    /**
     * URL relative to which URIs are resolved.
     *
     * @param url A URI which anchors other, possibly relative URIs.
     */
    @Override
    public void setBaseURL(final String url) {
        _baseURL = url;
    }

    /**
     * Resolves the URI; if absolute, leaves as is, if relative, returns an absolute URI based on the baseUrl for
     * the agent.
     *
     * @param uri A URI, possibly relative.
     *
     * @return A URI as String, resolved, or null if there was an exception (for example if the URI is malformed).
     */
    @Override
    public String resolveURI(final String uri) {
        if (uri == null) return null;
        String ret = null;
        if (_baseURL == null) {//first try to set a base URL
            try {
                final URL result = new URL(uri);
                setBaseURL(result.toExternalForm());
            } catch (final MalformedURLException e) {
                try {
                    setBaseURL(new File(".").toURI().toURL().toExternalForm());
                } catch (final Exception e1) {
                   LOGGER.error("The default NaiveUserAgent doesn't know how to resolve the base URL for " + uri);
                    return null;
                }
            }
        }
        // test if the URI is valid; if not, try to assign the base url as its parent
        try {
            return new URL(uri).toString();
        } catch (final MalformedURLException e) {
            LOGGER.info(uri + " is not a URL; may be relative. Testing using parent URL " + _baseURL);
            try {
                final URL result = new URL(new URL(_baseURL), uri);
                ret = result.toString();
            } catch (final MalformedURLException e1) {
               LOGGER.error("The default NaiveUserAgent cannot resolve the URL " + uri + " with base URL " + _baseURL);
            }
        }
        return ret;
    }

    /**
     * Returns the current baseUrl for this class.
     */
    @Override
    public String getBaseURL() {
        return _baseURL;
    }

    @Override
    public void documentStarted() {
        shrinkImageCache();
    }

    @Override
    public void documentLoaded() { /* ignore*/ }

    @Override
    public void onLayoutException(final Throwable t) { /* ignore*/ }

    @Override
    public void onRenderException(final Throwable t) { /* ignore*/ }

	@Override
	public StylesheetCache getStylesheetCache() {
		return _styleCache;
	}
}
