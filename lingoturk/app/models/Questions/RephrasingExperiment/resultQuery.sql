SELECT * FROM
(
	SELECT
		Rephrasing_sentence1 AS sentence,
		Rephrasing_question1 AS question,
		Rephrasing_questionFirst1 AS questionFirst,
		workerId,
		readingTime1 AS readingTime,
		choice1 AS choice,
		regexp_replace(answer1, E'[\\n\\r\\u2028]+', ' ', 'g' ) AS answer
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
		regexp_replace(answer2, E'[\\n\\r\\u2028]+', ' ', 'g' ) AS answer
	FROM
		RephrasingResults
	JOIN
		Questions
USING (QuestionId)
)
