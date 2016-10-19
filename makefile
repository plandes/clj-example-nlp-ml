## makefile automates the build and deployment for lein projects

APP_NAME=	saclassify

# location of the http://github.com/plandes/clj-zenbuild cloned directory
ZBHOME=		../clj-zenbuild

# clean the generated app assembly file
ADD_CLEAN+=	$(ASBIN_DIR)

all:		info

include $(ZBHOME)/src/mk/compile.mk
include $(ZBHOME)/src/mk/dist.mk

.PHONEY:
prepare-dist:
	mkdir src/asbin
	echo 'JAVA_OPTS="-Dzensols.model=$(HOME)/opt/nlp/model"' > src/asbin/setupenv

.PHONEY:
clean-prepare-dist:
	rm -rf src/asbin
