all: build

build:
	chmod +x clean.sh
	chmod +x timeout.sh
	chmod +x pipair 
	javac pipair_java.java

clean: 
	rm -rf *class *o 
