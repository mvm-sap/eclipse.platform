/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core.model;

/**
 * An expression that can report errors which occurred during the
 * expression's evaluation.
 */
public interface IErrorReportingExpression extends IExpression {
	/**
	 * Returns whether this expression has errors to report. An expression
	 * can have errors if errors were generated the last time its value was
	 * computed
	 * 
	 * @return whether this expression's result has errors
	 */
	public boolean hasErrors();
	/**
	 * Returns this expression's error messages, if any. An expression can
	 * have errors if errors were generated the last time its value was
	 * computed.
	 *  
	 * @return this expression's error messages
	 */
	public String[] getErrorMessages();
}
