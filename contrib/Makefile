# Makefile for contributed programs

JAVA	=	java
JAR	=	jar
JAVAC	=	javac
DEBUG	=	-g -deprecation
JFLAGS	=	-classpath $(CLASSPATH):jar/cryptix.jar:.:../

.SUFFIXES:	.java .class

# compile java files to class
.java.class:
	$(JAVAC) $(DEBUG) $(JFLAGS) $<

all: hc/net/TelnetClient.class bsx

bsx:
	@echo "Creating BSX.jar ..."
	@find de -name \*.java -print | sed 's/java$$/class/' | xargs make
	@$(JAR) cvf ../jar/BSX.jar `find de -name \*.class` > /dev/null
	@echo "Done."
