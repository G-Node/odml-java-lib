For using the logger, the following line must be put in the class defintion:
 static Logger logger = Logger.getLogger(<name of class>.class.getName()); // for logging
 remind: that needs 'import org.apache.log4j.*;'

Messages are created like this:
 logger.<level>("message");

for level there are the following options:
 debug
 info
 warn
 error
 fatal

Messages are printed accoding to these specified options and the level given in the file 'loggingParams.txt'
 e.g. 'log4j.rootLogger=ERROR, stdout, W, R' means that by default only error messages or higher (= error 
and fatal) are handled
 with 'log4j.logger.odml.core.Property=DEBUG' the logger messages of the logger in the class Property are
all printed from level debug and higher (i.e. all messages as there is no lower level)

In the file 'loggingParams.txt' three output options are prepared:
 stdout: to the commandline
 W: to a file; with every run generating a new file and deleting the old one
 R: to a file; appending the content and starting new log-file when 100kb are exceeded, one backup copy held)

If not all these 3 options want to be used at the same time check the first (second, the first is a comment) 
line in the file and delete the unwanted one
e.g. no command-line output desired delete the stdout in 'log4j.rootLogger=ERROR, stdout, W, R'


For using the 'loggingParams.txt' file, the following code lines must be inserted in the main function:
 String configureFile = "loggingParams.txt";
 PropertyConfigurator.configureAndWatch(configureFile);

Also possible in init of given class, but then writing to one file (W of above) contains only last call of 
class, not all calls done with run command


More information:
http://logging.apache.org/log4j/1.2/faq.html
http://logging.apache.org/log4j/1.2/apidocs/index.html
http://www.torsten-horn.de/techdocs/java-log4j.htm