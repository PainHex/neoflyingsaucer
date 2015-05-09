/*
 * {{{ header & license
 * XRRuntimeException.java
 * Copyright (c) 2004, 2005 Patrick Wright
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
package org.xhtmlrenderer.util;

import com.github.neoflyingsaucer.extend.controller.error.FSError.FSErrorLevel;
import com.github.neoflyingsaucer.extend.controller.error.FSErrorController;
import com.github.neoflyingsaucer.extend.controller.error.LangId;



/**
 * General runtime exception used in XHTMLRenderer. Auto-logs messages to
 * plumbing.exception hierarchy.
 *
 * @author   Patrick Wright
 */
public class XRRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Exception with a "reason" message.
     *
     * @param msg  Reason the exception is being thrown.
     */
    public XRRuntimeException( final String msg ) {
        super( msg );
        log( msg );
    }

    /**
     * Instantiates a new Exception with a "reason" message.
     *
     * @param msg    Reason the exception is being thrown.
     * @param cause  Throwable that caused this exception to be thrown (e.g.
     *      IOException.
     */
    public XRRuntimeException( final String msg, final Throwable cause ) {
        super( msg, cause );
        log( msg, cause );
    }

    /**
     * Logs the exception message.
     *
     * @param msg  Message for the log.
     */
    private void log( final String msg ) {
        FSErrorController.log(XRRuntimeException.class, FSErrorLevel.ERROR, LangId.UNHANDLED_EXCEPTION, msg);
    }

    /**
     * Logs the exception's message, plus the Throwable that caused the
     * exception to be thrown.
     *
     * @param msg    Message for the log.
     * @param cause  Throwable that caused this exception to be thrown (e.g.
     *      IOException.
     */
    private void log( final String msg, final Throwable cause ) {
    	FSErrorController.log(XRRuntimeException.class, FSErrorLevel.ERROR, LangId.UNHANDLED_EXCEPTION_WITH_CAUSE, msg, cause);
    }
}
