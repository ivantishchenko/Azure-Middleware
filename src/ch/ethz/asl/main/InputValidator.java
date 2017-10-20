package ch.ethz.asl.main;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;

public class InputValidator {

    // TODO: Write a test to see how the method performs on crazy values
    // returns an error if there is one, otherwise just normal reply
    public static ByteBuffer getSingleResponse(Set<ByteBuffer> serverResponses) {

        for (ByteBuffer buf: serverResponses) {
            String resp = new String(buf.array());
            if (resp.toLowerCase().contains("error")) {
                //System.out.println("You have an error in responses: " + resp);
                return buf;
            }
        }

        // if no errors but distinct responses send the first one
        ByteBuffer res = serverResponses.iterator().next();
        //System.out.println(new String(res.array()));
        return res;
    }


    // TODO: Generate more sophisticated input validation
    // Classify request as GET SET
    public static void classifyRequest(Request req) {
        String[] msg = new String(req.getRawMessage()).split(" ");
        String operation = msg[0].toLowerCase();

        if (operation.equals("set")) {
            req.setType(Request.RequestType.SET);
        } else if (operation.equals("get") || operation.equals("gets")) {

            if (msg.length > 2) req.setType(Request.RequestType.MULTI_GET);
            else req.setType(Request.RequestType.GET);

        }
    }

}
