# This file is part of "The Java Telnet Application".
#
# (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
#
# The software is licensed under the terms and conditions in the
# license agreement included in the software distribution package.
#
# You should have received a copy of the license along with this
# software; see the file license.txt. If not, navigate to the 
# URL http://javatelnet.org/ and view the "License Agreement".
#

JAVAH	=	javah
TOPDIR	=	../..
SRCDIR	=	../../de
CDIR	=	../src
 
JNI_INCLUDE =	-I/usr/lib/j2sdk1.3/include -I/usr/lib/j2sdk1.3/include/linux

# Linux (glibc 2.1.3)
# libutil contains forkpty()
CC	=	gcc
SOCFLAGS =	-fPIC -c -Wall -I. $(JNI_INCLUDE)
SOLFLAGS =	-shared -lutil

#
# major rules to create files
#
all: 	libjtapty.so

HandlerPTY.h:  $(SRCDIR)/mud/jta/plugin/HandlerPTY.class
	$(JAVAH) -classpath /usr/java/lib/classes.zip:$(TOPDIR) -o $@ de.mud.jta.plugin.HandlerPTY

libjtapty.so: HandlerPTY.lo
	$(CC) $(SOLFLAGS) -o $@ HandlerPTY.lo

HandlerPTY.lo: $(CDIR)/HandlerPTY.c HandlerPTY.h
	$(CC) $(SOCFLAGS) -o $@ $(CDIR)/HandlerPTY.c

clean:
	-find . -name \*~ -print | xargs rm -f
	-rm -f HandlerPTY.h HandlerPTY.lo libjtapty.so

realclean: clean

# EOF
