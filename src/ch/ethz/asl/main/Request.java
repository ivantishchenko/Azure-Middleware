package ch.ethz.asl.main;

import java.nio.channels.SocketChannel;



public class Request {

    public enum RequestType {
        SET, GET, MULTI_GET
    }

    // type of request
    private RequestType type;
    // Socket channel to the client who sent a reques
    private SocketChannel requestChan;
    // message from the client
    private String requestMessage;
    // raw material
    private byte[] rawMessage;

    public Request() {

    }

    public Request(SocketChannel chan, byte[] msg) {
        requestChan = chan;
        rawMessage = msg;
    }

    public Request(SocketChannel chan, String msg) {
        requestChan = chan;
        requestMessage = msg;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public SocketChannel getRequestChan() {
        return requestChan;
    }

    public byte[] getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(byte[] rawMessage) {
        this.rawMessage = rawMessage;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

}
