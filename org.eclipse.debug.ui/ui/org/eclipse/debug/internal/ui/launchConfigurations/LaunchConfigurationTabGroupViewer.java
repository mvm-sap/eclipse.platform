/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

import com.ibm.icu.text.MessageFormat;

/**
 * A viewer that displays tabs for a launch configuration, with apply and revert
 * buttons.
 */
public class LaunchConfigurationTabGroupViewer extends Viewer {

	private final String EMPTY_STRING = ""; //$NON-NLS-1$
	/**
	 * Containing launch dialog
	 */
	private ILaunchConfigurationDialog fDialog;
	
	/**
	 * The this viewer's input
	 */
	private Object fInput;
	
	/**
	 * The launch configuration (original) being edited
	 */
	private ILaunchConfiguration fOriginal;
	
	/**
	 * The working copy of the original
	 */
	private ILaunchConfigurationWorkingCopy fWorkingCopy;
	
	/**
	 * This view's control, which contains a composite area of controls
	 */
	private Composite fViewerControl;
	
	/**
	 * The composite which is hidden/displayed as tabs are required.
	 */
	private Composite fVisibleArea;
	
	/**
	 * Name label widget
	 */
	private Label fNameLabel;
	
	/**
	 * Name text widget
	 */
	private Text fNameWidget;
	
	/**
	 * Composite containing the launch config tab widgets
	 */
	private Composite fTabComposite;
	
	/**
	 * Tab folder
	 */
	private CTabFolder fTabFolder;
	
	/**
	 * The current tab group being displayed
	 */
	private ILaunchConfigurationTabGroup fTabGroup;

	/**
	 * The type of config tabs are currently displayed
	 * for
	 */
	private ILaunchConfigurationType fTabType;	
	
	/**
	 * Index of the active tab
	 */
	private int fCurrentTabIndex = -1;
	
	/**
	 * Apply & Revert buttons
	 */
	private Button fApplyButton;
	private Button fRevertButton;
	
	/**
	 * Whether tabs are currently being disposed or initialized
	 */
	private boolean fDisposingTabs = false;
	private boolean fInitializingTabs = false;

	/**
	 * The description of the currently selected launch configuration or
	 * launch configuration type or <code>null</code> if none.
	 */
	private String fDescription = null;
	
	/**
	 * A placeholder for switching between the tabs for a config and the getting started tab
	 * @since 3.2
	 */
	private Composite fTabPlaceHolder = null;
	
	/**
	 * A link to allow users to select a valid set of launch options for the specified mode
	 * @since 3.3
	 * EXPERIMENTAL
	 */
	private Link fOptionsLink = null;
	
	/**
	 * A new composite replacing the perspectives tab
	 * @since 3.2
	 */
	private Composite fGettingStarted = null;

	private ViewForm fViewform;
	
	/**
	 * Constructs a viewer in the given composite, contained by the given
	 * launch configuration dialog.
	 * 
	 * @param parent composite containing this viewer
	 * @param dialog containing launch configuration dialog
	 */
	public LaunchConfigurationTabGroupViewer(Composite parent, ILaunchConfigurationDialog dialog) {
		super();
		fDialog = dialog;
		createControl(parent);
	}
	
	/**
	 * Cleanup
	 */
	public void dispose() {
		disposeTabGroup();
	}

	/**
	 * Dispose the active tab group, if any.
	 */
	protected void disposeTabGroup() {
		if (fTabGroup != null) {
			fTabGroup.dispose();
			fTabGroup = null;
			fTabType = null;
		}
	}	
	
