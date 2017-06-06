Release 0.8.23
==================================

General Changes
------------------

* PageReader#get{String,Timestamp,Json} calls #isNull to return null. [#654]
* Ignore JsonParseException in JSON guess if JsonGuessPlugin could parse at least one JSON. [#659]
* Ignore JsonParseException in JSON preview if at least one JSON is already parsed. [#661]


Release Date
------------------
2017-06-06
