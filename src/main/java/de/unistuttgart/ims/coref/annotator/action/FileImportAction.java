package de.unistuttgart.ims.coref.annotator.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.kordamp.ikonli.materialdesign.MaterialDesign;

import de.unistuttgart.ims.coref.annotator.Annotator;
import de.unistuttgart.ims.coref.annotator.plugins.IOPlugin;
import javafx.event.EventHandler;

public class FileImportAction extends IkonAction implements EventHandler<javafx.event.ActionEvent> {
	private static final long serialVersionUID = 1L;

	IOPlugin plugin;

	public FileImportAction(Annotator mApplication, IOPlugin plugin) {
		super(plugin.getName(), false, MaterialDesign.MDI_IMPORT);
		putValue(Action.SHORT_DESCRIPTION, plugin.getDescription());
		this.plugin = plugin;
	}

	public FileImportAction(IOPlugin plugin) {
		super(plugin.getName(), false, MaterialDesign.MDI_IMPORT);
		putValue(Action.SHORT_DESCRIPTION, plugin.getDescription());
		this.plugin = plugin;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Annotator.app.fileOpenDialog(null, plugin);

	}

	@Override
	public void handle(javafx.event.ActionEvent event) {
		actionPerformed(null);
	}
}
