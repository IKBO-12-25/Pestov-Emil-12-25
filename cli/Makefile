OUT_DIR := build/classes/java/main
SRC_FILES := $(shell find src/main/java -name '*.java')

.PHONY: build run test clean

build:
	mkdir -p $(OUT_DIR)
	javac -encoding UTF-8 -d $(OUT_DIR) $(SRC_FILES)

run: build
	java -cp $(OUT_DIR) com.r.Main $(ARGS)

test: build
	bash scripts/test-all.sh

clean:
	rm -rf build logs/*.xml