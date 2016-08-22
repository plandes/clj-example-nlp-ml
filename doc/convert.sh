#!/bin/sh

FILE=speech-act-J48-train-test-series

~/Applications/Graphics/Inkscape.app/Contents/Resources/bin/inkscape \
  --without-gui \
  --file=$FILE.pdf \
  --export-plain-svg=$FILE.svg
