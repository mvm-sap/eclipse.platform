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
package org.eclipse.team.internal.ccvs.core;

 
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;

 /**
  * This interface represents a file in a repository.
  * Instances of this interface can be used to fetch the contents
  * of the remote file.
  * 
  * In the future, additional information should be available (tags, revisions, etc.)
  * 
  * Clients are not expected to implement this interface.
  */
public interface ICVSRemoteFile extends ICVSRemoteResource, ICVSFile {

	/**
	 * Get the log entry for the revision the remote file represents.
	 * This method will return null until after the getContents(IProgressMonitor)
	 * method is called (i.e. the call to getContents also fetches the entry.
	 */
	public ILogEntry getLogEntry(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Get all the log entries of the remote file
	 */
	public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Get the revision of the remote file (e.g. 1.1)
	 * 
	 * The revision depends on any tagging associated with the remote parent used
	 * to access the file. 
	 */
	public String getRevision() throws TeamException;
}

