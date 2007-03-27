/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Global retargettable debug action.
 * 
 * @since 3.0
 */
public abstract class RetargetAction implements IWorkbenchWindowActionDelegate, IPartListener, IUpdate {
	
	protected IWorkbenchWindow fWindow = null;
	private IWorkbenchPart fActivePart = null;
	private Object fTargetAdapter = null;
	private IAction fAction = null;
	private static final ISelection EMPTY_SELECTION = new EmptySelection();  
	
	static class EmptySelection implements ISelection {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelection#isEmpty()
		 */
		public boolean isEmpty() {
			return true;
		}
		
	}
	
	/**
	 * Returns the current selection in the active part, possibly
	 * and empty selection, but never <code>null</code>.
	 * 
	 * @return the selection in the active part, possibly empty
	 */
	private ISelection getTargetSelection() {
		if (fActivePart != null) {
			ISelectionProvider selectionProvider = fActivePart.getSite().getSelectionProvider();
			if (selectionProvider != null) {
				return selectionProvider.getSelection();
			}
		}
		return EMPTY_SELECTION;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		fWindow.getPartService().removePartListener(this);
		fActivePart = null;
		fTargetAdapter = null;
		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		this.fWindow = window;
		IPartService partService = window.getPartService();
		partService.addPartListener(this);
		IWorkbenchPart part = partService.getActivePart();
		if (part != null) {
			partActivated(part);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fTargetAdapter != null) {
			try {
				performAction(fTargetAdapter, getTargetSelection(), fActivePart);
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(fWindow.getShell(), ActionMessages.RetargetAction_2, ActionMessages.RetargetAction_3, e.getStatus()); // 
			}
		}
	}
	
	/**
	 * Performs the specific breakpoint toggling.
	 * 
	 * @param selection selection in the active part 
	 * @param part active part
	 * @throws CoreException if an exception occurs
	 */
	protected abstract void performAction(Object target, ISelection selection, IWorkbenchPart part) throws CoreException;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.fAction = action;
		// if the active part did not provide an adapter, see if the selection does
		if (fTargetAdapter == null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (!ss.isEmpty()) {
				Object object = ss.getFirstElement();
				if (object instanceof IAdaptable) {
					fTargetAdapter = getAdapter((IAdaptable) object);
				}
			}
		}
		update();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partActivated(IWorkbenchPart part) {
		fActivePart = part;
		IResource resource = (IResource) part.getAdapter(IResource.class);
		if (resource == null && part instanceof IEditorPart) {
			resource = (IResource) ((IEditorPart)part).getEditorInput().getAdapter(IResource.class);
		}
		if (resource != null) {
			fTargetAdapter = getAdapter(resource);
		}
		if (fTargetAdapter == null) {
			fTargetAdapter = getAdapter(part);
		}
		update();
	}
	
	protected Object getAdapter(IAdaptable adaptable) {
		Object adapter  = adaptable.getAdapter(getAdapterClass());
		if (adapter == null) {
			IAdapterManager adapterManager = Platform.getAdapterManager();
			if (adapterManager.hasAdapter(adaptable, getAdapterClass().getName())) { 
				fTargetAdapter = adapterManager.loadAdapter(adaptable, getAdapterClass().getName()); 
			}
		}
		return adapter;
	}
	
	/**
	 * Returns the type of adapter (target) this action works on.
	 * 
	 * @return the type of adapter this action works on
	 */
	protected abstract Class getAdapterClass();
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partBroughtToTop(IWorkbenchPart part) {		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
		clearPart(part);
	}
	
	/**
	 * Clears reference to active part and adapter when a relevant part
	 * is closed or no longer active.
	 * 
	 * @param part workbench part that has been closed or no longer active
	 */
	protected void clearPart(IWorkbenchPart part) {
		if (part.equals(fActivePart)) {
			fActivePart = null;
			fTargetAdapter = null;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partDeactivated(IWorkbenchPart part) {
		clearPart(part);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partOpened(IWorkbenchPart part) {		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		if (fAction == null) {
			return;
		}
		final Object target = fTargetAdapter;
		if (target != null) {
			Runnable r = new Runnable() {
				public void run() {
                    if (fActivePart != null) {
                        fAction.setEnabled(canPerformAction(target, getTargetSelection(), fActivePart));
                    }
				}
			};
			DebugUIPlugin.getStandardDisplay().asyncExec(r);
		} else {
			fAction.setEnabled(false);
		}
	}
	
	/**
	 * Returns whether the specific operation is supported.
	 * 
	 * @param target the target adapter 
	 * @param selection the selection to verify the operation on
	 * @param part the part the operation has been requested on
	 * @return whether the operation can be performed
	 */
	protected abstract boolean canPerformAction(Object target, ISelection selection, IWorkbenchPart part); 
}
