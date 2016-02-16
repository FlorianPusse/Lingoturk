# Additional tables,functions

# --- !Ups

DROP SEQUENCE IF EXISTS LingoExpModelPublished_Seq CASCADE;
DROP SEQUENCE IF EXISTS LinkingResult_Seq CASCADE;
DROP SEQUENCE IF EXISTS LinkingResultV2_Seq CASCADE;
DROP SEQUENCE IF EXISTS ErrorMessages_Seq CASCADE;
DROP SEQUENCE IF EXISTS StoryCompletionResults_Seq CASCADE;
DROP SEQUENCE IF EXISTS StoryCompletionResultV2_Seq CASCADE;
DROP SEQUENCE IF EXISTS PictureNaming_Seq CASCADE;
DROP SEQUENCE IF EXISTS PictureNamingMailAddress_Seq CASCADE;
DROP SEQUENCE IF EXISTS PlausibilityResult_Seq CASCADE;

CREATE SEQUENCE LingoExpModelPublished_Seq START 1;
CREATE SEQUENCE ErrorMessages_Seq START 1;
CREATE SEQUENCE PictureNamingMailAddress_Seq START 1;
CREATE SEQUENCE PictureNaming_Seq START 1;
CREATE SEQUENCE LinkingResult_Seq START 1;
CREATE SEQUENCE LinkingResultV2_Seq START 1;
CREATE SEQUENCE StoryCompletionResults_Seq START 1;
CREATE SEQUENCE StoryCompletionResultV2_Seq START 1;
CREATE SEQUENCE PlausibilityResult_Seq START 1;

CREATE TABLE pendingAssignments(
	assignmentID varchar PRIMARY KEY
);

CREATE TABLE Workers_participateIn_Parts(
	WorkerID varchar,
	PartID int,
	assignmentID varchar,
	hitID varchar,
	QuestionID int,
	QuestionID2 int,
	verified bool DEFAULT false,
	timestamp timestamp DEFAULT now(),
	PRIMARY KEY (WorkerID,assignmentID),
	FOREIGN KEY (WorkerID) REFERENCES Workers ON DELETE CASCADE
);

CREATE TABLE Workers_areBlockedFor_LingoExpModels(
	WorkerID varchar,
	LingoExpModelID int,
	PRIMARY KEY (WorkerID,LingoExpModelID),
	FOREIGN KEY (LingoExpModelID) REFERENCES LingoExpModels ON DELETE CASCADE,
	FOREIGN KEY (WorkerID) REFERENCES Workers ON DELETE CASCADE

);

CREATE TABLE LingoExpModels_contain_Parts(
	LingoExpModelID int,
	PartID int,
	PRIMARY KEY (LingoExpModelID,PartID),
	FOREIGN KEY (LingoExpModelID) REFERENCES LingoExpModels ON DELETE CASCADE,
	FOREIGN KEY (PartID) REFERENCES Groups ON DELETE CASCADE
);

CREATE TABLE Parts_contain_Questions(
	PartID int,
	QuestionID int,
	PRIMARY KEY (QuestionID,PartID),
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE,
	FOREIGN KEY (PartID) REFERENCES Groups ON DELETE CASCADE
);

CREATE TABLE LingoExpModels_contain_CheaterDetectionQuestions(
	LingoExpModelID int,
	QuestionID int,
	PRIMARY KEY (LingoExpModelID,QuestionID),
	FOREIGN KEY (LingoExpModelID) REFERENCES LingoExpModels ON DELETE CASCADE,
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE
);

CREATE TABLE LingoExpModels_contain_ExampleQuestions(
	LingoExpModelID int,
	QuestionID int,
	PRIMARY KEY (LingoExpModelID,QuestionID),
	FOREIGN KEY (LingoExpModelID) REFERENCES LingoExpModels ON DELETE CASCADE,
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE
);

CREATE TABLE examplequestions_haverightanswer_words(
	QuestionID int,
	WordID int,
	PRIMARY KEY (QuestionID,WordID),
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE,
	FOREIGN KEY (WordID) REFERENCES Words ON DELETE CASCADE
);

