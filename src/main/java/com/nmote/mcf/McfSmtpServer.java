package com.nmote.mcf;

import org.apache.commons.lang3.StringUtils;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class McfSmtpServer extends SMTPServer {

    @Inject
    public McfSmtpServer(MessageHandlerFactory handlerFactory) {
        super(handlerFactory);
    }

    @Inject
    public void setBindAddress(@Named("listen") String address) throws UnknownHostException {
        String[] a = StringUtils.split(StringUtils.trimToNull(address), ':');
        if (a != null && a.length > 0) {
            if (!"*".equals(a[0])) {
                setBindAddress(InetAddress.getByName(a[0]));
            }
            if (a.length > 1) {
                setPort(Integer.parseInt(a[1]));
            }
        }
    }

    @Inject
    @Override
    public void setHostName(@Named("hostName") String hostName) {
        super.setHostName(hostName);
    }

    @Inject
    @Override
    public void setShutdownTimeout(@Named("shutdownTimeout") int shutdownTimeout) {
        super.setShutdownTimeout(shutdownTimeout);
    }

    @Inject
    @Override
    public void setSoftwareName(@Named("softwareName") String value) {
        super.setSoftwareName(value);
    }
}
