# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table Groups (
  DTYPE                     varchar(30) not null,
  PartId                    integer not null,
  availability              integer,
  file_name                 varchar(255),
  number                    integer,
  constraint pk_Groups primary key (PartId))
;

create table LinkingItem (
  id                        integer not null,
  h                         varchar(255),
  head                      varchar(255),
  original                  varchar(255),
  slot                      integer,
  text                      varchar(255),
  script_QuestionID         integer,
  constraint pk_LinkingItem primary key (id))
;

create table LingoExpModels (
  LingoExpModelID           integer not null,
  name                      varchar(255),
  description               varchar(255),
  nameOnAmt                 varchar(255),
  additionalExplanations    TEXT,
  experimentType            varchar(255),
  constraint pk_LingoExpModels primary key (LingoExpModelID))
;

create table ptcp (
  id                        integer not null,
  item_id                   integer not null,
  head                      varchar(255),
  text                      varchar(255),
  constraint pk_ptcp primary key (id))
;

create table pair (
  id                        integer not null,
  item_id                   integer not null,
  sentence_id               integer,
  slot                      integer,
  none                      boolean,
  constraint pk_pair primary key (id))
;

create table PictureNaming (
  id                        integer not null,
  picture_naming_chunk_QuestionID integer not null,
  file_name                 varchar(255),
  additionalExplanations    TEXT,
  constraint pk_PictureNaming primary key (id))
;

create table Questions (
  DTYPE                     varchar(30) not null,
  QuestionID                integer not null,
  LingoExpModelId           integer,
  Availability              integer default 1,
  inner_id                  varchar(255),
  number                    varchar(255),
  condition                 varchar(255),
  text                      TEXT,
  sentence1                 TEXT,
  question1                 TEXT,
  questionFirst1            boolean,
  sentence2                 TEXT,
  question2                 TEXT,
  questionFirst2            boolean,
  filler_sentence1          varchar(255),
  filler_question1          varchar(255),
  filler_sentence2          varchar(255),
  filler_question2          varchar(255),
  restatement               TEXT,
  answer                    boolean,
  sentence                  varchar(255),
  question                  varchar(255),
  question_first            boolean,
  item_id                   varchar(255),
  story_type                varchar(255),
  story                     TEXT,
  SentenceType              varchar(255),
  script_id                 varchar(255),
  side                      varchar(255),
  list                      varchar(255),
  item_nr                   varchar(255),
  item_length               varchar(255),
  item_type                 varchar(255),
  lhs                       integer,
  rhs                       integer,
  constraint pk_Questions primary key (QuestionID))
;

create table Words (
  WordID                    integer not null,
  Word                      varchar(255),
  constraint pk_Words primary key (WordID))
;

create table Workers (
  WorkerID                  varchar(255) not null,
  banned                    boolean,
  constraint pk_Workers primary key (WorkerID))
;

create sequence Groups_seq;

create sequence LinkingItem_seq;

create sequence LingoExpModels_seq;

create sequence ptcp_seq;

create sequence pair_seq;

create sequence PictureNaming_seq;

create sequence Questions_seq;

create sequence Words_seq;

create sequence Workers_seq;

alter table LinkingItem add constraint fk_LinkingItem_script_1 foreign key (script_QuestionID) references Questions (QuestionID) on delete restrict on update restrict;
create index ix_LinkingItem_script_1 on LinkingItem (script_QuestionID);
alter table ptcp add constraint fk_ptcp_LinkingItem_2 foreign key (item_id) references LinkingItem (id) on delete restrict on update restrict;
create index ix_ptcp_LinkingItem_2 on ptcp (item_id);
alter table pair add constraint fk_pair_LinkingItem_3 foreign key (item_id) references LinkingItem (id) on delete restrict on update restrict;
create index ix_pair_LinkingItem_3 on pair (item_id);
alter table PictureNaming add constraint fk_PictureNaming_Questions_4 foreign key (picture_naming_chunk_QuestionID) references Questions (QuestionID) on delete restrict on update restrict;
create index ix_PictureNaming_Questions_4 on PictureNaming (picture_naming_chunk_QuestionID);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists Groups;

drop table if exists LinkingItem;

drop table if exists LingoExpModels;

drop table if exists ptcp;

drop table if exists pair;

drop table if exists PictureNaming;

drop table if exists Questions;

drop table if exists Words;

drop table if exists Workers;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists Groups_seq;

drop sequence if exists LinkingItem_seq;

drop sequence if exists LingoExpModels_seq;

drop sequence if exists ptcp_seq;

drop sequence if exists pair_seq;

drop sequence if exists PictureNaming_seq;

drop sequence if exists Questions_seq;

drop sequence if exists Words_seq;

drop sequence if exists Workers_seq;