CREATE TABLE ExampleQuestions_havePossible_Words(
	QuestionID int,
	WordID int,
	PRIMARY KEY (QuestionID,WordID),
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE,
	FOREIGN KEY (WordID) REFERENCES Words ON DELETE CASCADE
);

CREATE TABLE CheaterDetectionQuestions_mustNotHave_Words(
	QuestionID int,
	WordID int,
	PRIMARY KEY (QuestionID,WordID),
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE,
	FOREIGN KEY (WordID) REFERENCES Words ON DELETE CASCADE
);

CREATE TABLE CheaterDetectionQuestions_mustHave_Words(
	QuestionID int,
	WordID int,
	PRIMARY KEY (QuestionID,WordID),
	FOREIGN KEY (QuestionID) REFERENCES Questions ON DELETE CASCADE,
	FOREIGN KEY (WordID) REFERENCES Words ON DELETE CASCADE
);

CREATE TABLE LingoExpModelPublishedAs (
	publishID int DEFAULT nextval('LingoExpModelPublished_Seq') PRIMARY KEY,
	LingoExpModelID int REFERENCES LingoExpModels ON DELETE CASCADE,
	timestamp timestamp DEFAULT current_timestamp,
	lifetime bigint,
	url varchar,
	destination varchar
);

CREATE TABLE PictureNamingMailAddress(
	id int DEFAULT nextval('PictureNamingMailAddress_Seq') PRIMARY KEY,
	WorkerId VARCHAR(60) NOT NULL,
	timestamp timestamp DEFAULT now(),
	mailAddress VARCHAR(100)
);

DROP TABLE IF EXISTS PictureNamingResult;
CREATE TABLE IF NOT EXISTS PictureNamingResult(
	id int DEFAULT nextval('PictureNaming_Seq') PRIMARY KEY,
	WorkerId VARCHAR(60) NOT NULL,
	timestamp timestamp DEFAULT now(),
	partId int REFERENCES Groups,
	chunkId int REFERENCES Questions,
  pictureId int REFERENCES PictureNaming,
  answer VARCHAR
);

DROP TABLE IF EXISTS PlausibilityResult;
CREATE TABLE IF NOT EXISTS PlausibilityResult(
	id int DEFAULT nextval('PlausibilityResult_Seq') PRIMARY KEY,
	workerId VARCHAR(60) NOT NULL,
	timestamp timestamp DEFAULT now(),
	partId int REFERENCES Groups,
	questionId int REFERENCES Questions,
	answer int
);

CREATE TABLE IF NOT EXISTS StoryCompletionResultV2(
	id int DEFAULT nextval('StoryCompletionResultV2_Seq') PRIMARY KEY,
	workerId VARCHAR(60) NOT NULL,
	timestamp timestamp DEFAULT now(),
	partId int REFERENCES Groups,
	questionId int REFERENCES Questions,
	answer varchar(500)
);

CREATE TABLE LinkingResult(
	id int DEFAULT nextval('LinkingResult_Seq') PRIMARY KEY,
	WorkerId VARCHAR(40) NOT NULL,
	AssignmentId VARCHAR(40) NOT NULL,
	HitId VARCHAR(40) NOT NULL,
	timestamp timestamp DEFAULT now(),
	workingTimes int,
	lhs_script int REFERENCES Questions,
	rhs_script int REFERENCES Questions,
	lhs_item int REFERENCES LinkingItem,
	rhs_item int REFERENCES LinkingItem,
	before int REFERENCES LinkingItem,
	after int REFERENCES LinkingItem,
	noLinkingPossible boolean DEFAULT false
);

CREATE TABLE LinkingResultV2(
	id int DEFAULT nextval('LinkingResultV2_Seq') PRIMARY KEY,
	WorkerId VARCHAR(40) NOT NULL,
	AssignmentId VARCHAR(40) NOT NULL,
	HitId VARCHAR(40) NOT NULL,
	timestamp timestamp DEFAULT now(),
	workingTimes int,
	lhs_script int REFERENCES Questions,
	rhs_script int REFERENCES Questions,
	lhs_slot int,
	rhs_slot int,
	result VARCHAR(50)
);

