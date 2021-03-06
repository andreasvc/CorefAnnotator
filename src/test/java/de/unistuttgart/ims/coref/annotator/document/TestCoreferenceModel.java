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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unistuttgart.ims.coref.annotator.Constants;
import de.unistuttgart.ims.coref.annotator.Span;
import de.unistuttgart.ims.coref.annotator.api.v1.DetachedMentionPart;
import de.unistuttgart.ims.coref.annotator.api.v1.Entity;
import de.unistuttgart.ims.coref.annotator.api.v1.EntityGroup;
import de.unistuttgart.ims.coref.annotator.api.v1.Mention;
import de.unistuttgart.ims.coref.annotator.document.Event.Type;
import de.unistuttgart.ims.coref.annotator.document.op.AddMentionsToEntity;
import de.unistuttgart.ims.coref.annotator.document.op.AddMentionsToNewEntity;
import de.unistuttgart.ims.coref.annotator.document.op.AttachPart;
import de.unistuttgart.ims.coref.annotator.document.op.GroupEntities;
import de.unistuttgart.ims.coref.annotator.document.op.MergeEntities;
import de.unistuttgart.ims.coref.annotator.document.op.MoveMentionsToEntity;
import de.unistuttgart.ims.coref.annotator.document.op.RemoveDuplicateMentionsInEntities;
import de.unistuttgart.ims.coref.annotator.document.op.RemoveEntities;
import de.unistuttgart.ims.coref.annotator.document.op.RemoveEntitiesFromEntityGroup;
import de.unistuttgart.ims.coref.annotator.document.op.RemoveMention;

public class TestCoreferenceModel {

	DocumentModel model;
	CoreferenceModel cmodel;
	JCas jcas;
	static Preferences preferences;
	DummyListener listener;

	@BeforeClass
	public static void setUpClass() {
		preferences = Preferences.systemRoot();
		preferences.putBoolean(Constants.CFG_FULL_TOKENS, false);
	}

	@Before
	public void setUp() throws UIMAException {
		jcas = JCasFactory.createText("the dog barks.");
		model = new DocumentModel(jcas, preferences);
		cmodel = new CoreferenceModel(model);
		model.setCoreferenceModel(cmodel);
		listener = new DummyListener();
		cmodel.addCoreferenceModelListener(listener);
	}

