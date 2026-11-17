Library Management System (CS25C09 Java Programming)
Build:  javac -d out $(find src -name "*.java")
Run:    java -cp out com.college.library.ui.LibraryApp data
Tests:  javac -d outtest -cp out test/*.java ; java -cp out:outtest LibraryTests
Timing: java -cp out:outtest PerfCheck
(On Windows use ; instead of : in the classpath.)