CREATE TABLE StoryCompletionResults(
	id int DEFAULT nextval('StoryCompletionResults_Seq') PRIMARY KEY,
	WorkerId VARCHAR(50) NOT NULL,
	timestamp timestamp DEFAULT now(),
	itemId VARCHAR(100) NOT NULL,
	result VARCHAR(400) NOT NULL
);

CREATE TABLE QuestionPublishedAs(
	publishedId int REFERENCES LingoExpModelPublishedAs ON DELETE CASCADE,
	QuestionID int REFERENCES Questions ON DELETE CASCADE,
	mTurkID varchar,
	PRIMARY KEY(mTurkID)
);

CREATE TABLE participatesInCD_Question(
	QuestionID int REFERENCES Questions ON DELETE CASCADE,
	WorkerID varchar,
	assignmentID varchar,
	answerCorrect boolean,
	PRIMARY KEY(assignmentID,WorkerID),
	UNIQUE(QuestionID,WorkerID)
);

CREATE TABLE failedAssignments(
	assignmentID varchar PRIMARY KEY,
	timestamp timestamp DEFAULT current_timestamp
);


CREATE TABLE PartPublishedAs(
	publishedId int REFERENCES LingoExpModelPublishedAs ON DELETE CASCADE,
	timestamp timestamp DEFAULT now(),
	PartID int REFERENCES Groups ON DELETE CASCADE,
	mTurkID varchar,
	question1 int,
	question2 int,
	PRIMARY KEY(mTurkID)
);

CREATE TABLE ErrorMessages(
	id int DEFAULT nextval('ErrorMessages_Seq') PRIMARY KEY,
	timestamp timestamp DEFAULT now(),
	message VARCHAR NOT NULL
);

CREATE FUNCTION publishLingoExpModel(int,bigint,varchar,varchar) RETURNS int AS $$
	BEGIN
		INSERT INTO LingoExpModelPublishedAs(LingoExpModelID,lifetime,url,destination) VALUES ($1,$2,$3,$4);;
		RETURN currval('LingoExpModelPublished_Seq');;
	END;;
$$ LANGUAGE PLPGSQL;

CREATE FUNCTION getScriptId(lhs_script integer) RETURNS integer AS $$
	DECLARE
		nr int;;
  BEGIN
		SELECT script_id INTO nr FROM Questions WHERE QuestionId = lhs_script;;
    RETURN nr;;
  END;;
$$ LANGUAGE plpgsql;

CREATE FUNCTION getItemSlot(itemId integer) RETURNS integer AS $$
	DECLARE
		nr int;;
  BEGIN
		SELECT slot INTO nr FROM LinkingItem WHERE id = itemId;;
    RETURN nr;;
  END;;
$$ LANGUAGE plpgsql;

CREATE FUNCTION getScenarioName(id integer) RETURNS varchar(125) AS $$
	DECLARE
		n VARCHAR(125);;
  BEGIN
		SELECT file_Name INTO n FROM parts_contain_questions JOIN Groups USING (PartId) WHERE QuestionId = id;;
    RETURN n;;
  END;;
$$ LANGUAGE plpgsql;

CREATE FUNCTION getStandart(lhs_item integer,rhs_script_id int) RETURNS int AS $$
	DECLARE
		s int;;
  BEGIN
		SELECT slot INTO s FROM Pair WHERE item_id = lhs_item AND sentence_id = rhs_script_id;;
    RETURN s;;
  END;;
$$ LANGUAGE plpgsql;

