.phony: all clean

all:
	javac packet.java Receiver.java Sender.java

clean:
	rm *.class
	rm *.log
	rm output.txt
