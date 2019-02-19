package de.unistuttgart.ims.coref.annotator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import de.unistuttgart.ims.coref.annotator.action.AddDirectedRelationAction;
import de.unistuttgart.ims.coref.annotator.action.AddUndirectedRelationAction;
import de.unistuttgart.ims.coref.annotator.action.DeleteAction;
import de.unistuttgart.ims.coref.annotator.api.v1.DirectedEntityRelation;
import de.unistuttgart.ims.coref.annotator.api.v1.Entity;
import de.unistuttgart.ims.coref.annotator.api.v1.EntityRelation;
import de.unistuttgart.ims.coref.annotator.api.v1.Flag;
import de.unistuttgart.ims.coref.annotator.api.v1.SymmetricEntityRelation;
import de.unistuttgart.ims.coref.annotator.comp.DefaultTableHeaderCellRenderer;
import de.unistuttgart.ims.coref.annotator.comp.EntityPanel;
import de.unistuttgart.ims.coref.annotator.document.DocumentModel;
import de.unistuttgart.ims.coref.annotator.document.FeatureStructureEvent;
import de.unistuttgart.ims.coref.annotator.document.RelationModelListener;
import de.unistuttgart.ims.coref.annotator.document.adapter.DirectedRelationsTableModel;
import de.unistuttgart.ims.coref.annotator.document.adapter.EntityComboBoxModel;
import de.unistuttgart.ims.coref.annotator.document.adapter.FlagComboBoxModel;
import de.unistuttgart.ims.coref.annotator.document.op.UpdateEntityRelation;
import de.unistuttgart.ims.coref.annotator.document.op.UpdateEntityRelation.EntityRelationProperty;

public class RelationEditor extends JFrame {

	private static final long serialVersionUID = 1L;

	DocumentModel documentModel;
	DocumentWindow documentWindow;

	Component tableWithDirectedRelations;
	Component tableWithUndirectedRelations;
	JPanel toolbar;

