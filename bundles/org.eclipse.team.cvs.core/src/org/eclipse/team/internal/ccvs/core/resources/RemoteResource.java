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
package org.eclipse.team.internal.ccvs.core.resources;

 
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * The purpose of this class and its subclasses is to implement the corresponding
 * ICVSRemoteResource interfaces for the purpose of communicating information about 
 * resources that reside in a CVS repository but have not necessarily been loaded
 * locally.
 */
public abstract class RemoteResource extends PlatformObject implements ICVSRemoteResource {

	protected RemoteFolder parent;
	protected String name;

	// relative synchronization state calculated by server of this remote file compare to the current local 
	// workspace copy.
	private int workspaceSyncState = Update.STATE_NONE;

	/**
	 * Constructor for RemoteResource.
	 */
	public RemoteResource(RemoteFolder parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	/*
	 * @see ICVSRemoteResource#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * @see ICVSResource#getRelativePath(ICVSFolder)
	 */
	public String getRelativePath(ICVSFolder ancestor) throws CVSException {
		return Util.appendPath(parent.getRelativePath(ancestor), getName());
	}
	
	/*
	 * @see ICVSRemoteResource#getParent()
	 */
	public ICVSRemoteResource getRemoteParent() {
		return parent;
	}
			
	public abstract String getRepositoryRelativePath();
	
	public abstract ICVSRepositoryLocation getRepository();
	
 	public int getWorkspaceSyncState() {
 		return workspaceSyncState;
 	}
 	
 	public void setWorkspaceSyncState(int workspaceSyncState) {
 		this.workspaceSyncState = workspaceSyncState;
 	}
	
	/*
	 * @see ICVSResource#delete()
	 */
	public void delete() {
		// For now, do nothing but we could provide this in the future.
	}

	/*
	 * @see ICVSResource#exists()
	 * 
	 * This method is used by the Command framework so it must return true so that 
	 * the proper information gets sent to the server. (i.e. it is used to fake that 
	 * the file exists locally so cvs commands can be used to retrieve information about
	 * the remote resource from the server)
	 */
	public boolean exists() {
		return true;
	}
	
	/*
	 * @see ICVSRemoteResource#exists(IProgressMonitor)
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException {
		return parent.exists(this, monitor);
	}

	/*
	 * @see ICVSResource#getParent()
	 */
	public ICVSFolder getParent() {
		return parent;
 	}

	/*
	 * @see ICVSResource#isIgnored()
	 */
	public boolean isIgnored() {
		return false;
	}

	/*
	 * @see ICVSResource#isManaged()
	 */
	public boolean isManaged() {
		return parent != null;
	}

	public boolean isModified(IProgressMonitor monitor) throws CVSException {
		// it is safe to always consider a remote file handle as modified. This will cause any
		// CVS command to fetch new contents from the server.
		return true;
	}
	
	/*
	 * @see ICVSResource#unmanage()
	 */
	public void unmanage(IProgressMonitor monitor) throws CVSException {
		// do nothing
	}

	/*
	 * @see ICVSResource#getSyncInfo()
	 */
	public abstract ResourceSyncInfo getSyncInfo();
	
	/*
	 * @see ICVSResource#setSyncInfo(ResourceSyncInfo)
	 */
	public void setSyncInfo(ResourceSyncInfo info, int modificationState) {
		// ensure that clients are not trying to set sync info on remote handles.
		Assert.isTrue(false);
	}
	
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof RemoteResource))
			return false;
		RemoteResource remote = (RemoteResource) target;
		return remote.isContainer() == isContainer() && remote.getRepositoryRelativePath().equals(getRepositoryRelativePath());
	}
	/*
	 * @see ICVSResource#setIgnored()
	 */
	public void setIgnored() throws CVSException {
		// ensure that clients are not trying to set sync info on remote handles.
		Assert.isTrue(false);
	}

	/*
	 * @see ICVSResource#setIgnoredAs(String)
	 */
	public void setIgnoredAs(String pattern) throws CVSException {
		// ensure that clients are not trying to set sync info on remote handles.
		Assert.isTrue(false);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResource#getIResource()
	 */
	public IResource getIResource() throws CVSException {
		return null;
	}
	
	/**
	 * Return a copy of the receiver that is associated with the given tag. The parent
	 * should be a copy of the receiver's parent which has been copied to the same tag.
	 * 
	 * @param parent
	 * @param tagName
	 * @return ICVSRemoteFolder
	 */
	public abstract ICVSRemoteResource forTag(ICVSRemoteFolder parent, CVSTag tagName);

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getRepositoryRelativePath().hashCode();
	}

}
