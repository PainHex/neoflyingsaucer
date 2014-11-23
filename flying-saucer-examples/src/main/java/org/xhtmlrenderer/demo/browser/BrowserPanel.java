/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
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
package org.xhtmlrenderer.demo.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.swing.Java2DImageResolver;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;
import org.xhtmlrenderer.util.*;

import com.github.danfickle.flyingsaucer.swing.ScalableXHTMLPanel;
import com.github.neoflyingsaucer.defaultuseragent.HTMLResourceHelper;
import com.github.neoflyingsaucer.other.PDFRenderer;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.*;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;


/**
 * Description of the Class
 *
 * @author empty
 */
public class BrowserPanel extends JPanel implements DocumentListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserPanel.class);
	private static final long serialVersionUID = 1L;

	/**
	 * Description of the Field
	 */
	JButton forward;
	/**
	 * Description of the Field
	 */
	JButton backward;
	/**
	 * Description of the Field
	 */
	JButton stop;
	/**
	 * Description of the Field
	 */
	JButton reload;
	/**
	 * Description of the Field
	 */
	JButton goHome;
	/**
	 * Description of the Field
	 */
	JButton font_inc;
	/**
	 * Description of the Field
	 */
	JButton font_rst;
	/**
	 * Description of the Field
	 */
	JButton font_dec;
	JButton print;
	/**
	 * Description of the Field
	 */
	JTextField url;
	/**
	 * Description of the Field
	 */
	BrowserStatus status;
	/**
	 * Description of the Field
	 */
	public ScalableXHTMLPanel view;
	/**
	 * Description of the Field
	 */
	JScrollPane scroll;
	/**
	 * Description of the Field
	 */
	BrowserStartup root;
	/**
	 * Description of the Field
	 */
	BrowserPanelListener listener;

	JButton print_preview;

	private PanelManager manager;
	JButton goToPage;
	public JToolBar toolbar;

	private String currentUri = null;
	
	/**
	 * Constructor for the BrowserPanel object
	 *
	 * @param root	 PARAM
	 * @param listener PARAM
	 */
	public BrowserPanel(final BrowserStartup root, final BrowserPanelListener listener) {
		super();
		this.root = root;
		this.listener = listener;
	}

	/**
	 * Description of the Method
	 */
	public void init() {
		forward = new JButton();
		backward = new JButton();
		stop = new JButton();
		reload = new JButton();
		goToPage = new JButton();
		goHome = new JButton();

		url = new JTextField();
		url.addFocusListener(new FocusAdapter() {
			public void focusGained(final FocusEvent e) {
				super.focusGained(e);
				url.selectAll();
			}

			public void focusLost(final FocusEvent e) {
				super.focusLost(e);
				url.select(0, 0);
			}
		});


		manager = new PanelManager();
        view = new ScalableXHTMLPanel(manager);
        //final ImageResourceLoader irl = new ImageResourceLoaderImpl();
        //manager.setImageResourceLoader(irl);
        view.getSharedContext().setReplacedElementFactory(new SwingReplacedElementFactory());
        view.getSharedContext().setImageResolver(new Java2DImageResolver());
        view.addDocumentListener(manager);
        view.setCenteredPagedView(true);
        view.setBackground(Color.LIGHT_GRAY);
        scroll = new FSScrollPane(view);
		print_preview = new JButton();
		print = new JButton();

		loadCustomFonts();

		status = new BrowserStatus();
		status.init();

		initToolbar();

		final int text_width = 200;
		view.setPreferredSize(new Dimension(text_width, text_width));

		setLayout(new BorderLayout());
		this.add(scroll, BorderLayout.CENTER);
	}

	private void initToolbar() {
		toolbar = new JToolBar();
		toolbar.setRollover(true);
		toolbar.add(backward);
		toolbar.add(forward);
		toolbar.add(reload);
		toolbar.add(goHome);
		toolbar.add(url);
		toolbar.add(goToPage);
		// disabled for R6
		// toolbar.add(print);
        toolbar.setFloatable(false);
    }

	private void loadCustomFonts() {
		final SharedContext rc = view.getSharedContext();
		try {
			rc.setFontMapping("Fuzz", Font.createFont(Font.TRUETYPE_FONT,
					new DemoMarker().getClass().getResourceAsStream("/demos/fonts/fuzz.ttf")));
		} catch (final Exception ex) {
			Uu.p(ex);
		}
	}

	/**
	 * Description of the Method
	 */
	public void createLayout() {
		final GridBagLayout gbl = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		setLayout(gbl);

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(toolbar, c);
		add(toolbar);

		//c.gridx = 0;
		c.gridx++;
		c.gridy++;
		c.weightx = c.weighty = 0.0;
		c.insets = new Insets(5, 0, 5, 5);
		gbl.setConstraints(backward, c);
		add(backward);

		c.gridx++;
		gbl.setConstraints(forward, c);
		add(forward);

		c.gridx++;
		gbl.setConstraints(reload, c);
		add(reload);

		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		gbl.setConstraints(print_preview, c);
		add(print_preview);

		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 5;
		c.ipady = 5;
		c.weightx = 10.0;
		c.insets = new Insets(5, 0, 5, 0);
		gbl.setConstraints(url, c);
		url.setBorder(BorderFactory.createLoweredBevelBorder());
		add(url);

		c.gridx++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = c.weighty = 0.0;
		c.insets = new Insets(0, 5, 0, 0);
		gbl.setConstraints(goToPage, c);
		add(goToPage);

		c.gridx = 0;
		c.gridy++;
		c.ipadx = 0;
		c.ipady = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 7;
		c.weightx = c.weighty = 10.0;
		gbl.setConstraints(scroll, c);
		add(scroll);

		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0.1;
		gbl.setConstraints(status, c);
		add(status);

	}

	/**
	 * Description of the Method
	 */
	public void createActions() {
		// set text to "" to avoid showing action text in button--
		// we only want it in menu items
		backward.setAction(root.actions.backward);
		backward.setText("");
		forward.setAction(root.actions.forward);
		forward.setText("");
		reload.setAction(root.actions.reload);
		reload.setText("");
		goHome.setAction(root.actions.goHome);
		goHome.setText("");
		print_preview.setAction(root.actions.print_preview);
		print_preview.setText("");

		url.setAction(root.actions.load);
		goToPage.setAction(root.actions.goToPage);
		updateButtons();
	}


	/**
	 * Description of the Method
	 */
	public void goForward() {
		final String uri = manager.getForward();
		view.setDocument(uri);
		updateButtons();
	}

	/**
	 * Description of the Method
	 */
	public void goBack() {
		final String uri = manager.getBack();
		view.setDocument(uri);
		updateButtons();
	}

	/**
	 * Description of the Method
	 */
	public void reloadPage() {
		LOGGER.info("Reloading Page: ");
		if (currentUri != null) {
			loadPage(currentUri);
		}
	}

	/**
	 * Description of the Method
	 *
	 * @param url_text PARAM
	 */
	//TODO: make this part of an implementation of UserAgentCallback instead
	public void loadPage(final String url_text) {
		try {
			LOGGER.info("Loading Page: " + url_text);
			view.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			currentUri = url_text;
			view.setDocument(url_text);
			view.addDocumentListener(BrowserPanel.this);

			updateButtons();

			setStatus("Successfully loaded: " + url_text);

			if (listener != null) {
				listener.pageLoadSuccess(url_text, view.getDocumentTitle());
			}
		} catch (final XRRuntimeException ex) {
			LOGGER.error("Runtime exception", ex);
            setStatus("Can't load document");
            handlePageLoadFailed(url_text, ex);
        } catch (final Exception ex) {
			LOGGER.error("Could not load page for display.", ex);
			ex.printStackTrace();
		}
	}
	
	public void exportToPdf( final String path )
	{
		try {
			PDFRenderer.renderToPDF(currentUri, path, PdfWriter.VERSION_1_7, this.manager);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
//       if (manager.getBaseURL() != null) {
//           setStatus( "Exporting to " + path + "..." );
//           OutputStream os = null;
//           try {
//               os = new FileOutputStream(path);
//               try {
//               ITextRenderer renderer = new ITextRenderer();
//
//               Document doc = XMLResource.load().getDocument();
//               
//               Document doc =  db.parse(manager.getBaseURL().);
//
//               PDFCreationListener pdfCreationListener = new XHtmlMetaToPdfInfoAdapter( doc );
//               renderer.setListener( pdfCreationListener );
//                              
//               renderer.setDocument(manager.getBaseURL());
//               renderer.layout();
//
//               renderer.createPDF(os);
//               setStatus( "Done export." );
//            } catch (Exception e) {
//                LOGGER.error"Could not export PDF.", e);
//                e.printStackTrace();
//                setStatus( "Error exporting to PDF." );
//               } finally {
//                   try {
//                       os.close();
//                   } catch (IOException e) {
//                       // swallow
//            }
//        }
//           } catch (Exception e) {
//               e.printStackTrace();
//	}
//       }
	}

    private void handlePageLoadFailed(final String url_text, final XRRuntimeException ex) {
        final HTMLResourceHelper xr;
        final String rootCause = getRootCause(ex);
        final String msg = GeneralUtil.escapeHTML(addLineBreaks(rootCause, 80));
        final String notFound =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE html PUBLIC \" -//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
                        "<body>\n" +
                        "<h1>Document can't be loaded</h1>\n" +
                        "<p>Could not load the page at \n" +
                        "<pre>" + GeneralUtil.escapeHTML(url_text) + "</pre>\n" +
                        "</p>\n" +
                        "<p>The page failed to load; the error was </p>\n" +
                        "<pre>" + msg + "</pre>\n" +
                        "</body>\n" +
                        "</html>";

        xr = HTMLResourceHelper.load(notFound);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                root.panel.view.setDocument(xr.getDocument(), null);
            }
        });
   }

    private String addLineBreaks(final String _text, final int maxLineLength) {
        final StringBuffer broken = new StringBuffer(_text.length() + 10);
        boolean needBreak = false;
        for (int i = 0; i < _text.length(); i++) {
            if (i > 0 && i % maxLineLength == 0) needBreak = true;

            final char c = _text.charAt(i);
            if (needBreak && Character.isWhitespace(c)) {
                System.out.println("Breaking: " + broken.toString());
                needBreak = false;
                broken.append('\n');
            } else {
                broken.append(c);
            }
        }
        System.out.println("Broken! " + broken.toString());
        return broken.toString();  
    }

    private String getRootCause(final Exception ex) {
        // FIXME
        Throwable cause = ex;
        while (cause != null) {
            cause = cause.getCause();
        }

        return cause == null ? ex.getMessage() : cause.getMessage();
    }

    public void documentStarted() {
		// TODO...
	}

	/**
	 * Description of the Method
	 */
	public void documentLoaded() {
		view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Sets the status attribute of the BrowserPanel object
	 *
	 * @param txt The new status value
	 */
	public void setStatus(final String txt) {
		status.text.setText(txt);
	}

	/**
	 * Description of the Method
	 */
	protected void updateButtons() {
		if (manager.hasBack()) {
			root.actions.backward.setEnabled(true);
		} else {
			root.actions.backward.setEnabled(false);
		}
		if (manager.hasForward()) {
			root.actions.forward.setEnabled(true);
		} else {
			root.actions.forward.setEnabled(false);
		}

		url.setText(currentUri);
	}


	public void onLayoutException(final Throwable t) {
        // TODO: clean
        t.printStackTrace();
	}

	public void onRenderException(final Throwable t) {
        // TODO: clean
		t.printStackTrace();
	}
}
