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
JFLAGS	=	-classpath $(CLASSPATH):jar/cryptix.jar:.
SRCDIR	=	de
PKGNAME	=	jta20
VERSION	=	`java -version 2>&1 | head -1 | \
		 sed 's/^java version //' | sed 's/"//g'`
DATE	=	`date +%Y%m%d-%H%M`

.SUFFIXES:	.java .class

# compile java files to class
.java.class:
	$(JAVAC) $(DEBUG) $(JFLAGS) $<

#
# major rules to create files
#
all: 	app doc jar

run:	app
	$(JAVA) $(JFLAGS) de.mud.jta.Main

doc:	app
	@echo Creating source documentation ...
	@if [ ! -d doc ]; then mkdir doc; fi
	@-rm -r doc/source/*.html doc/source/de
	@javadoc -d doc/source -version -author \
	  -sourcepath $(CLASSPATH):. \
	  `find de/mud -type d -print | \
	    grep -v CVS | grep -v '^de/mud$$' | sed 's/\//./g'`; > /dev/null
	@echo Source documentation done.

jar:	app 
	@echo Creating binary archive ...
	@if [ ! -d jar ]; then mkdir jar; fi
	@touch "Created-$(DATE)"
	@$(JAR) cvf jar/$(PKGNAME).jar \
	  "Created-$(DATE)" README \
	  license/COPYING license/COPYING.LIB \
	  de/mud/ssh/license.txt \
	  `find $(SRCDIR) -name *.class` \
	  `find $(SRCDIR) -name defaults.\*` > /dev/null
	@rm -f Created-*
	@echo Created jar/$(PKGNAME).jar

dist:	jar doc revision changes
	@echo Creating distribution package ...
	@if [ "$(CVSROOT)" = "" ]; then echo "Missing CVSROOT!"; exit -1; fi
	@(cvs -Q -d $(CVSROOT) export -D now -d $(PKGNAME) jta && \
	  cp REVISION CHANGES $(PKGNAME)/ && \
	  cp -r doc/source $(PKGNAME)/doc/ && \
	  touch "$(PKGNAME)/Created-$(DATE)" && \
	  sed "s/<!-- DATE -->/$(DATE)/g" < $(PKGNAME)/index.html \
	                                  > $(PKGNAME)/index.new && \
	  mv $(PKGNAME)/index.new $(PKGNAME)/index.html && \
	  sed "s/<!-- DATE -->/$(DATE)/g" < $(PKGNAME)/html/download.html \
	                                  > $(PKGNAME)/html/download.new && \
	  mv $(PKGNAME)/html/download.new $(PKGNAME)/html/download.html && \
	  $(JAR) cvMf jar/$(PKGNAME)-src.jar $(PKGNAME)) > /dev/null 
	 @rm -rf $(PKGNAME) 
	 @echo Created jar/$(PKGNAME)-src.jar

changes:
	@rcs2log > CHANGES
	@echo Created CHANGES.

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
	  @echo Created REVISION.

# 
# application dependencies
#
app:
	@find $(SRCDIR) -name \*.java | sed 's/java$$/class/' | xargs make
	@echo Done.

clean:
	-find . -name *.class -print | xargs rm > /dev/null 2>&1
	-find . -name *~ -print | xargs rm > /dev/null 2>&1
