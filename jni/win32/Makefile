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

#### This makefile is incomplete! Someone please finish it.

JAVAH	=	/q/opt/jdk1.2.2/bin/javah
TOPDIR	=	../..
SRCDIR	=	../../de
CDIR	=	../src
 
JNI_INCLUDE =	-Ic:\java\include -Ic:\java\include\win32

CC	=	gcc
SOCFLAGS =	-fPIC -c -Wall -I. $(JNI_INCLUDE)
SOLFLAGS =	-shared -lutil

all:  jtapty.dll

#
# On Win32, the following command builds a dynamic link library hello.dll using Microsoft Visual C++ 4.0: 
#
#    cl -Ic:\java\include -Ic:\java\include\win32
#                    -LD HelloWorldImp.c -Fehello.dll
#
# On Linux libutil contains forkpty()

clean:

realclean: clean

# EOF
