/**
 * Packet class wraps the byte array to send and adds an acknowledgment field.
 */
public class Packet {
    public Packet(byte[] newBytes, Boolean newAck){
        bytes = newBytes;
        acknowledged = newAck;
    }
    public Packet(){}

    public Boolean acknowledged = false;
    public byte[] bytes;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
}
