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
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * This is the main page of the Check Out As wizard. It allows the user to specify
 * whether they want to check out the remote folder(s) as project(s) or into an 
 * existing project. For single project checkout, the page will also allow the user to
 * choose whether to configure the new project (if it is missing a .project file).
 */
public class CheckoutAsMainPage extends CVSWizardPage {

	private String newProjectName;
	private boolean allowProjectConfiguration;
	private Button intoProjectButton;
	private Button simpleProjectButton;
	private Button configuredProjectButton;
	private Text projectNameField;
	private ICVSRemoteFolder[] folders;

	public static final String NAME = "CheckoutAsMainPage"; //$NON-NLS-1$
	
	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public CheckoutAsMainPage(ImageDescriptor titleImage, ICVSRemoteFolder[] folders, boolean allowProjectConfiguration) {
		super(NAME, Policy.bind("CheckoutAsMainPage.title"), titleImage, Policy.bind("CheckoutAsMainPage.description")); //$NON-NLS-1$ //$NON-NLS-2$
		this.folders = folders;
		this.allowProjectConfiguration = allowProjectConfiguration;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {

		Composite composite = createComposite(parent, 1);
		setControl(composite);
		
		WorkbenchHelp.setHelp(composite, IHelpContextIds.CHECKOUT_CONFIGURATION_PAGE);
		
		if (isSingleFolder()) {
			createSingleFolderArea(composite);
		} else {
			createMultipleFoldersArea(composite);
		}

		updateEnablements();
        Dialog.applyDialogFont(parent);
	}

	/*
	 * Is the input to the wizard a single folder or multiple folders
	 */
	private boolean isSingleFolder() {
		return folders.length == 1;
	}

	/*
	 * For the single folder case, return the name of the folder
	 */
	private String getFolderName() {
		String name = folders[0].getName();
		if (name .equals(".")) { //$NON-NLS-1$
			name = new Path(folders[0].getRepository().getRootDirectory()).lastSegment();
		}
		return name;
	}
	
	/*
	 * Create the page contents for a single folder checkout
	 */
	private void createSingleFolderArea(Composite composite) {
		createLabel(composite, Policy.bind("CheckoutAsMainPage.singleFolder", getFolderName())); //$NON-NLS-1$
		configuredProjectButton = createRadioButton(composite, Policy.bind("CheckoutAsMainPage.asConfiguredProject"), 1); //$NON-NLS-1$
		if (!allowProjectConfiguration) {
			configuredProjectButton.setEnabled(false);
			Label configuredLabel = createWrappingLabel(composite, Policy.bind("CheckoutAsMainPage.10"), 5); //$NON-NLS-1$
			configuredLabel.setEnabled(false);
		}
		createCheckoutAsProjectRadioArea(composite);
		createCheckoutIntoRadioArea(composite);
		if (allowProjectConfiguration) {
			configuredProjectButton.setSelection(true);
		} else {
			simpleProjectButton.setSelection(true);
		}
	}

	/*
	 * Create the page contents for a multiple folder checkout
	 */
	private void createMultipleFoldersArea(Composite composite) {
		createLabel(composite, Policy.bind("CheckoutAsMainPage.multipleFolders", new Integer(folders.length).toString())); //$NON-NLS-1$
		createCheckoutAsProjectRadioArea(composite);
		createCheckoutIntoRadioArea(composite);
		simpleProjectButton.setSelection(true);
	}

	/**
	 * @param composite
	 */
	private void createCheckoutAsProjectRadioArea(Composite composite) {
		if (isSingleFolder()) {
			simpleProjectButton = createRadioButton(composite, Policy.bind("CheckoutAsMainPage.asSimpleProject"), 1); //$NON-NLS-1$
			createProjectNameGroup(composite);
		} else {
			simpleProjectButton = createRadioButton(composite, Policy.bind("CheckoutAsMainPage.asProjects"), 1); //$NON-NLS-1$
		}
	}
	
	/**
	 * @param composite
	 */
	private void createCheckoutIntoRadioArea(Composite composite) {
		intoProjectButton = createRadioButton(composite, Policy.bind("CheckoutAsMainPage.intoProject"), 1); //$NON-NLS-1$
	}

	/**
	 * Creates the project name specification controls.
	 *
	 * @param parent the parent composite
	 */
	private void createProjectNameGroup(Composite parent) {
		// project specification group
		Composite projectGroup = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project label
		Label projectLabel = new Label(projectGroup,SWT.NONE);
		projectLabel.setText(Policy.bind("CheckoutAsMainPage.projectNameLabel")); //$NON-NLS-1$

		// new project name entry field
		projectNameField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		projectNameField.setLayoutData(data);
	
		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		newProjectName = getFolderName();
		projectNameField.setText(newProjectName);
		projectNameField.selectAll();
	
		// Set the listener to capture modify events
		projectNameField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateEnablements();
			}
		});
	}
	
	/**
	 * Check if the entries in the widget are valid. If they are return null otherwise
	 * return a string that indicates the problem.
	 */
	private void updateEnablements() {

		if (projectNameField != null) {
			projectNameField.setEnabled(simpleProjectButton.getSelection());
			if (projectNameField.isEnabled()) {
				newProjectName = this.projectNameField.getText();
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IStatus nameStatus = workspace.validateName(newProjectName, IResource.PROJECT);
				if (!nameStatus.isOK()) {
					setErrorMessage(nameStatus.getMessage());
					setPageComplete(false);
				}
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
	}
	
	public String getProjectName() {
		if (isSingleFolder() && simpleProjectButton.getSelection()) return newProjectName;
		return null;
	}

	public boolean isPerformConfigure() {
		if (configuredProjectButton == null) return false;
		return configuredProjectButton.getSelection();
	}

	public boolean isPerformCheckoutInto() {
		return intoProjectButton.getSelection();
	}

	public boolean isPerformCheckoutAs() {
		return simpleProjectButton.getSelection();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage#createRadioButton(org.eclipse.swt.widgets.Composite, java.lang.String, int)
	 */
	protected Button createRadioButton(Composite parent, String label, int span) {
		Button radio = super.createRadioButton(parent, label, span);
		radio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateEnablements();
			}
		});
		return radio;
	}

}
