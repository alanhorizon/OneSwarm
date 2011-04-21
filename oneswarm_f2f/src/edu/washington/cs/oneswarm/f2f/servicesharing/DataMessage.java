package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;

class DataMessage implements Message {

    private DirectByteBuffer buffer = null;
    private static String ID = "RAW_MESSAGE";
    private final String DESC = "Raw message";

    public DataMessage(DirectByteBuffer _buffer) {
        buffer = _buffer;
    }

    public String getID() {
        return ID;
    }

    public byte[] getIDBytes() {
        return ID.getBytes();
    }

    public String getFeatureID() {
        return (null);
    }

    public int getFeatureSubID() {
        return (0);
    }

    public int getType() {
        return (TYPE_DATA_PAYLOAD);
    }

    public String getDescription() {
        return DESC;
    }

    public byte getVersion() {
        return (1);
    }

    public DirectByteBuffer getPayload() {
        return (buffer);
    }

    public DirectByteBuffer[] getData() {
        return new DirectByteBuffer[] { buffer };
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        throw (new MessageException("not imp"));
    }

    public void destroy() {
        buffer.returnToPool();
    }

    static class RawMessageEncoder implements MessageStreamEncoder {
        @Override
        public RawMessage[] encodeMessage(Message base_message) {
            return new RawMessage[] { new RawMessageImpl(base_message, base_message.getData(),
                    RawMessage.PRIORITY_NORMAL, true, null) };
        }

    }

    static class RawMessageDecoder implements MessageStreamDecoder {
        private static final byte SS = DirectByteBuffer.SS_MSG;

        private static int MAX_PAYLOAD = OSF2FMessage.MAX_PAYLOAD_SIZE;
        DirectByteBuffer payload_buffer;
        private boolean paused = false;

        private ArrayList<Message> messages_last_read = new ArrayList<Message>();

        @Override
        public void resumeDecoding() {
            paused = false;
        }

        @Override
        public Message[] removeDecodedMessages() {
            if (messages_last_read.isEmpty()) {
                return null;
            }
            Message[] msgs = messages_last_read.toArray(new Message[messages_last_read.size()]);
            messages_last_read.clear();
            return msgs;
        }

        @Override
        public int performStreamDecode(Transport transport, int max_bytes) throws IOException {
            int bytes_left = max_bytes;
            while (bytes_left > 0) {
                if (payload_buffer == null) {
                    payload_buffer = DirectByteBufferPool.getBuffer(SS, MAX_PAYLOAD);
                }
                long read = transport.read(new ByteBuffer[] { payload_buffer.getBuffer(SS) }, 0,
                        bytes_left);
                bytes_left -= read;
                // Message is done if:
                // * payload is full
                // * transport has no more data
                if (payload_buffer.remaining(SS) == 0 || read == 0) {
                    if (payload_buffer.position(SS) > 0) {
                        Message msg = new DataMessage(payload_buffer);
                        messages_last_read.add(msg);
                        payload_buffer = null;
                    }
                    // If transport has no more data, break
                    if (read == 0) {
                        break;
                    }
                }
            }
            return max_bytes - bytes_left;
        }

        @Override
        public void pauseDecoding() {
            paused = true;
        }

        @Override
        public int getProtocolBytesDecoded() {
            return 0;
        }

        @Override
        public int getPercentDoneOfCurrentMessage() {
            return (int) (getDataBytesDecoded() * 100.0 / MAX_PAYLOAD);
        }

        @Override
        public int getDataBytesDecoded() {
            if (payload_buffer == null) {
                return 0;
            }
            return payload_buffer.position(SS);
        }

        @Override
        public ByteBuffer destroy() {
            if (payload_buffer != null) {
                payload_buffer.returnToPool();
                payload_buffer = null;
            }

            for (Message msg : messages_last_read) {
                msg.destroy();
            }
            messages_last_read.clear();
            return ByteBuffer.allocate(0);
        }
    }

}