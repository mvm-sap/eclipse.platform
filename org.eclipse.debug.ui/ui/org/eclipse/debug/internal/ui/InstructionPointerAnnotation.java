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
package org.eclipse.debug.internal.ui;

 
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.IDebugEditorPresentation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * An annotation for the vertical ruler in text editors that shows one of two
 * images for the current instruction pointer when debugging (one for the top
 * stack frame, one for all others).
 */
public class InstructionPointerAnnotation extends Annotation {

	/**
	 * The frame for this instruction pointer annotation.  This is necessary only so that
	 * instances of this class can be distinguished by equals().
	 */
	private IStackFrame fStackFrame;
	
	/**
	 * Flag indicating if this annotation represents a top stack frame.  Top stack frames
	 * have different images from all other stack frames.
	 */
	private boolean fTopStackFrame;

	/**
	 * The layer at which to draw the instruction pointer annotation.  The instruction pointer
	 * should be rendered on top of any other type of annotation or marker.
	 * 
	 * @see org.eclipse.jface.text.source.Annotation
	 */
	private static final int INSTRUCTION_POINTER_ANNOTATION_LAYER = 6;

	/**
	 * Construct an instruction pointer annotation for the given stack frame.
	 * 
	 * @param stackFrame frame to create an instruction pointer annotation for
	 */
	public InstructionPointerAnnotation(IStackFrame stackFrame) {
		setLayer(INSTRUCTION_POINTER_ANNOTATION_LAYER);
		setStackFrame(stackFrame);
	}

	/**
	 * @see org.eclipse.jface.text.source.Annotation#paint(org.eclipse.swt.graphics.GC, org.eclipse.swt.widgets.Canvas, org.eclipse.swt.graphics.Rectangle)
	 */
	public void paint(GC gc, Canvas canvas, Rectangle bounds) {
		Image image = getInstructionPointerImage(isTopStackFrame());
		drawImage(image, gc, canvas, bounds, SWT.CENTER);
	}
	
	/**
	 * Returns the image associated with this instruction pointer.
	 * 
	 * @return image associated with this instruction pointer
	 */
	private Image getInstructionPointerImage(boolean topStackFrame) {
		IDebugEditorPresentation presentation = (IDebugEditorPresentation)DebugUIPlugin.getModelPresentation();
		Image image = presentation.getInstructionPointerImage(getStackFrame());
		if (image == null) {
			if (topStackFrame) {
				image = DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER_TOP);
			} else {
				image = DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER);			
			}
		}
		return image;
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other instanceof InstructionPointerAnnotation) {
			return getStackFrame().equals(((InstructionPointerAnnotation)other).getStackFrame());			
		}
		return false;
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getStackFrame().hashCode();
	}

	/**
	 * Sets the stack frame associated with this annotation and determines
	 * if the frame is on top of the stack.
	 * 
	 * @param stackFrame frame associated with this annotation
	 */
	private void setStackFrame(IStackFrame stackFrame) {
		fStackFrame = stackFrame;
		IThread thread = stackFrame.getThread();
		try {
			fTopStackFrame = stackFrame.equals(thread.getTopStackFrame());
		} catch (DebugException de) {
			fTopStackFrame = false;
		}		
	}

	/**
	 * Returns the stack frame associated with this annotation
	 * 
	 * @return the stack frame associated with this annotation
	 */
	private IStackFrame getStackFrame() {
		return fStackFrame;
	}
		
	/**
	 * Returns whether the stack frame associated with this annotation is the
	 * top stack frame in its thread.
	 * 
	 * @return whether the stack frame associated with this annotation is the
	 * top stack frame in its thread
	 */
	private boolean isTopStackFrame() {
		return fTopStackFrame;
	}

}
