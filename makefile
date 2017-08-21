## makefile automates the build and deployment for lein projects

APP_NAME=	saclassify

# location of the http://github.com/plandes/clj-zenbuild cloned directory
ZBHOME=		../clj-zenbuild

# clean the generated app assembly file
MLINK ?=	$(HOME)/opt/nlp/model
ADD_CLEAN +=	model $(ASBIN_DIR)

all:		info

include $(ZBHOME)/src/mk/compile.mk
include $(ZBHOME)/src/mk/dist.mk

.PHONY:	prepare-dist
prepare-dist:
	mkdir src/asbin
	echo 'JAVA_OPTS="-Dzensols.model=$(HOME)/opt/nlp/model"' > src/asbin/setupenv

.PHONY:	clean-prepare-dist
clean-prepare-dist:
	rm -rf src/asbin

.PHONY: test
test:
	docker-compose up -d
	ln -s $(MLINK) || true
	lein test
	docker-compose down
