# This file is part of "The Java Telnet Application".
#
# This is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2, or (at your option)
# any later version.
#
# "The Java Telnet Application" is distributed in the hope that it will be
# useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this software; see the file COPYING.  If not, write to the
# Free Software Foundation, Inc., 59 Temple Place - Suite 330,
# Boston, MA 02111-1307, USA.

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
