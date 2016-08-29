SELECT * FROM
(
	SELECT
		Rephrasing_sentence1 AS sentence,
		Rephrasing_question1 AS question,
		Rephrasing_questionFirst1 AS questionFirst,
		workerId,
		readingTime1 AS readingTime,
		choice1 AS choice,
		answer1 AS answer
	FROM
		RephrasingResults
	JOIN
		Questions
	USING (QuestionId)
) AS tmp
UNION
(
	SELECT
		Rephrasing_sentence2 AS sentence,
		Rephrasing_question2 AS question,
		Rephrasing_questionFirst2 AS questionFirst,
		workerId,
		readingTime2 AS readingTime,
		choice2 AS choice,
		answer2 AS answer
	FROM
		RephrasingResults
	JOIN
		Questions
USING (QuestionId)
)
