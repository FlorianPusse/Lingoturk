# ** LingoTurk **

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
    
  - Java Development Kit 8: [[Download]](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html "JDK 8")
  
  - Play Framework 2.3.x [[Download]](https://downloads.typesafe.com/typesafe-activator/1.3.7/typesafe-activator-1.3.7-minimal.zip "Play Framework 2.3.x") [[Installation instructions]](https://www.playframework.com/documentation/2.3.x/Installing "Play Installation instructions")
  
  - PostgreSQL [[Download]](http://www.postgresql.org/download/ "PostgreSQL")
  
  - A local copy of the `lingoturk` directory
  
  
### Configuring LingoTurk
   
   1. Open the `lingoturk/conf/application.conf` file in any text editor.
   
   2. Create a new Application secret:
   	- Open console and enter: `play-generate-secret`
  	- Replace `APPLICATION_SECRET` in the line `application.secret="APPLICATION_SECRET"` by the freshly created ApplicationSecret.
   
   3. Configure database connection:
   	- Replace `db.default.url="postgres://USERNAME:PASSWORD@URL/DATABASE_NAME?characterEncoding=utf8" by the corresponding entries.
    
### Running LingoTurk
tba.
