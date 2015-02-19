.PHONY: clean default

default: HDRUtil.jar

HDRUtil.jar: $(shell find src)
	rm -rf bin
	find src -name *.java >.src.lst
	mkdir -p bin
	javac -source 1.6 -target 1.6 -sourcepath src -d bin @.src.lst
	jar -ce togos.hdrutil.AdjusterUI -C bin . >HDRUtil.jar

clean:
	rm -rf bin HDRUtil.jar .src.lst
