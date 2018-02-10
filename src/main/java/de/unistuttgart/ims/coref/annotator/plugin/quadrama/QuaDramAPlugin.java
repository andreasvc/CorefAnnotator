package de.unistuttgart.ims.coref.annotator.plugin.quadrama;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import de.unistuttgart.ims.coref.annotator.plugins.DefaultIOPlugin;
import de.unistuttgart.ims.coref.annotator.plugins.StylePlugin;

public class QuaDramAPlugin extends DefaultIOPlugin {

	@Override
	public String getDescription() {
		return "Plugin that provides an importer and style.";
	}

	@Override
	public String getName() {
		return "QuaDramA/WebAnno";
	}

	@Override
	public AnalysisEngineDescription getImporter() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(ImportQuaDramA.class);
	}

	@Override
	public AnalysisEngineDescription getExporter() {
		return null;
	}

	@Override
	public Class<? extends StylePlugin> getStylePlugin() {
		return QDStylePlugin.class;
	}
}
