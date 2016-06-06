# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table Groups (
  DTYPE                     varchar(50) not null,
  PartId                    integer not null,
  availability              integer,
  file_name                 varchar(255),
  DisjointGroup_number      integer,
  constraint pk_Groups primary key (PartId))
;

create table DiscourseConnectivesResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  constraint pk_DiscourseConnectivesResults primary key (id))
;

create table ElicitingParaphrasesResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  answer                    TEXT,
  constraint pk_ElicitingParaphrasesResults primary key (id))
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

create table LinkingV1Results (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  lhs_script                integer,
  rhs_script                integer,
  lhs_item                  integer,
  rhs_item                  integer,
  before                    integer,
  after                     integer,
  noLinkingPossible         boolean,
  constraint pk_LinkingV1Results primary key (id))
;

create table LinkingV2Results (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  workingTimes              integer,
  lhs_script                integer,
  rhs_script                integer,
  lhs_slot                  integer,
  rhs_slot                  integer,
  result                    integer,
  constraint pk_LinkingV2Results primary key (id))
;

create table NewDiscourseConnectivesResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  connective1               varchar(255),
  connective2               varchar(255),
  manualAnswer1             varchar(255),
  manualAnswer2             varchar(255),
  constraint pk_NewDiscourseConnectivesResult primary key (id))
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

create table PictureNamingExperiment_PictureNaming (
  id                        integer not null,
  picture_naming_chunk_QuestionID integer not null,
  fileName                  TEXT,
  additionalExplanations    TEXT,
  constraint pk_PictureNamingExperiment_Pictu primary key (id))
;

create table PictureNamingResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  chunk_id                  integer,
  picture_id                integer,
  answer                    TEXT,
  constraint pk_PictureNamingResults primary key (id))
;

create table PlausibilityResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  answer                    integer,
  constraint pk_PlausibilityResults primary key (id))
;

create table Questions (
  DTYPE                     varchar(100) not null,
  QuestionID                integer not null,
  LingoExpModelId           integer,
  Availability              integer default 1,
  Plausibility_number       TEXT,
  Plausibility_condition    TEXT,
  Plausibility_text         TEXT,
  Rephrasing_sentence1      TEXT,
  Rephrasing_question1      TEXT,
  Rephrasing_questionFirst1 boolean,
  Rephrasing_sentence2      TEXT,
  Rephrasing_question2      TEXT,
  Rephrasing_questionFirst2 boolean,
  filler_sentence1          varchar(255),
  filler_question1          varchar(255),
  filler_sentence2          varchar(255),
  filler_question2          varchar(255),
  restatement               TEXT,
  answer                    boolean,
  sentence                  varchar(255),
  question                  varchar(255),
  question_first            boolean,
  lhs                       integer,
  rhs                       integer,
  StoryCompletion_itemId    TEXT,
  StoryCompletion_storyType TEXT,
  StoryCompletion_story     TEXT,
  DiscourseConnectives_sentence1 TEXT,
  DiscourseConnectives_sentence2 TEXT,
  DiscourseConnectives_innerID TEXT,
  DiscourseConnectives_sentenceType TEXT,
  SentenceCompletion_story  TEXT,
  SentenceCompletion_list   TEXT,
  SentenceCompletion_itemNr TEXT,
  SentenceCompletion_itemLength TEXT,
  SentenceCompletion_itemType TEXT,
  LinkingV1_scriptId        TEXT,
  LinkingV1_side            TEXT,
  PictureNaming_text        TEXT,
  NewDiscourseConnectives_sentence1 TEXT,
  NewDiscourseConnectives_sentence2 TEXT,
  NewDiscourseConnectives_context1 TEXT,
  NewDiscourseConnectives_context2 TEXT,
  NewDiscourseConnectives_condition TEXT,
  NewDiscourseConnectives_WSJID TEXT,
  NewDiscourseConnectives_relation TEXT,
  NewDiscourseConnectives_pdtbConn TEXT,
  NewDiscourseConnectives_pdtbSense TEXT,
  NewDiscourseConnectives_rstSense TEXT,
  NewDiscourseConnectives_nr TEXT,
  ElicitingParaphrases_text TEXT,
  ElicitingParaphrases_fileName TEXT,
  constraint pk_Questions primary key (QuestionID))
;