CREATE FUNCTION wrongAnswersCount(wId varchar) RETURNS integer AS $$
	DECLARE
		nr int;;
	BEGIN
		SELECT count(*) INTO nr FROM (
		SELECT scenarioName,scenarioName,lhs_script_id,rhs_script_id,lhs_item_slot,
		CASE WHEN noLinkingPossible THEN '-1' WHEN rhs_item_slot IS NOT NULL THEN rhs_item_slot || '' WHEN before_slot IS NULL THEN 'after_' || after_slot WHEN after_slot IS NULL THEN 'before_' || before_slot ELSE 'between_' || after_slot || '_' || before_slot END
		AS result, goldenStandart || '' AS goldenStandart FROM (
		SELECT DISTINCT * FROM (SELECT getScenarioName(lhs_script) AS scenarioName,getScriptId(lhs_script) AS lhs_script_id,
			getScriptId(rhs_script) AS rhs_script_id,getItemSlot(lhs_item) AS lhs_item_slot,getItemSlot(rhs_item) AS rhs_item_slot,
			getItemSlot(before) AS before_slot,getItemSlot(after) AS after_slot,noLinkingPossible,getStandart(lhs_item,getScriptId(rhs_script)) AS goldenStandart,workerId
		FROM LinkingResult) AS tmp
		WHERE goldenStandart IS NOT NULL AND workerId = wId
		ORDER BY scenarioName,lhs_script_id,rhs_script_id,lhs_item_slot,rhs_item_slot) as tmp2) as tmp3
		WHERE result != goldenstandart;;

		RETURN nr;;
	END;;
$$ LANGUAGE plpgsql;

# --- !Downs

DROP SEQUENCE IF EXISTS LingoExpModelPublished_Seq CASCADE;
DROP SEQUENCE IF EXISTS LinkingResult_Seq CASCADE;
DROP SEQUENCE IF EXISTS LinkingResultV2_Seq CASCADE;
DROP SEQUENCE IF EXISTS ErrorMessages_Seq CASCADE;
DROP SEQUENCE IF EXISTS StoryCompletionResults_Seq CASCADE;
DROP SEQUENCE IF EXISTS PictureNaming_Seq CASCADE;
DROP SEQUENCE IF EXISTS PictureNamingMailAddress_Seq CASCADE;
DROP SEQUENCE IF EXISTS PlausibilityResult_Seq CASCADE;
DROP TABLE IF EXISTS ErrorMessages;
DROP TABLE IF EXISTS Workers_participateIn_Parts CASCADE;
DROP TABLE IF EXISTS Workers_areBlockedFor_LingoExpModels CASCADE;
DROP TABLE IF EXISTS LingoExpModels_contain_Parts CASCADE;
DROP TABLE IF EXISTS Parts_contain_Questions CASCADE;
DROP TABLE IF EXISTS LingoExpModels_contain_CheaterDetectionQuestions CASCADE;
DROP TABLE IF EXISTS LingoExpModels_contain_ExampleQuestions CASCADE;
DROP TABLE IF EXISTS ExampleQuestions_haverightAnswer_Words CASCADE;
DROP TABLE IF EXISTS ExampleQuestions_havePossible_Words CASCADE;
DROP TABLE IF EXISTS CheaterDetectionQuestions_mustNotHave_Words CASCADE;
DROP TABLE IF EXISTS CheaterDetectionQuestions_mustHave_Words CASCADE;
DROP TABLE IF EXISTS LingoExpModelPublishedAs CASCADE;
DROP TABLE IF EXISTS QuestionPublishedAs CASCADE;
DROP TABLE IF EXISTS LinkingResult CASCADE;
DROP TABLE IF EXISTS LinkingResultV2 CASCADE;
DROP TABLE IF EXISTS participatesInCD_Question CASCADE;
DROP TABLE IF EXISTS StoryCompletionResults CASCADE;
DROP TABLE IF EXISTS pendingAssignments;
DROP TABLE IF EXISTS failedAssignments;
DROP TABLE IF EXISTS PartPublishedAs;
DROP TABLE IF EXISTS examplequestions_haverightanswer_words;
DROP FUNCTION IF EXISTS publishLingoExpModel(int,bigint,varchar,varchar);
DROP FUNCTION IF EXISTS pwrongAnswersCount(varchar);
DROP FUNCTION IF EXISTS pgetStandart(int,int);
DROP FUNCTION IF EXISTS pgetScenarioName(int);
DROP FUNCTION IF EXISTS pgetItemSlot(int);
DROP FUNCTION IF EXISTS pgetScriptId(int);