Checkstyle for the Embulk project
==================================

* google_check.xml: Downloaded from: https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml
     * Commit: 60f41e3c16e6c94b0bf8c2e5e4b4accf4ad394ab
* checkstyle.xml: Customized from google_check.xml.
    * To enable suppressions through suppressions.xml.
    * To enable suppressions with @SuppressWarnings.
    * To accept package names with underscores.
    * To indent with 4-column spaces.
    * To limit columns to 180 characters, which will be shortened later.
    * To reject unused imports.
