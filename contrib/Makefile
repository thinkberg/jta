# Makefile for contributed programs

JAVA	=	java
JAR	=	jar
JAVAC	=	javac
DEBUG	=	-g -deprecation
JFLAGS	=	-classpath $(CLASSPATH):../jar/gnu-regexp-1.0.8.jar:.:../

.SUFFIXES:	.java .class

# compile java files to class
.java.class:
	$(JAVAC) $(DEBUG) $(JFLAGS) $<

all: hc/net/TelnetClient.class bsx mud examples

compile:
	@find de -name \*.java -print | sed 's/java$$/class/' | xargs make

bsx: compile
	@echo -n "Creating BSX.jar ... "
	@$(JAR) cvf ../jar/BSX.jar `find de/mud/bsx -name \*.class` \
	  de/mud/jta/plugin/BSX*.class > /dev/null
	@echo "Done."

mud: compile
	@echo -n "Creating MUD.jar ... "
	@$(JAR) cvf ../jar/MUD.jar de/mud/jta/plugin/MUDColorizer*.class \
	  > /dev/null
	@echo "Done."

examples: compile
	@echo -n "Creating examples.jar ... "
	@$(JAR) cvf ../jar/examples.jar \
	  de/mud/jta/plugin/EInput01*.class > /dev/null
	@echo "Done."
