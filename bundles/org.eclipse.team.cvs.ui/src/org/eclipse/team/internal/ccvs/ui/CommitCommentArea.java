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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;

/**
 * This area provides the widgets for providing the CVS commit comment
 */
public class CommitCommentArea extends DialogArea {

	private static final int WIDTH_HINT = 350;
	private static final int HEIGHT_HINT = 150;
	
	private Text text;
	private Combo previousCommentsCombo;
	private IProject mainProject;
	private String[] comments = new String[0];
	private String comment = ""; //$NON-NLS-1$
	
	public static final String OK_REQUESTED = "OkRequested";//$NON-NLS-1$
	
	/**
	 * Constructor for CommitCommentArea.
	 * @param parentDialog
	 * @param settings
	 */
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings) {
		super(parentDialog, settings);
		comments = CVSUIPlugin.getPlugin().getRepositoryManager().getPreviousComments();
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.DialogArea#createArea(org.eclipse.swt.widgets.Composite)
	 */
	public Control createArea(Composite parent) {
		Composite composite = createGrabbingComposite(parent, 1);
		initializeDialogUnits(composite);
						
		Label label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(Policy.bind("ReleaseCommentDialog.enterComment")); //$NON-NLS-1$
				
		text = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = WIDTH_HINT;
		data.heightHint = HEIGHT_HINT;
		
		text.setLayoutData(data);
		text.selectAll();
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) != 0) {
					e.doit = false;
					CommitCommentArea.this.signalCtrlEnter();
				}
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				comment = text.getText();
			}
		});
		
		
		label = new Label(composite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(Policy.bind("ReleaseCommentDialog.choosePrevious")); //$NON-NLS-1$
		
		previousCommentsCombo = new Combo(composite, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		previousCommentsCombo.setLayoutData(data);
		previousCommentsCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = previousCommentsCombo.getSelectionIndex();
				if (index != -1)
					text.setText(comments[index]);
			}
		});
		
		
		initializeValues();
		return composite;
	}

	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		
		// populate the previous comment list
		for (int i = 0; i < comments.length; i++) {
			previousCommentsCombo.add(flattenText(comments[i]));
		}
		
		// determine the initial comment text
		String initialComment;
		try {
			initialComment = getCommitTemplate();
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			initialComment = null;
		}
		if (initialComment != null && initialComment.length() != 0) {
			text.setText(initialComment);
		}
	}

	/*
	 * Flatten the text in the multiline comment
	 * @param string
	 * @return String
	 */
	private String flattenText(String string) {
		StringBuffer buffer = new StringBuffer(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(Policy.bind("separator")); //$NON-NLS-1$
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}

	/**
	 * Method signalCtrlEnter.
	 */
	private void signalCtrlEnter() {
		firePropertyChangeChange(OK_REQUESTED, null, null);
	}

	/**
	 * Method clearCommitText.
	 */
	private void clearCommitText() {
		try {
			text.setText(getCommitTemplate());
			previousCommentsCombo.deselectAll();
		} catch (CVSException e) {
			CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC);
		}
	}

	private String getCommitTemplate() throws CVSException {
		CVSTeamProvider provider = getProvider();
		if (provider == null) return ""; //$NON-NLS-1$
		String template = provider.getCommitTemplate();
		if (template == null) template = ""; //$NON-NLS-1$
		return template;
	}
	
	/**
	 * Method getProvider.
	 */
	private CVSTeamProvider getProvider() throws CVSException {
		if (mainProject == null) return null;
		return (CVSTeamProvider) RepositoryProvider.getProvider(mainProject, CVSProviderPlugin.getTypeId());
	}
	
	/**
	 * Method getSelectedComment.
	 * @return String
	 */
	private String getSelectedComment() {
		if (comments.length == 0) {
			// There are no previous comments so use the template
			try {
				return getCommitTemplate();
			} catch (CVSException e) {
				// log the exception for now. 
				// The user can surface the problem by trying to reset the comment
				CVSUIPlugin.log(e);
			}
		} else {
			int index = previousCommentsCombo.getSelectionIndex();
			if (index != -1)
				return comments[index];
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Return the entered comment
	 * 
	 * @return the comment
	 */
	public String[] getComments() {
		return comments;
	}
	
	/**
	 * Returns the comment.
	 * @return String
	 */
	public String getComment() {
		if (comment != null && comment.length() > 0) finished();
		return comment;
	}

	/**
	 * Method setProject.
	 * @param iProject
	 */
	public void setProject(IProject iProject) {
		this.mainProject = iProject;
	}
	
	private void finished() {
		// if the comment is the same as the template, ignore it
		try {
			if (comment.equals(getCommitTemplate())) {
				comment = ""; //$NON-NLS-1$
			}
		} catch (CVSException e) {
			// we couldn't get the commit template. Log the error and continue
			CVSUIPlugin.log(e);
		}
		// if there is still a comment, remember it
		if (comment.length() > 0) {
			CVSUIPlugin.getPlugin().getRepositoryManager().addComment(comment);
		}
	}
}
