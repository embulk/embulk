Undocumented Features
======================

These features are implemented, but undocumented in the official document. They:

* are experimental,
* are fragile,
* are unstable,
* are unsupported,
* are only for developers,
* may break something,
* may be changed in an incompatible manner, and/or
* may be removed later.

`prints_column_names` in the standard `stdout` plugin
------------------------------------------------------

It prints the column names in the beginning before record data since v0.9.6. But the implementation is not tested well, and it at least has problems in multi-threaded execution. The behavior may change without notifications in the future.
