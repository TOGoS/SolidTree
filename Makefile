all: SolidTree.jar
clean:
	rm -rf bin SolidTree.jar

.PHONY: .FORCE

src: .FORCE
	touch "$@"

../HDRUtil/src:
	touch "$@"

bin: src ../HDRUtil/src
	mkdir -p bin
	find src ../HDRUtil/src -name '*.java' >.java-src.lst
	javac -d bin @.java-src.lst

SolidTree.jar: bin
	rm -f "$@"
	jar -c -C bin . >"$@"
