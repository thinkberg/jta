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

JAVA	=	java
JAR	=	jar
JAVAC	=	javac
DEBUG	=	-g -deprecation
JFLAGS	=	-classpath $(CLASSPATH):.
SRCDIR	=	de
PKGNAME	=	jta20
VERSION	=	`java -version 2>&1 | head -1 | \
		  sed 's/^java version //' | sed 's/"//g'`

.SUFFIXES:	.java .class

# compile java files to class
.java.class:
	$(JAVAC) $(DEBUG) $(JFLAGS) $<

#
# major rules to create files
#
all: 	app doc jar

doc:	app
	-mkdir doc
	-rm -r doc/*.html doc/de
	javadoc -d doc -version -author -sourcepath $(CLASSPATH):. \
	  `find de/mud -type d -print | \
	    grep -v CVS | grep -v '^de/mud$$' | sed 's/\//./g'`; \



run:	app
	$(JAVA) $(JFLAGS) de.mud.jta.Main

jar:	app
	@echo Version: $(VERSION)
	-mkdir jar
	$(JAR) cvf jar/$(PKGNAME)-`date +%Y%m%d`-$(VERSION).jar \
	  `find $(SRCDIR) -name *.class` \
	  `find $(SRCDIR) -name defaults.\*`

dist:	clean jar doc revision changes
	-mkdir jar
	(cvs -Q -d $(CVSROOT) export -D now -d $(PKGNAME) telnet && \
	 /bin/cp REVISION CHANGES $(PKGNAME)/ && \
	 /bin/cp -r doc $(PKGNAME)/ && \
	 $(JAR) cvMf jar/$(PKGNAME)-`date +%Y%m%d`-src.jar $(PKGNAME) && \
	 rm -rf $(PKGNAME))

changes:
	rcs2log > CHANGES

revision:
	@find de -name \*.java | xargs cat | grep @version | \
	  awk 'BEGIN{ \
	         printf("%-26.26s %2.2s.%-2.2s (%10s) %s\n", \
		        "File","R","M", "Date", "Last Accessed by:"); \
	       } \
	       { \
	         split($$5,rev,"."); \
	         printf("%-26.26s %2.2s.%-2.2s (%10s) %s\n", \
		   $$4,rev[1],rev[2],$$6,$$8); \
	       }' \
	  > REVISION

# 
# application dependencies
#
app:
	@find $(SRCDIR) -name \*.java | sed 's/java$$/class/' | xargs make

clean:
	-find . -name *.class -print | xargs rm > /dev/null 2>&1
	-find . -name *~ -print | xargs rm > /dev/null 2>&1
