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
package org.eclipse.debug.core.model;


import org.eclipse.debug.core.DebugException;

/**
 * A thread is a sequential flow of execution in a debug target.
 * A thread contains stack frames.  Stack frames are only available when the
 * thread is suspended, and are returned in top-down order.
 * Minimally, a thread supports the following:
 * <ul>
 * <li>suspend/resume
 * <li>stepping
 * <li>terminate
 * </ul>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ISuspendResume
 * @see IStep
 * @see ITerminate
 * @see IStackFrame
 */

public interface IThread extends IDebugElement, ISuspendResume, IStep, ITerminate {
	/**
	 * Returns the stack frames contained in this thread. An
	 * empty collection is returned if this thread contains
	 * no stack frames, or is not currently suspended. Stack frames
	 * are returned in top down order.
	 * 
	 * @return a collection of stack frames
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	public IStackFrame[] getStackFrames() throws DebugException;
	
	/**
	 * Returns whether this thread currently contains any stack
	 * frames.
	 * 
	 * @return whether this thread currently contains any stack frames
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	public boolean hasStackFrames() throws DebugException;
	
	/**
	 * Returns the priority of this thread. The meaning of this
	 * number is operating-system dependent.
	 *
	 * @return thread priority
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public int getPriority() throws DebugException;
	/** 
	 * Returns the top stack frame or <code>null</code> if there is
	 * currently no top stack frame.
	 *
	 * @return the top stack frame, or <code>null</code> if none
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public IStackFrame getTopStackFrame() throws DebugException;
	/**
	 * Returns the name of this thread. Name format is debug model
	 * specific, and should be specified by a debug model.
	 *
	 * @return this thread's name
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public String getName() throws DebugException;

	/**
	 * Returns the breakpoints that caused this thread to suspend,
	 * or an empty collection if this thread is not suspended or
	 * was not suspended by a breakpoint. Usually a single breakpoint
	 * will be returned, but this collection can contain more than
	 * one breakpoint if two breakpoints are at the same location in
	 * a program.
	 *
	 * @return the collection of breakpoints that caused this thread to suspend
	 */
	public IBreakpoint[] getBreakpoints();	
}
