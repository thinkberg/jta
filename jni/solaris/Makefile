# This file is part of "The Java Telnet Application".
#
# (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Righs Reserved.
#
# The software is licensed under the terms and conditions in the
# license agreement included in the software distribution package.
#
# You should have received a copy of the license along with this
# software; see the file license.txt. If not, navigate to the 
# URL http://javatelnet.org/ and view the "License Agreement".
#

### This makefile is incomplete. Please adjust it to your environment.

JAVAH	=	/usr/java/bin/javah
TOPDIR	=	../..
SRCDIR	=	../../de
CDIR	=	../src
 
JNI_INCLUDE =	-I/usr/java/include -I/usr/java/include/linux

CC	=	/opt/SUNWspro/SC5.0/bin/cc
SOCFLAGS =	-c -I. $(JNI_INCLUDE) -G
SOLFLAGS =	-G

#
# major rules to create files
#
all: 	libjtapty.so

HandlerPTY.h:  $(SRCDIR)/mud/jta/plugin/HandlerPTY.class
	$(JAVAH) -force -classpath $(TOPDIR) -o $@ de.mud.jta.plugin.HandlerPTY

libjtapty.so: HandlerPTY.lo
	$(CC) $(SOLFLAGS) -o $@ HandlerPTY.lo

HandlerPTY.lo: $(CDIR)/HandlerPTY.c HandlerPTY.h
	$(CC) $(SOCFLAGS) -o $@ $(CDIR)/HandlerPTY.c

clean:
	-find . -name \*~ -print | xargs rm -f
	-rm -f HandlerPTY.h libjtapty.so

realclean: clean

# EOF
