package de.unistuttgart.ims.coref.annotator.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.prefs.Preferences;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unistuttgart.ims.coref.annotator.Constants;
import de.unistuttgart.ims.coref.annotator.Span;
import de.unistuttgart.ims.coref.annotator.api.DetachedMentionPart;
import de.unistuttgart.ims.coref.annotator.api.Entity;
import de.unistuttgart.ims.coref.annotator.api.Mention;

public class TestCoreferenceModel {

	CoreferenceModel model;
	JCas jcas;
	static Preferences preferences;

	@BeforeClass
	public static void setUpClass() {
		preferences = Preferences.systemRoot();
		preferences.putBoolean(Constants.CFG_FULL_TOKENS, false);
	}

	@Before
	public void setUp() throws UIMAException {
		jcas = JCasFactory.createText("the dog barks.");
		model = new CoreferenceModel(new DocumentModel(jcas), preferences);
	}

	@Test
	public void testEditAddMentionsToNewEntity() {
		model.edit(new Op.AddMentionsToNewEntity(new Span(0, 2)));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertFalse(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertTrue(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());

		Mention m = JCasUtil.selectByIndex(jcas, Mention.class, 0);
		assertNotNull(m.getEntity());
		assertNotNull(m.getEntity().getLabel());
		assertNotNull(m.getEntity().getColor());
		assertNotNull(m.getEntity().getFlags());

		model.undo();

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertTrue(model.getMentions(0).isEmpty());
		assertTrue(model.getMentions(1).isEmpty());
		assertTrue(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
	}

	@Test
	public void testEditAddMentionsToExistingEntity() {
		Entity e = new Entity(jcas);
		e.setLabel("Test");
		e.setColor(0);
		e.setFlags(new StringArray(jcas, 0));
		e.addToIndexes();

		model.edit(new Op.AddMentionsToEntity(e, new Span(1, 3)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());

		Mention m = JCasUtil.selectByIndex(jcas, Mention.class, 0);
		assertEquals(e, m.getEntity());

		model.undo();

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertTrue(model.getMentions(1).isEmpty());
		assertTrue(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
	}

	@Test
	public void testEditAttachPart() {
		Mention m = model.createMention(1, 3);

		model.edit(new Op.AttachPart(m, new Span(4, 5)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertFalse(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());

		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertTrue(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

	}

	@Test
	public void testEditRemoveMention() {
		Mention m = model.createMention(1, 3);

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());

		model.edit(new Op.RemoveMention(m));

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertTrue(model.getMentions(1).isEmpty());
		assertTrue(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());

	}

	@Test
	public void testEditRemoveMention2() {
		Mention m = model.createMention(1, 3);
		DetachedMentionPart dmp = model.createDetachedMentionPart(4, 6);
		m.setDiscontinuous(dmp);
		dmp.setMention(m);

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertFalse(model.getMentions(4).isEmpty());
		assertFalse(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		model.edit(new Op.RemoveMention(m));

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertTrue(model.getMentions(1).isEmpty());
		assertTrue(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertTrue(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertFalse(model.getMentions(4).isEmpty());
		assertFalse(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());
	}

	@Test
	public void testEditMergeEntities() {
		model.edit(new Op.AddMentionsToNewEntity(new Span(0, 1), new Span(2, 3)));
		model.edit(new Op.AddMentionsToNewEntity(new Span(4, 5), new Span(6, 7)));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());

		model.edit(new Op.MergeEntities(JCasUtil.select(jcas, Entity.class).toArray(new Entity[2])));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(1, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		Entity e = JCasUtil.selectSingle(jcas, Entity.class);
		for (Mention m : JCasUtil.select(jcas, Mention.class))
			assertEquals(e, m.getEntity());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());

		MutableSetMultimap<Entity, Mention> map = Sets.mutable.withAll(JCasUtil.select(jcas, Mention.class))
				.groupBy(m -> m.getEntity());
		assertEquals(2, map.keySet().size());
	}

	@Test
	public void testEditMoveEntities() {
		model.edit(new Op.AddMentionsToNewEntity(new Span(0, 1), new Span(2, 3)));
		model.edit(new Op.AddMentionsToNewEntity(new Span(4, 5), new Span(6, 7)));
		MutableList<Entity> entities = Lists.mutable.withAll(JCasUtil.select(jcas, Entity.class));

		Mention m = ((Mention) model.getMentions(0).iterator().next());
		assertNotEquals(entities.get(1), m.getEntity());
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(2, model.entityMentionMap.get(entities.get(1)).size());
		assertEquals(2, model.entityMentionMap.get(entities.get(0)).size());

		model.edit(new Op.MoveMentionsToEntity(entities.get(1), m));

		assertEquals(entities.get(1), m.getEntity());
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(3, model.entityMentionMap.get(entities.get(1)).size());
		assertEquals(1, model.entityMentionMap.get(entities.get(0)).size());

		model.undo();

		assertNotEquals(entities.get(1), m.getEntity());
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(2, model.entityMentionMap.get(entities.get(1)).size());
		assertEquals(2, model.entityMentionMap.get(entities.get(0)).size());

	}

	@Test
	public void testSequence1() {
		Entity e = model.createEntity("test");
		Mention m = model.createMention(1, 3);
		m.setEntity(e);

		model.edit(new Op.AttachPart(m, new Span(4, 5)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertFalse(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		model.edit(new Op.RemoveMention(m));

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertTrue(model.getMentions(1).isEmpty());
		assertTrue(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertTrue(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertFalse(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(model.getMentions(0).isEmpty());
		assertFalse(model.getMentions(1).isEmpty());
		assertFalse(model.getMentions(2).isEmpty());
		assertTrue(model.getMentions(3).isEmpty());
		assertTrue(model.getMentions(4).isEmpty());
		assertTrue(model.getMentions(5).isEmpty());
		assertTrue(model.getMentions(6).isEmpty());

		assertEquals(0, model.getHistory().size());
	}
}