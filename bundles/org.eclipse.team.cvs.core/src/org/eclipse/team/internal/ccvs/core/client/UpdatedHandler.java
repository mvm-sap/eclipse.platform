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
package org.eclipse.team.internal.ccvs.core.client;


import java.util.Date;

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Handles any "Updated" and "Merged" responses
 * from the CVS server.
 * <p>
 * Suppose as a result of performing a command the CVS server responds
 * as follows:<br>
 * <pre>
 *   [...]
 *   Updated ???\n
 *   [...]
 * </pre>
 * Then 
 * </p>
 */

/**
 * Does get information about the file that is updated
 * and the file-content itself and puts it on the fileSystem.
 * 
 * The difference beetween the "Updated" and the "Merged" is, that
 * an "Merged" file is not going to be up-to-date after the operation.
 * 
 * Requiers a exisiting parent-folder.
 */
class UpdatedHandler extends ResponseHandler {
	
	private int handlerType;
	
	protected static final int HANDLE_UPDATED = ICVSFile.UPDATED;
	protected static final int HANDLE_MERGED = ICVSFile.MERGED;
	protected static final int HANDLE_UPDATE_EXISTING = ICVSFile.UPDATE_EXISTING;
	protected static final int HANDLE_CREATED = ICVSFile.CREATED;
	
	private static final String READ_ONLY_FLAG = "u=rw"; //$NON-NLS-1$
	
	public UpdatedHandler(int handlerType) {
		this.handlerType = handlerType;
	}
	
	public String getResponseID() {
		switch (handlerType) {
			case HANDLE_UPDATED: return "Updated"; //$NON-NLS-1$
			case HANDLE_MERGED: return "Merged"; //$NON-NLS-1$
			case HANDLE_UPDATE_EXISTING: return "Update-existing"; //$NON-NLS-1$
			case HANDLE_CREATED: return "Created"; //$NON-NLS-1$
		}
		return null;
	}

	public void handle(Session session, String localDir,
		IProgressMonitor monitor) throws CVSException {
		// read additional data for the response
		String repositoryFile = session.readLine();
		String entryLine = session.readLine();
		byte[] entryLineBytes = entryLine.getBytes(); /* TODO: could read as bytes */
		String permissionsLine = session.readLine();

		// clear file update modifiers
		Date modTime = session.getModTime();
		session.setModTime(null);
		
		// Get the local file
		String fileName = repositoryFile.substring(repositoryFile.lastIndexOf("/") + 1); //$NON-NLS-1$
		ICVSFolder mParent = getExistingFolder(session, localDir);
		ICVSFile mFile = mParent.getFile(fileName);
		
		boolean binary = ResourceSyncInfo.isBinary(entryLineBytes);
		boolean readOnly = permissionsLine.indexOf(READ_ONLY_FLAG) == -1;
		
		// The file may have been set as read-only by a previous checkout/update
		if (mFile.isReadOnly()) mFile.setReadOnly(false);
		try {
			session.receiveFile(mFile, binary, handlerType, monitor);
		} catch (CVSException e) {
			if (e.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
				// Record that we have a case collision and continue;
				session.addCaseCollision(new Path(localDir).append(fileName).toString(), Path.EMPTY.toString());
				return;
			} else {
				throw e;
			}
		}
		if (readOnly) mFile.setReadOnly(true);
		
		// Set the timestamp in the file and get it again so that we use the *real* timestamp
		// in the sync info. The os may not actually set the time we provided :)
		mFile.setTimeStamp(modTime);
		modTime = mFile.getTimeStamp();
		int modificationState = ICVSFile.UNKNOWN;
		if(handlerType==HANDLE_MERGED) {
			entryLineBytes = ResourceSyncInfo.setTimeStamp(entryLineBytes, modTime, true /* merged */);
		} else {
			entryLineBytes = ResourceSyncInfo.setTimeStamp(entryLineBytes, modTime, false /* merged */);
			if (!session.isIgnoringLocalChanges() && (handlerType==HANDLE_UPDATE_EXISTING || handlerType==HANDLE_CREATED)) {
			// both these cases result in an unmodified file.
				// reporting is handled by the FileModificationManager
				modificationState = ICVSFile.CLEAN;
				CVSProviderPlugin.getPlugin().getFileModificationManager().updated(mFile);
			}
		}
		mFile.setSyncBytes(entryLineBytes, modificationState);
	}
}
