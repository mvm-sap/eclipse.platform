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
package org.eclipse.ui.externaltools.internal.variable;


import org.eclipse.core.resources.IResource;

/**
 * Expands a resource's container type variable into the desired
 * result format.
 * <p>
 * This class is not intended to be extended by clients.
 * </p>
 */
public class ContainerExpander extends ResourceExpander {

	/**
	 * Create an instance
	 */
	public ContainerExpander() {
		super();
	}

	/* (non-Javadoc)
	 * Method declared on ResourceExpander.
	 */
	/*package*/ IResource expand(String varValue, ExpandVariableContext context) {
		IResource resource = super.expand(varValue, context);
		if (resource != null) {
			return resource.getParent();
		}
		return null;
	}
}
