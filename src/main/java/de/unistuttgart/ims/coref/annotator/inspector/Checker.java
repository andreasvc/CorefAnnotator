package de.unistuttgart.ims.coref.annotator.inspector;

import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.unistuttgart.ims.coref.annotator.Annotator;
import de.unistuttgart.ims.coref.annotator.api.Mention;
import de.unistuttgart.ims.coref.annotator.document.DocumentModel;

public class Checker extends SwingWorker<ListModel<Issue>, Object> {

	JCas jcas;
	DocumentModel documentModel;
	Inspector caller;
	DefaultListModel<Issue> listModel;

	public static char[] whitespace = new char[] { ' ', '\n', '\t', '\r', '\f' };

	public Checker(DocumentModel documentModel, Inspector caller, DefaultListModel<Issue> listModel) {
		this.jcas = documentModel.getJcas();
		this.documentModel = documentModel;
		this.caller = caller;
		this.listModel = listModel;
	}

	@Override
	protected ListModel<Issue> doInBackground() throws Exception {
		String textString = jcas.getDocumentText();
		char[] text = textString.toCharArray();

		for (Mention m : JCasUtil.select(jcas, Mention.class)) {
			int b = m.getBegin(), e = m.getEnd();
			String surface1 = textString.substring(b - 1, e + 1);
			if (ArrayUtils.contains(whitespace, text[b - 1]) && ArrayUtils.contains(whitespace, text[b]))
				listModel.addElement(new Issue1(getDocumentModel(), m));

			if (ArrayUtils.contains(whitespace, text[e - 1]) && ArrayUtils.contains(whitespace, text[e]))
				listModel.addElement(new Issue2(getDocumentModel(), m));

			if (!surface1.matches(".*\\b\\Q" + m.getCoveredText() + "\\E.*"))
				listModel.addElement(new Issue3(getDocumentModel(), m));

		}
		return listModel;
	}

	@Override
	protected void done() {
		try {
			caller.setListModel(get());
		} catch (InterruptedException | ExecutionException e) {
			Annotator.logger.catching(e);
		}
	}

	public DocumentModel getDocumentModel() {
		return documentModel;
	}

	public void setDocumentModel(DocumentModel documentModel) {
		this.documentModel = documentModel;
	}

}