package de.unistuttgart.ims.coref.annotator.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import de.unistuttgart.ims.coref.annotator.Annotator;
import de.unistuttgart.ims.coref.annotator.CoreferenceFlavor;

public class FileOpenAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	Annotator mainApplication;

	public FileOpenAction(Annotator mApplication) {
		putValue(Action.NAME, "Open");
		putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mainApplication = mApplication;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		mainApplication.fileOpenDialog(CoreferenceFlavor.Default);

	}
}