JFLAGS = -g
JC = javac -d out
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	ChatRoomGUI.java \
	Client.java \
	ClientGUI.java \
	Download.java \
	Find.java \
	FolderChooser.java \
	Server.java \
	ServerGUI.java \
	ServerSearchThread.java \
	ShutDownHook.java \
	Upload.java \
	UploadGUI.java \
	User.java \
	Util.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) out/*.class
