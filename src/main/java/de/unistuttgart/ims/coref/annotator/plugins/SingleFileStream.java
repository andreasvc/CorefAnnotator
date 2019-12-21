package de.unistuttgart.ims.coref.annotator.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import de.unistuttgart.ims.coref.annotator.Annotator;

public abstract class SingleFileStream extends JCasConsumer_ImplBase {
	public static final String PARAM_FILE = "File";

	@ConfigurationParameter(name = PARAM_FILE)
	String outputFile;

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		File f = new File(outputFile);
		try (OutputStream fos = new FileOutputStream(f)) {
			write(jcas, fos);
			fos.close();
		} catch (IOException e) {
			Annotator.logger.catching(e);
		}
	}

	public abstract void write(JCas jcas, OutputStream os) throws IOException;

}