	@Test
	public void testEditAddMentionsToNewEntity() {
		model.edit(new AddMentionsToNewEntity(new Span(0, 2)));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertFalse(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertTrue(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

		Mention m = JCasUtil.selectByIndex(jcas, Mention.class, 0);
		assertNotNull(m.getEntity());
		assertNotNull(m.getEntity().getLabel());
		assertNotNull(m.getEntity().getColor());
		assertNotNull(m.getEntity().getFlags());

		assertEquals(3, listener.events.size());
		assertEquals(Lists.immutable.of(Type.Init, Type.Add, Type.Add), listener.events.collect(ev -> ev.eventType));

		model.undo();

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertTrue(cmodel.getMentions(1).isEmpty());
		assertTrue(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

		assertEquals(Lists.immutable.of(Type.Init, Type.Add, Type.Add, Type.Remove, Type.Remove),
				listener.events.collect(ev -> ev.eventType));

	}

	@Test
	public void testEditAddMentionsToExistingEntity() {
		Entity e = new Entity(jcas);
		e.setLabel("Test");
		e.setColor(0);
		e.setFlags(new StringArray(jcas, 0));
		e.addToIndexes();

		model.edit(new AddMentionsToEntity(e, new Span(1, 3)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

		Mention m = JCasUtil.selectByIndex(jcas, Mention.class, 0);
		assertEquals(e, m.getEntity());

		assertEquals(2, listener.events.size());
		assertEquals(Lists.immutable.of(Event.Type.Init, Event.Type.Add), listener.events.collect(ev -> ev.eventType));

		model.undo();

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertTrue(cmodel.getMentions(1).isEmpty());
		assertTrue(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

		assertEquals(3, listener.events.size());
		assertEquals(Lists.immutable.of(Type.Init, Type.Add, Type.Remove), listener.events.collect(ev -> ev.eventType));

	}

	@Test
	public void testEditAttachPart() {
		Mention m = cmodel.createMention(1, 3);

		model.edit(new AttachPart(m, new Span(4, 5)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertFalse(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		assertEquals(Lists.immutable.of(Type.Init, Type.Add), listener.events.collect(ev -> ev.eventType));

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());

		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertTrue(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		assertEquals(Lists.immutable.of(Type.Init, Type.Add, Type.Remove), listener.events.collect(ev -> ev.eventType));

	}

	@Test
	public void testEditRemoveMention() {
		Mention m = cmodel.createMention(1, 3);

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

		model.edit(new RemoveMention(m));

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertTrue(cmodel.getMentions(1).isEmpty());
		assertTrue(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());

	}

	@Test
	public void testEditRemoveMention2() {
		Mention m = cmodel.createMention(1, 3);
		DetachedMentionPart dmp = cmodel.createDetachedMentionPart(4, 6);
		m.setDiscontinuous(dmp);
		dmp.setMention(m);

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertFalse(cmodel.getMentions(4).isEmpty());
		assertFalse(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		model.edit(new RemoveMention(m));

		assertEquals(Lists.immutable.of(Type.Init, Type.Remove, Type.Remove),
				listener.events.collect(ev -> ev.eventType));

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertTrue(cmodel.getMentions(1).isEmpty());
		assertTrue(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertTrue(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertFalse(cmodel.getMentions(4).isEmpty());
		assertFalse(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());
	}

	@Test
	public void testEditMergeEntities() {
		model.edit(new AddMentionsToNewEntity(new Span(0, 1), new Span(2, 3)));
		model.edit(new AddMentionsToNewEntity(new Span(4, 5), new Span(6, 7)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());

		listener.reset();

		model.edit(new MergeEntities(JCasUtil.select(jcas, Entity.class).toArray(new Entity[2])));

		assertEquals(Lists.immutable.of(Type.Move, Type.Remove), listener.events.collect(ev -> ev.eventType));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(1, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		Entity e = JCasUtil.selectSingle(jcas, Entity.class);
		for (Mention m : JCasUtil.select(jcas, Mention.class))
			assertEquals(e, m.getEntity());

		listener.reset();

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());

		MutableSetMultimap<Entity, Mention> map = Sets.mutable.withAll(JCasUtil.select(jcas, Mention.class))
				.groupBy(m -> m.getEntity());
		assertEquals(2, map.keySet().size());

		assertEquals(Lists.immutable.of(Type.Add, Type.Move), listener.events.collect(ev -> ev.eventType));

	}

	@Test
	public void testEditMoveEntities() {
		model.edit(new AddMentionsToNewEntity(new Span(0, 1), new Span(2, 3)));
		model.edit(new AddMentionsToNewEntity(new Span(4, 5), new Span(6, 7)));
		MutableList<Entity> entities = Lists.mutable.withAll(JCasUtil.select(jcas, Entity.class));

		Mention m = ((Mention) cmodel.getMentions(0).iterator().next());
		assertNotEquals(entities.get(1), m.getEntity());
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(2, cmodel.entityMentionMap.get(entities.get(1)).size());
		assertEquals(2, cmodel.entityMentionMap.get(entities.get(0)).size());

		model.edit(new MoveMentionsToEntity(entities.get(1), m));

		assertEquals(entities.get(1), m.getEntity());
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(3, cmodel.entityMentionMap.get(entities.get(1)).size());
		assertEquals(1, cmodel.entityMentionMap.get(entities.get(0)).size());

		model.undo();

		assertNotEquals(entities.get(1), m.getEntity());
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(4, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(2, cmodel.entityMentionMap.get(entities.get(1)).size());
		assertEquals(2, cmodel.entityMentionMap.get(entities.get(0)).size());

	}

	@Test
	public void testRemovEntityFromGroup() {
		AddMentionsToNewEntity op;
		Entity e1, e2;
		op = new AddMentionsToNewEntity(new Span(1, 2));
		model.edit(op);
		e1 = op.getEntity();
		op = new AddMentionsToNewEntity(new Span(3, 4));
		model.edit(op);
		e2 = op.getEntity();

		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(2, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(2, JCasUtil.select(jcas, Mention.class).size());
		assertFalse(JCasUtil.exists(jcas, EntityGroup.class));

		model.edit(new GroupEntities(e1, e2));

		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(3, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(2, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(JCasUtil.exists(jcas, EntityGroup.class));

		EntityGroup eg = JCasUtil.select(jcas, EntityGroup.class).iterator().next();
		assertEquals(2, eg.getMembers().size());

		model.edit(new RemoveEntitiesFromEntityGroup(eg, e1));

		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(3, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(2, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(JCasUtil.exists(jcas, EntityGroup.class));

		eg = JCasUtil.select(jcas, EntityGroup.class).iterator().next();
		assertEquals(1, eg.getMembers().size());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertEquals(3, JCasUtil.select(jcas, Entity.class).size());
		assertEquals(2, JCasUtil.select(jcas, Mention.class).size());
		assertTrue(JCasUtil.exists(jcas, EntityGroup.class));
		eg = JCasUtil.select(jcas, EntityGroup.class).iterator().next();
		assertEquals(2, eg.getMembers().size());

	}

	@Test
	public void testSequence1() {
		Entity e = cmodel.createEntity("test");
		Mention m = cmodel.createMention(1, 3);
		m.setEntity(e);

		model.edit(new AttachPart(m, new Span(4, 5)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertFalse(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		model.edit(new RemoveMention(m));

		assertFalse(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(0, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertTrue(cmodel.getMentions(1).isEmpty());
		assertTrue(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertTrue(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertFalse(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertFalse(JCasUtil.exists(jcas, DetachedMentionPart.class));
		assertEquals(1, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(0, JCasUtil.select(jcas, DetachedMentionPart.class).size());
		assertTrue(cmodel.getMentions(0).isEmpty());
		assertFalse(cmodel.getMentions(1).isEmpty());
		assertFalse(cmodel.getMentions(2).isEmpty());
		assertTrue(cmodel.getMentions(3).isEmpty());
		assertTrue(cmodel.getMentions(4).isEmpty());
		assertTrue(cmodel.getMentions(5).isEmpty());
		assertTrue(cmodel.getMentions(6).isEmpty());

		assertEquals(0, model.getHistory().size());
	}

	@Test
	public void testRemoveDuplicates() {
		model.edit(new AddMentionsToNewEntity(new Span(1, 3), new Span(1, 3), new Span(2, 4)));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(3, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, Entity.class).size());

		Entity e = JCasUtil.selectSingle(jcas, Entity.class);

		model.edit(new RemoveDuplicateMentionsInEntities(e));

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(2, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, Entity.class).size());

		model.undo();

		assertTrue(JCasUtil.exists(jcas, Mention.class));
		assertTrue(JCasUtil.exists(jcas, Entity.class));
		assertEquals(3, JCasUtil.select(jcas, Mention.class).size());
		assertEquals(1, JCasUtil.select(jcas, Entity.class).size());
	}

	@Test
	public void testRemoveEntityThatIsInGroup() {
		model.edit(new AddMentionsToNewEntity(new Span(0, 1)));
		model.edit(new AddMentionsToNewEntity(new Span(1, 2)));

		ImmutableList<Entity> entities = Lists.immutable.withAll(JCasUtil.select(jcas, Entity.class));
		ImmutableList<Mention> mentions = Lists.immutable.withAll(JCasUtil.select(jcas, Mention.class));

		assertEquals(entities.getFirst(), mentions.getFirst().getEntity());
		assertEquals(entities.getLast(), mentions.getLast().getEntity());

		model.edit(new GroupEntities(entities.get(0), entities.get(1)));

		EntityGroup group = JCasUtil.select(jcas, EntityGroup.class).iterator().next();
		assertEquals(2, group.getMembers().size());
		assertEquals(entities.get(0), group.getMembers(0));
		assertEquals(entities.get(1), group.getMembers(1));

		model.edit(new RemoveMention(mentions.getFirst()));
		model.edit(new RemoveEntities(entities.getFirst()));

		assertEquals(1, group.getMembers().size());
		assertEquals(entities.get(1), group.getMembers(0));

		model.undo();

		assertEquals(2, group.getMembers().size());
		assertEquals(entities.get(1), group.getMembers(0));
		assertEquals(entities.get(0), group.getMembers(1));

	}

	@Test
	public void testSequence2() {
		model.edit(new AddMentionsToNewEntity(new Span(0, 1)));
		model.edit(new AddMentionsToNewEntity(new Span(1, 2)));
		model.edit(new AddMentionsToNewEntity(new Span(2, 3)));

		ImmutableList<Entity> entities = Lists.immutable.withAll(JCasUtil.select(jcas, Entity.class));
		ImmutableList<Mention> mentions = Lists.immutable.withAll(JCasUtil.select(jcas, Mention.class));

		model.edit(new GroupEntities(entities.get(0), entities.get(1)));
		EntityGroup group = JCasUtil.selectSingle(jcas, EntityGroup.class);

		model.edit(new MoveMentionsToEntity(group, mentions.get(2)));

		assertEquals(4, JCasUtil.select(jcas, Entity.class).size());
	}
}
