package de.unistuttgart.ims.coref.annotator;

import static org.junit.Assert.assertEquals;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.junit.Test;

public class TestUtil {
	@Test
	public void testRemoveFromStringArray() throws UIMAException {
		JCas jcas = JCasFactory.createJCas();
		StringArray arr, newArray;
		arr = new StringArray(jcas, 2);
		arr.set(0, "Hello");
		arr.set(1, "World");
		newArray = Util.removeFrom(jcas, arr, "Hello");

		assertEquals(1, newArray.size());
		assertEquals("World", newArray.get(0));
	}
}