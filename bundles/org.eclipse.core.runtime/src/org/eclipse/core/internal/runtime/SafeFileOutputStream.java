package org.eclipse.core.internal.runtime;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
/**
 * This class should be used when there's a file already in the
 * destination and we don't want to lose its contents if a
 * failure writing this stream happens.
 * Basically, the new contents are written to a temporary location.
 * If everything goes OK, it is moved to the right place.
 * The user has the option to define the temporary location or 
 * it will be created in the default-temporary directory
 * (see java.io.File for details).
 */
public class SafeFileOutputStream extends OutputStream {
	protected File temp;
	protected File target;
	protected OutputStream output;
	protected boolean failed;
	protected static final String EXTENSION = ".bak";
public SafeFileOutputStream(File file) throws IOException {
	this(file.getAbsolutePath(), null);
}
public SafeFileOutputStream(String targetName) throws IOException {
	this(targetName, null);
}
/**
 * If targetPath is null, the file will be created in the default-temporary directory.
 */
public SafeFileOutputStream(String targetPath, String tempPath) throws IOException {
	failed = false;
	target = new File(targetPath);
	createTempFile(tempPath);
	// If we do not have a file at target location, but we do have at temp location,
	// it probably means something wrong happened the last time we tried to write it.
	// So, try to recover the backup file. And, if successful, write the new one.
	if (!target.exists() && !temp.exists()) {
		output = new BufferedOutputStream(new FileOutputStream(target));
		return;
	}
	copy(temp, target);
	output = new BufferedOutputStream(new FileOutputStream(temp));
}
public void close() throws IOException {
	try {
		output.close();
	} catch (IOException e) {
		failed = true;
		throw e; // rethrow
	}
	if (failed)
		temp.delete();
	else
		commit();
}
protected void commit() throws IOException {
	if (!temp.exists())
		return;
	target.delete();
	copy(temp, target);
	temp.delete();
}
protected void copy(File sourceFile, File destinationFile) throws IOException {
	if (!sourceFile.exists())
		return;
	FileInputStream source = new FileInputStream(sourceFile);
	FileOutputStream destination = new FileOutputStream(destinationFile);
	transferStreams(source, destination);
}
protected void createTempFile(String tempPath) throws IOException {
	if (tempPath == null)
		tempPath = target.getAbsolutePath() + EXTENSION;
	temp = new File(tempPath);
}
protected void finalize() throws Throwable {
	close();
}
public void flush() throws IOException {
	try {
		output.flush();
	} catch (IOException e) {
		failed = true;
		throw e; // rethrow
	}
}
public String getTempFilePath() {
	return temp.getAbsolutePath();
}
protected void transferStreams(InputStream source, OutputStream destination) throws IOException {
	try {
		byte[] buffer = new byte[8192];
		while (true) {
			int bytesRead = source.read(buffer);
			if (bytesRead == -1)
				break;
			destination.write(buffer, 0, bytesRead);
		}
	} finally {
		try {
			source.close();
		} catch (IOException e) {
		}
		try {
			destination.close();
		} catch (IOException e) {
		}
	}
}
public void write(int b) throws IOException {
	try {
		output.write(b);
	} catch (IOException e) {
		failed = true;
		throw e; // rethrow
	}
}
}
