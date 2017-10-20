package ch.ethz.asl.main;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;

public class InputValidator {

    // TODO: Write a test to see how the method performs on crazy values
    // returns an error if there is one, otherwise just normal reply
    public static ByteBuffer getSingleResponse(Set<ByteBuffer> serverResponses) {
        Iterator<ByteBuffer> iter = serverResponses.iterator();

        // first element
        ByteBuffer out = iter.next();

        if ( serverResponses.size() == 1 ) {
            return out;
        }

        for (ByteBuffer buf: serverResponses) {
            String resp = new String(out.array());
            if (resp.toLowerCase().contains("error")) {
                System.out.println("You have an error in responses: " + resp);
            }
            return buf;
        }

        // if no errors but distinct responses send the last one
        return out;
    }


    // TODO: Generate more sophisticated input validation
    // Classify request as GET SET
    public static void classifyRequest(Request req) {
        String operation = new String(req.getRawMessage()).split(" ")[0].toLowerCase();

        if (operation.equals("set")) {
            req.setType(Request.RequestType.SET);
        } else if (operation.equals("get") || operation.equals("gets")) {
            req.setType(Request.RequestType.GET);
        }
    }

}
