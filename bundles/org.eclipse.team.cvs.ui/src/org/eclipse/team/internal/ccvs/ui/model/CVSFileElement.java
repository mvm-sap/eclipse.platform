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
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.ui.PlatformUI;

public class CVSFileElement extends CVSResourceElement {
	
	private ICVSFile file;
	
	public CVSFileElement(ICVSFile file) {
		this.file = file;
	}
	
	/**
	 * Initial implementation: return null;
	 */
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
		return new Object[0];
	}
	/**
	 * Initial implementation: return null.
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(file.getName());
	}
	/**
	 * Initial implementation: return the file's name and version
	 */
	public String getLabel(Object o) {
		try {
			ResourceSyncInfo info = file.getSyncInfo();
			if(info!=null) {
				return file.getName() + " " + info.getRevision(); //$NON-NLS-1$
			} else {
				return file.getName();
			}
		} catch (TeamException e) {
			handle(e);
			return null;
		}
	}
	/**
	 * @see IWorkbenchAdapter#getParent(Object)
	 */
	public Object getParent(Object o) {
		return null;
	}
	
	public ICVSFile getCVSFile() {
		return file;
	}
	/**
	 * @see CVSResourceElement#getCVSResource()
	 */
	public ICVSResource getCVSResource() {
		return file;
	}
}
