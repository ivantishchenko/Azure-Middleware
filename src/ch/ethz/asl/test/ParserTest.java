package ch.ethz.asl.test;

import ch.ethz.asl.main.Parser;
import ch.ethz.asl.main.Request;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class ParserTest {

    Request r;
    Set<ByteBuffer> serverResponses;

    @BeforeEach
    public void beforeAll() {
        r = new Request();
        serverResponses = new HashSet<>();
    }

    @Test
    public void  testSplitParts() {
        int[] res = Parser.splitParts(27, 5);
        int[] out = {6,6,5,5,5};
        assertArrayEquals(out, res);
    }

    @Test
    public void testGetSingleResponse() {

        ByteBuffer error = ByteBuffer.wrap(new String("ERROR\\r\\n").getBytes());
        ByteBuffer stored = ByteBuffer.wrap(new String("STORED\\r\\n").getBytes());

        serverResponses.add(stored);
        serverResponses.add(stored);
        serverResponses.add(error);

        assertEquals(error, Parser.getSingleResponse(serverResponses));

    }

    @Test
    public void testRequestClassify() {
        // Test SET
        byte[] msg = new String("set mykey 0 60 5\\r\\nhello\\r").getBytes();
        r.setRawMessage(msg);

        Parser.classifyRequest(r);
        assertEquals(Request.RequestType.SET, r.getType());

        // Test GET
        msg = new String("get mykey\\r").getBytes();
        r.setRawMessage(msg);

        Parser.classifyRequest(r);
        assertEquals(Request.RequestType.GET, r.getType());

        // test MULTI GET
        msg = new String("get mykey yourkey\\r").getBytes();
        r.setRawMessage(msg);

        Parser.classifyRequest(r);
        assertEquals(Request.RequestType.MULTI_GET, r.getType());

    }

}
