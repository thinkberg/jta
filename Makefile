JAVA	=	java
JAR	=	jar
JAVAC	=	javac
DEBUG	=	-g -deprecation
JFLAGS	=	-classpath $(CLASSPATH):.
SRCDIR	=	de

.SUFFIXES:	.java .class

# compile java files to class
.java.class:
	$(JAVAC) $(DEBUG) $(JFLAGS) $<

#
# major rules to create files
#
all: 	app doc jar
	
doc:	app
	javadoc -d doc -version -author \
	  `find de/mud -type d -print | \
	    grep -v CVS | grep -v '^de/mud$$' | sed 's/\//./g'`

run:	app
	$(JAVA) $(JFLAGS) de.mud.jta.Main

jar:	app
	$(JAR) cvf jar/jta.jar `find $(SRCDIR) -name *.class`

# 
# application dependencies
#
app:
	@find $(SRCDIR) -name \*.java | sed 's/java$$/class/' | xargs make

clean:
	-find . -name *.class -print | xargs rm > /dev/null 2>&1
	-find . -name *~ -print | xargs rm > /dev/null 2>&1
