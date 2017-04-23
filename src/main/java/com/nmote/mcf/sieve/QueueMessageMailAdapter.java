package com.nmote.mcf.sieve;

import com.nmote.mcf.QueueMessage;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressParser;
import org.apache.james.mime4j.field.address.LenientAddressParser;
import org.apache.james.mime4j.field.address.ParseException;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.InternetAddressException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.AddressImpl;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.SieveMailException;
import org.apache.jsieve.mail.optional.EnvelopeAccessors;

import java.io.IOException;
import java.util.*;

public class QueueMessageMailAdapter implements MailAdapter, EnvelopeAccessors {

    private static final Address[] NO_ADDRESSES = new Address[0];

    private static final List<String> NO_VALUES = Collections.emptyList();
    private static final List<String> ENVELOPE_NAMES = Arrays.asList(new String[]{"From", "To"});

    public QueueMessageMailAdapter(QueueMessage message) throws IOException {
        this.msg = message;
        this.actions = new ArrayList<Action>();
    }

    public void addAction(Action action) {
        this.actions.add(action);
    }

    public void executeActions() throws SieveException {
    }

    public List<Action> getActions() {
        return actions;
    }

    public String getContentType() throws SieveMailException {
        String contentType = msg.getHeader().get("content-type");
        if (contentType == null) {
            contentType = "text/plain";
        }
        return contentType;
    }

    public List<String> getEnvelope(String name) throws SieveMailException {
        if (name.equalsIgnoreCase("from")) {
            return Collections.singletonList(msg.getFrom());
        } else if (name.equalsIgnoreCase("to")) {
            return msg.getRecipients();
        } else {
            return NO_VALUES;
        }
    }

    public List<String> getEnvelopeNames() throws SieveMailException {
        return ENVELOPE_NAMES;
    }

    public List<String> getHeader(String name) throws SieveMailException {
        List<String> r = msg.getHeader().getAll(name, true);
        if (r == null) {
            r = NO_VALUES;
        }
        return r;
    }

    public List<String> getHeaderNames() throws SieveMailException {
        Set<String> r = msg.getHeader().getFieldNames();
        return r == null ? NO_VALUES : new ArrayList<String>(r);
    }

    public List<String> getMatchingEnvelope(String name) throws SieveMailException {
        return getEnvelope(name);
    }

    public List<String> getMatchingHeader(String name) throws SieveMailException {
        List<String> r = msg.getHeader().getAll(name, false);
        if (r == null) {
            r = NO_VALUES;
        }
        return r;
    }

    public int getSize() throws SieveMailException {
        return (int) msg.getSize();
    }

    public boolean isInBodyText(String phraseCaseInsensitive) throws SieveMailException {
        return false;
    }

    public Address[] parseAddresses(String name) throws SieveMailException, InternetAddressException {
        AddressParser builder = LenientAddressParser.DEFAULT;
        List<String> adr = getMatchingHeader(name);
        if (adr == null) {
            return NO_ADDRESSES;
        }
        int len = adr.size();
        Address[] result = new Address[len];
        for (int i = 0; i < len; ++i) {
            String address = adr.get(i);
            try {
                Mailbox mailbox = builder.parseMailbox(address);
                result[i] = new AddressImpl(mailbox.getLocalPart(), mailbox.getDomain());
            } catch (ParseException e) {
                result[i] = new AddressImpl(address, "address.invalid");
                //throw new InternetAddressException("");
            }
        }
        return result;
    }

    public void setContext(SieveContext context) {
        this.ctx = context;
    }

    @Override
    public String toString() {
        ToStringBuilder b = new ToStringBuilder(this);
        b.append("actions", getActions());
        b.append("msg", msg);
        return b.toString();
    }

    protected final QueueMessage msg;
    protected SieveContext ctx;
    private List<Action> actions = new ArrayList<Action>();
}