	public RelationEditor(DocumentModel documentModel, DocumentWindow documentWindow) {
		this.documentModel = documentModel;
		this.documentWindow = documentWindow;
		this.setTitle(Annotator.getString(Constants.Strings.RELATION_EDITOR) + ": " + documentWindow.getTitle());
		this.addWindowListener(new FlagEditorWindowListener());

		// Actions
		AbstractAction addDirectedRelationAction = new AddDirectedRelationAction(documentModel);
		AbstractAction addUndirectedRelationAction = new AddUndirectedRelationAction(documentModel);

		// Table
		tableWithDirectedRelations = initDirected();
		tableWithUndirectedRelations = initUndirected();

		this.toolbar = new JPanel();
		this.toolbar.add(new JButton(addDirectedRelationAction));
		this.toolbar.add(new JButton(addUndirectedRelationAction));

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tableWithDirectedRelations),
				new JScrollPane(tableWithUndirectedRelations));

		this.getContentPane().add(splitPane, BorderLayout.CENTER);
		this.getContentPane().add(toolbar, BorderLayout.NORTH);
		this.setVisible(true);
		this.pack();
	}

	private Component initUndirected() {

		JPanel panel = new SymmetricTable(documentModel);
		panel.setBorder(BorderFactory.createTitledBorder("undirected relations"));
		return panel;

	}

	private Component initDirected() {
		JPanel panel = new DirectedTable(documentModel);
		panel.setBorder(BorderFactory.createTitledBorder("directed relations"));

		EntityComboBoxModel entityComboBoxModel = new EntityComboBoxModel();
		documentModel.getCoreferenceModel().addCoreferenceModelListener(entityComboBoxModel);

		FlagComboBoxModel flagDirectedComboBoxModel = new FlagComboBoxModel(DirectedEntityRelation.class);
		documentModel.getFlagModel().addFlagModelListener(flagDirectedComboBoxModel);

		JComboBox<Entity> entityCombobox = new JComboBox<Entity>(entityComboBoxModel);
		entityCombobox.setRenderer(new EntityListCellRenderer());

		JComboBox<Flag> directedflagCombobox = new JComboBox<Flag>(flagDirectedComboBoxModel);
		directedflagCombobox.setRenderer(new FlagListCellRenderer());

		DirectedRelationsTableModel directedRelationsTableModel = new DirectedRelationsTableModel(
				documentModel.getRelationModel());

		JTable tableWithDirectedRelations = new JTable(directedRelationsTableModel);
		tableWithDirectedRelations.setGridColor(Color.GRAY);
		tableWithDirectedRelations.setAutoCreateColumnsFromModel(true);
		tableWithDirectedRelations.setAutoCreateRowSorter(true);
		tableWithDirectedRelations.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		tableWithDirectedRelations.setDefaultRenderer(Entity.class, new EntityTableCellRenderer());
		tableWithDirectedRelations.setDefaultRenderer(Flag.class, new FlagTableCellRenderer());
		tableWithDirectedRelations.setDefaultEditor(Entity.class, new DefaultCellEditor(entityCombobox));
		tableWithDirectedRelations.setDefaultEditor(Flag.class, new DefaultCellEditor(directedflagCombobox));
		tableWithDirectedRelations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableWithDirectedRelations.setDropMode(DropMode.ON);
		tableWithDirectedRelations.setDragEnabled(true);
		tableWithDirectedRelations.setTransferHandler(directedRelationsTableModel.getTransferHandler());
		tableWithDirectedRelations.setRowHeight(25);

		/*
		 * this.tableWithDirectedRelations.getColumnModel().getColumn(0)
		 * .setHeaderRenderer(new DefaultTableHeaderCellRenderer() {
		 * 
		 * private static final long serialVersionUID = 1L;
		 * 
		 * @Override public String getToolTipText() { return
		 * Annotator.getString(Constants.Strings.FLAG_EDITOR_ICON_TOOLTIP); } });
		 * this.tableWithDirectedRelations.getColumnModel().getColumn(1)
		 * .setHeaderRenderer(new DefaultTableHeaderCellRenderer() {
		 * 
		 * private static final long serialVersionUID = 1L;
		 * 
		 * @Override public String getToolTipText() { return
		 * Annotator.getString(Constants.Strings.FLAG_EDITOR_KEY_TOOLTIP); } });
		 * this.tableWithDirectedRelations.getColumnModel().getColumn(2)
		 * .setHeaderRenderer(new DefaultTableHeaderCellRenderer() {
		 * 
		 * private static final long serialVersionUID = 1L;
		 * 
		 * @Override public String getToolTipText() { return
		 * Annotator.getString(Constants.Strings.FLAG_EDITOR_LABEL_TOOLTIP); } });
		 */
		return panel;
	}

	class EntityListCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value != null) {
				setText(((Entity) value).getLabel());
				setIcon(FontIcon.of(MaterialDesign.MDI_ACCOUNT, new Color(((Entity) value).getColor())));
			}
			return this;
		}
	}

	class EntityTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		String tooltipText;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value != null) {
				setText(((Entity) value).getLabel());
				tooltipText = ((Entity) value).getLabel();
				setIcon(FontIcon.of(MaterialDesign.MDI_ACCOUNT, new Color(((Entity) value).getColor())));
			}
			return this;
		}

		@Override
		public String getToolTipText() {
			return tooltipText;
		}
	}

	class FlagListCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		int maxLength = 20;

		public FlagListCellRenderer() {
		}

		public FlagListCellRenderer(int maxLength) {
			this.maxLength = maxLength;
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value != null) {
				setText(StringUtils.abbreviate(((Flag) value).getLabel(), maxLength));
				setIcon(FontIcon.of(MaterialDesign.valueOf(((Flag) value).getIcon())));
			}
			return this;
		}
	}

	class FlagTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			Flag flag = (Flag) value;
			if (flag != null) {
				setText(flag.getLabel());
				setIcon(FontIcon.of(MaterialDesign.valueOf(flag.getIcon())));
			}
			return this;
		}
	}

	class MyTableHeader extends JTableHeader {

		private static final long serialVersionUID = 1L;
		String[] tooltips;

		MyTableHeader(TableColumnModel columnModel, String[] columnTooltips) {
			super(columnModel);// do everything a normal JTableHeader does
			this.tooltips = columnTooltips;// plus extra data
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			java.awt.Point p = e.getPoint();
			int index = columnModel.getColumnIndexAtX(p.x);
			int realIndex = columnModel.getColumn(index).getModelIndex();
			return this.tooltips[realIndex];
		}
	}

	class FlagEditorWindowListener implements WindowListener {

		@Override
		public void windowOpened(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosing(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosed(WindowEvent e) {

		}

		@Override
		public void windowIconified(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeiconified(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowActivated(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			// TODO Auto-generated method stub

		}

	}

	class MyHeaderRenderer extends DefaultTableHeaderCellRenderer {

		private static final long serialVersionUID = 1L;

	}

	class SymmetricTable extends GridLayoutTable<SymmetricEntityRelation> {

		private static final long serialVersionUID = 1L;

		public SymmetricTable(DocumentModel documentModel) {
			super(SymmetricEntityRelation.class, documentModel);
		}

	}

	class DirectedTable extends GridLayoutTable<DirectedEntityRelation> {

		private static final long serialVersionUID = 1L;

		public DirectedTable(DocumentModel documentModel) {
			super(DirectedEntityRelation.class, documentModel);
		}

	}

	abstract class GridLayoutTable<T extends EntityRelation> extends JPanel implements RelationModelListener

	{

		private static final long serialVersionUID = 1L;

		MutableMap<EntityRelation, Integer> componentMap = Maps.mutable.empty();
		GridLayout layout = new GridLayout(0, 1);
		Class<T> clazz;

		public GridLayoutTable(Class<T> clazz, DocumentModel documentModel) {
			setLayout(layout);
			this.clazz = clazz;
			documentModel.getRelationModel().addRelationModelListener(this);
		}

		protected void addRow(T relation) {
			JPanel rowPanel = new Row(relation);

			layout.setRows(layout.getRows() + 1);
			int idx = -1;
			if (componentMap.containsKey(relation)) {
				idx = componentMap.get(relation);
				remove(idx);
			} else
				idx = getComponentCount();
			add(rowPanel, idx);
			componentMap.put(relation, idx);
		}

		protected void updateRowColor() {
			for (int i = 0; i < getComponentCount(); i++) {
				Component c = getComponent(i);
				if (i % 2 == 0)
					c.setBackground(Color.WHITE);
			}
		}

		@Override
		public void relationEvent(FeatureStructureEvent event) {
			switch (event.getType()) {
			case Update:
			case Add:
				@SuppressWarnings("unchecked")
				T reln = (T) event.getArgument1();
				if (clazz.isAssignableFrom(reln.getClass())) {
					addRow(reln);
				}
				updateRowColor();
				validate();
				repaint();
				break;
			case Init:
				break;
			case Merge:
				break;
			case Move:
				break;
			case Op:
				break;
			case Remove:
				int cIndex = componentMap.get(event.getArgument1());
				remove(cIndex);
				updateRowColor();
				validate();
				repaint();
				break;
			default:
				break;
			}
		}

		class Row extends JPanel {
			private static final long serialVersionUID = 1L;
			T relation;

			public Row(T relation) {
				this.relation = relation;

				@SuppressWarnings("unused")
				DropTargetListener dropTargetListener = new DropTargetListener();
				FlagComboBoxModel boxModel;
				if (relation instanceof SymmetricEntityRelation)
					boxModel = new FlagComboBoxModel(SymmetricEntityRelation.class);
				else
					boxModel = new FlagComboBoxModel(DirectedEntityRelation.class);
				documentModel.getFlagModel().addFlagModelListener(boxModel);

				Dimension combobox_dimension = new Dimension(150, 30);
				JComboBox<Flag> combobox = new JComboBox<Flag>(boxModel);
				combobox.setRenderer(new FlagListCellRenderer(20));
				combobox.setMaximumSize(combobox_dimension);
				combobox.setMinimumSize(combobox_dimension);
				combobox.setPreferredSize(combobox_dimension);
				if (relation.getFlag() != null)
					combobox.setSelectedItem(relation.getFlag());

				JToolBar toolBar = new JToolBar();
				toolBar.setFloatable(false);
				toolBar.add(new DeleteAction(getDocumentWindow(), relation));
				add(combobox);
				add(toolBar);
				if (relation instanceof SymmetricEntityRelation)
					add(addFSArray(relation, Util.toList(((SymmetricEntityRelation) relation).getEntities())));
				else if (relation instanceof DirectedEntityRelation) {
					DirectedEntityRelation der = (DirectedEntityRelation) relation;
					if (der.getSource() != null && der.getTarget() != null)
						add(addFSArray(relation, Lists.immutable.of(der.getSource(), der.getTarget())));
					else
						add(addFSArray(relation, Lists.immutable.empty()));
				}

				setBorder(BorderFactory.createLineBorder(Color.gray));
				pack();
			}

			protected Component addFSArray(T relation, Iterable<Entity> arr) {
				JPanel panel = new JPanel();
				boolean empty = true;
				panel.setBorder(BorderFactory.createTitledBorder("Relation members"));
				for (Entity entity : arr) {

					empty = false;
					EntityPanel el = new EntityPanel(documentModel, entity);
					el.setShowText(false);
					el.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseReleased(MouseEvent e) {
							if (SwingUtilities.isRightMouseButton(e)) {
								JPopupMenu menu = new JPopupMenu();
								menu.add(new AbstractAction("delete") {

									private static final long serialVersionUID = 1L;

									@Override
									public void actionPerformed(ActionEvent e) {
										documentModel.edit(new UpdateEntityRelation(relation,
												EntityRelationProperty.REMOVE_ENTITY, entity));
									}

								});
								menu.show(el, e.getX(), e.getY());
							}
						}

					});
					panel.add(el);
				}
				if (empty) {
					panel.add(new JLabel("drop zone"));
				}
				return panel;
			}

			class DropTargetListener extends DropTargetAdapter {

				public DropTargetListener() {
					@SuppressWarnings("unused")
					DropTarget dropTarget = new DropTarget(Row.this, DnDConstants.ACTION_LINK, this, true, null);
				}

				@SuppressWarnings("unchecked")
				@Override
				public void drop(DropTargetDropEvent event) {
					Transferable transferable = event.getTransferable();
					ImmutableList<CATreeNode> object;
					try {
						object = (ImmutableList<CATreeNode>) transferable
								.getTransferData(NodeListTransferable.dataFlavor);
						documentModel.edit(new UpdateEntityRelation(relation, EntityRelationProperty.ADD_ENTITY,
								object.getFirst().getEntity()));
						event.acceptDrop(TransferHandler.LINK);
					} catch (UnsupportedFlavorException | IOException e) {
						Annotator.logger.catching(e);
					}
				}
			}

		}

	}

	public DocumentWindow getDocumentWindow() {
		return documentWindow;

	}
}