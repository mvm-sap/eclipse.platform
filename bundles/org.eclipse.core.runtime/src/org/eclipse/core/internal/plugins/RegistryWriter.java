package org.eclipse.core.internal.plugins;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.*;
import java.io.PrintWriter;

public class RegistryWriter {
public RegistryWriter() {
	super();
}
public void writeConfigurationElement(ConfigurationElementModel configElement, PrintWriter w, int indent) {
	String element = configElement.getName();
	if (element == null)
		return;

	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";
	String gap2 = gap1;
	for (int i = 0; i < IModel.INDENT; i++)
		gap2 += " ";

	w.print(gap1 + "<" + element);
	ConfigurationPropertyModel[] propList = configElement.getProperties();
	int propSize = (propList == null) ? 0 : propList.length;
	for (int i = 0; i < propSize; i++) 
		writeConfigurationProperty(propList[i], w, indent + IModel.INDENT);

	ConfigurationElementModel[] subElementList = configElement.getSubElements();
	int subElementSize = (subElementList == null) ? 0 : subElementList.length;
	if (configElement.getValue() == null && subElementSize == 0) {
		w.println("/>");
		return;
	}
	w.println(">");

	if (configElement.getValue() != null)
		w.println(gap2 + PluginParser.xmlSafe(configElement.getValue()));
	for (int i = 0; i < subElementSize; i++) 
		writeConfigurationElement(subElementList[i], w, indent + IModel.INDENT);

	w.println(gap1 + "</" + element + ">");
}
public void writeConfigurationProperty(ConfigurationPropertyModel configProp, PrintWriter w, int indent) {
	if (configProp.getName() == null)
		return;
	w.print(" " + configProp.getName() + "=\"");
	if (configProp.getValue() != null)
		w.print(PluginParser.xmlSafe(configProp.getValue()));
	w.print("\"");
}
public void writeExtension(ExtensionModel extension, PrintWriter w, int indent) {
	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";

	w.print(gap1 + "<" + IModel.EXTENSION);
	if (extension.getExtensionPoint() != null)
		w.print(" " + IModel.EXTENSION_TARGET + "=\"" + PluginParser.xmlSafe(extension.getExtensionPoint()) + "\"");
	if (extension.getId() != null)
		w.print(" " + IModel.EXTENSION_ID + "=\"" + PluginParser.xmlSafe(extension.getId()) + "\"");
	if (extension.getName() != null)
		w.print(" " + IModel.EXTENSION_NAME + "=\"" + PluginParser.xmlSafe(extension.getName()) + "\"");

	ConfigurationElementModel[] subElements = extension.getSubElements();
	int size = (subElements == null) ? 0 : subElements.length;
	if (size == 0) {
		w.println("/>");
		return;
	}
	w.println(">");

	for (int i = 0; i < size; i++) 
		writeConfigurationElement(subElements[i], w, indent + IModel.INDENT);

	w.println(gap1 + "</" + IModel.EXTENSION + ">");
}
public void writeExtensionPoint(ExtensionPointModel extPt, PrintWriter w, int indent) {
	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";

	w.print(gap1 + "<" + IModel.EXTENSION_POINT);
	if (extPt.getId() != null)
		w.print(" " + IModel.EXTENSION_POINT_ID + "=\"" + PluginParser.xmlSafe(extPt.getId()) + "\"");
	if (extPt.getName() != null)
		w.print(" " + IModel.EXTENSION_POINT_NAME + "=\"" + PluginParser.xmlSafe(extPt.getName()) + "\"");
	if (extPt.getSchema() != null)
		w.print(" " + IModel.EXTENSION_POINT_SCHEMA + "=\"" + PluginParser.xmlSafe(extPt.getSchema()) + "\"");
	w.println("/>");
}
public void writeLibrary(LibraryModel library, PrintWriter w, int indent) {
	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";
	String gap2 = gap1;
	for (int i = 0; i < IModel.INDENT; i++)
		gap2 += " ";

	w.print(gap1 + "<" + IModel.LIBRARY);
	if (library.getName() != null)
		w.print(" " + IModel.LIBRARY_NAME + "=\"" + PluginParser.xmlSafe(library.getName()) + "\"");
	if (library.getType() != null)
		w.print(" " + IModel.LIBRARY_TYPE + "=\"" + PluginParser.xmlSafe(library.getType()) + "\"");
	if (!library.isExported())
		w.println("/>");
	else {
		w.println(">");
		String[] exports = library.getExports();
		int size = (exports == null) ? 0 : exports.length;
		for (int i = 0; i < size; i++)
			w.println(gap2 + "<" + IModel.LIBRARY_EXPORT + " " + IModel.LIBRARY_EXPORT_MASK + "=\"" + PluginParser.xmlSafe(exports[i]) + "\"/>");
		w.println(gap1 + "</" + IModel.LIBRARY + ">");
	}
}
public void writePluginDescriptor(PluginDescriptorModel plugin, PrintWriter w, int indent) {

	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";
	String gap2 = gap1;
	for (int i = 0; i < IModel.INDENT; i++)
		gap2 += " ";

	w.println("");
	w.print(gap1 + "<" + IModel.PLUGIN);
	if (plugin.getId() != null)
		w.print(" " + IModel.PLUGIN_ID + "=\"" + PluginParser.xmlSafe(plugin.getId()) + "\"");
	if (plugin.getName() != null)
		w.print(" " + IModel.PLUGIN_NAME + "=\"" + PluginParser.xmlSafe(plugin.getName()) + "\"");
	if (plugin.getProviderName() != null)
		w.print(" " + IModel.PLUGIN_PROVIDER + "=\"" + PluginParser.xmlSafe(plugin.getProviderName()) + "\"");
	if (plugin.getVersion() != null)
		w.print(" " + IModel.PLUGIN_VERSION + "=\"" + PluginParser.xmlSafe(plugin.getVersion()) + "\"");
	if (plugin.getPluginClass() != null)
		w.print(" " + IModel.PLUGIN_CLASS + "=\"" + PluginParser.xmlSafe(plugin.getPluginClass()) + "\"");
	w.println(">");

	PluginPrerequisiteModel[] requires = plugin.getRequires();
	int reqSize = (requires == null) ? 0 : requires.length;
	if (reqSize != 0) {
		w.print(gap2 + "<" + IModel.PLUGIN_REQUIRES);
		w.println(">");
		for (int i = 0; i < reqSize; i++) 
			writePluginPrerequisite(requires[i], w, indent + 2 * IModel.INDENT);
		w.println(gap2 + "</" + IModel.PLUGIN_REQUIRES + ">");
	}

	LibraryModel[] runtime = plugin.getRuntime();
	int runtimeSize = (runtime == null) ? 0 : runtime.length;
	if (runtimeSize != 0) {
		w.println(gap2 + "<" + IModel.RUNTIME + ">");
		for (int i = 0; i < runtimeSize; i++) {
			writeLibrary(runtime[i], w, indent + 2 * IModel.INDENT);
		}
		w.println(gap2 + "</" + IModel.RUNTIME + ">");
	}

	ExtensionPointModel[] extensionPoints = plugin.getDeclaredExtensionPoints();
	int extPointsSize = (extensionPoints == null) ? 0 : extensionPoints.length;
	if (extPointsSize != 0) {
		w.println("");
		for (int i = 0; i < extPointsSize; i++)
			writeExtensionPoint(extensionPoints[i], w, indent + IModel.INDENT);
	}

	ExtensionModel[] extensions = plugin.getDeclaredExtensions();
	int extSize = (extensions == null) ? 0 : extensions.length;
	if (extSize != 0) {
		for (int i = 0; i < extSize; i++) {
			w.println("");
			writeExtension(extensions[i], w, indent + IModel.INDENT);
		}
	}
	
	PluginFragmentModel[] fragments = plugin.getFragments();
	int fragmentSize = (fragments == null) ? 0 : fragments.length;
	if (fragmentSize != 0) {
		for (int i = 0; i < fragmentSize; i++) {
			w.println("");
			writePluginFragment(fragments[i], w, indent + IModel.INDENT);
		}
	}

	w.println(gap1 + "</" + IModel.PLUGIN + ">");
}
public void writePluginFragment(PluginFragmentModel fragment, PrintWriter w, int indent) {

	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";
	String gap2 = gap1;
	for (int i = 0; i < IModel.INDENT; i++)
		gap2 += " ";

	w.println("");
	w.print(gap1 + "<" + IModel.FRAGMENT);
	if (fragment.getId() != null)
		w.print(" " + IModel.FRAGMENT_ID + "=\"" + PluginParser.xmlSafe(fragment.getId()) + "\"");
	if (fragment.getName() != null)
		w.print(" " + IModel.FRAGMENT_NAME + "=\"" + PluginParser.xmlSafe(fragment.getName()) + "\"");
	if (fragment.getProviderName() != null)
		w.print(" " + IModel.FRAGMENT_PROVIDER + "=\"" + PluginParser.xmlSafe(fragment.getProviderName()) + "\"");
	if (fragment.getVersion() != null)
		w.print(" " + IModel.FRAGMENT_VERSION + "=\"" + PluginParser.xmlSafe(fragment.getVersion()) + "\"");
	if (fragment.getPluginId() != null)
		w.print(" " + IModel.FRAGMENT_PLUGIN_ID + "=\"" + PluginParser.xmlSafe(fragment.getPluginId()) + "\"");
	if (fragment.getPluginVersion() != null)
		w.print(" " + IModel.FRAGMENT_PLUGIN_VERSION + "=\"" + PluginParser.xmlSafe(fragment.getPluginVersion()) + "\"");
	w.println(">");

	PluginPrerequisiteModel[] requires = fragment.getRequires();
	int reqSize = (requires == null) ? 0 : requires.length;
	if (reqSize != 0) {
		w.print(gap2 + "<" + IModel.PLUGIN_REQUIRES);
		w.println(">");
		for (int i = 0; i < reqSize; i++) 
			writePluginPrerequisite(requires[i], w, indent + 2 * IModel.INDENT);
		w.println(gap2 + "</" + IModel.PLUGIN_REQUIRES + ">");
	}

	LibraryModel[] runtime = fragment.getRuntime();
	int runtimeSize = (runtime == null) ? 0 : runtime.length;
	if (runtimeSize != 0) {
		w.println(gap2 + "<" + IModel.RUNTIME + ">");
		for (int i = 0; i < runtimeSize; i++) {
			writeLibrary(runtime[i], w, indent + 2 * IModel.INDENT);
		}
		w.println(gap2 + "</" + IModel.RUNTIME + ">");
	}

	ExtensionPointModel[] extensionPoints = fragment.getDeclaredExtensionPoints();
	int extPointsSize = (extensionPoints == null) ? 0 : extensionPoints.length;
	if (extPointsSize != 0) {
		w.println("");
		for (int i = 0; i < extPointsSize; i++)
			writeExtensionPoint(extensionPoints[i], w, indent + IModel.INDENT);
	}

	ExtensionModel[] extensions = fragment.getDeclaredExtensions();
	int extSize = (extensions == null) ? 0 : extensions.length;
	if (extSize != 0) {
		for (int i = 0; i < extSize; i++) {
			w.println("");
			writeExtension(extensions[i], w, indent + IModel.INDENT);
		}
	}

	w.println(gap1 + "</" + IModel.FRAGMENT + ">");
}
public void writePluginPrerequisite(PluginPrerequisiteModel req, PrintWriter w, int indent) {
	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";

	w.print(gap1 + "<" + IModel.PLUGIN_REQUIRES_IMPORT);
	w.print(" " + IModel.PLUGIN_REQUIRES_PLUGIN + "=\"" + PluginParser.xmlSafe(req.getPlugin()) + "\"");
	if (req.getVersion() != null)
		w.print(" " + IModel.PLUGIN_REQUIRES_PLUGIN_VERSION + "=\"" + PluginParser.xmlSafe(req.getVersion()) + "\"");
	if (req.getExport())
		w.print(" " + IModel.PLUGIN_REQUIRES_EXPORT + "=\"" + IModel.TRUE + "\"");
	if (req.getOptional())
		w.print(" " + IModel.PLUGIN_REQUIRES_OPTIONAL + "=\"" + IModel.TRUE + "\"");
	if (req.getMatch())
		w.print(" " + IModel.PLUGIN_REQUIRES_MATCH + "=\"" + IModel.PLUGIN_REQUIRES_MATCH_EXACT + "\"");
	else
		w.print(" " + IModel.PLUGIN_REQUIRES_MATCH + "=\"" + IModel.PLUGIN_REQUIRES_MATCH_COMPATIBLE + "\"");
	w.println("/>");
}
public void writePluginRegistry(PluginRegistryModel registry, PrintWriter w, int indent) {
	String gap1 = "";
	for (int i = 0; i < indent; i++)
		gap1 += " ";
	w.println(gap1 + "<" + IModel.REGISTRY + ">");
	PluginDescriptorModel[] pluginList = registry.getPlugins();
	for (int i = 0; i < pluginList.length; i++)
		writePluginDescriptor(pluginList[i], w, indent + IModel.INDENT);
	w.println(gap1 + "</" + IModel.REGISTRY + ">");
	
	// XXX: We don't print out the fragments.  They are printed out
	// as part of the plugin descriptor.  But what if this is a
	// fragment that doesn't have an associated plugin?
}
}
