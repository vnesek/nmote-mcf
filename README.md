nmote-mcf
=========

SMTP proxy and local delivery agent. Support receiving messages over SMTP, processing
with pluggable filters (ClamAV, SpamAssassin, etc) filtering with sieve rules and
delivering to local mailbox, maildir or SMTP servers.


linux/unix installation
=======================

/opt/mcf					installation directory (JAR file)
/etc/mcf/mcf.properties		configuration
/etc/mcf/logback.xml		logging configuration
/etc/init.d/mcf 			start-stop-status script
/var/run/mcf.pid			file holding a PID of mcf process
/var/spool/mcf				queue directories
/var/log/mcf				logging files

Process, queue directory and logging files are owned by mcfmail user, mcfmail group.

/etc/init.d/mcf script launches jsvc (jakarta commons daemon) process. It should
be run as a root, jsvc will change a child process to mcfmail user.

Default configuration listens on port 8025. To accept SMTP connections on standard
port 25, run following iptables command:

`iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 25 -j REDIRECT --to-port 8025`

