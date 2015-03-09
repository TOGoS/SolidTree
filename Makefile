# May need to override this on Windows to the full path to git.exe, e.g. by running
# > set git_exe="C:\Program Files (x86)\Git\bin\git.exe"
# before running make.
GIT ?= git

default: SolidTree.jar

.PHONY: \
	.FORCE \
	clean \
	default \
	update-libraries

clean:
	rm -rf bin SolidTree.jar

bin: $(shell find src ext-lib)
	mkdir -p bin
	find src ext-lib/HDRUtil/src -name '*.java' >.java-src.lst
	javac -d bin @.java-src.lst

update-libraries:
	${GIT} subtree pull --prefix=ext-lib/HDRUtil https://github.com/TOGoS/HDRUtil.git

SolidTree.jar: bin
	rm -f "$@"
	jar -c -C bin . >"$@"
