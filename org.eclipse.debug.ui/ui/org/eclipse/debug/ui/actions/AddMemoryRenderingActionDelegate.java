/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     WindRiver - Bug 192028 [Memory View] Memory view does not 
 *                 display memory blocks that do not reference IDebugTarget     
 *******************************************************************************/

package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A cascade menu to add a memory rendering to the memory view. This action delegate can be
 * contributed to a an editor, view or object via standard workbench extension points. 
 * The action works on the {@link IAddMemoryRenderingsTarget} adapter provided 
 * by the active debug context, creating a context menu to add applicable renderings
 * to the memory view.
 * <p>
 * Clients may reference/contribute this class as an action delegate
 * in plug-in XML. This class is not intended to be subclassed.
 * </p>
 * @since 3.2
 */
public class AddMemoryRenderingActionDelegate extends Action implements IViewActionDelegate, IEditorActionDelegate, IObjectActionDelegate, IActionDelegate2{
	
	private IAction fAction;
	private IWorkbenchPart fPart;
	private ISelection fCurrentSelection;
	private IAddMemoryRenderingsTarget fActionDelegate;
	private IMenuCreator fMenuCreator;
	private IAdaptable fDebugContext;
	private IWorkbenchWindow fWindow;
	private DebugContextListener fDebugContextListener = new DebugContextListener();
	
	private class AddMemoryRenderingAction extends Action
	{
		private IMemoryRenderingType fRenderingType;			// type of rendering to add
		AddMemoryRenderingAction(IMemoryRenderingType renderingType)
		{
			super(renderingType.getLabel());
			fRenderingType = renderingType;
		}

		public void runWithEvent(Event event) {
			if (fActionDelegate != null)
			{
				try {
					fActionDelegate.addMemoryRenderings(fPart, fCurrentSelection, new IMemoryRenderingType[]{fRenderingType});
				} catch (CoreException e) {
					DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), ActionMessages.AddMemoryRenderingActionDelegate_0, ActionMessages.AddMemoryRenderingActionDelegate_1, e);
				}
			}
		}
	}
	
	private class AddMemoryRenderingMenuCreator implements IMenuCreator
	{

		public void dispose() {
			
		}

		public Menu getMenu(Control parent) {
			return null;
		}

		public Menu getMenu(Menu parent) {
			Menu menu = new Menu(parent);
			menu.addMenuListener(new MenuAdapter() {
				public void menuShown(MenuEvent e) {
					Menu m = (Menu)e.widget;
					MenuItem[] items = m.getItems();
					for (int i=0; i < items.length; i++) {
						items[i].dispose();
					}
					fillMenu(m);
				}
			});		
			return menu;
		}
		
		private void fillMenu(Menu parent)
		{
			if (fActionDelegate != null)
			{
				IMemoryRenderingType[] types = fActionDelegate.getMemoryRenderingTypes(fPart, fCurrentSelection);
				
				for (int i=0; i<types.length; i++)
				{
					AddMemoryRenderingAction action = new AddMemoryRenderingAction(types[i]);
					ActionContributionItem item = new ActionContributionItem(action);
					item.fill(parent, -1);
				}
			}
		}
	}	
	
	private class DebugContextListener implements IDebugContextListener
	{

		private void contextActivated(ISelection selection) {
			setupActionDelegate(selection);
			updateAction(fAction, fCurrentSelection);
			
		}

		public void debugContextChanged(DebugContextEvent event) {
			contextActivated(event.getContext());
		}

		
	}
	
	private void setupActionDelegate(ISelection context)
	{
		IAdaptable debugContext = null;
		if (context instanceof IStructuredSelection)
		{
			if (((IStructuredSelection)context).getFirstElement() instanceof IAdaptable)
				debugContext = (IAdaptable)((IStructuredSelection)context).getFirstElement();
		}
		
		if (debugContext == null)
			fActionDelegate = null;
		
		if (debugContext == fDebugContext)
			return;
		
		fDebugContext = debugContext;
		
		if (fDebugContext == null)
			return;
		
		IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(fDebugContext);
		
		if (retrieval == null)
			return;
		
		IAddMemoryRenderingsTarget target = null;
		if (fCurrentSelection instanceof IStructuredSelection)
		{
			// get target from current selection
			IStructuredSelection strucSel = (IStructuredSelection)fCurrentSelection;
			Object obj = strucSel.getFirstElement();
			target = getAddMemoryRenderingTarget(obj);
		}
		if (target == null)
		{
			// get the target from Debug View
			target = getAddMemoryRenderingTarget(fDebugContext);
		}
		if (target == null)
		{
			// try to get target from memory block retrieval
			target = getAddMemoryRenderingTarget(retrieval);
		}
		
		fActionDelegate = target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view) {
		bindPart(view);	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		bindAction(action);
		fCurrentSelection = selection;
		updateAction(action, selection);
	}
	
	private void updateAction(IAction action, ISelection selection)
	{
		if (fActionDelegate != null)
		{
			action.setEnabled(fActionDelegate.canAddMemoryRenderings(fPart, selection));
			bindAction(action);
		}
		else
		{
			action.setEnabled(false);
		}
	}

	private void bindAction(IAction action) {
		if (action == null)
			return;
		
		if (action != fAction) {
			if (fMenuCreator == null)
				fMenuCreator = new AddMemoryRenderingMenuCreator();
	        action.setMenuCreator(fMenuCreator);
	        fAction= action;
	    }
	}

	private IAddMemoryRenderingsTarget getAddMemoryRenderingTarget(Object elmt) {
		IAddMemoryRenderingsTarget target = null;
		
		if (elmt instanceof IAddMemoryRenderingsTarget)
		{
			target = (IAddMemoryRenderingsTarget)elmt;
		}
		else if (elmt instanceof IAdaptable)
		{
			target = (IAddMemoryRenderingsTarget)((IAdaptable)elmt).getAdapter(IAddMemoryRenderingsTarget.class);
		}
		return target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		bindPart(targetEditor);
		bindAction(action);
		updateAction(action, fCurrentSelection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		bindPart(targetPart);
		bindAction(action);
		updateAction(action, fCurrentSelection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
	 */
	public void init(IAction action) {		
		bindAction(action);
		if (action != null)
		{
			action.setText(ActionMessages.AddMemoryRenderingActionDelegate_2);
			action.setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_MONITOR_EXPRESSION));
			action.setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_MONITOR_EXPRESSION));
			action.setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_MONITOR_EXPRESSION));
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#dispose()
	 */
	public void dispose() {
		// remove as debug context listener
		fAction = null;
		fPart = null;
		fCurrentSelection = null;
		fActionDelegate = null;
		
		// remove debug context listener
		bindPart(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
	 */
	public void runWithEvent(IAction action, Event event) {
		// do nothing
	}
	
	private void bindPart(IWorkbenchPart part)
	{
		IWorkbenchWindow window = null;
		if (part != null)
		{
			window = part.getSite().getWorkbenchWindow();
		}
		if (window != fWindow)
		{
				
			if (fWindow != null)
			{
				DebugUITools.getDebugContextManager().getContextService(fWindow).removeDebugContextListener(fDebugContextListener);
			}
			
			if (window != null)
			{
				DebugUITools.getDebugContextManager().getContextService(window).addDebugContextListener(fDebugContextListener);
			}
			fWindow = window;
		}
		
		if (part != fPart)
			fPart = part;
		
		if (fWindow != null)
			setupActionDelegate(DebugUITools.getDebugContextManager().getContextService(fWindow).getActiveContext());
	}

}

