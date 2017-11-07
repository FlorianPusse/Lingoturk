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
  
## Update on 07/11/2017
There is a big update coming out today. It includes many bug fixes, improvements, and foundations for upcoming features. However, these come at a cost: If you already have an installation running, your existing experiments will be broken and your database scheme needs to be modified. If you need help with the update, please let us know. For a more detailed explanation of what has changed, take a look at the wiki.

## Coming (soon)
  - Multi user management: LingoTurk was once designed to be managed by a single administrator. In reality, there are often many people that want to design and run new experiments. We want to give experimenters this option without being afraid that they could interfere with each other. Foundations for this feature have been laid. More is coming soon. 
  - GUI for designing experiments: Designing new experiments using a graphical user interface without having to code is one of the most appealing features to have. However, it is also one of the hardest to implement in a maintainable and sustainable way.
  
## Help
You can find more information and how to get started in the Lingoturk [Wiki](https://github.com/FlorianPusse/Lingoturk/wiki).