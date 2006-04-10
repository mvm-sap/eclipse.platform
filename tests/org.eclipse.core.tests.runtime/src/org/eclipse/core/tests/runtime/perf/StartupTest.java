/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.perf;

import junit.framework.*;
import org.eclipse.test.performance.*;

public class StartupTest extends TestCase {

	public static Test suite() {
		return new TestSuite(StartupTest.class);
	}

	public StartupTest(String methodName) {
		super(methodName);
	}

	public void testApplicationStartup() {
		PerformanceMeter meter = Performance.getDefault().createPerformanceMeter(getClass().getName() + '.' + getName());
		try {
			meter.stop();
			// tag for showing in the performance fingerprint graph
			Performance.getDefault().tagAsGlobalSummary(meter, "Core Headless Startup", Dimension.ELAPSED_PROCESS);
			String reportOption = System.getProperty("eclipseTest.ReportResults");
			boolean bReport = (reportOption == null) ? true : !("false".equalsIgnoreCase(reportOption));
			if (bReport)
				meter.commit();
			Performance.getDefault().assertPerformanceInRelativeBand(meter, Dimension.ELAPSED_PROCESS, -100, 5);
		} finally {
			meter.dispose();
		}
	}
}
