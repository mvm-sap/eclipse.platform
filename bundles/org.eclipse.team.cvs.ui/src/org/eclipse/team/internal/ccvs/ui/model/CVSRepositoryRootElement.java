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
package org.eclipse.team.internal.ccvs.ui.model;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;

/**
 * RemoteRootElement is the model element for a repository that
 * appears in the repositories view. Its children are:
 * a) HEAD
 * b) Branch tags category
 * c) Version tags category
 */
public class CVSRepositoryRootElement extends CVSModelElement {
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof ICVSRepositoryLocation || object instanceof RepositoryRoot) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_REPOSITORY);
		}
		return null;
	}
	public String getLabel(Object o) {
		if (o instanceof ICVSRepositoryLocation) {
			ICVSRepositoryLocation root = (ICVSRepositoryLocation)o;
			return root.getLocation();
		}
		if (o instanceof RepositoryRoot) {
			RepositoryRoot root = (RepositoryRoot)o;
			String name = root.getName();
			if (name == null)
				return root.getRoot().getLocation();
			else
				return name;
		}
		return null;
	}
	public Object getParent(Object o) {
		return null;
	}
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
		ICVSRepositoryLocation location = null;
		if (o instanceof ICVSRepositoryLocation) {
			location = (ICVSRepositoryLocation)o;
		}
		if (o instanceof RepositoryRoot) {
			RepositoryRoot root = (RepositoryRoot)o;
			location = root.getRoot();
		}
		if (location == null) return null;
		return new Object[] {
			new CVSTagElement(CVSTag.DEFAULT, location),
			new BranchCategory(location),
			new VersionCategory(location)
		};
	}
}
