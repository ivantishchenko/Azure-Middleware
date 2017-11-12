package ch.ethz.asl.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;

public class Parser {
    // Logging
    private final static Logger log = LogManager.getLogger(Parser.class);

    public static boolean isEmptyResponse(byte[] message) {
        String msg = new String(message);
        msg = msg.replaceAll("END\\r\\n","");
        if (msg.length() == 0) return true;
        else return false;
    }

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


    // returns zero if the request is supportet, otherwise -1
    // Classify request as GET SET
    public static int classifyRequest(Request req) {
        String[] msg = new String(req.getRawMessage()).split(" ");
        String operation = msg[0].toLowerCase();

        //System.out.println("WORD COUNT: " + msg.length);

        if (operation.equals("set")) {
            req.setType(Request.RequestType.SET);
            return 0;
        } else if (operation.equals("get") || operation.equals("gets")) {

            if (msg.length > 2) req.setType(Request.RequestType.MULTI_GET);
            else req.setType(Request.RequestType.GET);
            return 0;
        }

        req.setType(Request.RequestType.UNSUPPORTED);
        return -1;
    }

    // assuming the command is valid
    public static ArrayList<byte[]> splitRequest(Request req, int serversNum) {
        // split into words
        String[] msg = new String(req.getRawMessage()).split(" ");
        String command = msg[0];

        // remove carrige return and new line at the end
        msg[msg.length - 1] = msg[msg.length - 1].replaceAll("\\r","");
        msg[msg.length - 1] = msg[msg.length - 1].replaceAll("\\n","");

        // get only the keyss
        String[] keys = Arrays.copyOfRange(msg, 1, msg.length);
        int[] parts = splitParts(keys.length, serversNum);

        ArrayList<String[]> splitGroups = new ArrayList<>(serversNum);
        int begIndex = 0;
        int endIndex = 0;

        // split requests into groups according to parts chunks
        for ( int i = 0; i < parts.length; i++ ) {
            endIndex += parts[i];

            String[] part = Arrays.copyOfRange(keys, begIndex, endIndex);
            splitGroups.add(part);

            begIndex += parts[i];
        }

        ArrayList<byte[]> multiGetsSplit = new ArrayList<>(serversNum);


        for (String[] singleMultiGet: splitGroups) {
            //construct new request

            if (singleMultiGet.length == 0) break;

            StringBuilder sb = new StringBuilder();
            if (command.equals("gets")) sb.append("gets");
            else if (command.equals("get")) sb.append("get");


            for (String key: singleMultiGet) {
                sb.append(" ");
                sb.append(key);
            }

            // close the request with \r
            sb.append("\r\n");
            System.out.println(sb.toString());
            multiGetsSplit.add(sb.toString().getBytes());

        }

        return multiGetsSplit;
    }

    public static byte[] combineSplitResponses(ArrayList<byte[]> serverResponses) {

        StringBuilder sb = new StringBuilder();

        for (int i =0; i < serverResponses.size(); i++ ) {
            String out = new String(serverResponses.get(i));
            out = out.replaceAll("END\\r\\n","");
            sb.append(out);
        }
        sb.append("END\r\n");
        //log.info(sb.toString());
        return sb.toString().getBytes();
    }

    public static int[] splitParts(int input, int numberOfPieces) {
        int quotient = input / numberOfPieces;
        int remainder = input % numberOfPieces;

        int [] parts = new int[numberOfPieces];
        for( int i = 0; i < numberOfPieces; i++ ) {
            parts[i] = i < remainder ? quotient + 1 : quotient;
        }
        return parts;
    }

}