create table RephrasingResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  choice1                   TEXT,
  choice2                   TEXT,
  answer1                   TEXT,
  answer2                   TEXT,
  readingTime1              integer,
  readingTime2              integer,
  constraint pk_RephrasingResults primary key (id))
;

create table SentenceCompletionResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  answer                    TEXT,
  constraint pk_SentenceCompletionResults primary key (id))
;

create table StoryCompletionResults (
  id                        integer not null,
  assignmentId              varchar(255),
  hitId                     varchar(255),
  workerId                  varchar(255),
  origin                    varchar(255),
  timestamp                 timestamp,
  partId                    integer,
  questionId                integer,
  result                    TEXT,
  constraint pk_StoryCompletionResults primary key (id))
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

create sequence DiscourseConnectivesResults_seq;

create sequence ElicitingParaphrasesResults_seq;

create sequence LinkingItem_seq;

create sequence LingoExpModels_seq;

create sequence LinkingV1Results_seq;

create sequence LinkingV2Results_seq;

create sequence NewDiscourseConnectivesResults_seq;

create sequence ptcp_seq;

create sequence pair_seq;

create sequence PictureNamingExperiment_PictureNaming_seq;

create sequence PictureNamingResults_seq;

create sequence PlausibilityResults_seq;

create sequence Questions_seq;

create sequence RephrasingResults_seq;

create sequence SentenceCompletionResults_seq;

create sequence StoryCompletionResults_seq;

create sequence Words_seq;

create sequence Workers_seq;

alter table LinkingItem add constraint fk_LinkingItem_script_1 foreign key (script_QuestionID) references Questions (QuestionID);
create index ix_LinkingItem_script_1 on LinkingItem (script_QuestionID);
alter table ptcp add constraint fk_ptcp_LinkingItem_2 foreign key (item_id) references LinkingItem (id);
create index ix_ptcp_LinkingItem_2 on ptcp (item_id);
alter table pair add constraint fk_pair_LinkingItem_3 foreign key (item_id) references LinkingItem (id);
create index ix_pair_LinkingItem_3 on pair (item_id);
alter table PictureNamingExperiment_PictureNaming add constraint fk_PictureNamingExperiment_Pic_4 foreign key (picture_naming_chunk_QuestionID) references Questions (QuestionID);
create index ix_PictureNamingExperiment_Pic_4 on PictureNamingExperiment_PictureNaming (picture_naming_chunk_QuestionID);



# --- !Downs

drop table if exists Groups cascade;

drop table if exists DiscourseConnectivesResults cascade;

drop table if exists ElicitingParaphrasesResults cascade;

drop table if exists LinkingItem cascade;

drop table if exists LingoExpModels cascade;

drop table if exists LinkingV1Results cascade;

drop table if exists LinkingV2Results cascade;

drop table if exists NewDiscourseConnectivesResults cascade;

drop table if exists ptcp cascade;

drop table if exists pair cascade;

drop table if exists PictureNamingExperiment_PictureNaming cascade;

drop table if exists PictureNamingResults cascade;

drop table if exists PlausibilityResults cascade;

drop table if exists Questions cascade;

drop table if exists RephrasingResults cascade;

drop table if exists SentenceCompletionResults cascade;

drop table if exists StoryCompletionResults cascade;

drop table if exists Words cascade;

drop table if exists Workers cascade;

drop sequence if exists Groups_seq;

drop sequence if exists DiscourseConnectivesResults_seq;

drop sequence if exists ElicitingParaphrasesResults_seq;

drop sequence if exists LinkingItem_seq;

drop sequence if exists LingoExpModels_seq;

drop sequence if exists LinkingV1Results_seq;

drop sequence if exists LinkingV2Results_seq;

drop sequence if exists NewDiscourseConnectivesResults_seq;

drop sequence if exists ptcp_seq;

drop sequence if exists pair_seq;

drop sequence if exists PictureNamingExperiment_PictureNaming_seq;

drop sequence if exists PictureNamingResults_seq;

drop sequence if exists PlausibilityResults_seq;

drop sequence if exists Questions_seq;

drop sequence if exists RephrasingResults_seq;

drop sequence if exists SentenceCompletionResults_seq;

drop sequence if exists StoryCompletionResults_seq;

drop sequence if exists Words_seq;

drop sequence if exists Workers_seq;