	/**
	 * Creates this viewer's control This area displays the name of the launch
	 * configuration currently being edited, as well as a tab folder of tabs
	 * that are applicable to the launch configuration.
	 *
	 * @return the composite used for launch configuration editing
	 */
	private void createControl(Composite parent) {
		fViewerControl = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		fViewerControl.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fViewerControl.setLayoutData(gd);
		
        fViewform = new ViewForm(fViewerControl, SWT.FLAT | SWT.BORDER);
        layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        fViewform.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		fViewform.setLayoutData(gd);
		fVisibleArea = fViewform;
        fViewform.setTopLeft(null);
        
        Composite mainComp = new Composite(fViewform, SWT.FLAT);
        layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        mainComp.setLayout(layout);
        fViewform.setContent(mainComp);

		fTabPlaceHolder = new Composite(mainComp, SWT.NONE);
		fTabPlaceHolder.setLayout(new StackLayout());
		gd = new GridData(GridData.FILL_BOTH);
		fTabPlaceHolder.setLayoutData(gd);
        
		fGettingStarted = new Composite(fTabPlaceHolder, SWT.NONE);
		fGettingStarted.setLayout(new GridLayout());
		gd = new GridData(GridData.FILL_BOTH);
		fGettingStarted.setLayoutData(gd);
		
		createGettingStarted(fGettingStarted);
		
		fTabComposite = new Composite(fTabPlaceHolder, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 5;
		fTabComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		fTabComposite.setLayoutData(gd);
		
		fNameLabel = new Label(fTabComposite, SWT.HORIZONTAL | SWT.LEFT);
		fNameLabel.setText(LaunchConfigurationsMessages.LaunchConfigurationDialog__Name__16); 
        fNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
       
		fNameWidget = new Text(fTabComposite, SWT.SINGLE | SWT.BORDER);
        fNameWidget.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fNameWidget.addModifyListener(new ModifyListener() {
    				public void modifyText(ModifyEvent e) {
    					handleNameModified();
    				}
    			}
    		);
    		
		createTabFolder(fTabComposite);
		
		Composite blComp = SWTUtil.createComposite(mainComp, mainComp.getFont(), 2, 1, GridData.FILL_HORIZONTAL);
		Composite linkComp = SWTUtil.createComposite(blComp, blComp.getFont(), 1, 1, GridData.FILL_HORIZONTAL);

	//a link for launch options
		fOptionsLink = new Link(linkComp, SWT.WRAP);
		fOptionsLink.setFont(linkComp.getFont());
		gd = new GridData(SWT.LEFT);
		gd.grabExcessHorizontalSpace = true;
		fOptionsLink.setLayoutData(gd);
		fOptionsLink.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				//collect the options available
				try {
					if(!canLaunchWithModes()) {
						SelectLaunchModesDialog sld = new SelectLaunchModesDialog(getShell(), 
								getLaunchConfigurationDialog().getMode(), getWorkingCopy());
						if(sld.open() == IDialogConstants.OK_ID) {
							//set the options to the config
							Object[] res = sld.getResult();
							if(res != null) {
								Set modes = (Set) res[0];
								modes.remove(getLaunchConfigurationDialog().getMode());
								ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
								wc.setModes(modes);
								refreshStatus();
							}
						}
					}
					else if(hasMultipleDelegates()) {
						SelectLaunchersDialog sldd = new SelectLaunchersDialog(getShell(), 
								getWorkingCopy().getType().getDelegates(getCurrentModeSet()), 
								getWorkingCopy(), 
								getLaunchConfigurationDialog().getMode());
						if(sldd.open() == IDialogConstants.OK_ID) {
							displayInstanceTabs(true);
						}
					}
				} catch (CoreException ex) {}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		fOptionsLink.setVisible(false);
		
		Composite buttonComp = new Composite(blComp, SWT.NONE);
		GridLayout buttonCompLayout = new GridLayout();
		buttonCompLayout.numColumns = 2;
		buttonComp.setLayout(buttonCompLayout);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComp.setLayoutData(gd);
		
		fApplyButton = new Button(buttonComp, SWT.PUSH);
		fApplyButton.setText(LaunchConfigurationsMessages.LaunchConfigurationDialog__Apply_17); 
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		fApplyButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(fApplyButton);
		fApplyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleApplyPressed();
			}
		});

		fRevertButton = new Button(buttonComp, SWT.PUSH);
		fRevertButton.setText(LaunchConfigurationsMessages.LaunchConfigurationDialog_Revert_2);   
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		fRevertButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(fRevertButton);
		fRevertButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleRevertPressed();
			}
		});
        Dialog.applyDialogFont(parent);
	}
	
	/**
	 * Creates some help text for the tab group launch types
	 * @param parent thep arent composite
	 * @since 3.2
	 */
	private void createGettingStarted(Composite parent) {
		Font font = parent.getFont();
		GridData gd = null;
		int width = parent.getBounds().width - 30;
		SWTUtil.createWrapLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_1, 1, width);
		SWTUtil.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_2, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_NEW_CONFIG), 1, width);
		SWTUtil.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_6, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_DUPLICATE_CONFIG), 1, width);
		SWTUtil.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_4, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_DELETE_CONFIG), 1, width);
        SWTUtil.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_8, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_FILTER_CONFIGS), 1, width);
        SWTUtil.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_3, DebugUITools.getImage(IInternalDebugUIConstants.IMG_OVR_TRANSPARENT), 1, width);
        
		SWTUtil.createHorizontalSpacer(parent, 2);
		Link link = new Link(parent, SWT.LEFT | SWT.WRAP);
		link.setText(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_5);
		link.setFont(font);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = width;
		link.setLayoutData(gd);
		link.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				SWTUtil.showPreferencePage("org.eclipse.debug.ui.PerspectivePreferencePage"); //$NON-NLS-1$
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
	
	/**
	 * Creates the tab folder for displaying config instances
	 * @param parent
	 */
	private void createTabFolder(Composite parent) {
		if (fTabFolder == null) {
			Display display = getShell().getDisplay();
			Color c1 = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND),
				  c2 = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);
			fTabFolder = new CTabFolder(parent, SWT.NO_REDRAW_RESIZE | SWT.NO_TRIM | SWT.FLAT);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			fTabFolder.setSelectionBackground(new Color[] {c1, c2},	new int[] {100}, true);
			fTabFolder.setSelectionForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));
			fTabFolder.setSimple(PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS));
			fTabFolder.setLayoutData(gd);
	        fTabFolder.setBorderVisible(true);
			fTabFolder.setFont(parent.getFont());
			fTabFolder.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					if (!fInitializingTabs) {
						handleTabSelected();
						refresh();
					}
				}
			});	
		}
	}
	
	/**
	 * Returns the apply button
	 */
	protected Button getApplyButton() {
		return fApplyButton;
	}	

	/**
	 * Returns the revert button
	 */
	protected Button getRevertButton() {
		return fRevertButton;
	}	
	
	/**
	 * Sets the tab folder
	 */
	protected CTabFolder getTabFolder() {
		return fTabFolder;
	}
	
	/**
	 * Sets the current name
	 */
	public void setName(String name) {
		if (getWorkingCopy() != null) {
			if (name == null) {
				fNameWidget.setText(EMPTY_STRING);
			}
			else {
				fNameWidget.setText(name.trim());
			}
			refreshStatus();
		}
	}	

	/**
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	public Control getControl() {
		return fViewerControl;
	}
	
	/**
	 * Returns the shell this viewer is contained in.
	 */
	protected Shell getShell() {
		return getControl().getShell();
	}

	/**
	 * @see org.eclipse.jface.viewers.IInputProvider#getInput()
	 */
	public Object getInput() {
		return fInput;
	}

	/**
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		return new StructuredSelection(fWorkingCopy);
	}

	/**
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	public void refresh() {
		if (fInitializingTabs) {
			return;
		}
		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs != null) {
			// update the working copy from the active tab
			getActiveTab().performApply(getWorkingCopy());
			updateButtons();
			// update error ticks
			CTabItem item = null;
			boolean error = false;
			Image image = null;
			for (int i = 0; i < tabs.length; i++) {
				item = fTabFolder.getItem(i);
				image = tabs[i].getImage();
				item.setImage(image);
				if(!tabs[i].isValid(getWorkingCopy())) {
					error = tabs[i].getErrorMessage() != null;
					if(error) {
						item.setImage(DebugUIPlugin.getDefault().getLaunchConfigurationManager().getErrorTabImage(tabs[i]));
					}
				}
			}
			showLink();
		}
	}
	
	/**
	 * Shows the link for either multiple launch delegates or bad launch mode combinations
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	private void showLink() {
		String text = null;
		if(!canLaunchWithModes()) {
			text = LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_13;
		}
		else if(hasMultipleDelegates()) {
			ILaunchDelegate delegate = getPreferredDelegate();
			if(delegate != null) {
				String name = delegate.getName();
				if(name == null) {
					text = LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_15;
				}
				else {
					text = MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_16, new String[] {name});
				}
			}
			else {
				text = LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_17;
			}
		}
		if(text != null) {
			fOptionsLink.setText(text);
		}
		fOptionsLink.setVisible(!canLaunchWithModes() || hasMultipleDelegates());
		fOptionsLink.getParent().layout(true);
	}
	
	/**
	 * Returns the preferred launch delegate for the current launch configuration and mode set
	 * @return the preferred launch delegate
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	protected ILaunchDelegate getPreferredDelegate() {
		ILaunchDelegate preferred = null;
		ILaunchConfigurationWorkingCopy config = getWorkingCopy();
		if(config != null) {
			try {
				Set modes = getCurrentModeSet();
				preferred = config.getPreferredDelegate(modes);
				if(preferred == null) {
					preferred = config.getType().getPreferredDelegate(modes);
				}
			}
			catch(CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return preferred;
	}
	
	/**
	 * Returns the listing of modes for the current config
	 * @return the listing of modes for the current config
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	private Set getCurrentModeSet() {
		Set set = new HashSet();
		ILaunchConfigurationWorkingCopy config = getWorkingCopy();
		if(config != null) {
			try {
				set.addAll(config.getModes());
				set.add(getLaunchConfigurationDialog().getMode());
			}
			catch(CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return set;
	}
	
	/**
	 * updates the button states
	 */
	private void updateButtons() {
		boolean dirty = isDirty();
		fApplyButton.setEnabled(dirty && canSave());
		fRevertButton.setEnabled(dirty);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#setInput(java.lang.Object)
	 */
	public void setInput(Object input) {
		if (input == null) {
			if (fInput == null) {
				return;
			}
			inputChanged(input);
		} else {
			if (!input.equals(fInput)) {
				inputChanged(input);
			}
		}
	}
	
	/**
	 * The input has changed to the given object, possibly <code>null</code>.
	 * 
	 * @param input the new input, possibly <code>null</code>
	 */
	protected void inputChanged(Object input) {
		fInput = input;
		Runnable r = new Runnable() {
			public void run() {
				try {
					fVisibleArea.setRedraw(false);
					if (fInput instanceof ILaunchConfiguration) {
						ILaunchConfiguration configuration = (ILaunchConfiguration)fInput;
						boolean refreshtabs = !delegatesEqual(fWorkingCopy, configuration);
						fOriginal = configuration;
						fWorkingCopy = configuration.getWorkingCopy();
						displayInstanceTabs(refreshtabs);
					} else if (fInput instanceof ILaunchConfigurationType) {
						fDescription = getDescription((ILaunchConfigurationType)fInput);
						setNoInput();
						refreshStatus();
					} else {
						setNoInput();
						refreshStatus();
					}
				} catch (CoreException ce) {
					errorDialog(ce);
					setNoInput();
				}
				finally {
					fVisibleArea.setRedraw(true);
				}
			}
		};
		BusyIndicator.showWhile(getShell().getDisplay(), r);
	}
	
	/**
	 * Sets the tab group viewer to have no input, this is the case when null is passed as an input type
	 * Setting no input is equivalent to resetting all items, clearing any messages and showing the 'getting started' pane
	 * @since 3.2 
	 */
	private void setNoInput() {
		fOriginal = null;
		fWorkingCopy = null;
		disposeExistingTabs();	
		updateButtons();
		updateVisibleControls(false);
		ILaunchConfigurationDialog lcd = getLaunchConfigurationDialog();
		if(lcd instanceof LaunchConfigurationsDialog) {
			if(((LaunchConfigurationsDialog)lcd).isTreeSelectionEmpty()) {
				fDescription = EMPTY_STRING;
			}
		}
	}
	
	/**
	 * Returns if the two configs are using the same <code>ILaunchDelegate</code> or not
	 * @param config1
	 * @param config2
	 * @return true if the configs are using the same <code>ILaunchDelegate</code> or false if they are not
	 * @since 3.3
	 */
	protected boolean delegatesEqual(ILaunchConfiguration config1, ILaunchConfiguration config2) {
		try {
			if(config1 == null || config2 == null) {
				return false;
			}
			Set modes = getCurrentModeSet();
			ILaunchDelegate d1 = config1.getPreferredDelegate(modes);
			if(d1 == null) {
				d1 = config1.getType().getPreferredDelegate(modes);
			}
			ILaunchDelegate d2 = config2.getPreferredDelegate(modes);
			if(d2 == null) {
				d2 = config2.getType().getPreferredDelegate(modes);
			}
			if(d1 != null) {
				return d1.equals(d2);
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
		return false;
	}
	
	/**
	 * Updates the visibility of controls based on the status provided 
	 * @param visible the visibility status to be applied to the controls
	 */
	private void updateVisibleControls(boolean visible) {
		fApplyButton.setVisible(visible);
		fRevertButton.setVisible(visible);
		fOptionsLink.setVisible(visible);
		if(visible) {
			((StackLayout)fTabPlaceHolder.getLayout()).topControl = fTabComposite;
			fTabComposite.layout();
		}
		else {
			((StackLayout)fTabPlaceHolder.getLayout()).topControl = fGettingStarted;
		}
		fTabPlaceHolder.layout(true);
	}
	
    /**
     * sets the current widget focus to the 'Name' widget
     */
    protected void setFocusOnName() {
        fNameWidget.setFocus();
    }
    
	/**
	 * Displays tabs for the current working copy
	 */
	protected void displayInstanceTabs(boolean redrawTabs) {
		// Turn on initializing flag to ignore message updates
		fInitializingTabs = true;
		ILaunchConfigurationType type = null;
		try {
			type = getWorkingCopy().getType();
		} 
		catch (CoreException e) {
			errorDialog(e);
			fInitializingTabs = false;
			return;
		}
		if(redrawTabs) {
			showInstanceTabsFor(type);
		}
		// show the name area
		updateVisibleControls(true);
		// Update the name field before to avoid verify error
		fNameWidget.setText(getWorkingCopy().getName());

		// Retrieve the current tab group.  If there is none, clean up and leave
		ILaunchConfigurationTabGroup tabGroup = getTabGroup();
		if (tabGroup == null) {
			IStatus status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0, MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_No_tabs_defined_for_launch_configuration_type__0__1, new String[]{type.getName()}), null); 
			CoreException e = new CoreException(status);
			errorDialog(e);
			fInitializingTabs = false;
			return;
		}

		// Update the tabs with the new working copy
		tabGroup.initializeFrom(getWorkingCopy());

		// Update the name field after in case client changed it
		fNameWidget.setText(getWorkingCopy().getName());
		
		fCurrentTabIndex = fTabFolder.getSelectionIndex();

		// Turn off initializing flag to update message
		fInitializingTabs = false;
		
		if (!fVisibleArea.isVisible()) {
			fVisibleArea.setVisible(true);
		}
		refreshStatus();		
	}
	
	/**
	 * Populate the tabs in the configuration edit area to be appropriate to the current
	 * launch configuration type.
	 */
	private void showInstanceTabsFor(ILaunchConfigurationType configType) {
		// try to keep on same tab
		Class tabKind = null;
		if (getActiveTab() != null) {
			tabKind = getActiveTab().getClass();
		}
		// Build the new tabs
		ILaunchConfigurationTabGroup group = null;
		try {
			group = createGroup();
		} catch (CoreException ce) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.LaunchConfigurationDialog_Error_19, LaunchConfigurationsMessages.LaunchConfigurationDialog_Exception_occurred_creating_launch_configuration_tabs_27,ce); // 
			return;
		}
		disposeExistingTabs();
		fTabGroup = group;
		fTabType = configType;
		ILaunchConfigurationTab[] tabs = getTabs();
		CTabItem tab = null;
		String name = EMPTY_STRING;
		Control control = null;
		for (int i = 0; i < tabs.length; i++) {
			tab = new CTabItem(fTabFolder, SWT.BORDER);
			name = tabs[i].getName();
			if (name == null) {
				name = LaunchConfigurationsMessages.LaunchConfigurationDialog_unspecified_28; 
			}
			tab.setText(name);
			tab.setImage(tabs[i].getImage());
			tabs[i].createControl(tab.getParent());
			control = tabs[i].getControl();
			if (control != null) {
				tab.setControl(control);
			}
		}
		//set the default tab as the first one
		setActiveTab(tabs[0]);
		// select same tab as before, if possible
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i].getClass().equals(tabKind)) {
				setActiveTab(tabs[i]);
				break;
			}
		}
		fDescription = getDescription(configType);
	}	

	/**
	 * Returns the description of the given configuration type
	 * in the current mode or <code>null</code> if none.
	 * 
	 * @param configType the config type
	 * @return the description of the given configuration type or <code>null</code>
	 */
	private String getDescription(ILaunchConfigurationType configType) {
		String description = null;
		if(configType != null) {
			String mode = fDialog.getMode();
			description = LaunchConfigurationPresentationManager.getDefault().getDescription(configType, mode);
		}	
		if (description == null) {
			description = EMPTY_STRING;
		}
		return description;
	}
	
	/**
	 * Returns tab group for the given type of launch configuration.
	 * Tabs are initialized to be contained in this dialog.
	 *
	 * @exception CoreException if unable to instantiate a tab group
	 */
	protected ILaunchConfigurationTabGroup createGroup() throws CoreException {
		// Use a final Object array to store the tab group and any exception that
		// results from the Runnable
		final Object[] finalArray = new Object[2];
		Runnable runnable = new Runnable() {
			public void run() {
				ILaunchConfigurationTabGroup tabGroup = null;
				try {
					tabGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(getWorkingCopy(), getLaunchConfigurationDialog().getMode());
					finalArray[0] = tabGroup;
				} catch (CoreException ce) {
					finalArray[1] = ce;
					return;
				}
				tabGroup.createTabs(getLaunchConfigurationDialog(), getLaunchConfigurationDialog().getMode());
				ILaunchConfigurationTab[] tabs = tabGroup.getTabs();
				for (int i = 0; i < tabs.length; i++) {
					tabs[i].setLaunchConfigurationDialog(getLaunchConfigurationDialog());
				}
			}
		};

		// Creating the tabs can result in plugin loading, so we show the busy cursor
		BusyIndicator.showWhile(getControl().getDisplay(), runnable);

		// Re-throw any CoreException if there was one
		if (finalArray[1] != null) {
			throw (CoreException)finalArray[1];
		}

		// Otherwise return the tab group
		return (ILaunchConfigurationTabGroup)finalArray[0];
	}	

	/**
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	public void setSelection(ISelection selection, boolean reveal) {
		if (getWorkingCopy() != null) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object object = structuredSelection.getFirstElement();
				if (object instanceof ILaunchConfigurationTab) {
					ILaunchConfigurationTab[] tabs = getTabs();
					for (int i = 0; i < tabs.length; i++) {
						if (tabs[i].equals(object)) {
							fCurrentTabIndex = i;
							fTabFolder.setSelection(i);
						}
						return;
					}
				}
			}
		}
			
	}

	/**
	 * Returns the tabs currently being displayed, or
	 * <code>null</code> if none.
	 *
	 * @return currently displayed tabs, or <code>null</code>
	 */
	public ILaunchConfigurationTab[] getTabs() {
		if (getTabGroup() != null) {
			return getTabGroup().getTabs();
		}
		return null;
	}

	/**
	 * Returns the currently active <code>ILaunchConfigurationTab</code>
	 * being displayed, or <code>null</code> if there is none.
	 *
	 * @return currently active <code>ILaunchConfigurationTab</code>, or <code>null</code>.
	 */
	public ILaunchConfigurationTab getActiveTab() {
		ILaunchConfigurationTab[] tabs = getTabs();
		if (fTabFolder != null && tabs != null) {
			int pageIndex = fTabFolder.getSelectionIndex();
			if (pageIndex >= 0) {
				return tabs[pageIndex];
			}
		}
		return null;
	}
	
	/**
	 * Returns whether the launch configuration being edited is dirty (i.e.
	 * needs saving)
	 * 
	 * @return whether the launch configuration being edited needs saving
	 */
	public boolean isDirty() {
		ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
		if (workingCopy == null) {
			return false;
		}
		// Working copy hasn't been saved
		if (workingCopy.getOriginal() == null) {
			return true;
		}
		ILaunchConfiguration original = getOriginal();
		return !original.contentsEqual(workingCopy);
	}
	
	/**
	 * Update apply & revert buttons, as well as buttons and message on the
	 * launch config dialog.
	 */
	protected void refreshStatus() {
		if (!fInitializingTabs) {
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
		}
	}	
	
	/**
	 * Returns the containing launch dialog
	 */
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return fDialog;
	}

	/**
	 * Returns the original launch configuration being edited, possibly
	 * <code>null</code>.
	 * 
	 * @return ILaunchConfiguration
	 */
	protected ILaunchConfiguration getOriginal() {
		return fOriginal;
	}
	
	/**
	 * Returns the working copy used to edit the original, possibly
	 * <code>null</code>.
 	 */
	protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
		return fWorkingCopy;
	}
	
	/**
	 * Return whether the current configuration can be saved.
	 * <p>
	 * Note this is NOT the same thing as the config simply being valid. It
	 * is possible to save a config that does not validate. This method
	 * determines whether the config can be saved without causing a serious
	 * error. For example, a shared config that has no specified location would
	 * cause this method to return <code>false</code>.
	 * </p>
	 */
	public boolean canSave() {
		if (fInitializingTabs) {
			return false;
		}
		// First make sure that name doesn't prevent saving the config
		try {
			verifyName();
		} catch (CoreException ce) {
			return false;
		}

		// Next, make sure none of the tabs object to saving the config
		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs == null) {
			return false;
		}
		for (int i = 0; i < tabs.length; i++) {
			if (!tabs[i].canSave()) {
				return false;
			}
		}
		if(getWorkingCopy() != null) {
			return !getWorkingCopy().isReadOnly();
		}
		return true;
	}	
	
	/**
	 * @see ILaunchConfigurationDialog#canLaunch()
	 */
	public boolean canLaunch() {
		if(fInitializingTabs) {
			return false;
		}
		if (getWorkingCopy() == null) {
			return false;
		}
		try {
			verifyName();
		} catch (CoreException e) {
			return false;
		}

		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs == null) {
			return false;
		}
		for (int i = 0; i < tabs.length; i++) {
			if (!tabs[i].isValid(getWorkingCopy())) {
				return false;
			}
		}
		return true;
	}	
	
	/**
	 * Determines if the tab groups that is currently visible can launch with the currently selected
	 * set of options.
	 * 
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This method has been added as
	 * part of a work in progress. There is no guarantee that this API will
	 * remain unchanged during the 3.3 release cycle. Please do not use this API
	 * without consulting with the Platform/Debug team.
	 * </p>
	 * @return
	 */
	public boolean canLaunchWithModes() {
		if(fInitializingTabs) {
			return false;
		}
		//check if selected options exist and that the selected combination can be launched
		try {
			ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
			if(wc != null) {
				return wc.getType().supportsModeCombination(getCurrentModeSet());
			}
		}  catch (CoreException e) {
		}
		return true;
	}
	
	/**
	 * Returns if the type currently showing in the tab group viewer has duplicate launch delegates for the given set of modes.
	 * 
	 * The given set of modes comprises the current mode that the launch dialog was opened in as well as any modes that have been set on the launch
	 * configuration.
	 * @return the true if there are duplicates, false otherwise
	 * 
	 * @since 3.3
	 * 
	 * EXPERIMENTAL
	 */
	public boolean hasDuplicateDelegates() {
		if(fInitializingTabs) {
			return false;
		}
		ILaunchConfiguration config = getWorkingCopy();
		if(config != null) {
			if(hasMultipleDelegates()) {
				return getPreferredDelegate() == null;
			}
		}
		return false;
	}
	
	/**
	 * Determines if the currently showing launch configuration has multiple launch delegates for the same mode set, but does not care
	 * if there has been a default selected yet or not
	 * @return true if the current launch configuraiton has multiple launch delegates, false otherwise
	 */
	private boolean hasMultipleDelegates() {
		ILaunchConfiguration config = getWorkingCopy();
		if(config != null) {
			try {
				Set modes = getCurrentModeSet();
				ILaunchDelegate[] delegates = LaunchConfigurationManager.filterLaunchDelegates(fTabType, modes);
				return delegates.length > 1;
			}
			catch (CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return false;
	}
	
	/**
	 * Returns the current error message or <code>null</code> if none.
	 */
	public String getErrorMesssage() {
		if (fInitializingTabs) {
			return null;
		}
		
		if (getWorkingCopy() == null) {
			return null;
		}
		try {
			verifyName();
		} catch (CoreException ce) {
			return ce.getStatus().getMessage();
		}
	
		String message = null;
		ILaunchConfigurationTab activeTab = getActiveTab();
		if (activeTab == null) {
			return null;
		} 
		message = activeTab.getErrorMessage();
		if (message != null) {
			return message;
		}
		
		ILaunchConfigurationTab[] allTabs = getTabs();
		for (int i = 0; i < allTabs.length; i++) {
			ILaunchConfigurationTab tab = allTabs[i];
			if (tab == activeTab) {
				continue;
			}
			message = tab.getErrorMessage();
			if (message != null) {
				StringBuffer temp= new StringBuffer();
				temp.append('[');
				temp.append(DebugUIPlugin.removeAccelerators(tab.getName()));
				temp.append("]: "); //$NON-NLS-1$
				temp.append(message);
				return temp.toString();
			}
		}
		if(getWorkingCopy().isReadOnly()) {
			return LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_9;
		}
		//EXPERIMENTAL
		if(!canLaunchWithModes()) {
			Set modes = getCurrentModeSet();
			List names = LaunchConfigurationPresentationManager.getDefault().getLaunchModeNames(modes);
			return MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_14, new String[]{names.toString()});
		}
		if(hasDuplicateDelegates()) {
			return LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_18;
		}
		return null;
	}	
	
	/**
	 * Returns the current message or <code>null</code> if none.
	 * @return Returns an appropriate message for display to user. The message returned will be:
	 * The message defined by the visible tab,
	 * or The tab group description for the particular launch mode,
	 * or The generic tab group description,
	 * or <code>null</code> if no message is defined 
	 */
	public String getMessage() {
		if (fInitializingTabs) {
			return null;
		}
		
		String message = fDescription;
		
		ILaunchConfigurationTab tab = getActiveTab();
		if (tab != null) {
			String tabMessage = tab.getMessage();
			if (tabMessage != null) {
				message = tabMessage;
			}
		}
		
		return message;
	}	
		
	/**
	 * Verify that the launch configuration name is valid.
	 */
	protected void verifyName() throws CoreException {
		if (fNameWidget.isVisible()) {
			String currentName = fNameWidget.getText().trim();
	
			// If there is no name, complain
			if (currentName.length() < 1) {
				throw new CoreException(new Status(IStatus.ERROR,
													 DebugUIPlugin.getUniqueIdentifier(),
													 0,
													 LaunchConfigurationsMessages.LaunchConfigurationDialog_Name_required_for_launch_configuration_11, 
													 null));
			}
	
			// See if name contains any 'illegal' characters
			IStatus status = ResourcesPlugin.getWorkspace().validateName(currentName, IResource.FILE);
			if (status.getCode() != IStatus.OK) {
				throw new CoreException(new Status(IStatus.ERROR,
													 DebugUIPlugin.getUniqueIdentifier(),
													 0,
													 status.getMessage(),
													 null));
			}
			
			// See if name contains any characters that we deem illegal.
			// '@' and '&' are disallowed because they corrupt menu items.
			char[] disallowedChars = new char[] { '@', '&' };
			for (int i = 0; i < disallowedChars.length; i++) {
				char c = disallowedChars[i];
				if (currentName.indexOf(c) > -1) {
					throw new CoreException(new Status(IStatus.ERROR,
														DebugUIPlugin.getUniqueIdentifier(),
														0,
														MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_0, new String[] { new String(new char[] {c}), currentName }), 
														null));
				}
			}
	
			// Otherwise, if there's already a config with the same name, complain
			if (!getOriginal().getName().equals(currentName)) {
				Set reservednames = ((LaunchConfigurationsDialog)getLaunchConfigurationDialog()).getReservedNameSet();
				if (DebugPlugin.getDefault().getLaunchManager().isExistingLaunchConfigurationName(currentName) || (reservednames != null ? reservednames.contains(currentName) : false)) {
					throw new CoreException(new Status(IStatus.ERROR,
														 DebugUIPlugin.getUniqueIdentifier(),
														 0,
														 LaunchConfigurationsMessages.LaunchConfigurationDialog_Launch_configuration_already_exists_with_this_name_12, 
														 null));
				}
			}
		}
	}		
	
	/**
	 * Remove the existing tabs that are showing 
	 */
	private void disposeExistingTabs() {
		fDisposingTabs = true;
        fTabFolder.dispose();
        fTabFolder = null;
		createTabFolder(fTabComposite);
		disposeTabGroup();
		fDisposingTabs = false;
	}	

	/**
	 * Returns the current tab group
	 *
	 * @return the current tab group, or <code>null</code> if none
	 */
	public ILaunchConfigurationTabGroup getTabGroup() {
		return fTabGroup;
	}
	
	/**
	 * Notification that a tab has been selected
	 *
	 * Disallow tab changing when the current tab is invalid.
	 * Update the config from the tab being left, and refresh
	 * the tab being entered.
	 */
	protected void handleTabSelected() {
		if (fDisposingTabs || fInitializingTabs) {
			return;
		}
		ILaunchConfigurationTab[] tabs = getTabs();
		if (fCurrentTabIndex == fTabFolder.getSelectionIndex() || tabs == null || tabs.length == 0 || fCurrentTabIndex > (tabs.length - 1)) {
			return;
		}
		if (fCurrentTabIndex != -1) {
			ILaunchConfigurationTab tab = tabs[fCurrentTabIndex];
			ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
			if (wc != null) {
				tab.deactivated(wc);
				getActiveTab().activated(wc);
			}
		}
		fCurrentTabIndex = fTabFolder.getSelectionIndex();
	}
	
	/**
	 * Notification the name field has been modified
	 */
	protected void handleNameModified() {
		getWorkingCopy().rename(fNameWidget.getText().trim());
		refreshStatus();
	}		
	
	/**
	 * Notification that the 'Apply' button has been pressed
	 */
	protected void handleApplyPressed() {
		try {
			// update launch config
			fInitializingTabs = true;
			// trim name
			String trimmed = fNameWidget.getText().trim();
			fNameWidget.setText(trimmed);
			ILaunchConfigurationWorkingCopy copy = getWorkingCopy();
			if(copy == null) {
				copy = fOriginal.getWorkingCopy();
			}
			copy.rename(trimmed);
			getTabGroup().performApply(copy);
			fInitializingTabs = false;

			if (isDirty()) {
				copy.doSave();
			}
			updateButtons();
		} catch (CoreException e) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.LaunchConfigurationDialog_Launch_Configuration_Error_46, LaunchConfigurationsMessages.LaunchConfigurationDialog_Exception_occurred_while_saving_launch_configuration_47, e); // 
			return;
		}
	}

	/**
	 * Notification that the 'Revert' button has been pressed
	 */
	protected void handleRevertPressed() {
		try {
			if(fTabGroup != null) {
				fTabGroup.initializeFrom(fOriginal);
				fWorkingCopy = fOriginal.getWorkingCopy();
				refreshStatus();
			}
		} 
		catch (CoreException e) {DebugUIPlugin.log(e);}
	}	
	
	/**
	 * Show an error dialog on the given exception.
	 *
	 * @param exception
	 */
	protected void errorDialog(CoreException exception) {
		ErrorDialog.openError(getShell(), null, null, exception.getStatus());
	}	

	/**
	 * Sets the displayed tab to the given tab. Has no effect if the specified
	 * tab is not one of the tabs being displayed in the dialog currently.
	 * 
	 * @param tab the tab to display/activate
	 */
	public void setActiveTab(ILaunchConfigurationTab tab) {
		ILaunchConfigurationTab[] tabs = getTabs();
		if(tabs != null) {
			for (int i = 0; i < tabs.length; i++) {
				if (tabs[i].getClass().equals(tab.getClass())) {
					setActiveTab(i);
					return;
				}
			}
		}
	}
	
	/**
	 * Sets the displayed tab to the tab with the given index. Has no effect if
	 * the specified index is not within the limits of the tabs returned by
	 * <code>getTabs()</code>.
	 * 
	 * @param index the index of the tab to dispay
	 */
	public void setActiveTab(int index) {
		ILaunchConfigurationTab[] tabs = getTabs();
		if (index >= 0 && index < tabs.length) {
			fTabFolder.setSelection(index);
			handleTabSelected();
		}
	}

}
