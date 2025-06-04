This program was developed by Mohan Chowdhury, Andy David Zaruma, and Kaiwen Liu. Each member contributed roughly a third of the work and each helped with testing, debugging and research.
To run the program, use your command line/terminal to go to the folder containing "DB-V1.0-0.0.1-SNAPSHOT-jar-with-dependencies.jar", and enter the following command, replacing "0" with your desired offset:
java -jar DB-V1.0-0.0.1-SNAPSHOT-jar-with-dependencies.jar 0
Navigate the menus as instructed in the program. 1 is for Problem Specifiers, 2 is for SQL Contributors, 3 is for Information Seekers. Enter 4 to exit the program.


Contributions:
Mohan Chowdhury - Information Seeker software requirements, finalization of program
Kaiwen Liu - Problem Specifier software requirements, testing
Andy David Zaruma - SQL Developer software requirements

Bugs/Limitations:
The program is rudimentary, and while it is meant to validate SQL statements, it does not validate if the SQL returns desired values.
Additionally, while SQL Contributors can include parameter entries into their SQL contributions, attempts to input parameters by Information Seekers seem to not be handled correctly and not return the correct values. As such, the program is best used with exact statements.