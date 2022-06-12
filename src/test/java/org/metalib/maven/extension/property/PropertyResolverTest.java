package org.metalib.maven.extension.property;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

public class PropertyResolverTest extends TestCase {

    static final String EMPTY = "empty";
    static final String EMPTY0 = "empty0";
    static final String EMPTY_VAL = "";
    static final String NAME = "name";
    static final String NAME0 = "name0";
    static final String VALUE = "value";

    static final PropertyResolver resolver = new PropertyResolver();

    @Test
    public void test6() {
        final var input = new HashMap<String,String>();
        input.put("n1","8888");
        input.put("n2","9999");
        input.put("n1 and n2","${n1} and ${n2}.");
        input.put("n1:n2","n1:n2 -> ${n1}:${n2}");
        input.put("n1n2","n1n2 -> ${n1}${n2}");
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertEquals("8888 and 9999.", r0.get("n1 and n2"));
        assertEquals("n1:n2 -> 8888:9999", r0.get("n1:n2"));
        assertEquals("n1n2 -> 88889999", r0.get("n1n2"));
    }

    @Test
    public void test5() {
        final var input = new HashMap<String,String>();
        input.put("n1","8888");
        input.put("n2","9999");
        input.put("n1 and n2","${n1} and ${n2} and the end.");
        input.put("n1:n2","n1:n2 -> ${n1}:${n2}");
        input.put("n1n2","n1n2 -> ${n1}${n2}");
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertEquals("8888 and 9999 and the end.", r0.get("n1 and n2"));
        assertEquals("n1:n2 -> 8888:9999", r0.get("n1:n2"));
        assertEquals("n1n2 -> 88889999", r0.get("n1n2"));
    }

    @Test
    public void test4() {
        final var input = new HashMap<String,String>();
        input.put("n1","8888");
        input.put("n2","9999");
        input.put("n1 and n2","n1 and n2 -> ${n1} and ${n2} and the end.");
        input.put("n1:n2","n1:n2 -> ${n1}:${n2}");
        input.put("n1n2","n1n2 -> ${n1}${n2}");
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertEquals("n1 and n2 -> 8888 and 9999 and the end.", r0.get("n1 and n2"));
        assertEquals("n1:n2 -> 8888:9999", r0.get("n1:n2"));
        assertEquals("n1n2 -> 88889999", r0.get("n1n2"));
    }

    @Test
    public void test3() {
        final var input = new HashMap<String,String>();
        input.put("n1","8888");
        input.put("n2","9999");
        input.put("n1 and n2","n1 and n2 -> ${n1} and ${n2}.");
        input.put("n1:n2","n1:n2 -> ${n1}:${n2}");
        input.put("n1n2","n1n2 -> ${n1}${n2}");
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertEquals("n1 and n2 -> 8888 and 9999.", r0.get("n1 and n2"));
        assertEquals("n1:n2 -> 8888:9999", r0.get("n1:n2"));
        assertEquals("n1n2 -> 88889999", r0.get("n1n2"));
    }

    @Test
    public void test2() {
        final var input = new HashMap<String,String>();
        input.put("n1","8888");
        input.put("n2","9999");
        input.put("n1 and n2","n1 and n2 -> ${n1} and ${n2}");
        input.put("n1:n2","n1:n2 -> ${n1}:${n2}");
        input.put("n1n2","n1n2 -> ${n1}${n2}");
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertEquals("n1 and n2 -> 8888 and 9999", r0.get("n1 and n2"));
        assertEquals("n1:n2 -> 8888:9999", r0.get("n1:n2"));
        assertEquals("n1n2 -> 88889999", r0.get("n1n2"));
    }

    @Test
    public void test1() {
        final var input = new HashMap<String,String>();
        input.put("n1","8888");
        input.put("n2","9999");
        input.put("n1:n2","${n1}:${n2}");
        input.put("n1n2","${n1}${n2}");
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertEquals("8888:9999", r0.get("n1:n2"));
        assertEquals("88889999", r0.get("n1n2"));
    }

    @Test
    public void test0() {

        final var input = new HashMap<String,String>();
        final var r0 = resolver.resolve(input);
        assertNotNull(r0);
        assertTrue(r0.isEmpty());

        input.put(EMPTY, EMPTY_VAL);
        final var r1 = resolver.resolve(input);
        assertNotNull(r1);
        assertFalse(r1.isEmpty());
        assertEquals(EMPTY_VAL, r1.get(EMPTY));

        input.put(NAME, VALUE);
        final var r2 = resolver.resolve(input);
        assertNotNull(r2);
        assertFalse(r2.isEmpty());
        assertEquals(VALUE, r2.get(NAME));

        input.put(EMPTY0, "${"+EMPTY+"}");
        final var r3 = resolver.resolve(input);
        assertNotNull(r3);
        assertFalse(r3.isEmpty());
        assertEquals(EMPTY_VAL, r3.get(EMPTY0));

        input.put(NAME0, "${"+NAME+"}");
        final var r4 = resolver.resolve(input);
        assertNotNull(r4);
        assertFalse(r4.isEmpty());
        assertEquals(VALUE, r4.get(NAME0));

        input.put(EMPTY+NAME, "${"+EMPTY+"}${"+NAME+"}");
        final var r5 = resolver.resolve(input);
        assertNotNull(r5);
        assertFalse(r5.isEmpty());
        assertEquals(EMPTY_VAL+VALUE, r5.get(EMPTY+NAME));

        input.put(EMPTY+EMPTY+NAME, "${"+EMPTY+NAME+"}");
        final var r6 = resolver.resolve(input);
        assertNotNull(r6);
        assertFalse(r6.isEmpty());
        assertEquals(EMPTY_VAL+VALUE, r6.get(EMPTY+EMPTY+NAME));

        final var randomName = UUID.randomUUID().toString();
        input.put(NAME+1, "${"+randomName+"}");
        final var r7 = resolver.resolve(input);
        assertNotNull(r7);
        assertFalse(r7.isEmpty());
        assertEquals("${"+randomName+"}", r7.get(NAME+1));

        input.put(NAME+2, "${"+NAME+1+"}");
        final var r8 = resolver.resolve(input);
        assertNotNull(r8);
        assertFalse(r8.isEmpty());
        assertEquals("${"+randomName+"}", r8.get(NAME+2));

        input.put(NAME+3, "${"+NAME);
        final var r9 = resolver.resolve(input);
        assertNotNull(r9);
        assertFalse(r9.isEmpty());
        assertEquals("${"+NAME, r9.get(NAME+3));

        input.put(NAME+4, "${}");
        final var r10 = resolver.resolve(input);
        assertNotNull(r10);
        assertFalse(r10.isEmpty());
        assertEquals("${"+NAME, r10.get(NAME+3));
    }

    @Test(expected = PropertyResolver.PropertyCircularReferenceException.class)
    public void testPropertyCircularReferenceExceptionOnCrossReference() {
        try{
            final var input = new HashMap<String,String>();
            input.put(NAME+0, "${"+NAME+1+"}");
            input.put(NAME+1, "${"+NAME+0+"}");
            final var r0 = resolver.resolve(input);
            fail();
        } catch (PropertyResolver.PropertyCircularReferenceException e) {
            return;
        }
    }

    @Test(expected = PropertyResolver.PropertyCircularReferenceException.class)
    public void testPropertyCircularReferenceExceptionOnSelfReference() {
        try{
            final var input = new HashMap<String,String>();
            input.put(NAME+0, "${"+NAME+0+"}");
            final var r0 = resolver.resolve(input);
            fail();
        } catch (PropertyResolver.PropertyCircularReferenceException e) {
            return;
        }
    }

}