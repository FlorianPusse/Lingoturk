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
  
  - easy to use
  - based on the [Play Framework](https://www.playframework.com/)
  - back-end is written in Java 
  - completly typesave (typesave HTTP router and Scala based templates)
  - (*almost*) no need to use the Mechanical Turk API.
  - can easily be modified and extended
  
## Help
You can find more information and how to get started in the Lingoturk [Wiki](https://github.com/FlorianPusse/Lingoturk/wiki).

## Update on 03/11/2016

e4eed19f5d35ad292b85703060d01af3548bf584

After pulling the newest updates, you should run `activator clean` in order to delete unnecessary files.

If you have created new experiment types, you will be faced a compile error after pulling the latest changes. To resolve them, go to your `app/models/Questions/<ExperimentName>/<ExperimentName>Question.java` file and replace the following occurences:
- `import models.Repository;` -> `import controllers.DatabaseController;`
- `Repository.getConnection()` -> `DatabaseController.getConnection()`
We'll probably implement an update tool for these things in the future.
