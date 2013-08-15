Fusepool-NER
============

This project is an implementation of the Stanford NER as an enhancer engine.

###What is Fusepool NER?
Fusepool NER is a pure Java based Apache Stanbol enhancement engine, it is based on the Stanford named-entity recognizer. Named-entity recognition or entity extraction is a process when special expressions that refer to unique real-world entities (like persons, locations, etc.) are extracted from the input text. NER works with predefined models from different domains. 

###Stanbol Enhancement Engine
The outcome of this project is an OSGi bundle (enhancement engine) for Apache Stanbol that takes plain, unstructured text as input and gives RDF triples as output that contains the extracted entities and additional information on the enhancement.

The enhancer engine can contains multiple NER instances, each instance is a separate module, and therefore each module has its own configuration page inside Stanbol Configuration Manager. It also means that different NER instances can be part of different enhancer chains.

Currenty Fusepool-SMA has the following instances for English texts:
* Default news-based domain: for extracting persons, location and organizations from English. 
* Mobile domain: technologies, device names and other concepts related to mobile communication.
* Elements domain: Name of chemical elements from English texts.
* Cancer domain: Drug or therapy names, diseases and other concepts related to cancer.
* Diseases domain: Disease names from http://disease-ontology.org/.
