JAVA	=	java
JAR	=	jar
JAVAC	=	javac
DEBUG	=	-g -deprecation
JFLAGS	=	-classpath $(CLASSPATH):.
SRCDIR	=	de
PKGNAME	=	telnet-v20

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
	javadoc -d doc -version -author \
	  `find de/mud -type d -print | \
	    grep -v CVS | grep -v '^de/mud$$' | sed 's/\//./g'`

run:	app
	$(JAVA) $(JFLAGS) de.mud.jta.Main

jar:	app
	-mkdir jar
	$(JAR) cvf jar/jta.jar `find $(SRCDIR) -name *.class` \
	  `find $(SRCDIR) -name defaults.\*`

dist:	clean doc revision changes
	-mkdir jar
	(cvs -Q -d $(CVSROOT) export -D now -d $(PKGNAME) telnet && \
	 /bin/cp REVISION CHANGES $(PKGNAME)/ && \
	 /bin/cp -r doc $(PKGNAME)/ && \
	 $(JAR) cvMf jar/jta-20-source.jar $(PKGNAME) && \
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
