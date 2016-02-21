# LingoTurk

  **LingoTurk** is an open-source, freely available crowdsourcing
  client/server system aimed primarily at psycholinguistic
  experimentation where custom and specialized user interfaces are
  required but not supported by popular crowdsourcing task management
  platforms.
  
  LingoTurk enables user-friendly local hosting of
  experiments as well as condition management and participant
  exclusion. It is compatible with Amazon Mechanical Turk and Prolific
  Academic.  New experiments can easily be set up via the Play
  Framework and the LingoTurk API, while multiple experiments can be
  managed from a single system.
  
## Getting started
  
### Requirements

  - Java Development Kit 8: [[Download JDK]](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html "JDK 8")
  
  - Play Framework 2.3.x [[Download Play]](https://downloads.typesafe.com/typesafe-activator/1.3.7/typesafe-activator-1.3.7-minimal.zip "Play Framework 2.3.x") [[Installation instructions]](https://www.playframework.com/documentation/2.3.x/Installing "Play Installation instructions")
  
  - An empty PostgreSQL database. [[Download PostgreSQL]](http://www.postgresql.org/download/ "PostgreSQL")
  
  - A local copy of the `lingoturk` directory
  
  
### Configuring LingoTurk

   1. Open the `lingoturk/conf/application.conf` file in any text editor.
   
   2. Create a new Application secret. (The Application secret will be used for cryptographic functions. Therefore, it should **never** be share with others!):
   	- Open console and enter: `play-generate-secret`.
  	- Replace `APPLICATION_SECRET` in the line `application.secret="APPLICATION_SECRET"` by the freshly created ApplicationSecret.
 
   3. Configure database connection:
   	- Replace `db.default.url="postgres://USERNAME:PASSWORD@URL/DATABASE_NAME?characterEncoding=utf8`" by the corresponding entries for `USERNAME`, `PASSWORD`, and `DATABASE_NAME`.
    
   4. (opt.) Configure SSL. An SSL certificate is necessary to run experiments on Amazon Mechanical Turk. It is possible to skip this step for testing purposes. You can find instructions how to configure SSL in Play directly here: [Play Framework SSL Configuration](https://www.playframework.com/documentation/2.3.x/ConfiguringHttps "Play Framework SSL Configuration")
   
   However, I do recommend to use a front end HTTP server. This allows you a more detailed configuration and additional possibilities such as load balancing or redirecting requests to other processes than Play. Detailed configuration examples can be found in the official Play documentation: [Setting up a front end HTTP server](https://playframework.com/documentation/2.3.x/HTTPServer "Setting up a front end HTTP server") 

### Running LingoTurk

   Running LingoTurk is fairly easy. To start the server, follow these steps:
   1. Start a terminal/command line
   2. Navigate to the local copy of the `lingoturk` directory
   3. To start start the server in developing mode enter `activator run` and to start the server in production mode, enter `activator start`. In developing mode, "the server will be launched with the auto-reload feature enabled, meaning that for each request Play will check your project and recompile required sources. If needed the application will restart automatically. If there are any compilation errors you will see the result of the compilation directly in your browser." (Source: [Using the Play console](https://www.playframework.com/documentation/2.3.x/PlayConsole "Using the Play console")).
   
   	You can specify parameters such as ports as additional arguments, e.g. `activator "run -Dhttps.port=443 -Dhttp.port=80"`. If no port is specified, Play will bind the application to port `9000`.
   4. Navigate to `PROTOCOL://SERVER_IP:PORT` in your browser. When testing the application locally, without specifying a port on startup, the correct URL would be `http://localhost:9000'.
   
### Using LingoTurk

   To be announced.

