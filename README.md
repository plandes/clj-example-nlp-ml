Example Project for Natural Language Processing and Machine Learning Libraries
==============================================================================

This is a simple and small example of how to use the following libraries:

* [Natural Language Processing](https://github.com/plandes/clj-nlp-parse)
* [Machine Learning](https://github.com/plandes/clj-ml-model)
* [Machine Learning Dataset](https://github.com/plandes/clj-ml-dataset)

This project extends Carin Meier's
[speech act classifier](http://gigasquidsoftware.com/blog/2015/10/20/speech-act-classification-for-text-with-clojure/).
None of her [code](https://github.com/gigasquid/speech-acts-classifier) was
used, only the data to test and train (found in the `resources` directory).

Note that the library also illustrates how to use the
[action command line interface library](https://github.com/plandes/clj-actioncli)
as you can [build out a CLI version](#command-line).

Documentation
-------------
API (incomplete) [documentation](https://plandes.github.io/clj-example-nlp-ml/codox/index.html).

Usage
-----
This project provides a real working example of a statistical natural language
processing program.  The code itself is given as examples in the libraries it
uses (see top of this README).  To use, clone the repository and build with
lein (see the [command line docs](#command-line)).

### REPL
```clojure
user> (System/setProperty "zensols.model" "path-to-model")
user> (require '[zensols.example.sa-model :as sa])
user> (sa/classify-utterance "when will we get there")
INFO  2016-07-15 18:19:00.957: stanford: parsing: <when will we get there>
INFO  2016-07-15 18:19:00.979: stanford: creating tagger model at .../stanford/pos/english-left3words-distsim.tagger
INFO  2016-07-15 18:19:01.565: stanford: creating ner annotators: ["edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz"]
=> "question"
```

### Command line
1. Install [Leiningen](http://leiningen.org) (this is just a script)
2. Install [GNU make](https://www.gnu.org/software/make/)
3. Install [Git](https://git-scm.com)
4. Follow the directions in [build section](#building)
5. Create the distribution on the desktop: `make dist`
6. Start the Elasticsearch server using the
   [ML Dataset project](https://github.com/plandes/clj-ml-dataset)
7. Load the corpus into Elasticsearch: `cd ~/Desktop/nlp-ml-example/bin ; ./saclassify load-corpus`
8. Run: `./saclassify classify -u 'when will we get there'`

Building
--------
All [leiningen](http://leiningen.org) tasks will work in this project.  For
additional build functionality (git tag convenience utility functionality)
clone the [Clojure build repo](https://github.com/plandes/clj-zenbuild) in the
same (parent of this file) directory as this project:
```bash
   cd ..
   git clone https://github.com/plandes/clj-zenbuild
```

License
--------
Copyright © 2016 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
